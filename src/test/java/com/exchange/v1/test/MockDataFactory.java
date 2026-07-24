package com.exchange.v1.test;

import com.exchange.v1.model.Order;
import quickfix.SessionID;

import java.util.ArrayList;
import java.util.List;

public class MockDataFactory {

    public static List<Order> createRealisticOrders() {
        List<Order> orders = new ArrayList<>();
        SessionID session = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");

        orders.add(new Order("ORD-001", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 1000, session));
        orders.add(new Order("ORD-002", "PETR4", Order.Side.SELL, Order.Type.LIMIT, 25.60, 500, session));
        orders.add(new Order("ORD-003", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.40, 2000, session));
        orders.add(new Order("ORD-004", "VALE3", Order.Side.BUY, Order.Type.LIMIT, 68.10, 800, session));
        orders.add(new Order("ORD-005", "VALE3", Order.Side.SELL, Order.Type.LIMIT, 68.20, 300, session));
        orders.add(new Order("ORD-006", "ITUB4", Order.Side.BUY, Order.Type.MARKET, 0, 1500, session));

        return orders;
    }

    public static List<Order> createMatchingScenario() {
        List<Order> orders = new ArrayList<>();
        SessionID session = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");

        orders.add(new Order("BUY-LIMIT-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.50, 100, session));
        orders.add(new Order("SELL-LIMIT-1", "PETR4", Order.Side.SELL, Order.Type.LIMIT, 25.40, 100, session));
        orders.add(new Order("BUY-MARKET-1", "PETR4", Order.Side.BUY, Order.Type.MARKET, 0, 50, session));

        return orders;
    }

    public static Order aFilledOrder() {
        SessionID session = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");
        Order order = new Order("FILLED-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.00, 100, session);
        order.execute(100);
        return order;
    }

    public static Order aPartiallyFilledOrder() {
        SessionID session = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");
        Order order = new Order("PARTIAL-1", "PETR4", Order.Side.BUY, Order.Type.LIMIT, 25.00, 100, session);
        order.execute(40);
        return order;
    }
}
