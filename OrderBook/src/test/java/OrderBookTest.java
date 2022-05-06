import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderBookTest {
    private final AtomicLong clientIdGenerator = new AtomicLong();

    private OrderBook orderBook;

    @BeforeEach
    void before() {

        orderBook = new OrderBook("NVDA");
        String testClientId1 = "client1";
        String testClientId2 = "client2";
        // BID
        Order o1 = new Order(testClientId1, clientIdGenerator.incrementAndGet(), 1L, Side.BID, OrderType.LIMIT, 100100, 10);
        Order o2 = new Order(testClientId1, clientIdGenerator.incrementAndGet(), 2L, Side.BID, OrderType.LIMIT, 110200, 5);
        Order o3 = new Order(testClientId1, clientIdGenerator.incrementAndGet(), 3L, Side.BID, OrderType.LIMIT, 120300, 5);
        Order o4 = new Order(testClientId1, clientIdGenerator.incrementAndGet(), 4L, Side.BID, OrderType.LIMIT, 120300, 20);
        // ASK
        Order o5 = new Order(testClientId2, clientIdGenerator.incrementAndGet(), 5L, Side.ASK, OrderType.LIMIT, 130100, 20);
        Order o6 = new Order(testClientId2, clientIdGenerator.incrementAndGet(), 6L, Side.ASK, OrderType.LIMIT, 140200, 30);
        Order o7 = new Order(testClientId2, clientIdGenerator.incrementAndGet(), 7L, Side.ASK, OrderType.LIMIT, 150300, 50);

        orderBook.addOrder(o1);
        orderBook.addOrder(o2);
        orderBook.addOrder(o3);
        orderBook.addOrder(o4);
        orderBook.addOrder(o5);
        orderBook.addOrder(o6);
        orderBook.addOrder(o7);
    }

    @Test
    void basicAddRemoveTest() {
        Order o = new Order("client3", clientIdGenerator.incrementAndGet(), 8L, Side.ASK, OrderType.LIMIT, 130100, 10);
        orderBook.addOrder(o);
        assertTrue(orderBook.isStateValid());
        orderBook.removeOrder(o.getOrderId());
        assertTrue(orderBook.isStateValid());
        orderBook.removeOrder(5L);
        assertTrue(orderBook.isStateValid());
        assertEquals(140200L, orderBook.getBestAsk());
    }

    @Test
    void basicMatching() {
        System.out.println("Before");
        orderBook.printOrderBook();
        Order o = new Order("client4", clientIdGenerator.incrementAndGet(), 9L, Side.BID, OrderType.LIMIT, 130100, 10);
        ArrayList<Order> matched = orderBook.match(o);
        assertTrue(orderBook.isStateValid());
        assertEquals(10, orderBook.getPriceLevel(130100, Side.ASK).getTotalVolume());
        System.out.println("After");
        orderBook.printOrderBook();
    }

    @Test
    void matchMultiLevels() {
        System.out.println("Before");
        orderBook.printOrderBook();
        Order o = new Order("client5", clientIdGenerator.incrementAndGet(), 10L, Side.BID, OrderType.LIMIT, 140300, 30);
        ArrayList<Order> matched = orderBook.match(o);
        assertTrue(orderBook.isStateValid());
        assertEquals(20, orderBook.getPriceLevel(140200, Side.ASK).getTotalVolume());
        assertEquals(140200, o.getLastExecutedPrice());
        assertEquals(Status.FILLED, o.getStatus());
        System.out.println("After");
        orderBook.printOrderBook();
    }

    @Test
    void matchPartial() {
        System.out.println("Before");
        orderBook.printOrderBook();
        Order o = new Order("client6", clientIdGenerator.incrementAndGet(), 11L, Side.BID, OrderType.LIMIT, 130200, 30);
        ArrayList<Order> matched = orderBook.match(o);
        assertTrue(orderBook.isStateValid());
        assertEquals(130100, o.getLastExecutedPrice());
        assertEquals(Status.PARTIALLY_FILLED, o.getStatus());
        System.out.println("After");
        orderBook.printOrderBook();
    }

    @Test
    void noMatch() {
        Order o = new Order("client6", clientIdGenerator.incrementAndGet(), 11L, Side.BID, OrderType.LIMIT, 130200, 30);
        ArrayList<Order> matched = orderBook.match(o);
        assertTrue(orderBook.isStateValid());
        assertEquals(130100, o.getLastExecutedPrice());
        assertEquals(Status.PARTIALLY_FILLED, o.getStatus());
    }

}
