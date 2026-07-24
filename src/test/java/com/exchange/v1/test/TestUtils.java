package com.exchange.v1.test;

import com.exchange.v1.model.Order;
import quickfix.SessionID;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestUtils {

    public static final SessionID DEFAULT_SESSION = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");

    public static Order createBuyLimit(String clOrdID, String symbol, double price, double qty) {
        return new Order(clOrdID, symbol, Order.Side.BUY, Order.Type.LIMIT, price, qty, DEFAULT_SESSION);
    }

    public static Order createSellLimit(String clOrdID, String symbol, double price, double qty) {
        return new Order(clOrdID, symbol, Order.Side.SELL, Order.Type.LIMIT, price, qty, DEFAULT_SESSION);
    }

    public static Order createBuyMarket(String clOrdID, String symbol, double qty) {
        return new Order(clOrdID, symbol, Order.Side.BUY, Order.Type.MARKET, 0, qty, DEFAULT_SESSION);
    }

    public static Order createSellMarket(String clOrdID, String symbol, double qty) {
        return new Order(clOrdID, symbol, Order.Side.SELL, Order.Type.MARKET, 0, qty, DEFAULT_SESSION);
    }

    public static <T> T awaitAndGet(AtomicReference<T> ref, CountDownLatch latch, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new AssertionError("Timeout waiting for value");
        }
        return ref.get();
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
