package ru.javaops.web;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppProperties;
import ru.javaops.model.User;
import ru.javaops.model.UserGroup;
import ru.javaops.service.*;
import ru.javaops.to.AuthUser;
import ru.javaops.to.pay.ProjectPayDetail;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * gkislin
 * 19.07.2017
 */
@Controller
public class PayOnlineController {
    private final SetMultimap<String, PayCallback> paysInProgress = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

    private static Logger log = LoggerFactory.getLogger("payment");

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CachedProjects cachedProjects;

    @Autowired
    private CachedGroups cachedGroups;

    @Autowired
    private GroupService groupService;

    @Autowired
    private PayService payService;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    private volatile boolean activate = false;

    @Getter
    @Setter
    private static class PayCallback {
        private String terminalKey;
        private String orderId;
        private boolean success;
        private boolean status;
        private String paymentId;
        private String errorCode;
        private int amount;
        private String pan;
        private String token;

        private ProjectPayDetail projectPayDetail;
        private UserGroup userGroup;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("terminalKey", terminalKey)
                    .add("orderId", orderId)
                    .add("success", success)
                    .add("status", status)
                    .add("paymentId", paymentId)
                    .add("errorCode", errorCode)
                    .add("amount", amount)
                    .add("pan", pan)
                    .add("token", token)
                    .add("userGroup", userGroup)
                    .toString();
        }
    }

    @PostMapping("/api/payonline")
    public ResponseEntity<String> activate(@RequestParam("activate") boolean activate) {
        log.warn(activate ? "Activate" : "Deactivate");
        this.activate = activate;
        return ResponseEntity.ok(paysInProgress.toString());
    }

    @GetMapping("/payonline/success")
    public ModelAndView success() {
        AuthUser authUser = AuthorizedUser.user();
        if (authUser != null) {
            Set<PayCallback> payments = paysInProgress.removeAll(authUser.getId());
            log.info("Payment success from{}\n{}", authUser, payments);
            if (payments == null || payments.size() != 1) {
                log.error("PayCallback != 1");
                return new ModelAndView("message/payFailed");
            }
            PayCallback payCallback = payments.iterator().next();
            UserGroup userGroup = payCallback.userGroup;
/*
            PayDetail payDetail = payCallback.payDetail;
            if (!payDetail.isInterview() && ParticipationType.isParticipant(userGroup.getParticipationType())) {
                payService.sendPaymentRefMail(userGroup);
                String mailResult = "";
                if (payDetail.getTemplate() != null) {
                    mailResult = mailService.sendToUser(payDetail.getTemplate(), payCallback.userGroup.getUser());
                }
                return new ModelAndView("message/paySuccess",
                        ImmutableMap.of("payCallback", payCallback, "mailResult", mailResult));
            } else {
*/
                return new ModelAndView("message/payManual", "payCallback", payCallback);
//            }
        } else {
            log.error("Payment Success from UNAUTHORIZED\n{}", paysInProgress);
            return new ModelAndView("message/payFailed");
        }
    }

    @GetMapping("/payonline/failed")
    public String failed() {
        if (AuthorizedUser.isAuthorized()) {
            String email = AuthorizedUser.user().getEmail();
            log.error("Payment Failed from {}\n{}", AuthorizedUser.user(), paysInProgress);
            paysInProgress.removeAll(email);
        } else {
            log.error("Payment Failed from UNAUTHORIZED\n{}", paysInProgress);
        }
        return "message/payFailed";
    }

    @PostMapping("/payonline/callback")
//    https://oplata.tinkoff.ru/documentation/?section=notification
    public ResponseEntity<String> callback(PayCallback payCallback) {
/*
        Сеть, с которой будут приходить уведомления: 91.194.226.0/23
        Проверить, не исказились ли данные в процессе передачи (проверить Token)
*/
        log.info("Pay callback: {}", payCallback);
        User user = normalize(payCallback);
/*
        PayDetail payDetail = payCallback.payDetail;

        paysInProgress.put(user.getEmail(), payCallback);
        if (payCallback.success && "0".equals(payCallback.errorCode)) {
            String projectName = payDetail.getProject().getName();
            Group group;
            if (ProjectUtil.INTERVIEW.equals(projectName)) {
                group = cachedGroups.findByName(ProjectUtil.INTERVIEW);
            } else {
                ProjectUtil.Props projectProps = groupService.getProjectProps(projectName);
                group = projectProps.currentGroup;
            }
            UserGroup ug = groupService.registerUserGroup(
                    new UserGroup(user, group, RegisterType.REGISTERED, null),
                    payDetail.findParticipationType(payCallback.orderId, payCallback.amount, user.getBonus()));
            payService.pay(new Payment(payCallback.amount, Currency.RUB, "Online " + payCallback.orderId), ug);
            payCallback.userGroup = ug;
*/
            return ResponseEntity.ok("OK");
/*
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
*/
    }

    private User normalize(PayCallback payCallback) {
        Preconditions.checkArgument(appProperties.getTerminalKey().equals(payCallback.terminalKey),
                "Неверный TerminalKey: '%s'", payCallback.terminalKey);

        String[] split = payCallback.orderId.split("-");
        String payId = split[0];
//        payCallback.payDetail = checkNotNull(ProjectUtil.getPayDetails(payId), "Неверный payId=%s", payId);
        int id = Integer.valueOf(split[1]);
        return checkNotNull(userService.get(id), "Не найден пользователь id=%d", id);
    }

    @GetMapping("/payonline")
    public ModelAndView payOnlineFromMail(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("payId") String payId) {
        log.info("payOnlineFromMail {} from {}", payId, email);
        return new ModelAndView("/util/postRedirect",
                ImmutableMap.of("redirectUrl", "/auth/payonline", "payId", payId));
    }

    @PostMapping("/auth/payonline")
    public ModelAndView payOnline(@RequestParam("payId") String payId) {
        log.info("payOnline {} from {}", payId, AuthorizedUser.user().getEmail());
        if (activate) {
//            PayDetail payDetails = ProjectUtil.getPayDetails(payId);
//            return new ModelAndView("payOnline",
//                    ImmutableMap.of("payDetail", payDetails, "payId", payId, "terminalKey", appProperties.getTerminalKey()));
            return null;
        } else {
            log.warn("payDisabled");
            return new ModelAndView("message/payDisabled");
        }
    }
}