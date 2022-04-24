import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.BufferUtil;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MatchingEngine implements FragmentHandler, AutoCloseable {
    // Matching Engine per group of securities
    private static Properties properties;
    private final Aeron aeron;
//    private Publisher marketDataPublisher;
    private Publisher gatewayPublisher;
    private Subscriber gatewaySubscriber;
    private static final Logger logger = LogManager.getLogger(MatchingEngine.class);
    final UnsafeBuffer outBuffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));

    final int engineId;

    final Object2ObjectHashMap<String, OrderBook> orderBooks;

    final Int2ObjectHashMap clients;
    static final String gatewayUri = new ChannelUriStringBuilder()
            .reliable(true)
            .media("udp")
            .endpoint("224.0.1.1:40456")
            .networkInterface("192.168.0.51")
            .build();


    public MatchingEngine(String aeronDirectory, String matchingEngineUri, int streamId, int engineId) {
        try {
            loadProperties("matching_engine.properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(AeronUtil::printError)
                .availableImageHandler(AeronUtil::printAvailableImage)
                .unavailableImageHandler(AeronUtil::printUnavailableImage);
        this.aeron = Aeron.connect(ctx);
        this.engineId = engineId;
        this.orderBooks = new Object2ObjectHashMap<>();
        this.clients = new Int2ObjectHashMap();
        loadOrderBooks(orderBooks, String.format("trading_symbols%d.txt", engineId));

//        marketDataPublisher = new Publisher(this.aeron);
        gatewayPublisher = new Publisher(this.aeron);
        gatewaySubscriber = new Subscriber(this.aeron, this);

        // same multicast address for all matching engines
        gatewayPublisher.addPublication(gatewayUri, streamId);
        // TODO: maintain sessionId to publication mapping to handle different connections from gateways
        gatewaySubscriber.addSubscription(matchingEngineUri, 10);
        gatewaySubscriber.addSubscription(matchingEngineUri, 11);
    }

    private static void loadProperties(String propertiesFile) throws IOException {
        try(InputStream inputStream = MatchingEngine.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            if (inputStream != null) {
                properties = new Properties();
                properties.load(inputStream);
            } else {
                throw new IOException("Unable to load properties file " + propertiesFile);
            }
        }
    }

    private void loadOrderBooks(Object2ObjectHashMap<String, OrderBook> orderBooks, String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        }
        try (InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String symbol = line.trim();
                orderBooks.put(symbol, new OrderBook(symbol));
                System.out.println(symbol);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final int session = header.sessionId();
        final byte[] data = new byte[length];
        buffer.getBytes(offset, data);

        logger.info(
                "Received message ({}) from session {} term id {} term offset {}",
                new String(data), session,
                header.termId(), header.termOffset());

        final int numBytes = outBuffer.putStringAscii(0, "OK " + session);
        boolean result = gatewayPublisher.sendMessage(outBuffer, gatewayUri, 10);

        if (result) {
            logger.debug("successfully sent to gateway");
        } else {
            logger.debug("failed to send to gateway");
        }
    }

    public void start(AtomicBoolean running) {
        gatewaySubscriber.start();
        while (running.get()) {
            Thread.yield();
        }
    }

    @Override
    public void close() {
        logger.info("Shutting down Matching Engine...");
        gatewayPublisher.stop();
        gatewaySubscriber.stop();
        CloseHelper.close(aeron);
    }

    public boolean process(Order order) {
        // check if the symbol is traded on the current ME before call process
        if (order.getType() == OrderType.MARKET) {
            //
        }
        return false;
    }

    public static void main(String[] args) {
        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));
        final String matchingEngineUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("192.168.0.51:40123")
                .build();
        try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron");
             MatchingEngine me = new MatchingEngine("/dev/shm/aeron", matchingEngineUri, 10, Integer.parseInt(args[0]))) {
            logger.info("Starting Matching Engine...");
            me.start(running);
        }
    }
}
