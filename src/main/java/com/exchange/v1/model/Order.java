package com.exchange.v1.model;

import quickfix.SessionID;

/**
 * Representa uma ordem dentro do sistema.
 * Guarda tudo que precisamos para processar e responder via FIX.
 */
public class Order {

    public enum Side { BUY, SELL }
    public enum Type { MARKET, LIMIT }
    public enum Status { NEW, PARTIAL, FILLED, CANCELED }

    private final String    clOrdID;    // ID original do cliente (Tag 11)
    private final String    symbol;     // Ativo (Tag 55)
    private final Side      side;       // Direção (Tag 54)
    private final Type      type;       // Tipo (Tag 40)
    private final double    price;      // Preço limite (Tag 44) — 0 se market
    private final double    qty;        // Quantidade total (Tag 38)
    private final SessionID sessionID;  // Para saber para quem responder

    private double executedQty = 0;     // Quantidade já executada
    private Status status = Status.NEW;

    public Order(String clOrdID, String symbol, Side side,
                 Type type, double price, double qty, SessionID sessionID) {
        if (clOrdID == null || clOrdID.isBlank()) {
            throw new IllegalArgumentException("clOrdID cannot be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol cannot be null or blank");
        }
        if (side == null) {
            throw new IllegalArgumentException("side cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (type == Type.LIMIT && price <= 0) {
            throw new IllegalArgumentException("price must be positive for LIMIT orders");
        }
        this.clOrdID   = clOrdID;
        this.symbol    = symbol;
        this.side      = side;
        this.type      = type;
        this.price     = price;
        this.qty       = qty;
        this.sessionID = sessionID;
    }

    // Quantidade ainda em aberto
    public double remainingQty() {
        return qty - executedQty;
    }

    // Registra uma execução parcial ou total
    public void execute(double executedAmount) {
        this.executedQty += executedAmount;
        this.status = (this.executedQty >= this.qty) ? Status.FILLED : Status.PARTIAL;
    }

    public boolean isFilled() {
        return status == Status.FILLED;
    }

    // Getters
    public String    getClOrdID()    { return clOrdID; }
    public String    getSymbol()     { return symbol; }
    public Side      getSide()       { return side; }
    public Type      getType()       { return type; }
    public double    getPrice()      { return price; }
    public double    getQty()        { return qty; }
    public double    getExecutedQty(){ return executedQty; }
    public SessionID getSessionID()  { return sessionID; }
    public Status    getStatus()     { return status; }
}