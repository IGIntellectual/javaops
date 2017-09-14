package ru.javaops.model;

/**
 * GKislin
 * 02.09.2015.
 */
public enum ParticipationType {
    PREPAID,
    REGULAR,
    HW_REVIEW,
    ASSIST,
    ONLINE_PROCESSING,
    PAY_ONLINE;

    public static boolean isParticipant(ParticipationType type) {
        return type == REGULAR || type == HW_REVIEW;
    }
}
