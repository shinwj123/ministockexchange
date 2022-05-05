import java.util.Objects;

public class MessageFromME {

    private final long orderId; //0 means the order in the message is still processed, 1 means order processing completed
    private final int orderStatus;//assume 0 means order is newed
    private final long timeStamp;
    private final String symbol;
    private final int size;
    private final long price;
    private final int side; //if 0, sell, if 1, buy

    public MessageFromME(
            final long orderIdInput,
            final int orderStatusInput,
            final long timestampInput,
            final String symbolInput,
            final int sizeInput,
            final long priceInput,
            final int sideInput) {
        this.orderId = orderIdInput;
        this.orderStatus = orderStatusInput;
        this.timeStamp = timestampInput;
        this.symbol = symbolInput;
        this.size = sizeInput;
        this.price = priceInput;
        this.side = sideInput;
    }

    public long getOrderId() {return orderId;}

    public int getOrderStatus() {return orderStatus;}

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSize() {
        return size;
    }

    public long getPrice() {return price;}

    public int getSide() {
        return this.side;
    }
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final MessageFromME that = (MessageFromME) o;
        return orderId == that.orderId &&
                orderStatus == that.orderStatus &&
                timeStamp == that.timeStamp &&
                Objects.equals(symbol, that.symbol) &&
                size == that.size &&
                price == that.price &&
                side == that.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, orderStatus, timeStamp, symbol, size, price, side);
    }

    @Override
    public String toString() {

        return "PriceLevelUpdateMessage{" +
                "orderId=" + orderId +
                ", orderStatus=" + orderStatus +
                ", timeStamp=" + timeStamp +
                ", symbol='" + symbol + '\'' +
                ", size=" + size +
                ", price=" + price +
                ", side=" + side +
                "} " + super.toString();
    }
}
