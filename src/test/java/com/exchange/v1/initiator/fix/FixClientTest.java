package com.exchange.v1.initiator.fix;

import com.exchange.v1.engine.MatchingEngine;
import com.exchange.v1.test.FixMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;

import static org.junit.jupiter.api.Assertions.*;

class FixClientTest {

    private FixClient fixClient;
    private MatchingEngine engine;
    private SessionID sessionID;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
        fixClient = new FixClient(engine);
        sessionID = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");
    }

    @Test
    void shouldHandleLogon() {
        assertDoesNotThrow(() -> fixClient.onLogon(sessionID));
    }

    @Test
    void shouldHandleLogout() {
        assertDoesNotThrow(() -> fixClient.onLogout(sessionID));
    }

    @Test
    void shouldHandleExecutionReport() throws Exception {
        ExecutionReport report = FixMessageBuilder.anExecutionReport("ORD-1", OrdStatus.FILLED, 100, 25.50);
        assertDoesNotThrow(() -> fixClient.fromApp(report, sessionID));
    }

    @Test
    void shouldHandlePartialFillReport() throws Exception {
        ExecutionReport report = FixMessageBuilder.anExecutionReport("ORD-1", OrdStatus.PARTIALLY_FILLED, 50, 25.50);
        assertDoesNotThrow(() -> fixClient.fromApp(report, sessionID));
    }

    @Test
    void shouldHandleCanceledReport() throws Exception {
        ExecutionReport report = FixMessageBuilder.anExecutionReport("ORD-1", OrdStatus.CANCELED, 0, 0);
        assertDoesNotThrow(() -> fixClient.fromApp(report, sessionID));
    }

    @Test
    void shouldHandleRejectedReport() throws Exception {
        ExecutionReport report = FixMessageBuilder.anExecutionReport("ORD-1", OrdStatus.REJECTED, 0, 0);
        assertDoesNotThrow(() -> fixClient.fromApp(report, sessionID));
    }

    @Test
    void shouldHandleOrderCancelReject() throws Exception {
        quickfix.fix42.OrderCancelReject reject = new quickfix.fix42.OrderCancelReject(
                new OrderID("NONE"),
                new ClOrdID("CANCEL-1"),
                new OrigClOrdID("ORD-1"),
                new OrdStatus(OrdStatus.REJECTED),
                new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST)
        );

        assertDoesNotThrow(() -> fixClient.fromApp(reject, sessionID));
    }

    @Test
    void shouldHandleRejectWithText() throws Exception {
        quickfix.fix42.OrderCancelReject reject = new quickfix.fix42.OrderCancelReject(
                new OrderID("NONE"),
                new ClOrdID("CANCEL-2"),
                new OrigClOrdID("ORD-X"),
                new OrdStatus(OrdStatus.REJECTED),
                new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST)
        );
        reject.set(new Text("Order not found"));

        assertDoesNotThrow(() -> fixClient.fromApp(reject, sessionID));
    }

    @Test
    void shouldHandleUnsupportedMessageType() {
        Message badMsg = new Message();
        badMsg.getHeader().setField(new MsgType("Z"));

        assertThrows(UnsupportedMessageType.class, () -> fixClient.fromApp(badMsg, sessionID));
    }

    @Test
    void shouldHandleOnCreate() {
        assertDoesNotThrow(() -> fixClient.onCreate(sessionID));
    }

    @Test
    void shouldHandleAdminMessages() {
        assertDoesNotThrow(() -> fixClient.toAdmin(new Message(), sessionID));
        assertDoesNotThrow(() -> fixClient.fromAdmin(new Message(), sessionID));
    }

    @Test
    void shouldHandleToApp() {
        assertDoesNotThrow(() -> fixClient.toApp(new Message(), sessionID));
    }

    @Test
    void shouldHandleMultipleReports() throws Exception {
        for (int i = 0; i < 10; i++) {
            ExecutionReport report = FixMessageBuilder.anExecutionReport("ORD-" + i, OrdStatus.FILLED, 100 * (i + 1), 25.50);
            assertDoesNotThrow(() -> fixClient.fromApp(report, sessionID));
        }
    }

    @Test
    void shouldHandleNewStatus() throws Exception {
        ExecutionReport report = FixMessageBuilder.anExecutionReport("ORD-1", OrdStatus.NEW, 0, 0);
        assertDoesNotThrow(() -> fixClient.fromApp(report, sessionID));
    }
}
