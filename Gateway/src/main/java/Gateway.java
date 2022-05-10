import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

import org.apache.log4j.Logger;
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

    private Map<String, Double> priceMap = null;
    private Map<SessionID, ClOrdID> sessionMap = null;
    private final HashSet<String> validOrderTypes = new HashSet<>();
//    private final Logger log = Logger.getLogger(getClass());



    public Gateway(SessionSettings settings) throws ConfigError, FieldConvertError {
        initializeValidOrderTypes(settings);

        priceMap = new HashMap<String, Double>();
        priceMap.put("AAPL", 150.0);
        priceMap.put("NVDA", 200.25);
        priceMap.put("TSLA", 850.33);
        priceMap.put("AMZN", 2750.0);

        sessionMap = new HashMap<SessionID, ClOrdID>();


    }

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Gateway Session Created with SessionID = "
                + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Gateway onLogon.." + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Gateway Logout.." + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {

    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        System.out.println("Gateway Order Reception : " + message.toString());
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            UnsupportedMessageType {
        crack(message, sessionId);
    }

    private Price getPrice(Message message, Symbol tickerSymbol) throws FieldNotFound {
        Price price = null;
        if (message.toString().contains("Cancel My Order!")) {
            price = new Price(this.priceMap.get(tickerSymbol.getValue()));
        } else {
            if (message.getChar(OrdType.FIELD) == OrdType.LIMIT) {
                price = new Price(this.priceMap.get(tickerSymbol.getValue()));
            } else if (message.getChar(OrdType.FIELD) == OrdType.MARKET) {
                price = new Price(this.priceMap.get(tickerSymbol.getValue()));
            } else if (message.toString().contains("Cancel")) {
                price = new Price(this.priceMap.get(tickerSymbol.getValue()));
            } else {
                char side = message.getChar(Side.FIELD);
                //            if (side == Side.BUY) {
                //                price = new Price(marketDataProvider.getAsk(message.getString(Symbol.FIELD)));
                //            } else if (side == Side.SELL) {
                //                price = new Price(marketDataProvider.getBid(message.getString(Symbol.FIELD)));
                //            } else {
                if (side != Side.BUY || side != Side.SELL) {
                    throw new RuntimeException("Invalid order side: " + side);
                }
            }
        }
        return price;
    }


    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
//        OrdType ordType = new OrdType(message.getChar(OrdType.FIELD));
//        System.out.println(ordType);
        validateOrder(message);

        String clOrdID = message.getClOrdID().getValue();
        OrderID orderNumber = new OrderID(clOrdID);

        OrdType orderType = message.getOrdType();
        Symbol tickerSymbol = message.getSymbol();

        Price price = getPrice(message, tickerSymbol);

        ExecID execId = new ExecID("1");
        ExecTransType exectutionTransactioType = new ExecTransType(ExecTransType.NEW);
        ExecType purposeOfExecutionReport =new ExecType(ExecType.FILL);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.NEW);
        Symbol symbol = tickerSymbol;
        Side side = message.getSide();
        LeavesQty leavesQty = new LeavesQty(100);
        CumQty cummulativeQuantity = new CumQty(100);
        AvgPx avgPx = new AvgPx(this.priceMap.get(tickerSymbol.getValue()));

        sessionMap.put(sessionID, message.getClOrdID());

        ExecutionReport executionReport = new ExecutionReport(orderNumber,execId, exectutionTransactioType,
                purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
        executionReport.set(price);
        executionReport.set(orderType);

        System.out.println(executionReport);

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    public void onMessage(OrderCancelRequest cancelRequest, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
//        System.out.println(sessionMap);
        validateCancelRequest(cancelRequest, sessionID);
//        System.out.println(sessionMap);

        String clOrdID = cancelRequest.getClOrdID().getValue();
        OrderID orderNumber = new OrderID(clOrdID);

        Symbol tickerSymbol = cancelRequest.getSymbol();
//
//        Price price = null;
//        if(this.priceMap.containsKey(tickerSymbol.getValue())) {
//            price = new Price(this.priceMap.get(tickerSymbol.getValue()));
//        }
        Price price = getPrice(cancelRequest, tickerSymbol);

        ExecID execId = new ExecID("2");
        ExecTransType exectutionTransactioType = new ExecTransType(ExecTransType.CANCEL);
        ExecType purposeOfExecutionReport =new ExecType(ExecType.CANCELED);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.CANCELED);
        Symbol symbol = tickerSymbol;
        Side side = cancelRequest.getSide();
        LeavesQty leavesQty = new LeavesQty(0);
        CumQty cummulativeQuantity = new CumQty(0);
        AvgPx avgPx = new AvgPx(this.priceMap.get(tickerSymbol.getValue()));

        ExecutionReport executionReport = new ExecutionReport(orderNumber,execId, exectutionTransactioType,
                purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);

        executionReport.set(price);
//        executionReport.set(orderType);

        System.out.println(executionReport);

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private void initializeValidOrderTypes(SessionSettings settings) throws ConfigError, FieldConvertError {
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

    private void validateCancelRequest(OrderCancelRequest cancelRequest, SessionID sessionID) throws FieldNotFound {
        for (Map.Entry<SessionID,ClOrdID> entry : sessionMap.entrySet()) {
            if (!(entry.getKey().toString().contains(sessionID.toString()))) {
                throw new RuntimeException("Not existing Session id");
            } else if (!(entry.getValue().toString().contains(cancelRequest.getOrigClOrdID().getValue())))  {
                throw new RuntimeException("Not existing Original client order id");
            }
        }

        String origClOrdID = cancelRequest.getOrigClOrdID().getValue();
        ClOrdID removeClOrdID = new ClOrdID(origClOrdID);
//        System.out.println(removeClOrdID);
        sessionMap.remove(sessionID, removeClOrdID);
        
    }



} //public class Gateway

