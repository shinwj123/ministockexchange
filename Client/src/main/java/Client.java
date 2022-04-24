import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MessageCracker;

public class Client extends MessageCracker implements Application {

    @Override
    public void fromAdmin(Message arg0, SessionID arg1) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, RejectLogon {
    }

    @Override
    public void fromApp(Message arg0, SessionID arg1) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType { }

    @Override
    public void onCreate(SessionID arg0) {}

    @Override
    public void onLogon(SessionID arg0) {}

    @Override
    public void onLogout(SessionID arg0) {}

    @Override
    public void toAdmin(Message arg0, SessionID arg1) {}

    @Override
    public void toApp(Message msg, SessionID sessionId) throws DoNotSend {
        System.out.println("Sender toApp: " + msg.toString());
    }

    @Override
    public void onMessage(ExecutionReport message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("Received Execution report from server");
        System.out.println("Order Id : " + message.getOrderID().getValue());
        System.out.println("Order Status : " + message.getOrdStatus().getValue());
        System.out.println("Order Price : " + message.getPrice().getValue());
    }
}