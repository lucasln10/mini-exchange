package com.exchange.v1.model;

import com.exchange.v1.test.OrderBuilder;
import org.junit.jupiter.api.Test;
import quickfix.SessionID;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private static final SessionID SESSION = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");

    @Test
    void shouldCreateOrderWithValidFields() {
        Order order = new Order("ORD-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION);

        assertEquals("ORD-1", order.getClOrdID());
        assertEquals("PETR4", order.getSymbol());
        assertEquals(Order.Side.BUY, order.getSide());
        assertEquals(Order.Type.LIMIT, order.getType());
        assertEquals(25.50, order.getPrice());
        assertEquals(100, order.getQty());
        assertEquals(0, order.getExecutedQty());
        assertEquals(Order.Status.NEW, order.getStatus());
        assertEquals(100, order.remainingQty());
        assertFalse(order.isFilled());
    }

    @Test
    void shouldRejectNullClOrdID() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order(null, "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION));
    }

    @Test
    void shouldRejectBlankClOrdID() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION));
    }

    @Test
    void shouldRejectNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD-1", null, Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION));
    }

    @Test
    void shouldRejectBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD-1", "", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION));
    }

    @Test
    void shouldRejectNullSide() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD-1", "PETR4", null, Order.Type.LIMIT, 25.50, 100, SESSION));
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD-1", "PETR4", Order.Side.BUY, null, 25.50, 100, SESSION));
    }

    @Test
    void remainingQtyShouldEqualQtyForNewOrder() {
        Order order = new Order("ORD-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION);
        assertEquals(100, order.remainingQty());
        assertFalse(order.isFilled());
    }

    @Test
    void remainingQtyShouldDecreaseAfterPartialExecution() {
        Order order = new Order("ORD-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION);
        order.execute(30);
        assertEquals(70, order.remainingQty());
        assertEquals(Order.Status.PARTIAL, order.getStatus());
        assertFalse(order.isFilled());
    }

    @Test
    void remainingQtyShouldBeZeroAfterFullExecution() {
        Order order = new Order("ORD-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION);
        order.execute(100);
        assertEquals(0, order.remainingQty());
        assertTrue(order.isFilled());
        assertEquals(Order.Status.FILLED, order.getStatus());
    }

    @Test
    void shouldHandleExcessExecution() {
        Order order = new Order("ORD-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION);
        order.execute(150);
        assertEquals(-50, order.remainingQty());
        assertTrue(order.isFilled());
    }

    @Test
    void multiplePartialExecutions() {
        Order order = new Order("ORD-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, SESSION);
        order.execute(20);
        assertEquals(80, order.remainingQty());
        assertEquals(Order.Status.PARTIAL, order.getStatus());
        order.execute(30);
        assertEquals(50, order.remainingQty());
        order.execute(50);
        assertEquals(0, order.remainingQty());
        assertTrue(order.isFilled());
    }

    @Test
    void shouldCreateOrderWithBuilder() {
        Order order = OrderBuilder.anOrder()
                .withClOrdID("ORD-BUILDER")
                .withSymbol("VALE3")
                .buy()
                .limit()
                .withPrice(68.10)
                .withQty(500)
                .build();

        assertEquals("ORD-BUILDER", order.getClOrdID());
        assertEquals("VALE3", order.getSymbol());
        assertEquals(Order.Side.BUY, order.getSide());
        assertEquals(68.10, order.getPrice());
        assertEquals(500, order.getQty());
    }

    @Test
    void concurrentOrderExecutions() throws InterruptedException {
        Order order = new Order("ORD-CONC", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 1000, SESSION);
        int threadCount = 10;
        int execPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            int threadIdx = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < execPerThread; j++) {
                        synchronized (order) {
                            if (!order.isFilled()) {
                                order.execute(10);
                            }
                        }
                    }
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertNull(error.get(), "No exception should occur during concurrent execution");
        assertEquals(1000, order.getExecutedQty());
        assertTrue(order.isFilled());
    }
}
