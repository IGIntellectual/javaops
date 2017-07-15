package ru.javaops.to;

import java.util.Date;

/**
 * gkislin
 * 29.06.2016
 */
public interface UserMail {

    String getEmail();

    String getFullName();

    default Date getRegisteredDate() {
        throw new UnsupportedOperationException();
    }

    default long getPartnerFlag() {
        throw new UnsupportedOperationException();
    }
}
