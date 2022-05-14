import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.SystemEpochNanoClock;
import org.agrona.concurrent.UnsafeBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBook {
    private final String symbol;
    private final TreeMap<Long, BookLevel> bids;
    private final TreeMap<Long, BookLevel> asks;

    private final Long2ObjectHashMap<BookLevel> price2Level; // limit price to price level
    private final Long2ObjectHashMap<Order> id2Order;
    private long bestBid;
    private long bestAsk;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new TreeMap<>(Collections.reverseOrder());
        this.asks = new TreeMap<>();
        this.bestAsk = Long.MAX_VALUE;
        this.bestBid = 0;
        this.id2Order = new Long2ObjectHashMap<>();
        this.price2Level = new Long2ObjectHashMap<>();
    }

    public String getSymbol() {
        return symbol;
    }

    public long getBestBid() {
        return bestBid;
    }

    public long getBestAsk() {
        return bestAsk;
    }

    public long getSpread() {
        return bestBid - bestAsk;
    }

    public ArrayList<Order> match(Order order) {
        // given a valid order, try fill and return all matched orders containing matched price and quantity
        ArrayList<Order> matchedOrders = new ArrayList<>();
        TreeMap<Long, BookLevel> book = order.getSide() == Side.BID ? getBook(Side.ASK) : getBook(Side.BID);
        long quantityToFill = order.getTotalQuantity() - order.getFilledQuantity();
        Long limitPrice = order.getPrice();
        NavigableMap<Long, BookLevel> subBook = book.headMap(limitPrice, true);
        ArrayList<Order> toRemove = new ArrayList<>();
        for (BookLevel level : subBook.values()) {
            Iterator<Map.Entry<Long, Order>> it = level.getOrderEntries().entrySet().iterator();
            while (it.hasNext() && quantityToFill > 0) {
                Map.Entry<Long, Order> pair = it.next();
                Order candidate = pair.getValue();
                long fillable = Math.min(candidate.getTotalQuantity() - candidate.getFilledQuantity(), quantityToFill);
                quantityToFill -= fillable;
                candidate.fill(fillable);
                order.fill(fillable, candidate.getPrice());
                level.reduceTotalVolume(fillable);
                matchedOrders.add(candidate);
                if (candidate.getTotalQuantity() == candidate.getFilledQuantity()) {
                    toRemove.add(candidate);
                }
            }
            if (quantityToFill == 0) {
                break;
            }
        }
        toRemove.forEach(o -> removeOrder(o.getOrderId()));
        if (quantityToFill > 0) {
            if (order.getType() == OrderType.LIMIT) {
                addOrder(order);
            }
        }
        return matchedOrders;
    }

    public boolean addOrder(Order order) {
        // TODO send update event
        return addOrder(order, getBook(order.getSide()));
    }

    private boolean addOrder(Order order, TreeMap<Long, BookLevel> book) {
        if (order == null || id2Order.containsKey(order.getOrderId())) {
            return false;
        }
        Long limitPrice = order.getPrice();
        Long orderId = order.getOrderId();

        if (price2Level.containsKey(limitPrice)) {
            // if price level exists
            price2Level.get(limitPrice).put(order);
        } else {
            BookLevel bookLevel = new BookLevel(limitPrice);
            bookLevel.put(order);
            book.put(limitPrice, bookLevel);
            price2Level.put(limitPrice, bookLevel);
            if (order.getSide() == Side.BID && limitPrice > bestBid) {
                bestBid = limitPrice;
            } else if (order.getSide() == Side.ASK && limitPrice < bestAsk) {
                bestAsk = limitPrice;
            }
        }

        id2Order.put(orderId, order);
        return true;
    }

    public boolean containsOrder(long orderId) {
        return id2Order.containsKey(orderId);
    }

    public Order removeOrder(long orderId) {
        // TODO send update event
        if (!id2Order.containsKey(orderId)) {
            return null;
        }
        Order orderToRemove = id2Order.get(orderId);
        Long limitPrice = orderToRemove.getPrice();
        id2Order.remove(orderId);
        TreeMap<Long, BookLevel> book = getBook(orderToRemove.getSide());
        BookLevel bookLevel = price2Level.get(limitPrice);
        Order order = bookLevel.remove(orderId);
        if (bookLevel.getNumOrders() == 0) {
            book.remove(limitPrice);
            price2Level.remove(limitPrice);
            if (orderToRemove.getSide() == Side.BID && limitPrice == bestBid) {
                bestBid = book.isEmpty() ? 0 : book.firstKey();
            } else if (orderToRemove.getSide() == Side.ASK && limitPrice == bestAsk) {
                bestAsk = book.isEmpty() ? 0 : book.firstKey();
            }
        }
        return order;
    }

    public TreeMap<Long, BookLevel> getBook(Side side) {
        return side == Side.ASK ? asks : bids;
    }

    public BookLevel getPriceLevel(long priceLevel, Side side) {
        return side == Side.BID ? bids.get(priceLevel) : asks.get(priceLevel);
    }

    public boolean isStateValid() {
        boolean isLevelValid = asks.values().stream().allMatch(BookLevel::checkLevelTotalVolume)
                && bids.values().stream().allMatch(BookLevel::checkLevelTotalVolume)
                && (asks.isEmpty() || asks.firstKey() == bestAsk)
                && (bids.isEmpty() ||  bids.firstKey() == bestBid);

        boolean isMappingSizeValid = asks.size() + bids.size() == price2Level.size()
                && id2Order.size() == asks.values().stream().mapToInt(BookLevel::getNumOrders).sum()
                + bids.values().stream().mapToInt(BookLevel::getNumOrders).sum();

        return isLevelValid && isMappingSizeValid;
    }

    public void printOrderBook() {
        Formatter formatter = new Formatter();
        formatter.format("%12s %12s\n", " ", symbol);
        formatter.format("%12s %12s %12s\n", "BID", "Price", "ASK");
        for (BookLevel level : asks.descendingMap().values()) {
            formatter.format("%12s %12s %12d\n", " ", level.printPriceLevel(), level.getTotalVolume());
        }
        formatter.format("%s\n", "-".repeat(50));
        for (BookLevel level : bids.values()) {
            formatter.format("%12d %12s %12s\n", level.getTotalVolume(), level.printPriceLevel(), " ");
        }
        System.out.println(formatter);
    }


    public static void main(String[] args) {
        final AtomicLong orderIdGenerator = new AtomicLong();
        OrderBook orderBook = new OrderBook("NVDA");
        Order o1 = new Order("client1", 1, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 100100, 10);
        Order o2 = new Order("client1", 2, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 110200, 5);
        Order o7 = new Order("client2", 7, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 120300, 5);
        Order o3 = new Order("client3", 3, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 120300, 20);

        Order o4 = new Order("client4",4, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 130100, 20);
        Order o5 = new Order("client5", 5, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 140200, 30);
        Order o6 = new Order("client6", 6, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 150300, 50);
        orderBook.addOrder(o1);
        orderBook.addOrder(o2);
        orderBook.addOrder(o3);
        orderBook.addOrder(o4);
        orderBook.addOrder(o5);
        orderBook.addOrder(o6);
        orderBook.addOrder(o7);

        orderBook.printOrderBook();
//        ArrayList<Order> matched = orderBook.match(new Order(8, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 110100, 22));
        Order incoming = new Order("client3", 8, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 140200, 30);
        ArrayList<Order> matched = orderBook.match(incoming);
        System.out.println(matched.size());
        System.out.println(incoming.getStatus());

//        matched.forEach(System.out::println);
//    orderBook.removeOrder(o3.getOrderId());
        orderBook.printOrderBook();
//
//    orderBook.removeOrder(o7.getOrderId());
//    orderBook.printOrderBook();
        Report report = new Report();
        UnsafeBuffer buffer = report.orderId(1234)
                .clientCompId("client1")
                .clientOrderId(5678)
                .orderStatus(Status.PARTIALLY_FILLED)
                .symbol("NVDA")
                .totalQuantity(20)
                .deltaQuantity(-100)
                .side(Side.BID)
                .timestamp(new SystemEpochNanoClock().nanoTime())
                .buildReport();
        System.out.println(Report.toJson(buffer));
    }
}
