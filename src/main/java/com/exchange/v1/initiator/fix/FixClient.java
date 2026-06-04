package com.exchange.v1.initiator.fix;

import com.exchange.v1.engine.MatchingEngine;
import com.exchange.v1.model.Order;
import org.springframework.stereotype.Component;
import quickfix.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;

import java.time.LocalDateTime;

/**
 * FixClient implementa Application como Initiator.
 * Ele INICIA a conexão com o servidor e envia ordens.
 */
@Component
public class FixClient implements Application {

    private static final Logger log = LoggerFactory.getLogger(FixClient.class);

    private SessionID sessionID;

    private final MatchingEngine matchingEngine;

    public FixClient(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    // ================================================================
    // CALLBACKS DE SESSÃO
    // ================================================================

    @Override
    public void onCreate(SessionID sessionID) {
        log.info("[CLIENT] Sessão criada: {}", sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        this.sessionID = sessionID;
        log.info("[CLIENT] Logon aceito! Enviando ordens de teste...");

        // ─── CENÁRIO 1: Partial Fill ───────────────────────────────────
        // Venda de 100 unidades entra no livro
        sendOrder("PETR4", Side.SELL, OrdType.LIMIT, 100, 99.00);

        sleep(100);

        // Compra de 300 — só 100 disponíveis → Partial Fill (39=1)
        // 200 restantes ficam no livro aguardando
        sendOrder("PETR4", Side.BUY, OrdType.LIMIT, 300, 99.00);
        sleep(100);

        // Nova venda de 200 — casa com os 200 restantes → Filled (39=2)
        sendOrder("PETR4", Side.SELL, OrdType.LIMIT, 200, 99.00);
        sleep(100);

        // ─── CENÁRIO 2: Cancel ────────────────────────────────────────
        // Envia uma ordem que vai ficar no livro sem contraparte
        sendOrder("VALE3", Side.BUY, OrdType.LIMIT, 500, 75.00);
        sleep(100);

        // Cancela ela logo em seguida
        sendCancelOrder("ORD-VALE3-CANCEL", lastClOrdID, "VALE3", Side.BUY);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        this.sessionID = null;
        log.info("[CLIENT] Desconectado do servidor.");
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        // Aqui você poderia adicionar senha no Logon se necessário
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) {
        // Heartbeats e mensagens administrativas chegam aqui
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        log.info("[CLIENT] Enviando ordem: {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // Respostas do servidor chegam aqui
        crack(message, sessionID);
    }

    // ================================================================
    // ROTEADOR — processa respostas do servidor
    // ================================================================
    private final MessageCracker cracker = new MessageCracker() {

        @Handler
        public void onExecutionReport(ExecutionReport report, SessionID sessionID)
                throws FieldNotFound {
            String clOrdID = report.getClOrdID().getValue();
            String orderID = report.getOrderID().getValue();
            char   status  = report.getOrdStatus().getValue();
            double cumQty  = report.getCumQty().getValue();
            double avgPx   = report.getAvgPx().getValue();

            log.info("[CLIENT] ExecutionReport recebido!");
            log.info("[CLIENT]   ClOrdID : {}", clOrdID);
            log.info("[CLIENT]   OrderID : {}", orderID);
            log.info("[CLIENT]   Status  : {}", describeStatus(status));
            log.info("[CLIENT]   CumQty  : {}", cumQty);
            log.info("[CLIENT]   AvgPx   : {}", avgPx);
        }

        @Handler
        public void onOrderCancelReject(
                quickfix.fix42.OrderCancelReject reject, SessionID sessionID)
                throws FieldNotFound {

            String clOrdID     = reject.getClOrdID().getValue();
            String origClOrdID = reject.getOrigClOrdID().getValue();

            log.warn("[CLIENT] CancelReject recebido! clOrdID={} origClOrdID={}",
                    clOrdID, origClOrdID);

            // Tenta ler o motivo se existir
            try {
                String text = reject.getText().getValue();
                log.warn("[CLIENT]   Motivo: {}", text);
            } catch (FieldNotFound ignored) {}
        }
    };

    private void crack(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        cracker.crack(message, sessionID);
    }

    // ================================================================
    // ENVIA UMA ORDEM
    // ================================================================


    // Guarda o último ClOrdID enviado para usar no cancel
    private String lastClOrdID;

    public void sendOrder(String symbol, char side, char ordType, double qty, double price) {
        try {
            lastClOrdID = "ORD-" + System.currentTimeMillis();

            NewOrderSingle order = new NewOrderSingle(
                    new ClOrdID(lastClOrdID),
                    new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK),
                    new Symbol(symbol),
                    new Side(side),
                    new TransactTime(LocalDateTime.now()),
                    new OrdType(ordType)
            );

            order.set(new OrderQty(qty));

            if (ordType == OrdType.LIMIT) {
                order.set(new Price(price));
            }

            Session.sendToTarget(order, sessionID);
            log.info("[CLIENT] Ordem enviada: {} {} @ R${} Qty:{}",
                    side == Side.BUY ? "COMPRA" : "VENDA", symbol, price, qty);

        } catch (SessionNotFound e) {
            log.error("[CLIENT] Sessão não encontrada: {}", e.getMessage());
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    public void sendCancelOrder(String clOrdID, String origClOrdID,
                                String symbol, char side) {
        try {
            quickfix.fix42.OrderCancelRequest cancel = new quickfix.fix42.OrderCancelRequest(
                    new OrigClOrdID(origClOrdID),          // Tag 41 — ClOrdID da ordem original
                    new ClOrdID(clOrdID),                  // Tag 11 — ID desta requisição de cancel
                    new Symbol(symbol),                    // Tag 55 — ativo
                    new Side(side),                        // Tag 54 — lado
                    new TransactTime(LocalDateTime.now())  // Tag 60 — horário
            );

            Session.sendToTarget(cancel, sessionID);
            log.info("[CLIENT] CancelRequest enviado para origClOrdID={}", origClOrdID);

        } catch (SessionNotFound e) {
            log.error("[CLIENT] Sessão não encontrada: {}", e.getMessage());
        }
    }

    // ================================================================
    // HELPER
    // ================================================================
    private String describeStatus(char status) {
        return switch (status) {
            case OrdStatus.NEW              -> "New (0) — Ordem recebida";
            case OrdStatus.PARTIALLY_FILLED -> "Partial Fill (1) — Parcialmente executada";
            case OrdStatus.FILLED           -> "Filled (2) — Totalmente executada";
            case OrdStatus.CANCELED         -> "Canceled (4) — Cancelada";
            case OrdStatus.REJECTED         -> "Rejected (8) — Rejeitada";
            default                         -> "Desconhecido (" + status + ")";
        };
    }
}

