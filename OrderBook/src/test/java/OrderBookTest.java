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
        // BID
        Order o1 = new Order(clientIdGenerator.incrementAndGet(), 1L, Side.BID, OrderType.LIMIT, 100100, 10);
        Order o2 = new Order(clientIdGenerator.incrementAndGet(), 2L, Side.BID, OrderType.LIMIT, 110200, 5);
        Order o3 = new Order(clientIdGenerator.incrementAndGet(), 3L, Side.BID, OrderType.LIMIT, 120300, 5);
        Order o4 = new Order(clientIdGenerator.incrementAndGet(), 4L, Side.BID, OrderType.LIMIT, 120300, 20);
        // ASK
        Order o5 = new Order(clientIdGenerator.incrementAndGet(), 5L, Side.ASK, OrderType.LIMIT, 130100, 20);
        Order o6 = new Order(clientIdGenerator.incrementAndGet(), 6L, Side.ASK, OrderType.LIMIT, 140200, 30);
        Order o7 = new Order(clientIdGenerator.incrementAndGet(), 7L, Side.ASK, OrderType.LIMIT, 150300, 50);

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
        Order o = new Order(clientIdGenerator.incrementAndGet(), 8L, Side.ASK, OrderType.LIMIT, 130100, 10);
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
        Order o = new Order(clientIdGenerator.incrementAndGet(), 9L, Side.BID, OrderType.LIMIT, 130100, 10);
        ArrayList<Order> matched = orderBook.match(o);
        assertTrue(orderBook.isStateValid());
        assertEquals(10, orderBook.getPriceLevel(130100, Side.ASK).getTotalVolume());
        System.out.println("After");
        orderBook.printOrderBook();
    }

}
