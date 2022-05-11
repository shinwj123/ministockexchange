import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.agrona.concurrent.SigInt;
import quickfix.*;

public class GatewayApp {
    public static void main(String[] args) throws ConfigError, FieldConvertError {

        SessionSettings settings = new SessionSettings("conf/gateway.cfg");

        Application gatewayApplication = new Gateway(settings);
        FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
        FileLogFactory fileLogFactory = new FileLogFactory(settings);
        MessageFactory msgFactory = new DefaultMessageFactory();

        SocketAcceptor socketAcceptor = new SocketAcceptor(gatewayApplication, fileStoreFactory,
                settings, fileLogFactory, msgFactory);

        socketAcceptor.start();

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));
        System.out.println("press ctrl-c to quit");
        while (running.get());
        socketAcceptor.stop();
    }

}
