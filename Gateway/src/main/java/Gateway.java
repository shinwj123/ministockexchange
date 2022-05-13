import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelRequest;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class Gateway extends MessageCracker implements Application, FragmentHandler, AutoCloseable {
    private final Aeron aeron;
    private Publisher matchingEnginePublisher;
    private Subscriber matchingEngineSubscriber;
    private static final AtomicLong execIdGenerator = new AtomicLong();
    private final Map<SessionID, HashSet<ClOrdID>> sessionActiveOrders;
    private final Map<String, SessionID> clientCompId2sessionId;
    private final Map<String, String> symbol2PubChannel;
    private final Map<String, Integer> channel2StreamId;
    private final Map<Long, Long> clOrdId2OrderId;
    private final HashSet<String> validOrderTypes = new HashSet<>();
    private static final Logger logger = LogManager.getLogger(Gateway.class);


    public Gateway(SessionSettings settings, String aeronDirectory, int[] streamIds, String ipAddr) throws ConfigError {
        validOrderTypes.add("1"); // MARKET
        validOrderTypes.add("2"); // LIMIT
        sessionActiveOrders = new HashMap<>();
        clientCompId2sessionId = new HashMap<>();
        symbol2PubChannel = new HashMap<>();
        channel2StreamId = new HashMap<>();
        clOrdId2OrderId = new HashMap<>();
        final String pubUri1 = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("192.168.0.51:40123")
                .build();
        final String pubUri2 = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("192.168.0.52:40123")
                .build();
        final String matchingEngineSubUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("224.0.1.1:40456")
                .networkInterface(ipAddr)
                .build();
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(AeronUtil::printError)
                .availableImageHandler(AeronUtil::printAvailableImage)
                .unavailableImageHandler(AeronUtil::printUnavailableImage);

        this.aeron = Aeron.connect(ctx);
        String[] pubChannels = new String[] {pubUri1, pubUri2};
        // Instrument partition to ME mapping
        String [] group1 = new String[] {"AAPL", "MSFT", "NVDA", "SPY", "QQQ", "GOOGL", "RBLX", "GME", "NFLX", "AMZN"};
        String [] group2 = new String[] {"BABA", "TSLA", "PDD", "SNOW", "PLTR", "COIN", "AMD", "ZM", "BILI", "SPOT"};
        for (String symbol : group1) {
            symbol2PubChannel.put(symbol, pubUri1);
        }
        for (String symbol : group2) {
            symbol2PubChannel.put(symbol, pubUri2);
        }

        matchingEnginePublisher = new Publisher(this.aeron);
        matchingEngineSubscriber = new Subscriber(this.aeron, this);

        for (int i = 0; i < streamIds.length; i++) {
            channel2StreamId.put(pubChannels[i], streamIds[i]);
            matchingEngineSubscriber.addSubscription(matchingEngineSubUri, streamIds[i]);
            matchingEnginePublisher.addPublication(pubChannels[i], streamIds[i]);
        }

    }

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Gateway Session Created with SessionID = " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        sessionActiveOrders.put(sessionId, new HashSet<>());
        clientCompId2sessionId.put(sessionId.getSenderCompID(), sessionId);
        System.out.println("Gateway onLogon " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        sessionActiveOrders.remove(sessionId);
        clientCompId2sessionId.remove(sessionId.getSenderCompID());
        System.out.println("Gateway Logout " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        System.out.println("Admin >> " + reformatFIXMessage(message));
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        System.out.println("Admin << " + reformatFIXMessage(message));
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        System.out.println("Gateway Order Reception : " + reformatFIXMessage(message));
    }

    private String reformatFIXMessage(Message message) {
        return message.toString().replace((char) 1, '|');
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectTagValue,
            UnsupportedMessageType {
        crack(message, sessionId);
    }

    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound {
        String clientCompId = message.getHeader().getString(SenderCompID.FIELD);
        OrdType ordType = new OrdType(message.getChar(OrdType.FIELD));

        ClOrdID clOrdID = message.getClOrdID();
        OrderID orderNumber = new OrderID(clOrdID.getValue());
        Price price = message.getPrice();
        Symbol symbol = message.getSymbol();

        ExecTransType execTransType = new ExecTransType(ExecTransType.NEW);
        ExecType execType =new ExecType(ExecType.FILL);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.NEW);
        quickfix.field.Side side = message.getSide();
        LeavesQty leavesQty = new LeavesQty(100);
        CumQty cumQuantity = new CumQty(100);
        AvgPx avgPx = new AvgPx(message.getPrice().getValue());

        if (validateOrder(message)) {
            // send to ME
            sessionActiveOrders.get(sessionID).add(clOrdID);
            long quantity = ordType.getValue() == OrdType.LIMIT ? Double.valueOf(message.getOrderQty().getValue()).longValue() : 0;
            TradeRequest request = new TradeRequest(clientCompId, Long.parseLong(clOrdID.getValue()),
                    priceToLong(price.getValue()), quantity, symbol.getValue(), (byte) side.getValue(),
                    (byte) ordType.getValue());

            String channel = symbol2PubChannel.get(symbol.getValue());
            matchingEnginePublisher.sendMessage(request.getBuffer(), channel, channel2StreamId.get(channel));
        } else {
            reject(sessionID, new OrderID(clOrdID.getValue()), execTransType, side, symbol);
        }
    }

    public void onMessage(OrderCancelRequest cancelRequest, SessionID sessionID)
            throws FieldNotFound {
        String clOrdID = cancelRequest.getClOrdID().getValue();
        OrderID orderId = new OrderID(clOrdID); // TODO: ME sets this

        Symbol symbol = cancelRequest.getSymbol();
        ExecTransType execTransType = new ExecTransType(ExecTransType.CANCEL);
        ExecType execType = new ExecType(ExecType.CANCELED);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.CANCELED);
        quickfix.field.Side side = cancelRequest.getSide();
        LeavesQty leavesQty = new LeavesQty(0);
        CumQty cumQuantity = new CumQty(0);
        AvgPx avgPx = new AvgPx(0);

        if (validateCancelRequest(cancelRequest, sessionID)) {
            // send to ME


        } else {
            reject(sessionID, orderId, execTransType, side, symbol);
        }
    }

    private void sendExecReport(SessionID sessionID, OrderID orderId, ExecTransType execTransType,
                                ExecType execType, OrdStatus orderStatus, Symbol symbol, quickfix.field.Side side, LeavesQty leavesQty,
                                CumQty cumQuantity, AvgPx avgPx) {
        ExecutionReport executionReport = new ExecutionReport(orderId, new ExecID(Long.toString(execIdGenerator.incrementAndGet())),
                execTransType, execType, orderStatus, symbol, side, leavesQty, cumQuantity, avgPx);

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private void reject(SessionID sessionID, OrderID orderId, ExecTransType execTransType, quickfix.field.Side side, Symbol symbol) {
        ExecutionReport executionReport = new ExecutionReport(orderId, new ExecID(Long.toString(execIdGenerator.incrementAndGet())),
                execTransType, new ExecType(ExecType.REJECTED), new OrdStatus(ExecType.REJECTED),
                symbol, side, new LeavesQty(0), new CumQty(0), new AvgPx(0));

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private long priceToLong(double price) {
        return BigDecimal.valueOf(price).scaleByPowerOfTen(4).longValue();
    }

    private boolean validateOrder(Message order) throws FieldNotFound {
        return validOrderTypes.contains(order.getChar(OrdType.FIELD) + "");
    }

    private boolean validateCancelRequest(OrderCancelRequest cancelRequest, SessionID sessionID) throws FieldNotFound {
        String origClOrdID = cancelRequest.getOrigClOrdID().getValue();
        ClOrdID removeClOrdID = new ClOrdID(origClOrdID);

        if (sessionActiveOrders.containsKey(sessionID)) {
            return sessionActiveOrders.get(sessionID).remove(removeClOrdID);
        }
        return false;
    }

    public void start(AtomicBoolean running) {
        matchingEngineSubscriber.start();
        while (running.get()) {
            Thread.yield();
        }
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final int session = header.sessionId();
        UnsafeBuffer data = new UnsafeBuffer(buffer, offset, length);


//        sendExecReport(sessionID, orderId, execId, execTransType,
//                execType, orderStatus, symbol, side, leavesQty, cumQuantity, avgPx);
    }

    @Override
    public void close() throws Exception {
        logger.info("Shutting down Aeron ...");
        matchingEngineSubscriber.stop();
        matchingEnginePublisher.stop();
        CloseHelper.close(aeron);
    }
}

