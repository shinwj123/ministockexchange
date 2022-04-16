import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.SigIntBarrier;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.print.attribute.standard.Media;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MatchingEngine implements FragmentHandler {
    // Matching Engine per group of securities
    private final Aeron aeron;
//    private Publisher marketDataPublisher;
    private Publisher gatewayPublisher;
    private Subscriber gatewaySubscriber;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final Logger logger = LogManager.getLogger(MatchingEngine.class);
    final UnsafeBuffer outBuffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));
    static final String gatewayUri = new ChannelUriStringBuilder()
            .reliable(true)
            .media("udp")
            .endpoint("224.0.1.1:40456")
            .networkInterface("192.168.1.101")
            .build();


    public MatchingEngine(String aeronDirectory, String matchingEngineUri, int streamId) {
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(AeronUtil::printError)
                .availableImageHandler(AeronUtil::printAvailableImage)
                .unavailableImageHandler(AeronUtil::printUnavailableImage);
        this.aeron = Aeron.connect(ctx);
//        marketDataPublisher = new Publisher(this.aeron);
        gatewayPublisher = new Publisher(this.aeron);
        gatewaySubscriber = new Subscriber(this.aeron, this);

        // same multicast address for all matching engines
        gatewayPublisher.addPublication(gatewayUri, streamId);
        gatewaySubscriber.addSubscription(matchingEngineUri, streamId);
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

    public void start() {
        running.set(true);
        gatewaySubscriber.start();
    }

    public void stop() {
        running.set(false);
        gatewayPublisher.stop();
        gatewaySubscriber.stop();
    }

    public static void main(String[] args) {
        try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron")) {
            final String matchingEngineUri = new ChannelUriStringBuilder()
                    .reliable(true)
                    .media("udp")
                    .endpoint("localhost:40123")
                    .build();
            MatchingEngine me = new MatchingEngine("/dev/shm/aeron", matchingEngineUri, 10);
            logger.info("Starting Matching Engine...");
            me.start();
            SigInt.register(() -> {logger.info("Shutting down Matching Engine..."); me.stop();});
            new SigIntBarrier().await();
        }
    }
}
