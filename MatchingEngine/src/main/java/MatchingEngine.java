import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class MatchingEngine implements FragmentHandler, AutoCloseable {
    // Matching Engine per group of securities
    private static Properties properties;
    private final Aeron aeron;
    private final Publisher multicastPublisher;
    private final Subscriber gatewaySubscriber;
    private static final Logger logger = LogManager.getLogger(MatchingEngine.class);
    public final int streamId;
    final Object2ObjectHashMap<String, OrderBook> orderBooks;
    final String multicastUri;

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
        final int session = header.sessionId(); // sessionId identifies which gateway is the sender
        UnsafeBuffer data = new UnsafeBuffer(buffer, offset, length);

        String clientCompId = TradeRequest.getClientCompId(data);
        String symbol = TradeRequest.getSymbol(data);
        long price = TradeRequest.getPrice(data);
        long quantity = TradeRequest.getQuantity(data);
        long clOrdId = TradeRequest.getClientOrderId(data);
        long orderId = TradeRequest.getOrderId(data);
        Side side = TradeRequest.getSide(data) == (byte) '1' ? Side.BID : Side.ASK;
        byte type = TradeRequest.getOrderType(data);
        OrderBook orderBook = orderBooks.get(symbol);

        if (type == OrderType.CANCEL.getByteCode()) {
            cancel(orderId, clOrdId, clientCompId, side, orderBook);
        } else if (type == OrderType.MARKET.getByteCode()) {
            process(new Order(clientCompId, clOrdId, orderId, side,
                    OrderType.MARKET, 0, quantity), orderBook);
            // MARKET order is just a special case of LIMIT order, price is infinity for BID and 0 for ASK
        } else if (type == OrderType.LIMIT.getByteCode()) {
            process(new Order(clientCompId, clOrdId, orderId, side,
                    OrderType.LIMIT, price, quantity), orderBook);
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
                    .executionPrice(order.getPrice())
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

    public void reject(long orderId, long clOrdId, String clientCompId, Side side, OrderBook orderBook) {
        UnsafeBuffer buffer = new Report()
                .orderId(orderId)
                .clientCompId(clientCompId)
                .clientOrderId(clOrdId)
                .side(side)
                .orderStatus(Status.REJECTED)
                .timestamp(new SystemEpochNanoClock().nanoTime())
                .symbol(orderBook.getSymbol())
                .buildReport();
        multicastPublisher.sendMessage(buffer, multicastUri, streamId);
    }

    public void cancel(long orderId, long clOrdId, String clientCompId, Side side, OrderBook orderBook) {
        Order removed = orderBook.removeOrder(orderId);
        if (removed != null) {
            removed.setStatus(Status.CANCELLED);
            UnsafeBuffer buffer = new Report()
                    .orderId(removed.getOrderId())
                    .clientCompId(removed.getClientCompId())
                    .clientOrderId(removed.getClientOrderId())
                    .side(removed.getSide())
                    .orderStatus(removed.getStatus())
                    .totalQuantity(removed.getTotalQuantity())
                    .executionPrice(removed.getPrice())
                    .cumExecutionQuantity(removed.getFilledQuantity())
                    .deltaQuantity(-(removed.getTotalQuantity() - removed.getFilledQuantity()))
                    .timestamp(new SystemEpochNanoClock().nanoTime())
                    .symbol(orderBook.getSymbol())
                    .buildReport();
            multicastPublisher.sendMessage(buffer, multicastUri, streamId);
        } else {
            reject(orderId, clOrdId, clientCompId, side, orderBook);
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
