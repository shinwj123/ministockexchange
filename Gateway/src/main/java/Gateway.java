import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.SessionSettings;
import quickfix.FieldConvertError;
import quickfix.ConfigError;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelRequest;




public class Gateway extends MessageCracker implements Application {

    private static final String VALID_ORDER_TYPES_KEY = "ValidOrderTypes";
    private static final AtomicLong execIdGenerator = new AtomicLong();
    private final Map<SessionID, HashSet<ClOrdID>> sessionActiveOrders;
    private final HashSet<String> validOrderTypes = new HashSet<>();
    private static final Logger logger = LogManager.getLogger(Gateway.class);


    public Gateway(SessionSettings settings) throws ConfigError {
        initializeValidOrderTypes(settings);
        sessionActiveOrders = new HashMap<>();
    }

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Gateway Session Created with SessionID = " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        sessionActiveOrders.put(sessionId, new HashSet<>());
        System.out.println("Gateway onLogon.." + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        sessionActiveOrders.remove(sessionId);
        System.out.println("Gateway Logout.." + sessionId);
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
        return message.toString().replaceAll("\1", "1");
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectTagValue,
            UnsupportedMessageType {
        crack(message, sessionId);
    }

    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, IncorrectTagValue {
        OrdType ordType = new OrdType(message.getChar(OrdType.FIELD));
        validateOrder(message);

        ClOrdID clOrdID = message.getClOrdID();
        OrderID orderNumber = new OrderID(clOrdID.getValue());

        OrdType orderType = message.getOrdType();
        Symbol tickerSymbol = message.getSymbol();

        ExecID execId = new ExecID("1");
        ExecTransType execTransactionType = new ExecTransType(ExecTransType.NEW);
        ExecType purposeOfExecutionReport =new ExecType(ExecType.FILL);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.NEW);
        Side side = message.getSide();
        LeavesQty leavesQty = new LeavesQty(100);
        CumQty cumQuantity = new CumQty(100);
        AvgPx avgPx = new AvgPx(message.getPrice().getValue());

        sessionActiveOrders.get(sessionID).add(clOrdID);

        ExecutionReport executionReport = new ExecutionReport(orderNumber,execId, execTransactionType,
                purposeOfExecutionReport, orderStatus, tickerSymbol, side, leavesQty, cumQuantity, avgPx);
        executionReport.set(orderType);

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    public void onMessage(OrderCancelRequest cancelRequest, SessionID sessionID)
            throws FieldNotFound {
        String clOrdID = cancelRequest.getClOrdID().getValue();
        OrderID orderNumber = new OrderID(clOrdID); // TODO: ME sets this

        Symbol tickerSymbol = cancelRequest.getSymbol();
        ExecID execId = new ExecID(Long.toString(execIdGenerator.incrementAndGet()));
        ExecTransType execTransactionType = new ExecTransType(ExecTransType.CANCEL);
        ExecType purposeOfExecutionReport =new ExecType(ExecType.CANCELED);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.CANCELED);
        Side side = cancelRequest.getSide();
        LeavesQty leavesQty = new LeavesQty(0);
        CumQty cumQuantity = new CumQty(0);
        AvgPx avgPx = new AvgPx(0);

        if (!validateCancelRequest(cancelRequest, sessionID)) {
            execTransactionType = new ExecTransType(ExecType.REJECTED);
            orderStatus = new OrdStatus(ExecType.REJECTED);
        }

        ExecutionReport executionReport = new ExecutionReport(orderNumber, execId, execTransactionType,
                purposeOfExecutionReport, orderStatus, tickerSymbol, side, leavesQty, cumQuantity, avgPx);

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private void initializeValidOrderTypes(SessionSettings settings) throws ConfigError {
        if (settings.isSetting(VALID_ORDER_TYPES_KEY)) {
            List<String> orderTypes = Arrays.asList(settings.getString(VALID_ORDER_TYPES_KEY).trim().split("\\s*,\\s*"));
            validOrderTypes.addAll(orderTypes);
        } else {
            validOrderTypes.add(OrdType.LIMIT + "");
        }
    }

    private void validateOrder(Message order) throws IncorrectTagValue, FieldNotFound {
        OrdType ordType = new OrdType(order.getChar(OrdType.FIELD));
        if (!validOrderTypes.contains(Character.toString(ordType.getValue()))) {
//            log.error("Order type not in ValidOrderTypes setting");
            System.out.println("Order type not in ValidOrderTypes setting");
            throw new IncorrectTagValue(ordType.getField());
        }
//        if (ordType.getValue() == OrdType.MARKET && marketDataProvider == null) {
//            log.error("DefaultMarketPrice setting not specified for market order");
//            throw new IncorrectTagValue(ordType.getField());
//        }
    }

    private boolean validateCancelRequest(OrderCancelRequest cancelRequest, SessionID sessionID) throws FieldNotFound {
        String origClOrdID = cancelRequest.getOrigClOrdID().getValue();
        ClOrdID removeClOrdID = new ClOrdID(origClOrdID);

        if (sessionActiveOrders.containsKey(sessionID)) {
            return sessionActiveOrders.get(sessionID).remove(removeClOrdID);
        }
        return false;
    }
}

