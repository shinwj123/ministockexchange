import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class Report {
    // For client execution report and tickerplant price level update
    private byte[] clientCompId; // senderCompId for FIX client max 8 bytes
    private long clientOrderId;
    private long orderId;
    private long timestamp; // event timestamp (order status update)
    private long totalQuantity;
    private long executionPrice;
    private long executionQuantity; // last executed quantity
    private long cumExecutionQuantity; // cumulative executed quantity
    private byte[] symbol; // max 8 bytes
    private byte side; // consistent with IEX DEEP  BID: '8' 0x38 ASK: '5' 0x35
    private long deltaQuantity; // for TP to update price level; negative number indicates decrease in quantity
    private byte orderStatus; //

    private static int offset = 0;
    private static final int CLIENT_ORDER_ID_OFFSET = offset += 0;
    private static final int ORDER_ID_OFFSET = offset += Long.BYTES;
    private static final int TIMESTAMP_OFFSET = offset += Long.BYTES;
    private static final int EXEC_QUANTITY_OFFSET = offset += Long.BYTES;
    private static final int EXEC_PRICE_OFFSET = offset += Long.BYTES;
    private static final int CUM_EXEC_QUANTITY_OFFSET = offset += Long.BYTES;
    private static final int TOTAL_QUANTITY_OFFSET = offset += Long.BYTES;
    private static final int DELTA_QUANTITY_OFFSET = offset += Long.BYTES;
    private static final int SYMBOL_OFFSET = offset += Long.BYTES;
    private static final int CLIENT_COMP_ID_OFFSET = offset += 8;
    private static final int STATUS_OFFSET = offset += 8;
    private static final int SIDE_OFFSET = offset += 1;

    private final UnsafeBuffer buffer;
    public static int BUFFER_SIZE = 128;

    public Report() {
        this.buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(BUFFER_SIZE, 16));
    }

    public Report clientOrderId(long clientOrderId) {
        this.clientOrderId = clientOrderId;
        return this;
    }

    public Report orderId(long orderId) {
        this.orderId = orderId;
        return this;
    }

    public Report timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Report symbol(String symbol) {
        byte[] ascii = symbol.getBytes(StandardCharsets.US_ASCII);
        byte[] symbolBytes = new byte[8];
        if (ascii.length <= 8) {
            System.arraycopy(ascii, 0, symbolBytes, 0, ascii.length);
            this.symbol = symbolBytes;
        }
        return this;
    }

    public Report clientCompId(String clientCompId) {
        byte[] ascii = clientCompId.getBytes(StandardCharsets.US_ASCII);
        byte[] clientCompIdBytes = new byte[8];
        if (ascii.length <= 8) {
            System.arraycopy(ascii, 0, clientCompIdBytes, 0, ascii.length);
            this.clientCompId = clientCompIdBytes;
        }
        return this;
    }

    public Report totalQuantity(long totalQuantity) {
        this.totalQuantity = totalQuantity;
        return this;
    }

    public Report executionPrice(long executionPrice) {
        this.executionPrice = executionPrice;
        return this;
    }

    public Report executionQuantity(long executionQuantity) {
        this.executionQuantity = executionQuantity;
        return this;
    }

    public Report cumExecutionQuantity(long cumExecutionQuantity) {
        this.cumExecutionQuantity = cumExecutionQuantity;
        return this;
    }

    public Report side(Side side) {
        this.side = side.getByteCode();
        return this;
    }

    public Report orderStatus(Status status) {
        this.orderStatus = status.getByteCode();
        return this;
    }

    public Report deltaQuantity(long deltaQuantity) {
        this.deltaQuantity = deltaQuantity;
        return this;
    }

    public UnsafeBuffer buildReport() {
        buffer.putLong(CLIENT_ORDER_ID_OFFSET, clientOrderId);
        buffer.putLong(ORDER_ID_OFFSET, orderId);
        buffer.putLong(TIMESTAMP_OFFSET, timestamp);
        buffer.putLong(EXEC_QUANTITY_OFFSET, executionQuantity);
        buffer.putLong(EXEC_PRICE_OFFSET, executionPrice);
        buffer.putLong(CUM_EXEC_QUANTITY_OFFSET, cumExecutionQuantity);
        buffer.putLong(TOTAL_QUANTITY_OFFSET, totalQuantity);
        buffer.putLong(DELTA_QUANTITY_OFFSET, deltaQuantity);
        buffer.putBytes(SYMBOL_OFFSET, symbol);
        buffer.putBytes(CLIENT_COMP_ID_OFFSET, clientCompId);
        buffer.putByte(STATUS_OFFSET, orderStatus);
        buffer.putByte(SIDE_OFFSET, side);
        return buffer;
    }

    public static long getClientOrderId(UnsafeBuffer buffer) {
        return buffer.getLong(CLIENT_ORDER_ID_OFFSET);
    }

    public static long getOrderId(UnsafeBuffer buffer) {
        return buffer.getLong(ORDER_ID_OFFSET);
    }

    public static long getTimestamp(UnsafeBuffer buffer) {
        return buffer.getLong(TIMESTAMP_OFFSET);
    }

    public static long getTotalQuantity(UnsafeBuffer buffer) {
        return buffer.getLong(TOTAL_QUANTITY_OFFSET);
    }

    public static long getExecutionPrice(UnsafeBuffer buffer) {
        return buffer.getLong(EXEC_PRICE_OFFSET);
    }

    public static long getExecutionQuantity(UnsafeBuffer buffer) {
        return buffer.getLong(EXEC_QUANTITY_OFFSET);
    }

    public static long getCumExecutionQuantity(UnsafeBuffer buffer) {
        return buffer.getLong(CUM_EXEC_QUANTITY_OFFSET);
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

    public static byte getOrderStatus(UnsafeBuffer buffer) {
        return buffer.getByte(STATUS_OFFSET);
    }

    public static long getDeltaQuantity(UnsafeBuffer buffer) {
        return buffer.getLong(DELTA_QUANTITY_OFFSET);
    }

    public static JSONObject toJson(UnsafeBuffer buffer) {
        JSONObject json = new JSONObject();
        json.put("clientCompId", Report.getClientCompId(buffer));
        json.put("orderId", Report.getOrderId(buffer));
        json.put("clientOrderId", Report.getClientOrderId(buffer));
        String side = Report.getSide(buffer) == 0x38 ? "bid" : "ask";
        json.put("side", side);
        json.put("symbol", Report.getSymbol(buffer));
        json.put("status", Status.getNameFromByte(Report.getOrderStatus(buffer)));
        json.put("totalQuantity", Report.getTotalQuantity(buffer));
        long executionPrice = Report.getExecutionPrice(buffer);
        json.put("executionPrice", executionPrice != 0 ? Order.getPriceDouble(executionPrice) : JSONObject.NULL);
        long executionQuantity = Report.getExecutionQuantity(buffer);
        json.put("executionQuantity", executionQuantity != 0 ? executionQuantity : JSONObject.NULL);
        json.put("cumExecutionQuantity", Report.getCumExecutionQuantity(buffer));
        long deltaQuantity = Report.getDeltaQuantity(buffer);
        json.put("deltaQuantity", deltaQuantity != 0 ? deltaQuantity : JSONObject.NULL);
        json.put("timestamp", Report.getTimestamp(buffer));
        return json;
    }
}
