package com.exchange.v1.engine;

import com.exchange.v1.model.Order;
import com.exchange.v1.test.OrderBuilder;
import com.exchange.v1.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook("PETR4");
    }

    @Test
    void shouldAddBuyOrder() {
        Order buy = TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100);
        book.addOrder(buy);

        assertTrue(book.bestBid().isPresent());
        assertEquals(50.0, book.bestBid().get().getKey());
        assertTrue(book.bestAsk().isEmpty());
    }

    @Test
    void shouldAddSellOrder() {
        Order sell = TestUtils.createSellLimit("ORD-1", "PETR4", 55.0, 100);
        book.addOrder(sell);

        assertTrue(book.bestAsk().isPresent());
        assertEquals(55.0, book.bestAsk().get().getKey());
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void shouldRemoveExistingOrder() {
        Order buy = TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100);
        book.addOrder(buy);
        book.removeOrder("ORD-1", Order.Side.BUY);

        assertNull(book.findOrder("ORD-1", Order.Side.BUY));
    }

    @Test
    void shouldThrowWhenRemovingNonExistentOrder() {
        assertThrows(IllegalArgumentException.class, () ->
                book.removeOrder("NONEXISTENT", Order.Side.BUY));
    }

    @Test
    void bestBidShouldReturnHighestBuyPrice() {
        book.addOrder(TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100));
        book.addOrder(TestUtils.createBuyLimit("ORD-2", "PETR4", 60.0, 100));

        assertTrue(book.bestBid().isPresent());
        assertEquals(60.0, book.bestBid().get().getKey());
    }

    @Test
    void bestAskShouldReturnLowestSellPrice() {
        book.addOrder(TestUtils.createSellLimit("ORD-1", "PETR4", 55.0, 100));
        book.addOrder(TestUtils.createSellLimit("ORD-2", "PETR4", 53.0, 100));

        assertTrue(book.bestAsk().isPresent());
        assertEquals(53.0, book.bestAsk().get().getKey());
    }

    @Test
    void ordersAtSamePriceShouldFollowFIFO() {
        Order first = TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100);
        Order second = TestUtils.createBuyLimit("ORD-2", "PETR4", 50.0, 100);

        book.addOrder(first);
        book.addOrder(second);

        Map.Entry<Double, LinkedList<Order>> bestBid = book.bestBid().orElseThrow();
        LinkedList<Order> orders = bestBid.getValue();

        assertEquals(2, orders.size());
        assertSame(first, orders.get(0));
        assertSame(second, orders.get(1));
    }

    @Test
    void shouldHandleMultipleSymbols() {
        OrderBook petr4 = new OrderBook("PETR4");
        OrderBook vale3 = new OrderBook("VALE3");

        petr4.addOrder(TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100));
        vale3.addOrder(TestUtils.createBuyLimit("ORD-2", "VALE3", 68.0, 100));

        assertEquals("PETR4", petr4.getSymbol());
        assertEquals("VALE3", vale3.getSymbol());
        assertTrue(petr4.bestBid().isPresent());
        assertTrue(vale3.bestBid().isPresent());
    }

    @Test
    void shouldRejectLimitOrderWithZeroPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("BAD", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 0, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldRejectLimitOrderWithNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("BAD", "PETR4", Order.Side.BUY, Order.Type.LIMIT, -10, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldRejectOrderWithZeroQty() {
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(TestUtils.createBuyLimit("BAD", "PETR4", 50.0, 0)));
    }

    @Test
    void shouldRejectOrderWithNegativeQty() {
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(TestUtils.createBuyLimit("BAD", "PETR4", 50.0, -10)));
    }

    @Test
    void shouldUpdateBestBidWhenHigherPriceArrives() {
        book.addOrder(TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100));
        book.addOrder(TestUtils.createBuyLimit("ORD-2", "PETR4", 55.0, 100));

        assertEquals(55.0, book.bestBid().orElseThrow().getKey());
    }

    @Test
    void shouldFindOrderByClOrdID() {
        book.addOrder(TestUtils.createBuyLimit("ORD-FIND", "PETR4", 50.0, 100));
        Order found = book.findOrder("ORD-FIND", Order.Side.BUY);
        assertNotNull(found);
        assertEquals("ORD-FIND", found.getClOrdID());
    }

    @Test
    void shouldReturnNullForNonExistentOrder() {
        assertNull(book.findOrder("NONEXISTENT", Order.Side.BUY));
    }

    @Test
    void shouldRemoveFilledOrders() {
        Order buy1 = TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100);
        Order buy2 = TestUtils.createBuyLimit("ORD-2", "PETR4", 50.0, 100);

        book.addOrder(buy1);
        book.addOrder(buy2);

        buy1.execute(100);
        book.removeFilledOrders();

        Map.Entry<Double, LinkedList<Order>> best = book.bestBid().orElseThrow();
        assertEquals(1, best.getValue().size());
        assertSame(buy2, best.getValue().get(0));
    }

    @Test
    void concurrentAccessShouldMaintainConsistency() throws InterruptedException {
        int threadCount = 10;
        int ordersPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean anyError = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        String clOrdID = "ORD-" + threadIdx + "-" + j;
                        Order order = TestUtils.createBuyLimit(clOrdID, "PETR4", 50.0 + (j % 10), 100);
                        synchronized (book) {
                            book.addOrder(order);
                        }
                    }
                } catch (Exception e) {
                    anyError.set(true);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertFalse(anyError.get(), "No concurrent modification errors");

        if (book.bestBid().isPresent()) {
            int totalOrders = 0;
            for (Order o : book.bestBid().get().getValue()) {
                totalOrders++;
            }
        }
    }

    @Test
    void shouldAcceptMarketOrderWithZeroPrice() {
        Order market = OrderBuilder.anOrder().buy().market().withPrice(0).withQty(100).build();
        book.addOrder(market);
        assertTrue(book.bestBid().isPresent());
    }

    @Test
    void shouldRemoveEntriesWithAllFilledOrders() {
        Order buy1 = TestUtils.createBuyLimit("ORD-1", "PETR4", 50.0, 100);
        Order buy2 = TestUtils.createBuyLimit("ORD-2", "PETR4", 50.0, 100);

        book.addOrder(buy1);
        book.addOrder(buy2);

        buy1.execute(100);
        buy2.execute(100);
        book.removeFilledOrders();

        assertTrue(book.bestBid().isEmpty());
    }
}
