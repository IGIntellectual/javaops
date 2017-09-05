package ru.javaops.util.exception;

public class NoPartnerException extends RuntimeException {
    private String partnerKey;

    public NoPartnerException(String partnerKey) {
        this.partnerKey = partnerKey;
    }

    public String getPartnerKey() {
        return partnerKey;
    }
}
