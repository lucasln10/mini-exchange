package com.exchange.v1.engine;

import com.exchange.v1.model.Order;

import java.util.*;

/**
 * Livro de ofertas para um único ativo (ex: PETR4).
 *
 * BIDS (compras) → ordenados do MAIOR preço para o menor
 *   Quem paga mais tem prioridade de preço.
 *   Mesmo preço → FIFO (quem chegou primeiro)
 *
 * ASKS (vendas) → ordenados do MENOR preço para o maior
 *   Quem vende mais barato tem prioridade de preço.
 *   Mesmo preço → FIFO (quem chegou primeiro)
 */
public class OrderBook {

    private final String symbol;

    // Bids: maior preço primeiro → dentro do mesmo preço, FIFO (LinkedList)
    private final TreeMap<Double, LinkedList<Order>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    // Asks: menor preço primeiro → dentro do mesmo preço, FIFO (LinkedList)
    private final TreeMap<Double, LinkedList<Order>> asks =
            new TreeMap<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    // Adiciona ordem no lado correto do livro
    public void addOrder(Order order) {
        if (order.getSide() == Order.Side.BUY) {
            bids.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        } else {
            asks.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        }
    }

    // Remove ordens totalmente executadas do livro
    public void removeFilledOrders() {
        removeFilledFrom(bids);
        removeFilledFrom(asks);
    }

    private void removeFilledFrom(TreeMap<Double, LinkedList<Order>> side) {
        side.forEach((price, orders) -> orders.removeIf(Order::isFilled));
        side.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    // Retorna o melhor bid (maior compra disponível)
    public Optional<Map.Entry<Double, LinkedList<Order>>> bestBid() {
        return bids.isEmpty() ? Optional.empty() : Optional.of(bids.firstEntry());
    }

    // Retorna o melhor ask (menor venda disponível)
    public Optional<Map.Entry<Double, LinkedList<Order>>> bestAsk() {
        return asks.isEmpty() ? Optional.empty() : Optional.of(asks.firstEntry());
    }

    public String getSymbol() { return symbol; }

    // Imprime o estado atual do livro (útil para debug)
    public void print() {
        System.out.println("\n========= ORDER BOOK: " + symbol + " =========");
        System.out.println("--- ASKS (vendas) ---");
        asks.forEach((price, orders) ->
                orders.forEach(o -> System.out.printf("  R$%.2f | %.0f unidades | %s%n",
                        price, o.remainingQty(), o.getClOrdID())));
        System.out.println("--- BIDS (compras) ---");
        bids.forEach((price, orders) ->
                orders.forEach(o -> System.out.printf("  R$%.2f | %.0f unidades | %s%n",
                        price, o.remainingQty(), o.getClOrdID())));
        System.out.println("==========================================\n");
    }

    /**
     * Busca uma ordem no livro pelo ClOrdID e lado.
     */
    public Order findOrder(String clOrdID, Order.Side side) {
        TreeMap<Double, LinkedList<Order>> book = (side == Order.Side.BUY) ? bids : asks;

        return book.values().stream()
                .flatMap(LinkedList::stream)
                .filter(o -> o.getClOrdID().equals(clOrdID))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove uma ordem do livro pelo ClOrdID e lado.
     */
    public void removeOrder(String clOrdID, Order.Side side) {
        TreeMap<Double, LinkedList<Order>> book = (side == Order.Side.BUY) ? bids : asks;

        book.forEach((price, orders) ->
                orders.removeIf(o -> o.getClOrdID().equals(clOrdID)));

        // Remove entradas vazias
        book.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}