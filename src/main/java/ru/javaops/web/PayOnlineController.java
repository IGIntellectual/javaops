package ru.javaops.web;

import com.google.common.base.Joiner;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppProperties;
import ru.javaops.model.*;
import ru.javaops.service.*;
import ru.javaops.to.AuthUser;
import ru.javaops.to.pay.ProjectPayDetail.PayDetail;
import ru.javaops.util.ProjectUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.javaops.util.ProjectUtil.getProjectName;

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

        private String payId;
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
                log.error("PayCallback != 1", payments);
                return new ModelAndView("message/payFailed");
            }
            PayCallback payCallback = payments.iterator().next();
            String payId = payCallback.payId;
            String project = ProjectUtil.getProjectName(payId);
            PayDetail payDetail = ProjectUtil.getPayDetail(payId, project);
            log.info("Payment Success from {} for {}", authUser, project);

            ImmutableMap<String, Object> params = ImmutableMap.of("payCallback", payCallback, "payDetail", payDetail, "project", project);
            if (ProjectUtil.INTERVIEW.equals(project)) {
                return new ModelAndView("message/payManual", params);
            } else {
                UserGroup userGroup = payCallback.userGroup;
                ParticipationType type = ProjectUtil.getParticipation(payId, payDetail, payCallback.amount, userGroup.getRegisterType());
                String mailResult = "";
                if (type != null) {
                    userGroup.setParticipationType(type);
                    groupService.save(userGroup);
                    payService.sendPaymentRefMail(userGroup);
                    if (payDetail.getTemplate() != null) {
                        mailResult = mailService.sendToUser(payDetail.getTemplate(), authUser);
                    }
                }
                return new ModelAndView("message/paySuccess",
                        ImmutableMap.of("payCallback", payCallback, "mailResult", mailResult));
            }
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
        log.info("Pay callback: {}", payCallback);
        User user = checkAndNormalize(payCallback);
        paysInProgress.put(user.getEmail(), payCallback);
        if (payCallback.success && "0".equals(payCallback.errorCode)) {
            String projectName = getProjectName(payCallback.getPayId());
            Group group;
            if (ProjectUtil.INTERVIEW.equals(projectName)) {
                group = cachedGroups.findByName(ProjectUtil.INTERVIEW);
            } else {
                ProjectUtil.Props projectProps = groupService.getProjectProps(projectName);
                group = projectProps.currentGroup;
            }
            UserGroup ug = groupService.registerUserGroup(new UserGroup(user, group, RegisterType.REGISTERED, "online"), ParticipationType.ONLINE_PROCESSING);
            payService.pay(new Payment(payCallback.amount, Currency.RUB, "Online " + payCallback.orderId), ug);
            payCallback.userGroup = ug;
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private User checkAndNormalize(PayCallback payCallback) {
/*
        IP: 91.194.226.0/23
*/
        if (!checkToken(payCallback)) {
            String msg = "Mismatch token for " + payCallback;
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        Preconditions.checkArgument(appProperties.getTerminalKey().equals(payCallback.terminalKey),
                "Неверный TerminalKey: '%s'", payCallback.terminalKey);

        String[] split = payCallback.orderId.split("-");
        payCallback.payId = split[0];
        int id = Integer.parseInt(split[1]);
        return checkNotNull(userService.get(id), "Не найден пользователь id=%d", id);
    }

/*
    @GetMapping("/payonline")
    public ModelAndView payOnlineFromMail(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("project") String project) {
        log.info("payOnlineFromMail {} from {}", project, email);
        return new ModelAndView("/util/postRedirect",
                ImmutableMap.of("redirectUrl", "/auth/payonline", "project", project));
    }
*/

    @PostMapping("/auth/payonline")
    public ModelAndView payOnline(@RequestParam(value = "payId", defaultValue = "") String payId) {
        AuthUser authUser = AuthorizedUser.user();
        log.info("payOnline {} from {}", payId, authUser.getEmail());
        if (activate || authUser.hasRole(Role.ROLE_TEST)) {
            return new ModelAndView("payOnline",
                    ImmutableMap.of("project", getProjectName(payId), "payId", payId, "terminalKey", appProperties.getTerminalKey()));
        } else {
            log.warn("payDisabled");
            return new ModelAndView("message/payDisabled");
        }
    }

    private boolean checkToken(PayCallback payCallback) {
        final String paramString =
                Joiner.on("").skipNulls().join(new Object[]{
                        payCallback.amount,
                        payCallback.errorCode,
                        payCallback.orderId,
                        payCallback.pan,
                        appProperties.getTerminalPass(),
                        payCallback.paymentId,
                        payCallback.status,
                        payCallback.success,
                        payCallback.terminalKey});

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(paramString.getBytes(StandardCharsets.UTF_8));
            String expectedToken = Base64.getEncoder().encodeToString(hash);
            return expectedToken.equals(payCallback.token);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}