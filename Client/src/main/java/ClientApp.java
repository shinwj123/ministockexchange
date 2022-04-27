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
import quickfix.fix42.NewOrderSingle;

import java.io.IOException;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) throws ConfigError, InterruptedException, IOException {

        SessionSettings settings = new SessionSettings("Client/conf/client.cfg");

        Application clientApplication = new Client();
        FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
        FileLogFactory fileLogFactory = new FileLogFactory(settings);
        MessageFactory msgFactory = new DefaultMessageFactory();

        SocketInitiator socketInitiator = new SocketInitiator(clientApplication, fileStoreFactory, settings,
                fileLogFactory, msgFactory);

        socketInitiator.start();

        SessionID sessionId = socketInitiator.getSessions().get(0);

        Session.lookupSession(sessionId).logon();
        while(!Session.lookupSession(sessionId).isLoggedOn()){
            System.out.println("Waiting for login success");
            Thread.sleep(1000);
        }

        System.out.println("Logged In...");

        bookSingleOrder(sessionId);

        Scanner scanner = new Scanner(System.in);
        System.out.println("press <enter> to quit");
        scanner.nextLine();

        Session.lookupSession(sessionId).disconnect("Done",false);
        socketInitiator.stop();
    }

    private static void bookSingleOrder(SessionID sessionID){
        ClOrdID orderId = new ClOrdID("1");
        HandlInst instruction = new HandlInst('1');
        Symbol mainCurrency = new Symbol("EUR/USD");
        Side side = new Side(Side.BUY);
        TransactTime transactionTime = new TransactTime();
        OrdType orderType = new OrdType(OrdType.MARKET);

        NewOrderSingle newOrderSingle = new NewOrderSingle(orderId, instruction, mainCurrency,
                side, transactionTime,orderType);

        newOrderSingle.set(new OrderQty(100));
        try {
            Session.sendToTarget(newOrderSingle, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }
}