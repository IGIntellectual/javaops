package ru.javaops.payment;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.javaops.model.*;
import ru.javaops.repository.PaymentRepository;
import ru.javaops.service.RefService;
import ru.javaops.service.UserService;

/**
 * gkislin
 * 28.07.2017
 */

@Service
public class PayService {
    private static Logger log = LoggerFactory.getLogger("payment");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefService refService;

    @Autowired
    private UserService userService;

    public void pay(Payment payment, UserGroup ug) {
        log.info("{} in {}", payment, ug);
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
