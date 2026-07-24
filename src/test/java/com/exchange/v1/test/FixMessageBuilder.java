package com.exchange.v1.test;

import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;

import java.time.LocalDateTime;

public class FixMessageBuilder {

    public static NewOrderSingle aNewOrderSingle() {
        return aBuyLimitOrder("PETR4", 100.0, 100);
    }

    public static NewOrderSingle aBuyLimitOrder(String symbol, double price, double qty) {
        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID("ORD-" + System.currentTimeMillis()),
                new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK),
                new Symbol(symbol),
                new Side(Side.BUY),
                new TransactTime(LocalDateTime.now()),
                new OrdType(OrdType.LIMIT)
        );
        order.set(new OrderQty(qty));
        order.set(new Price(price));
        return order;
    }

    public static NewOrderSingle aSellLimitOrder(String symbol, double price, double qty) {
        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID("ORD-" + System.currentTimeMillis()),
                new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK),
                new Symbol(symbol),
                new Side(Side.SELL),
                new TransactTime(LocalDateTime.now()),
                new OrdType(OrdType.LIMIT)
        );
        order.set(new OrderQty(qty));
        order.set(new Price(price));
        return order;
    }

    public static NewOrderSingle aMarketOrder(String symbol, double qty, char side) {
        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID("ORD-" + System.currentTimeMillis()),
                new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK),
                new Symbol(symbol),
                new Side(side),
                new TransactTime(LocalDateTime.now()),
                new OrdType(OrdType.MARKET)
        );
        order.set(new OrderQty(qty));
        return order;
    }

    public static ExecutionReport anExecutionReport(String clOrdID, char ordStatus, double cumQty, double avgPx) {
        ExecutionReport report = new ExecutionReport(
                new OrderID("ORD-1"),
                new ExecID("EXEC-1"),
                new ExecTransType(ExecTransType.NEW),
                new ExecType(ordStatus),
                new OrdStatus(ordStatus),
                new Symbol("PETR4"),
                new Side(Side.BUY),
                new LeavesQty(0),
                new CumQty(cumQty),
                new AvgPx(avgPx)
        );
        report.set(new ClOrdID(clOrdID));
        return report;
    }
}
