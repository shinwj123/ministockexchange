import org.agrona.concurrent.SystemEpochNanoClock;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

enum OrderType {
    MARKET ((byte) 0x31),
    LIMIT ((byte) 0x32),
    CANCEL ((byte) 0x03);

    private final byte code;

    OrderType(byte code) {
        this.code = code;
    }

    public byte getByteCode() {
        return code;
    }
}

enum Side {
    BID ((byte) 0x38),
    ASK ((byte) 0x35);
    private final byte code;
    Side(byte code) {
        this.code = code;
    }

    public byte getByteCode() {
        return code;
    }
}

enum Status {
    NEW ((byte) 0x30),
    PARTIALLY_FILLED ((byte) 0x31),
    FILLED ((byte) 0x32),
    CANCELLED ((byte) 0x34),
    REJECTED ((byte) 0x38);

    private final byte code;

    private static final Map<Byte, Status> ENUM_MAP;
    Status(byte code) {
        this.code = code;
    }

    static {
        Map<Byte, Status> map = new HashMap<>();
        for (Status instance : Status.values()) {
            map.put(instance.getByteCode(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static String getNameFromByte(byte code) {
        return ENUM_MAP.get(code).toString().toLowerCase();
    }

    public byte getByteCode() {
        return code;
    }
}

public class Order {
    private final long entryTime;
    private final long orderId;
    private final String clientCompId;
    private final long clientOrderId;
    private final Side side;
    private final OrderType type;
    private Status status;
    private final long price;
    private final long totalQuantity;
    private long filledQuantity;
    private long lastExecutedQuantity;
    private long lastExecutedPrice;
    private long avgExecutedPrice;

    public Order(String clientCompId, long clientOrderId, long orderId, Side side, OrderType type, long price, long totalQuantity) {
        this.entryTime = new SystemEpochNanoClock().nanoTime();
        this.side = side;
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.clientCompId = clientCompId;
        this.type = type;
        if (type == OrderType.MARKET) {
            this.price = side == Side.BID ? Long.MAX_VALUE : 0;
        } else {
            this.price = price;
        }
        this.status = Status.NEW;
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

    public String getClientCompId() {
        return clientCompId;
    }

    public Side getSide() {
        return side;
    }

    public long getFilledQuantity() {
        return filledQuantity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getLastExecutedQuantity() {
        return lastExecutedQuantity;
    }

    public long getLastExecutedPrice() {
        return lastExecutedPrice;
    }

    public long getAvgExecutedPrice() {
        return avgExecutedPrice;
    }

    public void fill(long quantity) {
        fill(quantity, price);
    }

    public void fill(long quantity, long executionPrice) {
        avgExecutedPrice = ((quantity * executionPrice) + (avgExecutedPrice * filledQuantity)) / (quantity + filledQuantity);
        filledQuantity += quantity;
        lastExecutedPrice = executionPrice;
        lastExecutedQuantity = quantity;
        if (filledQuantity == totalQuantity) {
            status = Status.FILLED;
        } else {
            status = Status.PARTIALLY_FILLED;
        }
    }

    public static String getPriceDouble(long price) {
        return BigDecimal.valueOf(price).scaleByPowerOfTen(-4).toString();
    }

    public static String getPriceString(long price) {
        return NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(price).scaleByPowerOfTen(-4));
    }

    @Override
    public String toString() {
        return "Order{" +
                "entryTime=" + entryTime +
                ", orderId=" + orderId +
                ", clientCompId='" + clientCompId + '\'' +
                ", clientOrderId=" + clientOrderId +
                ", side=" + side +
                ", type=" + type +
                ", status=" + status +
                ", price=" + price +
                ", totalQuantity=" + totalQuantity +
                ", filledQuantity=" + filledQuantity +
                ", lastExecutedQuantity=" + lastExecutedQuantity +
                ", lastExecutedPrice=" + lastExecutedPrice +
                ", avgExecutedPrice=" + avgExecutedPrice +
                '}';
    }
}
