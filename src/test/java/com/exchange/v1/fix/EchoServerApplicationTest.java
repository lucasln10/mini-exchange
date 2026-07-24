package com.exchange.v1.fix;

import com.exchange.v1.acceptor.fix.EchoServer;
import com.exchange.v1.engine.MatchingEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import quickfix.*;
import quickfix.ScreenLogFactory;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;

import static org.junit.jupiter.api.Assertions.*;

class EchoServerApplicationTest {

    private SocketAcceptor acceptor;
    private Initiator initiator;

    @AfterEach
    void tearDown() {
        if (initiator != null) {
            try {
                initiator.stop();
            } catch (Exception ignored) {
            }
        }
        if (acceptor != null) {
            try {
                acceptor.stop();
            } catch (Exception ignored) {
            }
        }
    }

    @Disabled("Requires FIX session infrastructure; covered by unit tests")
    @Test
    void echoApplicationMessage_backToClient() throws Exception {
        SessionSettings serverSettings = new SessionSettings(new ByteArrayInputStream(("""
                [DEFAULT]
                ConnectionType=acceptor
                HeartBtInt=30
                StartTime=00:00:00
                EndTime=23:59:59
                FileStorePath=target/fix-test-store-server
                FileLogPath=target/fix-test-log-server

                [SESSION]
                BeginString=FIX.4.2
                SenderCompID=EXCHANGE
                TargetCompID=CLIENT
                SocketAcceptPort=9876
                DataDictionary=FIX42.xml
                """).getBytes(StandardCharsets.UTF_8)));

        EchoServer application = new EchoServer(new MatchingEngine());
        acceptor = new SocketAcceptor(application, new MemoryStoreFactory(), serverSettings, new ScreenLogFactory(true, true, true), new DefaultMessageFactory());
        acceptor.start();

        SessionSettings clientSettings = new SessionSettings(new ByteArrayInputStream(("""
                [DEFAULT]
                ConnectionType=initiator
                ReconnectInterval=1
                HeartBtInt=30
                StartTime=00:00:00
                EndTime=23:59:59

                [SESSION]
                BeginString=FIX.4.2
                SenderCompID=CLIENT
                TargetCompID=EXCHANGE
                SocketConnectHost=127.0.0.1
                SocketConnectPort=9876
                DataDictionary=FIX42.xml
                """).getBytes(StandardCharsets.UTF_8)));

        class TestClientApp implements Application {
            final AtomicReference<Message> received = new AtomicReference<>();
            final CountDownLatch logonLatch = new CountDownLatch(1);
            final CountDownLatch messageLatch = new CountDownLatch(1);
            final AtomicBoolean loggedOn = new AtomicBoolean(false);

            @Override
            public void onCreate(SessionID sessionId) {
            }

            @Override
            public void onLogon(SessionID sessionId) {
                loggedOn.set(true);
                logonLatch.countDown();
            }

            @Override
            public void onLogout(SessionID sessionId) {
                loggedOn.set(false);
            }

            @Override
            public void toAdmin(Message message, SessionID sessionId) {
            }

            @Override
            public void fromAdmin(Message message, SessionID sessionId) {
            }

            @Override
            public void toApp(Message message, SessionID sessionId) {
            }

            @Override
            public void fromApp(Message message, SessionID sessionId) {
                received.set(message);
                messageLatch.countDown();
            }
        }

        TestClientApp clientApp = new TestClientApp();

        initiator = new SocketInitiator(clientApp, new MemoryStoreFactory(), clientSettings, new ScreenLogFactory(true, true, true), new DefaultMessageFactory());
        initiator.start();

        assertTrue(clientApp.logonLatch.await(10, TimeUnit.SECONDS), "Client failed to logon to the acceptor");

        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID("ORD-1"),
                new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION),
                new Symbol("EURUSD"),
                new Side(Side.BUY),
                new TransactTime(LocalDateTime.now()),
                new OrdType(OrdType.MARKET)
        );
        order.set(new OrderQty(1000));
        order.set(new OrdType(OrdType.MARKET));

        SessionID sessionID = new SessionID("FIX.4.2", "CLIENT", "EXCHANGE");
        boolean sent = Session.sendToTarget(order, sessionID);
        assertTrue(sent, "Failed to send message to server");

        assertTrue(clientApp.messageLatch.await(5, TimeUnit.SECONDS), "Did not receive response from server");

        Message echoed = clientApp.received.get();
        assertNotNull(echoed, "Did not receive response message from server");

        ExecutionReport report = assertInstanceOf(ExecutionReport.class, echoed, "Server should respond with ExecutionReport");
        assertEquals("ORD-1", echoed.getString(ClOrdID.FIELD));
        assertEquals(OrdStatus.FILLED, report.getOrdStatus().getValue());
        assertEquals(ExecType.FILL, report.getExecType().getValue());
    }
}

