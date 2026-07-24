package com.exchange.v1.engine;

import com.exchange.v1.model.Order;
import com.exchange.v1.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import quickfix.Session;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void shouldMatchBuyAndSellExactly() {
        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 100);
        engine.process(sell);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertTrue(sell.isFilled());
        assertEquals(Order.Status.FILLED, sell.getStatus());
        assertEquals(100, sell.getExecutedQty());

        assertTrue(buy.isFilled());
        assertEquals(Order.Status.FILLED, buy.getStatus());
        assertEquals(100, buy.getExecutedQty());
    }

    @Test
    void shouldPartiallyMatchWhenIncomingOrderIsLarger() {
        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 50);
        engine.process(sell);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertTrue(sell.isFilled());
        assertEquals(50, sell.getExecutedQty());

        assertEquals(Order.Status.PARTIAL, buy.getStatus());
        assertEquals(50, buy.getExecutedQty());
        assertEquals(50, buy.remainingQty());
    }

    @Test
    void shouldNotMatchWhenBuyPriceIsLowerThanBestAsk() {
        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 100);
        engine.process(sell);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 99.0, 100);
        engine.process(buy);

        assertFalse(sell.isFilled());
        assertEquals(0, sell.getExecutedQty());

        assertFalse(buy.isFilled());
        assertEquals(0, buy.getExecutedQty());
    }

    @Test
    void shouldNotMatchWhenSellPriceIsHigherThanBestBid() {
        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 101.0, 100);
        engine.process(sell);

        assertFalse(buy.isFilled());
        assertEquals(0, buy.getExecutedQty());
    }

    @Test
    void shouldMatchMarketOrder() {
        Order sell = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 100);
        engine.process(sell);

        Order buy = TestUtils.createBuyMarket("BUY-1", "PETR4", 100);
        engine.process(buy);

        assertTrue(sell.isFilled());
        assertTrue(buy.isFilled());
    }

    @Test
    void shouldMatchFirstInFirstOutForSamePrice() {
        Order sell1 = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 50);
        Order sell2 = TestUtils.createSellLimit("SELL-2", "PETR4", 100.0, 50);
        engine.process(sell1);
        engine.process(sell2);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertTrue(sell1.isFilled());
        assertEquals(50, sell1.getExecutedQty());
        assertTrue(sell2.isFilled());
        assertEquals(50, sell2.getExecutedQty());
    }

    @Test
    void shouldMatchMultipleOrdersUntilFilled() {
        Order sell1 = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 30);
        Order sell2 = TestUtils.createSellLimit("SELL-2", "PETR4", 101.0, 40);
        Order sell3 = TestUtils.createSellLimit("SELL-3", "PETR4", 102.0, 50);
        engine.process(sell1);
        engine.process(sell2);
        engine.process(sell3);

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 101.0, 100);
        engine.process(buy);

        assertEquals(Order.Status.PARTIAL, buy.getStatus());
        assertEquals(70, buy.getExecutedQty());
        assertEquals(30, buy.remainingQty());
        assertTrue(sell1.isFilled());
    }

    @Test
    void shouldProcessCancelOfExistingOrder() {
        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertDoesNotThrow(() ->
                engine.cancel("CANCEL-1", "BUY-1", "PETR4", quickfix.field.Side.BUY, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldHandleCancelForNonExistentOrder() {
        assertDoesNotThrow(() ->
                engine.cancel("CANCEL-1", "NONEXISTENT", "PETR4", quickfix.field.Side.BUY, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldHandleMultipleSymbols() {
        Order buyPetr4 = TestUtils.createBuyLimit("BUY-PETR4", "PETR4", 100.0, 100);
        Order sellPetr4 = TestUtils.createSellLimit("SELL-PETR4", "PETR4", 100.0, 100);
        engine.process(buyPetr4);
        engine.process(sellPetr4);

        Order buyVale3 = TestUtils.createBuyLimit("BUY-VALE3", "VALE3", 68.0, 200);
        Order sellVale3 = TestUtils.createSellLimit("SELL-VALE3", "VALE3", 68.0, 200);
        engine.process(buyVale3);
        engine.process(sellVale3);

        assertTrue(buyPetr4.isFilled());
        assertTrue(sellPetr4.isFilled());
        assertTrue(buyVale3.isFilled());
        assertTrue(sellVale3.isFilled());
    }

    @Test
    void concurrentOrderProcessing() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean anyError = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    if (idx % 2 == 0) {
                        Order sell = TestUtils.createSellLimit("SELL-" + idx, "PETR4", 100.0, 100);
                        engine.process(sell);
                    } else {
                        Order buy = TestUtils.createBuyLimit("BUY-" + idx, "PETR4", 100.0, 100);
                        engine.process(buy);
                    }
                } catch (Exception e) {
                    anyError.set(true);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertFalse(anyError.get(), "No exceptions during concurrent processing");
    }

    @Test
    void shouldRejectOrderWithInvalidPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("BUY-BAD", "PETR4", Order.Side.BUY, Order.Type.LIMIT, -10, 100, TestUtils.DEFAULT_SESSION));
    }

    @Test
    void shouldHandlePartialFillAcrossMultipleRestingOrders() {
        Order sell1 = TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 20);
        Order sell2 = TestUtils.createSellLimit("SELL-2", "PETR4", 100.0, 30);
        engine.process(sell1);
        engine.process(sell2);

        engine.process(TestUtils.createSellLimit("SELL-3", "PETR4", 101.0, 50));

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertEquals(Order.Status.PARTIAL, buy.getStatus());
        assertEquals(50, buy.getExecutedQty());
        assertEquals(50, buy.remainingQty());
        assertTrue(sell1.isFilled());
        assertTrue(sell2.isFilled());
    }

    @Test
    void shouldFillIncomingOrderAgainstMultipleRestingOrders() {
        engine.process(TestUtils.createSellLimit("SELL-1", "PETR4", 100.0, 30));
        engine.process(TestUtils.createSellLimit("SELL-2", "PETR4", 100.0, 30));
        engine.process(TestUtils.createSellLimit("SELL-3", "PETR4", 100.0, 40));

        Order buy = TestUtils.createBuyLimit("BUY-1", "PETR4", 100.0, 100);
        engine.process(buy);

        assertTrue(buy.isFilled());
        assertEquals(100, buy.getExecutedQty());
    }
}
