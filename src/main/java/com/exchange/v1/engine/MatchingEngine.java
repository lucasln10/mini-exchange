package com.exchange.v1.engine;

import com.exchange.v1.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Motor de casamento de ordens.
 *
 * Regra: price-time priority
 *   1. Melhor preço casa primeiro
 *   2. Mesmo preço → FIFO (quem chegou antes)
 *
 * Uma ordem de COMPRA casa com uma de VENDA quando:
 *   preço do comprador >= preço do vendedor
 */
@Component
public class MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

    // Um OrderBook por ativo — chave é o symbol (ex: "PETR4")
    private final ConcurrentHashMap<String, OrderBook> books = new ConcurrentHashMap<>();

    // Contadores para IDs únicos
    private final AtomicLong orderIdCounter = new AtomicLong(1);
    private final AtomicLong execIdCounter  = new AtomicLong(1);

    /**
     * Ponto de entrada principal.
     * Toda ordem nova passa por aqui.
     */
    public synchronized void process(Order incoming) {
        log.info("[ENGINE] Processando ordem: {} {} {} @ R${} | Qty: {}",
                incoming.getSide(), incoming.getSymbol(),
                incoming.getClOrdID(), incoming.getPrice(), incoming.getQty());

        // Pega ou cria o livro do ativo
        OrderBook book = books.computeIfAbsent(
                incoming.getSymbol(), OrderBook::new);

        // Tenta casar a ordem com o livro
        match(incoming, book);

        // Se ainda tem quantidade em aberto, adiciona no livro
        if (!incoming.isFilled()) {
            book.addOrder(incoming);
            log.info("[ENGINE] Ordem {} adicionada ao livro. Restante: {}",
                    incoming.getClOrdID(), incoming.remainingQty());

            // Envia ExecutionReport NEW para o cliente
            sendExecutionReport(incoming, 0, 0, OrdStatus.NEW, ExecType.NEW);
        }

        // Limpa ordens preenchidas e imprime o livro
        book.removeFilledOrders();
        book.print();
    }

    /**
     * Lógica de casamento — price-time priority.
     */
    private void match(Order incoming, OrderBook book) {
        // Define contra qual lado do livro vamos casar
        // BUY bate contra ASKS (vendas)
        // SELL bate contra BIDS (compras)
        boolean isBuy = incoming.getSide() == Order.Side.BUY;

        while (!incoming.isFilled()) {
            // Pega a melhor oferta do lado oposto
            var bestEntry = isBuy ? book.bestAsk() : book.bestBid();

            if (bestEntry.isEmpty()) {
                break; // Sem contrapartes disponíveis
            }

            Map.Entry<Double, LinkedList<Order>> entry = bestEntry.get();
            double counterPrice = entry.getKey();

            // Verifica se o preço casa
            // Compra: preço do comprador >= preço do vendedor
            // Venda:  preço do vendedor  <= preço do comprador
            boolean priceMatches = isBuy
                    ? incoming.getPrice() >= counterPrice
                    : incoming.getPrice() <= counterPrice;

            // Ordens a mercado sempre casam (sem limite de preço)
            if (incoming.getType() == Order.Type.MARKET) {
                priceMatches = true;
            }

            if (!priceMatches) {
                break; // Melhor preço disponível não satisfaz a ordem
            }

            if (entry.getValue().isEmpty()) {
                book.removeFilledOrders();
                continue;
            }

            // FIFO — pega a primeira ordem da fila nesse preço
            Order resting = entry.getValue().peek();
            if (resting == null) {
                book.removeFilledOrders();
                continue;
            }

            // Quantidade que será executada = mínimo entre as duas ordens
            double execQty = Math.min(incoming.remainingQty(), resting.remainingQty());
            double execPrice = counterPrice; // Preço da ordem que estava no livro

            log.info("[ENGINE] MATCH! {} x {} | Qty: {} @ R${}",
                    incoming.getClOrdID(), resting.getClOrdID(), execQty, execPrice);

            // Registra execução nos dois lados
            incoming.execute(execQty);
            resting.execute(execQty);

            // Envia ExecutionReport para os DOIS lados
            char incomingStatus = incoming.isFilled() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
            char restingStatus  = resting.isFilled()  ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;

            sendExecutionReport(incoming, execQty, execPrice, incomingStatus, ExecType.FILL);
            sendExecutionReport(resting,  execQty, execPrice, restingStatus,  ExecType.FILL);

            // Remove a ordem do livro se foi totalmente executada
            if (resting.isFilled()) {
                entry.getValue().poll(); // Remove o primeiro da fila (FIFO)
                if (entry.getValue().isEmpty()) {
                    book.removeFilledOrders();
                }
            }
        }
    }

    /**
     * Monta e envia ExecutionReport via FIX para o cliente correto.
     */
    private void sendExecutionReport(Order order, double execQty,
                                     double execPrice, char ordStatus, char execType) {
        try {
            ExecutionReport report = new ExecutionReport(
                    new OrderID("ORD-" + orderIdCounter.getAndIncrement()),
                    new ExecID("EXEC-" + execIdCounter.getAndIncrement()),
                    new ExecTransType(ExecTransType.NEW),
                    new ExecType(execType),
                    new OrdStatus(ordStatus),
                    new Symbol(order.getSymbol()),
                    new Side(order.getSide() == Order.Side.BUY ? Side.BUY : Side.SELL),
                    new LeavesQty(order.remainingQty()),
                    new CumQty(order.getExecutedQty()),
                    new AvgPx(execPrice)
            );

            report.set(new ClOrdID(order.getClOrdID()));

            if (execQty > 0) {
                report.set(new LastShares(execQty));   // Tag 32 — qtd executada neste fill
                report.set(new LastPx(execPrice));     // Tag 31 — preço deste fill
            }

            // Envia para a sessão correta do cliente
            Session.sendToTarget(report, order.getSessionID());

            log.info("[ENGINE] ExecutionReport enviado → {} | Status: {} | Fill: {}@{}",
                    order.getClOrdID(), ordStatus, execQty, execPrice);

        } catch (SessionNotFound e) {
            log.error("[ENGINE] Sessão não encontrada para {}: {}", order.getClOrdID(), e.getMessage());
        }
    }

    /**
     * Processa um pedido de cancelamento.
     *
     * Busca a ordem no livro pelo origClOrdID.
     * Se encontrar → cancela, envia ExecutionReport Canceled (39=4)
     * Se não encontrar → envia OrderCancelReject (39=8)
     */
    public synchronized void cancel(String clOrdID, String origClOrdID,
                                    String symbol, char side, SessionID sessionID) {

        OrderBook book = books.get(symbol);

        if (book == null) {
            log.warn("[ENGINE] Cancel rejeitado — livro não encontrado para {}", symbol);
            sendCancelReject(clOrdID, origClOrdID, sessionID,
                    CxlRejReason.UNKNOWN_ORDER, "Livro não encontrado para " + symbol);
            return;
        }

        // Busca a ordem no lado correto do livro
        Order.Side orderSide = (side == quickfix.field.Side.BUY)
                ? Order.Side.BUY : Order.Side.SELL;

        Order found = book.findOrder(origClOrdID, orderSide);

        if (found == null) {
            log.warn("[ENGINE] Cancel rejeitado — ordem não encontrada: {}", origClOrdID);
            sendCancelReject(clOrdID, origClOrdID, sessionID,
                    CxlRejReason.UNKNOWN_ORDER, "Ordem não encontrada: " + origClOrdID);
            return;
        }

        // Remove do livro e marca como cancelada
        book.removeOrder(origClOrdID, orderSide);

        log.info("[ENGINE] Ordem {} cancelada com sucesso", origClOrdID);

        // Envia ExecutionReport Canceled para o cliente
        sendExecutionReport(found, 0, 0, OrdStatus.CANCELED, ExecType.CANCELED);

        book.print();
    }

    /**
     * Envia OrderCancelReject (35=9) quando não é possível cancelar.
     */
    private void sendCancelReject(String clOrdID, String origClOrdID,
                                  SessionID sessionID, int reason, String text) {
        try {
            quickfix.fix42.OrderCancelReject reject = new quickfix.fix42.OrderCancelReject(
                    new OrderID("NONE"),                          // Tag 37
                    new ClOrdID(clOrdID),                        // Tag 11 — ID do cancel request
                    new OrigClOrdID(origClOrdID),                // Tag 41 — ID da ordem original
                    new OrdStatus(OrdStatus.REJECTED),           // Tag 39
                    new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST) // Tag 434
            );

            reject.set(new Text(text));                      // Tag 58 — motivo legível
            reject.set(new CxlRejReason(reason));            // Tag 102 — código do motivo

            Session.sendToTarget(reject, sessionID);
            log.info("[ENGINE] OrderCancelReject enviado → clOrdID={} motivo={}", clOrdID, text);

        } catch (SessionNotFound e) {
            log.error("[ENGINE] Sessão não encontrada ao rejeitar cancel: {}", e.getMessage());
        }
    }
}