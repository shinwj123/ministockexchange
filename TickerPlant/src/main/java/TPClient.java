import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class TPClient extends WebSocketClient {

    public TPClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public TPClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send("Hello, client opened:)");
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received message: " + message);//be careful of buffer
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");

        //next, generate json object and use the message to update orderbook on the front end.

    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

}
