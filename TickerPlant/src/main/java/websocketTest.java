import org.agrona.concurrent.SigInt;
import org.apache.log4j.BasicConfigurator;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

public class websocketTest {
    public static void main(String args[]) throws URISyntaxException, InterruptedException {
        BasicConfigurator.configure();
        String host = "localhost";
        int port = 8081;

        TPServer server = new TPServer(new InetSocketAddress(host, port));
        server.start();

        TPClient client = new TPClient(new URI("ws://localhost:8081"));
        client.connect();

        String message = "message to send";

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        int counter = 0;
        while(running.get()) {
            server.broadcast(message + counter);
            counter++;

        }
        server.stop();
        System.out.println("shutting down");




    }
}
