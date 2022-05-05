

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.UnsafeBuffer;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Enumeration;

public class MarketDataPublisher {




    //https://github.com/eleventy7/aeron-cookbook-code/blob/main/aeron-mdc/aeron-mdc-publisher/src/main/java/com/aeroncookbook/aeron/mdc/MultiDestinationPublisher.java
    //https://github.com/eleventy7/aeron-cookbook-code/blob/main/aeron-mdc/aeron-mdc-publisher/src/main/java/com/aeroncookbook/aeron/mdc/MultiDestinationPublisherAgent.java#L44
}
