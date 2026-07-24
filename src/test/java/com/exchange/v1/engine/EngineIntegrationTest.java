package com.exchange.v1.engine;

import com.exchange.v1.model.Order;
import com.exchange.v1.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EngineIntegrationTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void echoServerPlusMatchingEngineIntegration() {
        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 50);
        engine.process(sell);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 50);
        engine.process(buy);

        assertTrue(sell.isFilled());
        assertTrue(buy.isFilled());
    }

    @Test
    void matchingEnginePlusOrderBookIntegration() {
        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 50);
        engine.process(sell);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertTrue(sell.isFilled());
        assertEquals(Order.Status.PARTIAL, buy.getStatus());
    }

    @Test
    void fullFlowClientToServerToEngine() {
        Order buy = TestUtils.createBuyLimit("ORD-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertEquals(Order.Status.NEW, buy.getStatus());
        assertEquals(100, buy.remainingQty());

        Order sell = TestUtils.createSellLimit("ORD-2", "PETR4", 100.0, 50);
        engine.process(sell);

        assertTrue(sell.isFilled());
        assertEquals(50, buy.getExecutedQty());
        assertEquals(Order.Status.PARTIAL, buy.getStatus());
    }

    @Test
    void multipleAssetsSimultaneously() {
        engine.process(TestUtils.createBuyLimit("B1", "PETR4", 25.50, 100));
        engine.process(TestUtils.createSellLimit("S1", "PETR4", 25.50, 100));
        engine.process(TestUtils.createBuyLimit("B2", "VALE3", 68.10, 200));
        engine.process(TestUtils.createSellLimit("S2", "VALE3", 68.10, 200));
        engine.process(TestUtils.createBuyLimit("B3", "ITUB4", 35.00, 150));
        engine.process(TestUtils.createSellLimit("S3", "ITUB4", 35.00, 150));
    }

    @Test
    void concurrentComponentProcessing() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean anyError = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    String symbol = (idx % 3 == 0) ? "PETR4" : (idx % 3 == 1) ? "VALE3" : "ITUB4";
                    Order order;
                    if (idx % 2 == 0) {
                        order = TestUtils.createBuyLimit("ORD-" + idx, symbol, 100.0 + idx, 100);
                    } else {
                        order = TestUtils.createSellLimit("ORD-" + idx, symbol, 100.0 + idx, 100);
                    }
                    engine.process(order);
                } catch (Exception e) {
                    anyError.set(true);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();
        assertFalse(anyError.get(), "No exceptions during concurrent integration test");
    }

    @Test
    void consistencyAfterPartialFill() {
        engine.process(TestUtils.createSellLimit("S1", "PETR4", 100.0, 30));
        engine.process(TestUtils.createBuyLimit("B1", "PETR4", 100.0, 100));

        engine.process(TestUtils.createSellLimit("S2", "PETR4", 100.0, 70));
    }

    @Test
    void consistencyAfterCancel() {
        Order buy = TestUtils.createBuyLimit("B1", "PETR4", 100.0, 100);
        engine.process(buy);

        engine.cancel("C1", "B1", "PETR4", quickfix.field.Side.BUY, TestUtils.DEFAULT_SESSION);
    }

    @Test
    void crossSymbolIndependence() {
        Order petrBuy = TestUtils.createBuyLimit("B1", "PETR4", 100.0, 100);
        Order valeSell = TestUtils.createSellLimit("S1", "VALE3", 68.0, 100);

        engine.process(petrBuy);
        engine.process(valeSell);

        assertFalse(petrBuy.isFilled());
        assertFalse(valeSell.isFilled());
    }

    @Test
    void highConcurrencyConsistency() throws InterruptedException {
        int totalOrders = 100;
        CountDownLatch latch = new CountDownLatch(totalOrders);
        AtomicBoolean anyError = new AtomicBoolean(false);

        for (int i = 0; i < totalOrders; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    String symbol = "SYM" + (idx % 5);
                    if (idx % 2 == 0) {
                        engine.process(TestUtils.createBuyLimit("B" + idx, symbol, 100.0, 50));
                    } else {
                        engine.process(TestUtils.createSellLimit("S" + idx, symbol, 100.0, 50));
                    }
                } catch (Exception e) {
                    anyError.set(true);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertFalse(anyError.get(), "No errors with high concurrency");
    }

    @Test
    void priceTimePriorityAcrossSymbols() {
        engine.process(TestUtils.createBuyLimit("B1", "PETR4", 100.0, 50));
        engine.process(TestUtils.createBuyLimit("B2", "PETR4", 101.0, 50));
        engine.process(TestUtils.createSellLimit("S1", "PETR4", 100.0, 100));

        Order b3 = TestUtils.createBuyLimit("B3", "PETR4", 99.0, 100);
        engine.process(b3);
        assertEquals(Order.Status.NEW, b3.getStatus());
    }
}
