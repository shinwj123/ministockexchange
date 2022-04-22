import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;


public class Gateway extends MessageCracker implements Application {

    @Override
    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("Inside onMessage for New Order Single");
        super.onMessage(message, sessionID);
    }

    @Override
    public void fromAdmin(Message arg0, SessionID arg1) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, RejectLogon {
    }

    @Override
    public void fromApp(Message arg0, SessionID arg1) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType {
        System.out.println("Receiver fromApp..  " + arg0);
    }

    @Override
    public void onCreate(SessionID arg0) {
        System.out.println("Receiver onCreate.. " + arg0);
    }

    @Override
    public void onLogon(SessionID arg0) {
        System.out.println("Receiver onLogon.." + arg0);
    }

    @Override
    public void onLogout(SessionID arg0) {}

    @Override
    public void toAdmin(Message arg0, SessionID arg1) {}

    @Override
    public void toApp(Message arg0, SessionID arg1) throws DoNotSend {}
}