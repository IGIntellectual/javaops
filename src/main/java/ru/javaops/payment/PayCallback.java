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
    public String orderId;
    public int amount;

    public String payId;
    public UserGroup userGroup;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("orderId", orderId)
                .add("amount", amount)
                .add("userGroup", userGroup)
                .toString();
    }
}
