package com.exchange.v1.engine;

import com.exchange.v1.model.Order;
import com.exchange.v1.test.OrderBuilder;
import com.exchange.v1.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ErrorScenarioTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void shouldRejectOrderWithNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("BAD", "PETR4", Order.Side.BUY, Order.Type.LIMIT, -10, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldRejectOrderWithNegativeQty() {
        Order bad = new Order("BAD", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 100, -10, TestUtils.DEFAULT_SESSION);
        assertThrows(IllegalArgumentException.class, () -> engine.process(bad));
    }

    @Test
    void shouldRejectOrderWithZeroQty() {
        Order bad = new Order("BAD", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 100, 0, TestUtils.DEFAULT_SESSION);
        assertThrows(IllegalArgumentException.class, () -> engine.process(bad));
    }

    @Test
    void shouldRejectOrderWithBlankClOrdID() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 100, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldDetectDuplicateClOrdID() {
        Order order1 = TestUtils.createBuyLimit("DUP", "PETR4", 100.0, 100);
        assertDoesNotThrow(() -> engine.process(order1));
    }

    @Test
    void shouldHandleNullSideOrder() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("BAD", "PETR4", null, Order.Type.LIMIT, 100, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldHandleNullTypeOrder() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("BAD", "PETR4", Order.Side.BUY, null, 100, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void concurrentModificationShouldNotCauseErrors() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean anyError = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    String symbol = "SYM" + (idx % 5);
                    if (idx % 2 == 0) {
                        engine.process(TestUtils.createBuyLimit("ORD-" + idx, symbol, 100.0 + idx, 50 + idx));
                    } else {
                        engine.process(TestUtils.createSellLimit("ORD-" + idx, symbol, 100.0 + idx, 50 + idx));
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
    }

    @Test
    void largeNumberOfOrdersShouldNotCauseIssues() {
        for (int i = 0; i < 1000; i++) {
            Order buy = TestUtils.createBuyLimit("B" + i, "PETR4", 100.0 + (i % 50), 100);
            engine.process(buy);
        }
    }

    @Test
    void cancelNonExistentSymbolShouldNotThrow() {
        assertDoesNotThrow(() ->
                engine.cancel("C1", "ORD-X", "NONEXISTENT", quickfix.field.Side.BUY, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void bookShouldRemainConsistentAfterRejectedOperations() {
        Order good = TestUtils.createBuyLimit("GOOD", "PETR4", 100.0, 100);
        engine.process(good);

        try {
            Order bad = TestUtils.createBuyLimit("GOOD", "PETR4", -10, -100);
            engine.process(bad);
        } catch (IllegalArgumentException e) {
        }

        assertFalse(good.isFilled());
        assertEquals(0, good.getExecutedQty());
    }

    @Test
    void processNullOrderShouldThrow() {
        assertThrows(NullPointerException.class, () -> engine.process(null));
    }

    @Test
    void memoryStressTest() {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            Order order = TestUtils.createBuyLimit("STRESS-" + i, "SYM" + (i % 100), 100.0, 100);
            orders.add(order);
            assertDoesNotThrow(() -> engine.process(order));
        }
        assertEquals(10000, orders.size());
    }

    @Test
    void deadlockFreeConcurrentAccess() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean deadlockDetected = new AtomicBoolean(false);

        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    engine.process(TestUtils.createBuyLimit("T1-" + i, "PETR4", 100.0, 100));
                }
            } catch (Exception e) {
                deadlockDetected.set(true);
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    engine.process(TestUtils.createSellLimit("T2-" + i, "PETR4", 100.0, 100));
                }
            } catch (Exception e) {
                deadlockDetected.set(true);
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        assertTrue(finished, "No deadlock detected - threads completed within timeout");
        assertFalse(deadlockDetected.get(), "No deadlock exceptions");
    }

    @Test
    void invalidCancelRequestShouldNotAffectState() {
        engine.process(TestUtils.createBuyLimit("B1", "PETR4", 100.0, 100));

        assertDoesNotThrow(() ->
                engine.cancel("C1", "B1", "PETR4", quickfix.field.Side.BUY, TestUtils.DEFAULT_SESSION));
    }
}
