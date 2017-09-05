package ru.javaops.payment;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.javaops.config.AppProperties;
import ru.javaops.model.*;
import ru.javaops.repository.PaymentRepository;
import ru.javaops.service.RefService;
import ru.javaops.service.UserService;
import ru.javaops.util.exception.TokenMismatchException;

import static com.google.common.base.Preconditions.checkNotNull;

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

    @Autowired
    private AppProperties appProperties;

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

    public String getTerminalKey() {
        return appProperties.getTerminalKey();
    }

    public User checkAndNormalize(PayCallback payCallback) {
/*
        IP: 91.194.226.0/23
*/
        if (!PayUtil.checkToken(payCallback, appProperties.getTerminalPass())) {
            throw new TokenMismatchException(payCallback);
        }

        Preconditions.checkArgument(appProperties.getTerminalKey().equals(payCallback.terminalKey),
                "Неверный TerminalKey: '%s'", payCallback.terminalKey);

        String[] split = payCallback.orderId.split("-");
        payCallback.payId = split[0];
        int id = Integer.parseInt(split[1]);
        return checkNotNull(userService.get(id), "Не найден пользователь id=%d", id);
    }
}
