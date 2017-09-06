package ru.javaops.payment;

import org.junit.Test;

import java.util.HashMap;

/**
 * gkislin
 * 05.09.2017
 */
public class PayUtilTest {
    @Test
    public void checkToken() throws Exception {
        PayUtil.checkToken(new HashMap<String, String>() {
            {
                put("TerminalKey", "");
                put("orderId", "BPHW-3915");
                put("success", "true");
                put("status", "CONFIRMED");
                put("paymentId", "3803143");
                put("errorCode", "0");
                put("Amount", "1080000");
                put("pan", "430000******0777");
                put("Token", "b7ebd4b8c6496f3aa5fc0a16f35a0ea3fdffad14ee3055861a8a4713233ca55e");
            }
        }, "");
    }
}