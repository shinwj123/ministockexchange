import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class Subscriber {
    private final Aeron aeron;
    private final FragmentHandler fragmentHandler;
    private final int fragmentLimitCount;
    private final Object2ObjectHashMap <String, Subscription> subscriptions;
    private final AtomicBoolean running = new AtomicBoolean(true);
    IdleStrategy idleStrategy;
    private static final Logger logger = LogManager.getLogger(Subscriber.class);

    public Subscriber(String aeronDirectory, FragmentHandler fragmentHandler, int fragmentLimitCount) {
        this.fragmentHandler = fragmentHandler;
        this.fragmentLimitCount = fragmentLimitCount;
        this.subscriptions = new Object2ObjectHashMap<>();
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(this::printError);
        aeron = Aeron.connect(ctx);
        idleStrategy = new BusySpinIdleStrategy();
    }

    public Subscriber(String aeronDirectory, FragmentHandler fragmentHandler) {
        this.fragmentHandler = fragmentHandler;
        this.fragmentLimitCount = 1;
        this.subscriptions = new Object2ObjectHashMap<>();
        Aeron.Context ctx = new Aeron.Context()
                .aeronDirectoryName(aeronDirectory)
                .errorHandler(this::printError);
        aeron = Aeron.connect(ctx);
        idleStrategy = new BusySpinIdleStrategy();
    }

    public void addSubscription(String channel,int streamId){
        subscriptions.put(getKey(channel, streamId), aeron.addSubscription(channel, streamId,
                this::onClientConnected, this::onClientDisconnected));
    }

    public void start() {
        int fragmentsRead;
        final FragmentAssembler assembler = new FragmentAssembler(fragmentHandler);
        while (running.get()) {
            fragmentsRead = 0;
            for (Subscription sub : subscriptions.values()) {
                fragmentsRead += sub.poll(assembler, fragmentLimitCount);
            }
            idleStrategy.idle(fragmentsRead);
        }
    }

    public void stop() {
        running.set(false);
        CloseHelper.close(aeron);
        subscriptions.forEach((key, sub) -> CloseHelper.close(sub));
    }

    public void onClientConnected(final Image image) {
        final int sessionId = image.sessionId();

        final Subscription subscription = image.subscription();
        System.out.printf(
                "Available image on %s streamId=%d sessionId=%d from %s%n",
                subscription.channel(), subscription.streamId(), image.sessionId(), image.sourceIdentity());
    }

    public void onClientDisconnected(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.printf(
                "Unavailable image on %s streamId=%d sessionId=%d%n",
                subscription.channel(), subscription.streamId(), image.sessionId());
    }

    private void printError(Throwable throwable){
        System.out.println(throwable.toString());
    }

    private String getKey(String channel,int streamId){
        return channel + "_" + streamId;
    }
}
