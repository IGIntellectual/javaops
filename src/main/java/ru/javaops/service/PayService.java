package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.javaops.model.*;
import ru.javaops.repository.PaymentRepository;

/**
 * gkislin
 * 28.07.2017
 */

@Service
@Slf4j
public class PayService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefService refService;

    @Autowired
    private UserService userService;

    public void pay(Payment payment, UserGroup ug) {
        log.info("Pay {}", payment);
        payment.setUserGroup(ug);
        paymentRepository.save(payment);
    }

    public String sendPaymentRefMail(UserGroup ug) {
        User user = ug.getUser();
        User refUser = refService.getRefUser(ug.getUser());
        Group group = ug.getGroup();
        if (refUser != null && group.isMembers()) {
            String msg;
            if (ug.getRegisterType() == RegisterType.DUPLICATED) {
                msg = String.format("User %s from reference %s already present in group %s", user, refUser.getEmail(), group.getName());
            } else {
                String project = group.getProject().getName();
                int addBonus = "topjava".equals(project) || "masterjava".equals(project) ? 25 : 10;
                refUser.addBonus(addBonus);
                refUser = userService.save(refUser);
                msg = String.format("User %s from reference (%s, bonus=%d) registered in group %s", user, refUser.getEmail(), refUser.getBonus(), group.getName());
                refService.sendAsyncMail(refUser, "ref/refParticipation", ImmutableMap.of("project", project, "email", user.getEmail(), "addBonus", addBonus));
            }
            log.info(msg);
            return msg;
        }
        return "";
    }
}
