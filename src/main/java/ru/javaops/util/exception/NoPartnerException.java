package ru.javaops.util.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NoPartnerException extends RuntimeException {
    private final String partnerKey;
}
