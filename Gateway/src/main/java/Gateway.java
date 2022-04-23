import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;


public class Gateway extends MessageCracker implements Application {

    @Override
    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("###NewOrderived:" + message.toString());
        System.out.println("###SymbolOrder" + message.getSymbol().toString());
        System.out.println("###SideOrder" + message.getSide().toString());
        System.out.println("###TypeOrder" + message.getOrdType().toString());
        System.out.println("###TransactioTimeOrder" + message.getTransactTime().toString());
        sendMessageToClient (message, sessionID);
    }

    public void sendMessageToClient(NewOrderSingle message, SessionID sessionID) {
        try {
            OrderQty orderQty = null;

            orderQty = new OrderQty(56.0);
            quickfix.fix40.ExecutionReport accept = new quickfix.fix40.ExecutionReport(new OrderID("133456"), new ExecID("789"),
                    new ExecTransType(ExecTransType.NEW), new OrdStatus(OrdStatus.NEW), message.getSymbol(), message.getSide(),
                    orderQty, new LastShares(0), new LastPx(0), new CumQty(0), new AvgPx(0));
            accept.set(message.getClOrdID());
            System.out.println("###Sending Order Acceptance:" + accept.toString() + "sessionID:" + sessionID.toString());
            Session.sendToTarget(accept, sessionID);
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        } catch (FieldNotFound fieldNotFound) {
            fieldNotFound.printStackTrace();
        } catch (SessionNotFound sessionNotFound) {
            sessionNotFound.printStackTrace();
        }
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