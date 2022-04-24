import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.MessageFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.ClOrdID;
import quickfix.field.HandlInst;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

import java.io.IOException;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) throws ConfigError, InterruptedException, IOException {

        SessionSettings settings = new SessionSettings("Gateway/conf/client.cfg");

        Application clientApplication = new Client();
        FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
        FileLogFactory fileLogFactory = new FileLogFactory(settings);
        MessageFactory msgFactory = new DefaultMessageFactory();

        SocketInitiator socketInitiator = new SocketInitiator(clientApplication, fileStoreFactory, settings,
                fileLogFactory, msgFactory);

        System.out.print("penis");

        socketInitiator.start();

        System.out.print("hehe");

        SessionID sessionId = socketInitiator.getSessions().get(0);

        System.out.print("dick");
        Session.lookupSession(sessionId).logon();
        while(!Session.lookupSession(sessionId).isLoggedOn()){
            System.out.println("Waiting for login success");
            Thread.sleep(1000);
        }

        System.out.println("Logged In...");

        Thread.sleep(5000);
        bookSingleOrder(sessionId);

        System.out.println("Type to quit");
        Scanner scanner = new Scanner(System.in);
        scanner.next();
        Session.lookupSession(sessionId).disconnect("Done",false);
        socketInitiator.stop();
    }

    private static void bookSingleOrder(SessionID sessionID){
        ClOrdID orderId = new ClOrdID("1");
        //HandlInst instruction = new HandlInst('1');
        Side side = new Side(Side.BUY);
        TransactTime transactionTime = new TransactTime();
        OrdType orderType = new OrdType(OrdType.FOREX_MARKET);

        quickfix.fix44.NewOrderSingle newOrderSingle = new NewOrderSingle(orderId, side, transactionTime,orderType);

        newOrderSingle.set(new OrderQty(100));
        try {
            Session.sendToTarget(newOrderSingle, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }
}