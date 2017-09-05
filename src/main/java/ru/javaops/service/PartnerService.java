package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.javaops.model.User;
import ru.javaops.to.UserMail;
import ru.javaops.util.PartnerUtil;
import ru.javaops.util.exception.NoPartnerException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * gkislin
 * 15.07.2017
 */
@Service
public class PartnerService {
    private static final String PARTNER_GROUP_NAME = "partner";

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    public User checkPartner(String partnerKey) {
        User partner = userService.findByEmailAndGroupName(partnerKey.toLowerCase(), PartnerService.PARTNER_GROUP_NAME);
        if (partner == null) {
            throw new NoPartnerException(partnerKey);
        }
        return partner;
    }


    public void checkAndProcessNewCandidate(User candidate) {
        if (candidate.isNewCandidate()) {
            Set<UserMail> partnersForNotify = userService.findByGroupName(PARTNER_GROUP_NAME).stream()
                    .filter(um -> PartnerUtil.hasPartnerFlag(um.getPartnerFlag(), PartnerUtil.CANDIDATE_NOTIFY))
                    .collect(Collectors.toSet());

            mailService.sendToUsersAsync(partnersForNotify, "partner/notifyNewCandidate", ImmutableMap.of("candidate", candidate));
        }
    }
}
