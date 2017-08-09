package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.javaops.model.ParticipationType;
import ru.javaops.model.Payment;
import ru.javaops.model.User;
import ru.javaops.model.UserGroup;
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
        User refUser = null;
        User user = ug.getUser();
        if (ug.isAlreadyExist()) {
            log.info("User {} already exist in {}", user, ug.getGroup().getName());
        } else if (ParticipationType.isParticipant(ug.getParticipationType())) {
            refUser = refService.getRefUser(ug.getUser());
            if (refUser != null) {
                String project = ug.getGroup().getProject().getName();
                int addBonus = "topjava".equals(project) || "masterjava".equals(project) ? 25 : 10;
                refUser.addBonus(addBonus);
                log.info("!!! Ref Participation from user {}, bonus {}", refUser.getEmail(), refUser.getBonus());
                refUser = userService.save(refUser);
                refService.sendAsyncMail(refUser, "ref/refParticipation", ImmutableMap.of("project", project, "email", user.getEmail(), "addBonus", addBonus));
            }
        }
        return refUser == null ? "" : "Reference from " + refUser.getEmail() + ", bonus=" + refUser.getBonus() + '\n';
    }
}
