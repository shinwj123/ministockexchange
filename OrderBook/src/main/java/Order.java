import org.agrona.concurrent.SystemEpochNanoClock;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicLong;

enum OrderType {
    MARKET,
    LIMIT
}

enum Side {
    BID,
    ASK
}

enum Status {
    NEW,
    CANCELLED,
    REJECTED,
    FILLED,
    PARTIALLY_FILLED
}

public class Order {
//    private static AtomicLong orderIds = new AtomicLong();
    private final long entryTime;
    private final long orderId;
    private final long clientOrderId;
    private final Side side;
    private final OrderType type;
    private final long price;
    private final long totalQuantity;
    private long filledQuantity;

    public Order(long clientOrderId, long orderId, Side side, OrderType type, long price, long totalQuantity) {
        this.entryTime = new SystemEpochNanoClock().nanoTime();
        this.side = side;
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.type = type;
        if (type == OrderType.MARKET) {
            this.price = side == Side.BID ? Long.MAX_VALUE : 0;
        } else {
            this.price = price;
        }
        this.totalQuantity = totalQuantity;
        this.filledQuantity = 0;
    }

    public long getClientOrderId() {
        return clientOrderId;
    }

    public OrderType getType() {
        return type;
    }

    public long getPrice() {
        return price;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }

    public long getEntryTime() {
        return entryTime;
    }

    public long getOrderId() {
        return orderId;
    }

    public Side getSide() {
        return side;
    }

    public long getFilledQuantity() {
        return filledQuantity;
    }

    public void fill(long quantity) {
        // TODO change orderstatus to FILLED
        filledQuantity += quantity;
    }

    public String printPrice() {
        return NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(price).scaleByPowerOfTen(-4));
    }

    @Override
    public String toString() {
        return "Order{" +
                "entryTime=" + entryTime +
                ", orderId=" + orderId +
                ", clientOrderId=" + clientOrderId +
                ", side=" + side +
                ", type=" + type +
                ", price=" + price +
                ", totalQuantity=" + totalQuantity +
                ", filledQuantity=" + filledQuantity +
                '}';
    }
}
