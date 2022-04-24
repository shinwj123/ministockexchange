import org.agrona.collections.Long2ObjectHashMap;
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
        this.id2Order = new Long2ObjectHashMap<>();
        this.price2Level = new Long2ObjectHashMap<>();
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
        for (BookLevel level : subBook.values()) {
            Iterator<Map.Entry<Long, Order>> it = level.getOrderEntries().entrySet().iterator();
            while (it.hasNext() && quantityToFill > 0) {
                Map.Entry<Long, Order> pair = it.next();
                Order candidate = pair.getValue();
                long fillable = Math.min(candidate.getTotalQuantity() - candidate.getFilledQuantity(), quantityToFill);
                quantityToFill -= fillable;
                candidate.fill(fillable);
                order.fill(fillable);
                level.reduceTotalVolume(fillable);
                if (candidate.getTotalQuantity() == candidate.getFilledQuantity()) {
                    matchedOrders.add(candidate);
                    removeOrder(candidate.getOrderId());
                }
            }
            if (quantityToFill == 0) {
                break;
            }
        }

        if (quantityToFill > 0) {
            if (order.getType() == OrderType.LIMIT) {
                addOrder(order);
            } else if (order.getType() == OrderType.MARKET) {
                // cancel the rest

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
//            if (order.getSide() == Side.BID && limitPrice > bestBid) {
//                bestBid = limitPrice;
//            } else if (order.getSide() == Side.ASK && limitPrice < bestAsk) {
//                bestAsk = limitPrice;
//            }
        }

        id2Order.put(orderId, order);
        return true;
    }

    public boolean removeOrder(long orderId) {
        // TODO send update event
        if (!id2Order.containsKey(orderId)) {
            return false;
        }
        Order orderToRemove = id2Order.get(orderId);
        Long limitPrice = orderToRemove.getPrice();
        id2Order.remove(orderId);
        TreeMap<Long, BookLevel> book = getBook(orderToRemove.getSide());
        BookLevel bookLevel = price2Level.get(limitPrice);
        bookLevel.remove(orderId);
        if (bookLevel.getNumOrders() == 0) {
            book.remove(limitPrice);
            price2Level.remove(limitPrice);
//            if (orderToRemove.getSide() == Side.BID && limitPrice == bestBid) {
//                bestBid = book.firstKey();
//            } else if (orderToRemove.getSide() == Side.ASK && limitPrice == bestAsk) {
//                bestAsk = book.firstKey();
//            }
        }
        return true;
    }

    private TreeMap<Long, BookLevel> getBook(Side side) {
        return side == Side.ASK ? asks : bids;
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
        Order o1 = new Order(1, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 100100, 10);
        Order o2 = new Order(2, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 110200, 5);
        Order o7 = new Order(7, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 120300, 5);
        Order o3 = new Order(3, orderIdGenerator.incrementAndGet(), Side.BID, OrderType.LIMIT, 120300, 20);

        Order o4 = new Order(4, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 130100, 20);
        Order o5 = new Order(5, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 140200, 30);
        Order o6 = new Order(6, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 150300, 50);
        orderBook.addOrder(o1);
        orderBook.addOrder(o2);
        orderBook.addOrder(o3);
        orderBook.addOrder(o4);
        orderBook.addOrder(o5);
        orderBook.addOrder(o6);
        orderBook.addOrder(o7);

        orderBook.printOrderBook();
        ArrayList<Order> matched = orderBook.match(new Order(8, orderIdGenerator.incrementAndGet(), Side.ASK, OrderType.LIMIT, 110100, 12));
        System.out.println(matched);
        matched.forEach(System.out::println);
//    orderBook.removeOrder(o3.getOrderId());
        orderBook.printOrderBook();
//
//    orderBook.removeOrder(o7.getOrderId());
//    orderBook.printOrderBook();
    }
}
