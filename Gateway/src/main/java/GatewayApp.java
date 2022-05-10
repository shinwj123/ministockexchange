import java.util.Scanner;

import quickfix.*;

public class GatewayApp {
    public static void main(String[] args) throws ConfigError, FieldConvertError {

        SessionSettings settings = new SessionSettings("./conf/gateway.cfg");

        Application gatewayApplication = new Gateway(settings);
        FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
        FileLogFactory fileLogFactory = new FileLogFactory(settings);
        MessageFactory msgFactory = new DefaultMessageFactory();

        SocketAcceptor socketAcceptor = new SocketAcceptor(gatewayApplication, fileStoreFactory,
                settings, fileLogFactory, msgFactory);

        socketAcceptor.start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("press <enter> to quit");
        scanner.nextLine();

        socketAcceptor.stop();
    }

}
