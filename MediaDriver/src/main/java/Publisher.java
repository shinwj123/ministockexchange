import io.aeron.Aeron;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.CloseHelper;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Publisher {
    private final Aeron aeron;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object2ObjectHashMap<String, Publication> publications;
    private static final Logger logger = LogManager.getLogger(Publisher.class);
    private final IdleStrategy idleStrategy;

    public Publisher(Aeron aeron) {
        this.aeron = aeron;
        this.publications = new Object2ObjectHashMap<>();
        idleStrategy = new YieldingIdleStrategy();
    }

    public void addPublication(String channel, int streamId) {
        publications.put(getKey(channel, streamId), aeron.addPublication(channel, streamId));
    }

    public boolean sendMessage(final UnsafeBuffer buffer, final String channel, int streamId) {
        // publication.offer makes a copy of the buffer. Use tryClaim to avoid a copy
        if (buffer != null && running.get()) {
            Publication pub = publications.get(getKey(channel, streamId));
            long response = 0;
            while (!pub.isConnected())
            {
                Thread.yield();
            }
            for (int retry = 0; retry < 5; retry++) {
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

                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                idleStrategy.idle();
            }
        }
        return false;
    }



    private String getKey(String channel,int streamId){
        return channel + "_" + streamId;
    }

    public void stop() {
        running.set(false);
        publications.forEach((key, pub) -> CloseHelper.close(pub));
    }
}
