package com.exchange.v1.test;

import com.exchange.v1.model.Order;
import quickfix.SessionID;

public class OrderBuilder {
    private String clOrdID = "ORD-123";
    private String symbol = "PETR4";
    private Order.Side side = Order.Side.BUY;
    private Order.Type type = Order.Type.LIMIT;
    private double price = 100.0;
    private double qty = 100;
    private SessionID sessionID = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    public OrderBuilder withSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public OrderBuilder withSide(Order.Side side) {
        this.side = side;
        return this;
    }

    public OrderBuilder buy() {
        this.side = Order.Side.BUY;
        return this;
    }

    public OrderBuilder sell() {
        this.side = Order.Side.SELL;
        return this;
    }

    public OrderBuilder limit() {
        this.type = Order.Type.LIMIT;
        return this;
    }

    public OrderBuilder market() {
        this.type = Order.Type.MARKET;
        return this;
    }

    public OrderBuilder withType(Order.Type type) {
        this.type = type;
        return this;
    }

    public OrderBuilder withPrice(double price) {
        this.price = price;
        return this;
    }

    public OrderBuilder withQty(double qty) {
        this.qty = qty;
        return this;
    }

    public OrderBuilder withSessionID(SessionID sessionID) {
        this.sessionID = sessionID;
        return this;
    }

    public Order build() {
        return new Order(clOrdID, symbol, side, type, price, qty, sessionID);
    }
}
