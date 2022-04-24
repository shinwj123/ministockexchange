import java.util.Scanner;

import quickfix.*;

public class GatewayApp {
    public static void main(String[] args) throws ConfigError {

        SessionSettings settings = new SessionSettings("Gateway/conf/gateway.cfg");

        Application gatewayApplication = new Gateway();
        FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
        FileLogFactory fileLogFactory = new FileLogFactory(settings);
        MessageFactory msgFactory = new DefaultMessageFactory();

        SocketAcceptor socketAcceptor = new SocketAcceptor(gatewayApplication, fileStoreFactory,
                settings, fileLogFactory, msgFactory);

        socketAcceptor.start();

        Scanner reader = new Scanner(System.in);
        System.out.println("press <enter> to quit");

        //get user input for a
        reader.nextLine();

        socketAcceptor.stop();
    }

}
