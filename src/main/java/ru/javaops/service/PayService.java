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
        User user = ug.getUser();
        User refUser = refService.getRefUser(ug.getUser());
        if (ParticipationType.isParticipant(ug.getParticipationType())) {
            if (refUser != null) {
                String project = ug.getGroup().getProject().getName();
                int addBonus = "topjava".equals(project) || "masterjava".equals(project) ? 25 : 10;
                refUser.addBonus(addBonus);
                log.info("!!! Ref Participation from user {}, bonus {}", refUser.getEmail(), refUser.getBonus());
                refUser = userService.save(refUser);
                refService.sendAsyncMail(refUser, "ref/refParticipation", ImmutableMap.of("project", project, "email", user.getEmail(), "addBonus", addBonus));
            }
        } else {
            log.info("User {} from reference {} group {} paid with type {} without refParticipation", user, refUser.getEmail(), ug.getGroup().getName(), ug.getParticipationType());
        }
        return refUser == null ? "" : "Reference from " + refUser.getEmail() + ", bonus=" + refUser.getBonus() + '\n';
    }
}
