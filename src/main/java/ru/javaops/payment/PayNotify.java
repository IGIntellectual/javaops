package ru.javaops.payment;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * gkislin
 * 05.09.2017
 */
@Getter
@Setter
@ToString(exclude = {"payId", "userId"})
public class PayNotify {
    public String success;
    public String orderId;
    public int amount;
    public long errorCode;
    public String message;
    public String details;

    public String payId;
    public int userId;
}
