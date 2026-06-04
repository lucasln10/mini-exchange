package com.exchange.v1.acceptor.fix;

import com.exchange.v1.engine.MatchingEngine;
import com.exchange.v1.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

@Component
public class EchoServer implements Application {

    private static final Logger log = LoggerFactory.getLogger(EchoServer.class);

    private final MatchingEngine matchingEngine;

    public EchoServer(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    @Override
    public void onCreate(SessionID sessionID) {
        log.info("[FIX] Sessão criada: {}", sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("[FIX] Cliente conectado: {}", sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.info("[FIX] Cliente desconectado: {}", sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {}

    @Override
    public void fromAdmin(Message message, SessionID sessionID) {}

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {}

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    private final MessageCracker cracker = new MessageCracker() {
        @Handler
        public void onNewOrderSingle(NewOrderSingle msg, SessionID sessionID)
                throws FieldNotFound {

            // Extrai campos da mensagem FIX
            String clOrdID = msg.getClOrdID().getValue();
            String symbol  = msg.getSymbol().getValue();
            char   side    = msg.getSide().getValue();
            double qty     = msg.getOrderQty().getValue();
            char   ordType = msg.getOrdType().getValue();

            // Preço — obrigatório para Limit, 0 para Market
            double price = 0;
            if (ordType == OrdType.LIMIT) {
                price = msg.getPrice().getValue();
            }

            log.info("[FIX] Nova ordem | {} {} {} @ R${} Qty:{}",
                    side == Side.BUY ? "COMPRA" : "VENDA",
                    symbol, clOrdID, price, qty);

            // Converte para nosso modelo interno
            Order order = new Order(
                    clOrdID,
                    symbol,
                    side == Side.BUY ? Order.Side.BUY : Order.Side.SELL,
                    ordType == OrdType.LIMIT ? Order.Type.LIMIT : Order.Type.MARKET,
                    price,
                    qty,
                    sessionID
            );

            // Delega para o MatchingEngine
            matchingEngine.process(order);
        }
    };

    private void crack(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        cracker.crack(message, sessionID);
    }
}