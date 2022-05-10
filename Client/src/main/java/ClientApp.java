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
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;


import static quickfix.field.OrdType.*;
import static quickfix.field.Side.*;

public class ClientApp {
    private static final AtomicLong clientIdGenerator = new AtomicLong();
    //TODO: replace with batch script
    static final String batchOrder = "NVDA  Side.BUY  OrdType.LIMIT  200.25  10\nAAPL  Side.BUY  OrdType.LIMIT  160  16";
    static ClientMessageParser clientMessageParser = new ClientMessageParser(batchOrder);

    static Symbol newSymbol = new Symbol();
    static int newQuantity;
    static Side newSide = new Side();
    static OrdType newOrdType = new OrdType();
    static ArrayList<String[]> orderArray = clientMessageParser.setOrderArray(batchOrder);



    public static void main(String[] args) throws ConfigError, InterruptedException, IOException, SessionNotFound  {

        SessionSettings settings = new SessionSettings("./conf/client.cfg");

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
            Thread.sleep(2000);
        }

        System.out.println("Logged In...");

        System.out.println(Arrays.deepToString(orderArray.toArray()));

        //NEW order ID generator

        ClOrdID orderId;
        OrigClOrdID origClOrdID = new OrigClOrdID();

        for (int i = 0; i < orderArray.size(); i++) {
            String idNumber = Long.toString(clientIdGenerator.incrementAndGet());
            orderId = new ClOrdID(idNumber);
            origClOrdID = new OrigClOrdID(idNumber);

            bookMultipleOrder(orderId, sessionId, i);
        }

        //CANCEL order ID generator
        String cancelIdNumber = Long.toString(clientIdGenerator.incrementAndGet());
        ClOrdID cancelId  = new ClOrdID(cancelIdNumber);

        sendOrderCancelRequest(origClOrdID, cancelId, sessionId);

        Scanner scanner = new Scanner(System.in);
        System.out.println("press <enter> to quit");
        scanner.nextLine();

        Session.lookupSession(sessionId).disconnect("Done",false);
        socketInitiator.stop();
    }

    private static void bookMultipleOrder(ClOrdID orderID, SessionID sessionID, int i){
//        ClOrdID orderId = new ClOrdID("1");
//        HandlInst instruction = new HandlInst('1');
//        Symbol ordProduct = new Symbol("NVDA");
//        Side side = new Side(Side.BUY);
//        TransactTime transactionTime = new TransactTime();
//        OrdType orderType = new OrdType(OrdType.LIMIT);
//
//        NewOrderSingle newOrderSingle = new NewOrderSingle(orderId, instruction, ordProduct,
//                side, transactionTime,orderType);
//
//        OrderQty quantity = new OrderQty(100);
//        newOrderSingle.set(quantity);

//        String idNumber = Long.toString(clientIdGenerator.incrementAndGet());
//        ClOrdID instructionId  = new ClOrdID(idNumber);

        String[] singleOrder = clientMessageParser.getSingleOrder(orderArray, i);
        newSymbol = clientMessageParser.getSymbol(singleOrder);
        newQuantity = clientMessageParser.getQuantity(singleOrder);
        newSide = clientMessageParser.getSide(singleOrder);
        newOrdType = clientMessageParser.getOrdType(singleOrder);

        NewOrderSingle newOrderSingle = enterOrder(orderID,newSymbol, newQuantity, newSide, newOrdType);

        try {
            Session.sendToTarget(newOrderSingle, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private static NewOrderSingle enterOrder(ClOrdID orderID, Symbol ordSymbol, int numQuantity, Side action, OrdType ordType) {
        HandlInst instruction = new HandlInst('1');
        TransactTime transactionTime = new TransactTime();

        OrderQty quantity = new OrderQty(numQuantity);

        NewOrderSingle newOrderSingle;
        newOrderSingle = new NewOrderSingle(orderID ,
                                            instruction,
                                            ordSymbol,
                                            action,
                                            transactionTime,
                                            ordType);

        newOrderSingle.set(quantity);

        return newOrderSingle;
    }

    private static void sendOrderCancelRequest(OrigClOrdID origClOrdID, ClOrdID cancelID, SessionID sessionID){
//        Symbol symbol = new Symbol("NVDA");
//        Side side = new Side(Side.BUY);
//        TransactTime transactionTime = new TransactTime();
//
//        OrderCancelRequest cancelRequest = new OrderCancelRequest(origClOrdID,
//                                                                    cancelID,
//                                                                    symbol,
//                                                                    side,
//                                                                transactionTime);
//
//        OrderQty quantity = new OrderQty(100);
//        cancelRequest.set(quantity);
//
//        cancelRequest.set(new Text("Cancel My Order!"));

        OrderCancelRequest cancelRequest = enterCancelOrder(origClOrdID, cancelID,"NVDA", BUY);

        try {
            Session.sendToTarget(cancelRequest, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private static OrderCancelRequest enterCancelOrder(OrigClOrdID origClOrdID, ClOrdID cancelID, String ordProduct, char action) {
        Symbol symbol = new Symbol(ordProduct);
        Side side = new Side(action);
        TransactTime transactionTime = new TransactTime();

        OrderCancelRequest cancelRequest;
        cancelRequest = new OrderCancelRequest(origClOrdID,
                                                cancelID,
                                                symbol,
                                                side,
                                                transactionTime);

        cancelRequest.set(new Text("Cancel My Order!"));

        return cancelRequest;
    }
}