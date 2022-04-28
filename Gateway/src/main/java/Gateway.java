import java.util.HashMap;
import java.util.Map;

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
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelRequest;



public class Gateway extends MessageCracker implements Application {

    private Map<String, Double> priceMap = null;


    public Gateway() {
        priceMap = new HashMap<String, Double>();
        priceMap.put("AAPL", 150.0);
        priceMap.put("NVDA", 200.25);
        priceMap.put("TSLA", 850.33);
        priceMap.put("AMZN", 2750.0);
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

    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String clOrdID = message.getClOrdID().toString();
        OrderID orderNumber = new OrderID(clOrdID);

        OrdType orderType = message.getOrdType();
        Symbol tickerSymbol = message.getSymbol();


        Price price = null;
        if (OrdType.MARKET == orderType.getValue()) {
            if(this.priceMap.containsKey(tickerSymbol.getValue())){
                price = new Price(this.priceMap.get(tickerSymbol.getValue()));
            }
        } else if (OrdType.LIMIT == orderType.getValue()) {
            if(this.priceMap.containsKey(tickerSymbol.getValue())){
                price = new Price(this.priceMap.get(tickerSymbol.getValue()));
            }
        }

//        OrderID orderNumber = new OrderID("1");
        ExecID execId = new ExecID("1");
        ExecTransType exectutionTransactioType = new ExecTransType(ExecTransType.NEW);
        ExecType purposeOfExecutionReport =new ExecType(ExecType.FILL);
        OrdStatus orderStatus = new OrdStatus(OrdStatus.NEW);
        Symbol symbol = tickerSymbol;
        Side side = message.getSide();
        LeavesQty leavesQty = new LeavesQty(100);
        CumQty cummulativeQuantity = new CumQty(100);
        AvgPx avgPx = new AvgPx(this.priceMap.get(tickerSymbol.getValue()));

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

    public void onMessage(OrderCancelRequest message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

    }

}