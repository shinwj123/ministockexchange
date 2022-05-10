import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.BufferUtil;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.SystemEpochNanoClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class MatchingEngine implements FragmentHandler, AutoCloseable {
    // Matching Engine per group of securities
    private static Properties properties;
    private final Aeron aeron;
    private Publisher multicastPublisher;
    private Subscriber gatewaySubscriber;
    private static final Logger logger = LogManager.getLogger(MatchingEngine.class);
    public static int streamId;
    final Object2ObjectHashMap<String, OrderBook> orderBooks;
    final String multicastUri;

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("HELLO ([0-9]+)");
    private State state;
    private UnsafeBuffer buffer;


    private enum State {
        INITIAL,
        CONNECTED
    }

    public MatchingEngine(String aeronDirectory, int streamId, String ipAddr) {
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
        this.streamId = streamId;
        this.state = State.INITIAL;
        this.orderBooks = new Object2ObjectHashMap<>();
        loadOrderBooks(orderBooks, String.format("trading_symbols%d.txt", streamId));



        final String matchingEngineUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint(String.format("%s:40123", ipAddr))
                .build();

        this.multicastUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("224.0.1.1:40456")
                .networkInterface(ipAddr)
                .build();

        multicastPublisher = new Publisher(this.aeron);
        gatewaySubscriber = new Subscriber(this.aeron, this);

        // same multicast address for all matching engines
        multicastPublisher.addPublication(multicastUri, streamId);
        gatewaySubscriber.addSubscription(matchingEngineUri, streamId);

        this.state = State.CONNECTED;
    }

    public void onReceiveMessage(final String message, String ipAddr) {
        Objects.requireNonNull(message, "message");

        logger.debug("receive [0x{}]: {}", Integer.toUnsignedString(streamId), message);

        switch (this.state) {
            case INITIAL: {
                this.onReceiveMessageInitial(message, ipAddr);
                break;
            }
            case CONNECTED: {
                sendMessage(multicastPublisher, this.buffer, message);
                break;
            }
        }
    }

    private void onReceiveMessageInitial(final String message, String ipAddr) {
        final Matcher matcher = MESSAGE_PATTERN.matcher(message);
        if (!matcher.matches()) {
            logger.warn("client sent malformed HELLO message: {}", message);
            return;
        }

        final int port = Integer.parseUnsignedInt(matcher.group(1));

        final String matchingEngineUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint(String.format("%s:40123", ipAddr))
                .build();

        multicastPublisher = new Publisher(this.aeron);

        this.state = State.CONNECTED;
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

    private static boolean sendMessage(final Publication pub, final UnsafeBuffer buffer, final String text) {
        logger.debug("send: [session 0x{}] {}", Integer.toUnsignedString(streamId), text);

        final byte[] value = text.getBytes(UTF_8);
        buffer.putBytes(0, value);

        long result = 0L;
        for (int index = 0; index < 5; ++index) {
            result = pub.offer(buffer, 0, text.length());
            if (result < 0L) {
                try {
                    Thread.sleep(100L);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            return true;
        }

        logger.error("could not send: {}", Long.valueOf(result));
        return false;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final int session = header.sessionId(); // sessionId identifies which gateway is the sender
        UnsafeBuffer data = new UnsafeBuffer(buffer, offset, length);

//        final int numBytes = outBuffer.putStringAscii(0, "OK " + session);
//        boolean result = multicastPublisher.sendMessage(outBuffer, multicastUri, 10);
//
//        if (result) {
//            logger.debug("successfully sent to gateway");
//        } else {
//            logger.debug("failed to send to gateway");
//        }
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
        multicastPublisher.stop();
        gatewaySubscriber.stop();
        CloseHelper.close(aeron);
    }

    public void process(Order order, OrderBook orderBook) {
        // check if the symbol is traded on the current ME before call process
        // try match immediately
        ArrayList<Order> matches = orderBook.match(order);
        if (matches.size() > 0) {
            for (Order o : matches) {
                // report for orders that are matched against
                UnsafeBuffer buffer = new Report()
                    .orderId(o.getOrderId())
                    .clientCompId(o.getClientCompId())
                    .clientOrderId(o.getClientOrderId())
                    .side(o.getSide())
                    .orderStatus(o.getStatus())
                    .executionPrice(o.getPrice())
                    .executionQuantity(o.getLastExecutedQuantity())
                    .cumExecutionQuantity(o.getFilledQuantity())
                    .deltaQuantity(-o.getLastExecutedQuantity())
                    .totalQuantity(o.getTotalQuantity())
                    .timestamp(new SystemEpochNanoClock().nanoTime())
                    .symbol(orderBook.getSymbol())
                    .buildReport();
                multicastPublisher.sendMessage(buffer, multicastUri, streamId);

                // report for incoming order
                UnsafeBuffer bufferAggOrder = new Report()
                    .orderId(order.getOrderId())
                    .clientCompId(order.getClientCompId())
                    .clientOrderId(order.getClientOrderId())
                    .side(order.getSide())
                    .orderStatus(order.getStatus())
                    .executionPrice(o.getPrice())
                    .executionQuantity(o.getLastExecutedQuantity())
                    .cumExecutionQuantity(order.getFilledQuantity())
                    .totalQuantity(order.getTotalQuantity())
                    .timestamp(new SystemEpochNanoClock().nanoTime())
                    .symbol(orderBook.getSymbol())
                    .buildReport();
                multicastPublisher.sendMessage(bufferAggOrder, multicastUri, streamId);
            }
        }

        // if LIMIT order is not matched at its entirety, then add to orderbook => TP need to update accordingly
        if (order.getType() == OrderType.LIMIT && (order.getStatus() == Status.NEW || order.getStatus() == Status.PARTIALLY_FILLED)) {
            UnsafeBuffer bufferNewOrder = new Report()
                    .orderId(order.getOrderId())
                    .clientCompId(order.getClientCompId())
                    .clientOrderId(order.getClientOrderId())
                    .side(order.getSide())
                    .orderStatus(order.getStatus())
                    .executionQuantity(order.getLastExecutedQuantity())
                    .cumExecutionQuantity(order.getFilledQuantity())
                    .totalQuantity(order.getTotalQuantity())
                    .deltaQuantity(order.getTotalQuantity() - order.getFilledQuantity())
                    .timestamp(new SystemEpochNanoClock().nanoTime())
                    .symbol(orderBook.getSymbol())
                    .buildReport();
            multicastPublisher.sendMessage(bufferNewOrder, multicastUri, streamId);
        }
    }

    public void reject(Order order, OrderBook orderBook) {
        order.setStatus(Status.REJECTED);
        UnsafeBuffer buffer = new Report()
                .orderId(order.getOrderId())
                .clientCompId(order.getClientCompId())
                .clientOrderId(order.getClientOrderId())
                .side(order.getSide())
                .orderStatus(order.getStatus())
                .totalQuantity(order.getTotalQuantity())
                .timestamp(new SystemEpochNanoClock().nanoTime())
                .symbol(orderBook.getSymbol())
                .buildReport();
        multicastPublisher.sendMessage(buffer, multicastUri, streamId);
    }

    public void cancel(Order order, OrderBook orderBook) {
        if (orderBook.removeOrder(order.getOrderId())) {
            order.setStatus(Status.CANCELLED);
            UnsafeBuffer buffer = new Report()
                    .orderId(order.getOrderId())
                    .clientCompId(order.getClientCompId())
                    .clientOrderId(order.getClientOrderId())
                    .side(order.getSide())
                    .orderStatus(order.getStatus())
                    .totalQuantity(order.getTotalQuantity())
                    .cumExecutionQuantity(order.getFilledQuantity())
                    .deltaQuantity(-(order.getTotalQuantity() - order.getFilledQuantity()))
                    .timestamp(new SystemEpochNanoClock().nanoTime())
                    .symbol(orderBook.getSymbol())
                    .buildReport();
            multicastPublisher.sendMessage(buffer, multicastUri, streamId);
        } else {
            reject(order, orderBook);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(
                    "Command line usage: java -jar MatchingEngine/target/MatchingEngine-1.0-SNAPSHOT-jar-with-dependencies.jar [local ip address] [streamId]");
            System.exit(0);
        }

        if (Integer.parseInt(args[1]) < 1) {
            System.out.println("StreamId must be greater or equal to 1");
            System.exit(0);
        }

        final InetAddress ipAddr = InetAddress.getByName(args[0]);

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron");
             MatchingEngine me = new MatchingEngine("/dev/shm/aeron", Integer.parseInt(args[1]), args[0])) {
            logger.info("Starting Matching Engine at " + ipAddr.toString() + "streamId=" + args[1]);
            me.start(running);
        }
    }
}
