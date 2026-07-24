package com.exchange.v1.test;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.*;

import static org.junit.jupiter.api.Assertions.*;

public class FixAssertions {

    public static void assertExecutionReport(Message msg) {
        assertNotNull(msg, "Message should not be null");
        try {
            assertEquals("8", msg.getHeader().getString(MsgType.FIELD),
                    "Should be ExecutionReport (35=8)");
        } catch (FieldNotFound e) {
            fail("Missing MsgType field");
        }
    }

    public static void assertOrdStatus(Message msg, char expectedStatus) {
        try {
            assertEquals(expectedStatus, msg.getChar(OrdStatus.FIELD),
                    "OrdStatus should be " + expectedStatus);
        } catch (FieldNotFound e) {
            fail("Missing OrdStatus field");
        }
    }

    public static void assertClOrdID(Message msg, String expectedClOrdID) {
        try {
            assertEquals(expectedClOrdID, msg.getString(ClOrdID.FIELD),
                    "ClOrdID should match");
        } catch (FieldNotFound e) {
            fail("Missing ClOrdID field");
        }
    }

    public static void assertHasRequiredFields(Message msg, int... tags) {
        for (int tag : tags) {
            try {
                assertNotNull(msg.getString(tag), "Required tag " + tag + " should be present");
            } catch (FieldNotFound e) {
                fail("Required tag " + tag + " is missing");
            }
        }
    }

    public static void assertNewOrderSingle(Message msg) {
        assertNotNull(msg, "Message should not be null");
        try {
            assertEquals("D", msg.getHeader().getString(MsgType.FIELD),
                    "Should be NewOrderSingle (35=D)");
        } catch (FieldNotFound e) {
            fail("Missing MsgType field");
        }
    }
}
