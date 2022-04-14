import io.aeron.Aeron;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.CloseHelper;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class Publisher {
    private final Aeron aeron;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object2ObjectHashMap<String, Publication> publications;
    private static final Logger logger = LogManager.getLogger(Publisher.class);

    public Publisher(String aeronDirectory) {
        this.publications = new Object2ObjectHashMap<>();
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(this::printError)
                .availableImageHandler(this::printAvailableImage)
                .unavailableImageHandler(this::printUnavailableImage);
        aeron = Aeron.connect(ctx);
    }

    public void addPublication(String channel, int streamId) {
        publications.put(getKey(channel, streamId), aeron.addPublication(channel, streamId));
    }

    public boolean sendMessage(final UnsafeBuffer buffer, final String channel, int streamId) {
        // publication.offer makes a copy of the buffer. User tryClaim to avoid a copy
        if (buffer != null && running.get()) {
            Publication pub = publications.get(getKey(channel, streamId));
            long response = 0;
            for (int retry = 0; retry < 3; retry++) {
                response = pub.offer(buffer);
                if (response > 0) {
                    return true;
                }

                if (response == Publication.NOT_CONNECTED) {
                    logger.debug("Offer failed because publisher is not connected to a subscriber");
                } else if (response == Publication.BACK_PRESSURED) {
                    logger.debug("Offer failed due to back pressure");
                } else if (response == Publication.ADMIN_ACTION) {
                    logger.debug("Offer failed because of an administration action in the system");
                } else if (response == Publication.CLOSED) {
                    logger.debug("Offer failed due to publication is closed and is unable to accept data");
                } else if (response == Publication.MAX_POSITION_EXCEEDED) {
                    logger.debug("Offer failed due to the log buffer reaching the maximum position of the stream");
                } else {
                    logger.debug("Offer failed due to unknown reason");
                }
                if (!pub.isConnected()) {
                    logger.debug("No active subscribers detected");
                }
            }
        }
        return false;
    }

    public void printAvailableImage(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.printf(
                "Available image on %s streamId=%d sessionId=%d from %s%n",
                subscription.channel(), subscription.streamId(), image.sessionId(), image.sourceIdentity());
    }

    public void printUnavailableImage(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.printf(
                "Unavailable image on %s streamId=%d sessionId=%d%n",
                subscription.channel(), subscription.streamId(), image.sessionId());
    }

    private String getKey(String channel,int streamId){
        return channel + "_" + streamId;
    }

    private void printError(Throwable throwable){
        System.out.println(throwable.toString());
    }

    public void stop() {
        running.set(false);
        CloseHelper.close(aeron);
        publications.forEach((key, pub) -> CloseHelper.close(pub));
    }
}
