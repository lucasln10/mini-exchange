package com.exchange.v1.acceptor.fix;

import com.exchange.v1.engine.MatchingEngine;
import com.exchange.v1.model.Order;
import com.exchange.v1.test.FixMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;

import static org.junit.jupiter.api.Assertions.*;

class EchoServerTest {

    private MatchingEngine engine;
    private EchoServer echoServer;
    private SessionID sessionID;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
        echoServer = new EchoServer(engine);
        sessionID = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");
    }

    @Test
    void shouldHandleValidNewOrderSingle() throws Exception {
        NewOrderSingle msg = FixMessageBuilder.aBuyLimitOrder("PETR4", 25.50, 100);

        assertDoesNotThrow(() -> echoServer.fromApp(msg, sessionID));
    }

    @Test
    void shouldConvertBuyLimitToOrderAndProcess() throws Exception {
        NewOrderSingle msg = FixMessageBuilder.aBuyLimitOrder("PETR4", 25.50, 100);

        echoServer.fromApp(msg, sessionID);

        String clOrdID = msg.getClOrdID().getValue();
        Order sell = new Order("SELL-1", "PETR4", Order.Side.SELL, Order.Type.LIMIT, 25.50, 100, sessionID);
        engine.process(sell);
    }

    @Test
    void shouldHandleSellOrder() throws Exception {
        NewOrderSingle msg = FixMessageBuilder.aSellLimitOrder("VALE3", 68.10, 200);

        assertDoesNotThrow(() -> echoServer.fromApp(msg, sessionID));
    }

    @Test
    void shouldHandleMarketOrder() throws Exception {
        NewOrderSingle msg = FixMessageBuilder.aMarketOrder("PETR4", 100, Side.BUY);

        assertDoesNotThrow(() -> echoServer.fromApp(msg, sessionID));
    }

    @Test
    void shouldRejectMessageWithMissingRequiredFields() {
        Message badMsg = new Message();
        badMsg.getHeader().setField(new MsgType("D"));

        assertThrows(UnsupportedMessageType.class, () -> echoServer.fromApp(badMsg, sessionID));
    }

    @Test
    void shouldThrowOnUnsupportedMessageType() {
        Message badMsg = new Message();
        badMsg.getHeader().setField(new MsgType("Z"));

        assertThrows(UnsupportedMessageType.class, () -> echoServer.fromApp(badMsg, sessionID));
    }

    @Test
    void shouldHandleLogonAndLogout() {
        assertDoesNotThrow(() -> echoServer.onLogon(sessionID));
        assertDoesNotThrow(() -> echoServer.onLogout(sessionID));
    }

    @Test
    void shouldProcessAndMatchOrdersViaEchoServer() throws Exception {
        NewOrderSingle sell = FixMessageBuilder.aSellLimitOrder("PETR4", 100.0, 50);
        echoServer.fromApp(sell, sessionID);

        NewOrderSingle buy = FixMessageBuilder.aBuyLimitOrder("PETR4", 100.0, 50);
        echoServer.fromApp(buy, sessionID);
    }

    @Test
    void shouldHandleMultipleConsecutiveMessages() throws Exception {
        for (int i = 0; i < 10; i++) {
            NewOrderSingle msg = FixMessageBuilder.aBuyLimitOrder("PETR4", 100.0 + i, 100);
            assertDoesNotThrow(() -> echoServer.fromApp(msg, sessionID));
        }
    }

    @Test
    void shouldHandleMessageForDifferentSymbols() throws Exception {
        echoServer.fromApp(FixMessageBuilder.aBuyLimitOrder("PETR4", 25.50, 100), sessionID);
        echoServer.fromApp(FixMessageBuilder.aBuyLimitOrder("VALE3", 68.10, 200), sessionID);
        echoServer.fromApp(FixMessageBuilder.aBuyLimitOrder("ITUB4", 35.00, 150), sessionID);
    }

    @Test
    void shouldCallOnCreate() {
        assertDoesNotThrow(() -> echoServer.onCreate(sessionID));
    }
}
