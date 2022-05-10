import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;

import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

public final class TickerPlant implements FragmentHandler, AutoCloseable {

    private static Properties properties;
    private final Aeron aeron;

    private Subscriber matchingEngineSubscriber;
    private static final Logger logger = LogManager.getLogger(TickerPlant.class);

    final Object2ObjectHashMap<String, OrderBookTP> orderBooks;

    final TPServer server;

    public TickerPlant(String aeronDirectory, int[] streamIds, String ipAddr) throws URISyntaxException {
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(AeronUtil::printError)
                .availableImageHandler(AeronUtil::printAvailableImage)
                .unavailableImageHandler(AeronUtil::printUnavailableImage);
        this.aeron = Aeron.connect(ctx);
        this.orderBooks = new Object2ObjectHashMap<>();


        final String matchingEngineUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("224.0.1.1:40456")
                .networkInterface("192.168.0.201")
                .build();

        matchingEngineSubscriber = new Subscriber(this.aeron, this);
        for (int i = 0; i < streamIds.length; i++) {
            matchingEngineSubscriber.addSubscription(matchingEngineUri, streamIds[i]);
        }

        String host = "localhost";
        int port = 8081;

        this.server = new TPServer(new InetSocketAddress(host, port));
        server.start();

    }



    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final int session = header.sessionId(); // sessionId identifies which gateway is the sender
        UnsafeBuffer data = new UnsafeBuffer(buffer, offset, length);
        process(data);

    }


    public void start(AtomicBoolean running) {
        matchingEngineSubscriber.start();
        while (running.get()) {
            Thread.yield();

        }
    }

    @Override
    public void close() {
        logger.info("Shutting down TickerPlant...");
        matchingEngineSubscriber.stop();
        CloseHelper.close(aeron);
    }

    public void process(UnsafeBuffer report) {
        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;

        String symbol = Report.getSymbol(report);

        long deltaQuantity = Report.getDeltaQuantity(report);
        long price = Report.getExecutionPrice(report);

        StockPrice stockPrice = new StockPrice(price);
        OrderBookTP toUpdate = orderBooks.get(symbol);

        byte side = Report.getSide(report);//size 1, offset 0

        if (toUpdate == null) {
            toUpdate = new OrderBookTP(symbol);
            orderBooks.put(symbol, toUpdate);
        }

        PriceLevel previousLevel;
        if (side == sellUpdateTag) {
            previousLevel = toUpdate.askSide.getSpecificLevel(stockPrice);
        } else if (side == buyUpdateTag){
            previousLevel = toUpdate.bidSide.getSpecificLevel(stockPrice);
        } else {
            throw new IllegalArgumentException("unknowed side for the book update");
        }

        toUpdate.priceLevelUpdate(symbol, stockPrice, deltaQuantity, side, previousLevel) ;

        server.broadcast(Report.toJson(report).toString());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(
                    "Command line usage: java -jar TickerPlant/target/TickerPlant-1.0-SNAPSHOT-jar-with-dependencies.jar [local ip address] [streamId]");
            System.exit(0);
        }

        if (Integer.parseInt(args[1]) < 1) {
            System.out.println("StreamId must be greater or equal to 1");
            System.exit(0);
        }
        int[] streamIds = new int[args.length - 1];
        for (int i = 0; i < streamIds.length - 1; i++) {
            streamIds[i] = Integer.valueOf(args[i + 1]);
        }

        final InetAddress ipAddr = InetAddress.getByName(args[0]);

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron");
             TickerPlant TP = new TickerPlant("/dev/shm/aeron", streamIds, args[0])) {
            for (int i = 0; i < streamIds.length; i++) {
                logger.info("Starting TickerPlant at " + ipAddr.toString() + "streamId=" + streamIds[i]);
            }

            TP.start(running);
        }
    }

}