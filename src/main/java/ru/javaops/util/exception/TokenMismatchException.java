package ru.javaops.util.exception;

import ru.javaops.payment.PayCallback;

public class TokenMismatchException extends RuntimeException {
    private PayCallback payCallback;

    public TokenMismatchException(PayCallback payCallback) {
        this.payCallback = payCallback;
    }

    public PayCallback getPayCallback() {
        return payCallback;
    }
}
