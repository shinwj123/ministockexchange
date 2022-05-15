import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.MessageCracker;

import java.util.Arrays;

public class Client extends MessageCracker implements Application {

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, RejectLogon {
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionId);
    }

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Client Session Created with SessionID = " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Client onLogon.." + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Client onLogout.." + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {}

    @Override
    public void toApp(Message message, SessionID sessionId) {
    }

    @Override
    public void onMessage(ExecutionReport message, SessionID sessionId)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("Received Execution report from server");
        System.out.println("Order Id : " + message.getOrderID().getValue());
        if (message.getOrdStatus().getValue() == '0') {
            System.out.println("Order Status : NEW");
        } else if (message.getOrdStatus().getValue() == '4') {
            System.out.println("Order Status : CANCELED");
        }
//        System.out.println("Order Status : " + message.getOrdStatus().getValue());
        System.out.println("Order Side : " + message.getSide().getValue());
        System.out.println("Order Product : " + message.getSymbol().getValue());
        System.out.println("Order Quantity : " + message.getCumQty().getValue());
        System.out.println("Order Price : " + message.getPrice().getValue());
//        if (message.getOrdType().getField() == '1') {
//            System.out.println("Order Type : MARKET");
//        } else if (message.getOrdType().getValue() == '2') {
//            System.out.println("Order Type : LIMIT");
//        }
        System.out.println("Order Type :" + message.getOrdType().getValue());
    }

}