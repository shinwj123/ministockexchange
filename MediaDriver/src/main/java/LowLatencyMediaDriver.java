import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.agrona.concurrent.SigIntBarrier;

import static org.agrona.SystemUtil.loadPropertiesFiles;

public class LowLatencyMediaDriver {
  public static void main(String[] args) {
    // Launch low latency media driver
      final MediaDriver.Context ctx = new MediaDriver.Context()
              .dirDeleteOnStart(true)
              .aeronDirectoryName(args[0])
              .termBufferSparseFile(false)
              .useWindowsHighResTimer(true)
              .threadingMode(ThreadingMode.DEDICATED)
              .conductorIdleStrategy(BusySpinIdleStrategy.INSTANCE)
              .receiverIdleStrategy(NoOpIdleStrategy.INSTANCE)
              .senderIdleStrategy(NoOpIdleStrategy.INSTANCE);

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
                .termBufferSparseFile(false)
                .useWindowsHighResTimer(true)
                .threadingMode(ThreadingMode.DEDICATED)
                .conductorIdleStrategy(BusySpinIdleStrategy.INSTANCE)
                .receiverIdleStrategy(NoOpIdleStrategy.INSTANCE)
                .senderIdleStrategy(NoOpIdleStrategy.INSTANCE);

        return MediaDriver.launchEmbedded(ctx);
  }

}
