import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.charset.StandardCharsets;

public final class TradeRequest {
    private byte[] clientCompId; // senderCompId for FIX client max 8 bytes
    private long clientOrderId;
    private long orderId;
    private long price;
    private byte[] symbol; // max 8 bytes
    private byte side; // consistent with IEX DEEP  BID: '8' 0x38 ASK: '5' 0x35
    private long quantity;
    private byte orderType;

    private static int offset = 0;
    private static final int CLIENT_ORDER_ID_OFFSET = offset += 0;
    private static final int ORDER_ID_OFFSET = offset += Long.BYTES;
    private static final int PRICE_OFFSET = offset += Long.BYTES;
    private static final int QUANTITY_OFFSET = offset += Long.BYTES;
    private static final int SYMBOL_OFFSET = offset += Long.BYTES;
    private static final int CLIENT_COMP_ID_OFFSET = offset += 8;
    private static final int SIDE_OFFSET = offset += 8;
    private static final int ORDER_TYPE_OFFSET = offset += 1;

    private final UnsafeBuffer buffer;
    public static int BUFFER_SIZE = 64;

    public TradeRequest(String clientCompId, long clientOrderId, long orderId, long price, long quantity, String symbol, byte side, byte orderType) {
        this.buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(BUFFER_SIZE, 8));
        this.clientOrderId = clientOrderId;
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.symbol = stringToBytes(symbol);
        this.clientCompId = stringToBytes(clientCompId);
        this.side = side;
        this.orderType = orderType;
        this.buffer.putLong(CLIENT_ORDER_ID_OFFSET, clientOrderId);
        this.buffer.putLong(ORDER_ID_OFFSET, orderId);
        this.buffer.putLong(PRICE_OFFSET, price);
        this.buffer.putLong(QUANTITY_OFFSET, quantity);
        this.buffer.putBytes(SYMBOL_OFFSET, this.symbol);
        this.buffer.putBytes(CLIENT_COMP_ID_OFFSET, this.clientCompId);
        this.buffer.putByte(SIDE_OFFSET, side);
        this.buffer.putByte(ORDER_TYPE_OFFSET, orderType);
    }

    public TradeRequest(String clientCompId, long clientOrderId, long price, long quantity, String symbol, byte side, byte orderType) {
        this(clientCompId, clientOrderId, 0, price, quantity, symbol, side, orderType);
    }

    public UnsafeBuffer getBuffer() {
        return buffer;
    }

    private byte[] stringToBytes(String s) {
        byte[] ascii = s.getBytes(StandardCharsets.US_ASCII);
        byte[] stringBytes = new byte[8];
        if (ascii.length <= 8) {
            System.arraycopy(ascii, 0, stringBytes, 0, ascii.length);
            return stringBytes;
        }
        return stringBytes;
    }

    public static long getClientOrderId(UnsafeBuffer buffer) {
        return buffer.getLong(CLIENT_ORDER_ID_OFFSET);
    }

    public static long getPrice(UnsafeBuffer buffer) {
        return buffer.getLong(PRICE_OFFSET);
    }

    public static long getQuantity(UnsafeBuffer buffer) {
        return buffer.getLong(QUANTITY_OFFSET);
    }

    public static long getOrderId(UnsafeBuffer buffer) {
        return buffer.getLong(ORDER_ID_OFFSET);
    }

    public static String getSymbol(UnsafeBuffer buffer) {
        return buffer.getStringWithoutLengthAscii(SYMBOL_OFFSET, 8).trim();
    }

    public static String getClientCompId(UnsafeBuffer buffer) {
        return buffer.getStringWithoutLengthAscii(CLIENT_COMP_ID_OFFSET, 8).trim();
    }

    public static byte getSide(UnsafeBuffer buffer) {
        return buffer.getByte(SIDE_OFFSET);
    }

    public static byte getOrderType(UnsafeBuffer buffer) {
        return buffer.getByte(ORDER_TYPE_OFFSET);
    }
}
