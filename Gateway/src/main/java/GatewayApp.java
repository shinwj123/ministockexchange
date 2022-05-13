import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.SigInt;
import quickfix.*;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class GatewayApp {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println(
                    "Command line usage: java -jar Gateway/target/Gateway-1.0-SNAPSHOT-jar-with-dependencies.jar [local ip address] [streamId for ME1] [streamId for ME2] ...");
            System.exit(0);
        }

        final InetAddress ipAddr = InetAddress.getByName(args[0]);

        int[] streamIds = new int[args.length - 1];
        for (int i = 0; i < streamIds.length; i++) {
            int streamId = Integer.parseInt(args[i + 1]);
            streamIds[i] = streamId;
            if (streamId < 1) {
                System.out.println("StreamId must be greater or equal to 1");
                System.exit(0);
            }
        }

        SessionSettings settings = new SessionSettings("conf/gateway.cfg");
        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));
        System.out.println("press ctrl-c to quit");

        try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron");
             Gateway gatewayApplication = new Gateway(settings, "/dev/shm/aeron", streamIds, args[0]);
             ) {
            FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
            FileLogFactory fileLogFactory = new FileLogFactory(settings);
            MessageFactory msgFactory = new DefaultMessageFactory();
            SocketAcceptor socketAcceptor = new SocketAcceptor(gatewayApplication, fileStoreFactory,
                    settings, fileLogFactory, msgFactory);

            System.out.println("Starting FIX gateway at " + ipAddr.toString());
            socketAcceptor.start();
            gatewayApplication.start(running);
            socketAcceptor.stop();
            System.out.println("Shutting down FIX gateway ...");
        }
    }
}
