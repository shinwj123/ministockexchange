import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.SigIntBarrier;

import java.util.Properties;

import static org.agrona.SystemUtil.loadPropertiesFile;

public class BasicMediaDriver {
  public static void main(String[] args) {
    // Launch Aeron Media Driver
    System.out.println("Initializing");
    loadPropertiesFile("mediadriver.properties");
    Properties prop = System.getProperties();
    System.out.println(prop.getProperty("aeron.dir"));
    final MediaDriver.Context ctx = new MediaDriver.Context();

    try (MediaDriver ignore = MediaDriver.launch(ctx)) {
      System.out.println("Starting Media Driver");
      new SigIntBarrier().await();
      System.out.println("Shutdown Media Driver...");
    }
  }

  public static MediaDriver start(String dir) {
    MediaDriver.Context ctx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .aeronDirectoryName(dir)
            .socketRcvbufLength(2097152)
            .socketSndbufLength(2097152)
            .initialWindowLength(2097152);

    return MediaDriver.launchEmbedded(ctx);
  }
}
