import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.BufferUtil;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.SigIntBarrier;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SimpleGateway implements FragmentHandler, AutoCloseable {
    private final Aeron aeron;
    private Publisher matchingEnginePublisher;
    private Subscriber matchingEngineSubscriber;
    private static final Logger logger = LogManager.getLogger(SimpleGateway.class);
    final UnsafeBuffer outBuffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));
    private final String gatewayPubUri;
    private final int streamId;

    public SimpleGateway(String aeronDirectory, String gatewayPubUri, String iface, int streamId) {
        // TODO add subscription for all matching engines, one streamId per ME
        this.gatewayPubUri = gatewayPubUri; // pub uri should be matching engine's address
        this.streamId = streamId;
        final String matchingEngineSubUri = new ChannelUriStringBuilder()
                .reliable(true)
                .media("udp")
                .endpoint("224.0.1.1:40456")
                .networkInterface(iface)
                .build();
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(AeronUtil::printError)
                .availableImageHandler(AeronUtil::printAvailableImage)
                .unavailableImageHandler(AeronUtil::printUnavailableImage);
        this.aeron = Aeron.connect(ctx);
        matchingEnginePublisher = new Publisher(this.aeron);
        matchingEngineSubscriber = new Subscriber(this.aeron, this);

        matchingEnginePublisher.addPublication(gatewayPubUri, streamId);
        matchingEngineSubscriber.addSubscription(matchingEngineSubUri, streamId);
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final byte[] data = new byte[length];
        buffer.getBytes(offset, data);
        logger.info(
                "Received message ({}) from Matching Engine",
                new String(data));
    }

    public void start(AtomicBoolean running) {
        final Random random = new Random();
        matchingEngineSubscriber.start();
        while (running.get()) {
            final int numBytes = outBuffer.putStringAscii(0, Integer.toUnsignedString(random.nextInt()));
            matchingEnginePublisher.sendMessage(outBuffer, gatewayPubUri, streamId);
        }

    }

    @Override
    public void close() {
        logger.info("Shutting down gateway...");
        matchingEnginePublisher.stop();
        matchingEngineSubscriber.stop();
        CloseHelper.close(aeron);
    }

  public static void main(String[] args) {
      final AtomicBoolean running = new AtomicBoolean(true);
      SigInt.register(() -> running.set(false));
      final String pubUri = new ChannelUriStringBuilder()
              .reliable(true)
              .media("udp")
              .endpoint("192.168.0.51:40123")
              .build();
      try (MediaDriver ignore = BasicMediaDriver.start("/dev/shm/aeron");
           SimpleGateway gw = new SimpleGateway("/dev/shm/aeron", pubUri, args[0], Integer.parseInt(args[1]))) {
          logger.info("Starting gateway...");
          gw.start(running);
      }
  }
}
