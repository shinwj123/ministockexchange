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
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TickerPlant implements FragmentHandler, AutoCloseable {
    private final Aeron aeron;

    private final Subscriber matchingEngineSubscriber;
    private static final Logger logger = LogManager.getLogger(TickerPlant.class);

    final Object2ObjectHashMap<String, OrderBookTP> orderBooks;

    final TPServer server;

    public TickerPlant(String aeronDirectory, int[] streamIds, String ipAddr) {
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
                .networkInterface(ipAddr)
                .build();

        matchingEngineSubscriber = new Subscriber(this.aeron, this);
        for (int streamId : streamIds) {
            matchingEngineSubscriber.addSubscription(matchingEngineUri, streamId);
        }

        int port = 8081;
        this.server = new TPServer(new InetSocketAddress("0.0.0.0", port));
    }



    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final int session = header.sessionId();
        UnsafeBuffer data = new UnsafeBuffer(buffer, offset, length);
        process(data);

    }


    public void start(AtomicBoolean running) {
        server.start();
        matchingEngineSubscriber.start();
        while (running.get()) {
            Thread.yield();

        }
    }

    @Override
    public void close() throws InterruptedException {
        logger.info("Shutting down TickerPlant...");
        matchingEngineSubscriber.stop();
        server.stop();
        CloseHelper.close(aeron);
    }

    public void process(UnsafeBuffer report) {
        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;

        String symbol = Report.getSymbol(report);

        long deltaQuantity = Report.getDeltaQuantity(report);

        if (deltaQuantity == 0) {
            return;
        }

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
            if (previousLevel == null) {
                previousLevel = BookSide.toPriceLevel(symbol, stockPrice, 0);
            }
        } else if (side == buyUpdateTag){
            previousLevel = toUpdate.bidSide.getSpecificLevel(stockPrice);
            if (previousLevel == null) {
                previousLevel = BookSide.toPriceLevel(symbol, stockPrice, 0);
            }
        } else {
            throw new IllegalArgumentException("unknown side for the book update");
        }
        JSONObject reportJson = Report.toJson(report);
        long newSize = toUpdate.priceLevelUpdate(symbol, stockPrice, deltaQuantity, side, previousLevel);
        reportJson.put("newSize", newSize);
        server.broadcast(reportJson.toString());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println(
                    "Command line usage: java -jar TickerPlant/target/TickerPlant-1.0-SNAPSHOT-jar-with-dependencies.jar [local ip address] [streamId for ME1] [streamId for ME2] ...");
            System.exit(0);
        }

        int[] streamIds = new int[args.length - 1];
        for (int i = 0; i < streamIds.length; i++) {
            int streamId = Integer.parseInt(args[i + 1]);
            streamIds[i] = streamId;
            if (streamId < 1) {
                System.out.println("StreamId must be greater or equal to 1");
                System.exit(0);
            }
        }

        final InetAddress ipAddr = InetAddress.getByName(args[0]);

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron");
             TickerPlant TP = new TickerPlant("/dev/shm/aeron", streamIds, args[0])) {
            for (int streamId : streamIds) {
                logger.info("Starting TickerPlant at " + ipAddr.toString() + "streamId=" + streamId);
            }

            TP.start(running);
        }
    }

}