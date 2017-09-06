package ru.javaops.util.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class PaymentException extends RuntimeException {
    private final Map<String, String> requestParams;

    public PaymentException(String message, Map<String, String> requestParams) {
        super(message);
        this.requestParams = requestParams;
    }
}
