import java.util.Arrays;
import java.util.Objects;

public class toPriceLevelUpdateMessage {

    private final String messageType;
    private final String eventFlag; // means the order in the message is still processed, true means order processing completed
    private final long timeStamp;
    private final String symbol;
    private final int size;
    private final StockPrice stockPrice;

    public toPriceLevelUpdateMessage(
            byte[] byteMessage) {
        byte typeByte = byteMessage[0];
        if (typeByte == (byte) 0x38) {
            this.messageType = "PRICE_LEVEL_UPDATE_BUY";
        } else if (typeByte == (byte) 0x35) {
            this.messageType = "PRICE_LEVEL_UPDATE_SELL";
        } else {
            throw new IllegalArgumentException("not message type for price level update message");
        }
        byte flagByte = byteMessage[1];
        if (flagByte == (byte) 0x0) {
            this.eventFlag = "ORDER_BOOK_IS_PROCESSING_EVENT";
        } else if (flagByte == (byte) 0x1) {
            this.eventFlag = "EVENT_PROCESSING_COMPLETE";
        } else {
            throw new IllegalArgumentException("not flag type for Price level update message");
        }

        this.timeStamp = ByteDecoder.convertBytesToLong(Arrays.copyOfRange(byteMessage, 2, 10));
        this.symbol = ByteDecoder.convertBytesToString(Arrays.copyOfRange(byteMessage, 10, 18));
        this.size = ByteDecoder.convertBytesToInt(Arrays.copyOfRange(byteMessage, 18, 22));
        this.stockPrice = new StockPrice(ByteDecoder.convertBytesToLong(Arrays.copyOfRange(byteMessage, 22, 30)));

    }

    public String getEventFlag() {
        return eventFlag;
    }

    public long getTimestamp() {
        return timeStamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSize() {
        return size;
    }

    public long getPrice() {
        return stockPrice.getNumber();
    }
    public String getMessageType() {
        return this.messageType;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        //if (!super.equals(o)) return false;
        final toPriceLevelUpdateMessage that = (toPriceLevelUpdateMessage) o;
        return  Objects.equals(messageType, that.messageType) &&
                Objects.equals(eventFlag, that.eventFlag) &&
                timeStamp == that.timeStamp &&
                Objects.equals(symbol, that.symbol) &&
                size == that.size &&
                stockPrice.getNumber() == that.stockPrice.getNumber();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), eventFlag, timeStamp, symbol, size, stockPrice, messageType);
    }

    @Override
    public String toString() {

        return "PriceLevelUpdateMessage{" +
                "messageType=" + messageType +
                ", eventFlag=" + eventFlag +
                ", timestamp=" + timeStamp +
                ", symbol='" + symbol + '\'' +
                ", size=" + size +
                ", price=" + stockPrice.toString() +
                "} " + super.toString();
    }

}
