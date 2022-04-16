import io.aeron.Image;
import io.aeron.Subscription;

public class AeronUtil {
    public static void printAvailableImage(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.printf(
                "Available image on %s streamId=%d sessionId=%d from %s%n",
                subscription.channel(), subscription.streamId(), image.sessionId(), image.sourceIdentity());
    }

    public static void printUnavailableImage(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.printf(
                "Unavailable image on %s streamId=%d sessionId=%d%n",
                subscription.channel(), subscription.streamId(), image.sessionId());
    }

    public static void printError(Throwable throwable){
        System.out.println(throwable.toString());
    }
}
