package ru.javaops.payment;

import com.google.common.base.MoreObjects;
import lombok.Getter;
import lombok.Setter;
import ru.javaops.model.UserGroup;

/**
 * gkislin
 * 05.09.2017
 */
@Getter
@Setter
public class PayCallback {
    public String terminalKey;
    public String orderId;
    public boolean success;
    public PayOnlineController.Status status;
    public long paymentId;
    public String errorCode;
    public int amount;
    public String pan;
    public long cardId;
    public String token;

    public String payId;
    public UserGroup userGroup;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("terminalKey", terminalKey)
                .add("orderId", orderId)
                .add("success", success)
                .add("status", status)
                .add("paymentId", paymentId)
                .add("errorCode", errorCode)
                .add("cardId", cardId)
                .add("amount", amount)
                .add("pan", pan)
                .add("token", token)
                .add("userGroup", userGroup)
                .toString();
    }
}
