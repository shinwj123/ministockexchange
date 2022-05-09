import java.util.Arrays;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
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
        byte[] messageByte = message.array(); // extract byte encoded message from the array;

        //extract info from the array
        byte side = messageByte[0];
        String symbol = ByteDecoder.convertBytesToString(Arrays.copyOfRange(messageByte, 1, 1 + 8));
        long deltaQuantity = ByteDecoder.convertBytesToLong(Arrays.copyOfRange(messageByte, 9, 9 + 8));
        long direction = ByteDecoder.convertBytesToInt(Arrays.copyOfRange(messageByte, 17, 17 + 4));
        long price = ByteDecoder.convertBytesToLong(Arrays.copyOfRange(messageByte, 21, 21 + 8));

        //next, generate json object and use the message to update orderbook on the front end.



    }



    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    /*public static void main(String[] args) throws URISyntaxException {
        WebSocketClient client = new EmptyClient(new URI("ws://localhost:8887"));
        client.connect();
    }*/
}
