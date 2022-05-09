import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

public class BookLevel implements Comparable<BookLevel> {
    private final long priceLevel;
    private long totalVolume;
    private final LinkedHashMap<Long, Order> entries;

    public BookLevel(long priceLevel) {
        this.priceLevel = priceLevel;
        this.totalVolume = 0;
        this.entries = new LinkedHashMap<>();
    }

    public void put(Order order) {
        entries.put(order.getOrderId(), order);
        totalVolume += order.getTotalQuantity() - order.getFilledQuantity();
    }


    public Order remove(long orderId) {
        Order order = entries.get(orderId);
        if (order == null) {
            return null;
        }

        entries.remove(orderId);

        totalVolume -= order.getTotalQuantity() - order.getFilledQuantity();
        return order;
    }

    public LinkedHashMap<Long, Order> getOrderEntries() {
        return entries;
    }

    public boolean containsOrder(long orderId) {
        return entries.containsKey(orderId);
    }

    public long getPriceLevel() {
        return priceLevel;
    }

    public long getTotalVolume() {
        return totalVolume;
    }

    public int getNumOrders() {
        return entries.size();
    }

    public void setTotalVolume(long totalVolume) {
        this.totalVolume = totalVolume;
    }

    public void reduceTotalVolume(long quantity) {
        if (totalVolume >= quantity) {
            totalVolume -= quantity;
        }
    }

    public boolean checkLevelTotalVolume() {
        long sum = entries.values().stream().mapToLong(o -> o.getTotalQuantity() - o.getFilledQuantity()).sum();
        return sum == totalVolume;
    }

    public String printPriceLevel() {
        return NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(priceLevel).scaleByPowerOfTen(-4));
    }


    @Override
    public int compareTo(BookLevel o) {
        return Long.compare(this.getPriceLevel(), o.getPriceLevel());
    }
}
