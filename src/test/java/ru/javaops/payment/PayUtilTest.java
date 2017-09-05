package ru.javaops.payment;

import org.junit.Test;

/**
 * gkislin
 * 05.09.2017
 */
public class PayUtilTest {
    private static String terminalPass = "";

    @Test
    public void checkToken() throws Exception {
        PayCallback pc = new PayCallback() {
            {
                terminalKey = "";
                orderId = "BPHW-3915";
                success = true;
                status = PayOnlineController.Status.CONFIRMED;
                paymentId = 3803143;
                errorCode = "0";
                amount = 1080000;
                pan = "430000******0777";
                token = "b7ebd4b8c6496f3aa5fc0a16f35a0ea3fdffad14ee3055861a8a4713233ca55e";
            }
        };
        PayUtil.checkToken(pc, terminalPass);
    }
}