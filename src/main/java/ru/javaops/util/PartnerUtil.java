package ru.javaops.util;

/**
 * gkislin
 * 15.07.2017
 */
public class PartnerUtil {
    public static final long CANDIDATE_NOTIFY = 0x1;
    public static final long CORPORATE_STUDY = 0x2;

    public static boolean hasPartnerFlag(long partnerFlag, long mask) {
        return (partnerFlag & mask) != 0;
    }

    public static long setPartnerFlag(long partnerFlag, long mask, boolean flag) {
        if (flag) {
            partnerFlag |= mask;
        } else {
            partnerFlag &= ~mask;
        }
        return partnerFlag;
    }
}
