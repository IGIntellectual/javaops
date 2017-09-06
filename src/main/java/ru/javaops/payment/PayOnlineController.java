package ru.javaops.payment;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
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
import ru.javaops.model.*;
import ru.javaops.payment.ProjectPayDetail.PayDetail;
import ru.javaops.service.CachedGroups;
import ru.javaops.service.GroupService;
import ru.javaops.service.MailService;
import ru.javaops.service.UserService;
import ru.javaops.to.AuthUser;
import ru.javaops.util.ProjectUtil;
import ru.javaops.util.exception.PaymentException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.javaops.payment.PayUtil.getProjectName;

/**
 * gkislin
 * 19.07.2017
 */
@Controller
public class PayOnlineController {
    private final SetMultimap<String, PayCallback> paysInProgress = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

    private static Logger log = LoggerFactory.getLogger("payment");

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

    @Autowired
    private AppProperties appProperties;

    private volatile boolean activate = false;

/*
    public enum Status {
        AUTHORIZED,          //	Деньги захолдированы на карте клиента. Ожидается подтверждение операции
        CONFIRMED,           //	Операция подтверждена
        REVERSED,            //	Операция отменена
        REFUNDED,            //	Произведён возврат
        PARTIAL_REFUNDED,    //	Произведён частичный возврат
        REJECTED             //Списание денежных средств закончилась ошибкой
    }
*/

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
            String project = getProjectName(payId);
            PayDetail payDetail = PayUtil.getPayDetail(payId, project);
            log.info("Payment Success from {} for {}", authUser, project);

            ImmutableMap<String, Object> params = ImmutableMap.of("payCallback", payCallback, "payDetail", payDetail, "project", project);
            if (PayUtil.INTERVIEW.equals(project)) {
                return new ModelAndView("message/payManual", params);
            } else {
                UserGroup userGroup = payCallback.userGroup;
                ParticipationType type = PayUtil.getParticipation(payId, payDetail, payCallback.amount, userGroup.getRegisterType());
                String mailResult = "";
                if (type != null) {
                    userGroup.setParticipationType(type);
                    groupService.save(userGroup);
                    User user = userGroup.getUser();
                    if (user.getBonus() != 0) {
                        authUser.setBonus(0);
                        user.setBonus(0); // clear after use
                        userService.save(user);
                    }
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
    public ResponseEntity<String> callback(PayCallback payCallback, @RequestParam Map<String, String> requestParams) {
        log.info("Pay callback: {}", requestParams);
        check(requestParams);

        String[] split = payCallback.orderId.split("-");
        payCallback.payId = split[0];
        int id = Integer.parseInt(split[1]);
        User user = checkNotNull(userService.get(id), "Не найден пользователь id=%d", id);
        paysInProgress.put(user.getEmail(), payCallback);

        String projectName = getProjectName(payCallback.getPayId());
        Group group;
        if (PayUtil.INTERVIEW.equals(projectName)) {
            group = cachedGroups.findByName(PayUtil.INTERVIEW);
        } else {
            ProjectUtil.Props projectProps = groupService.getProjectProps(projectName);
            group = projectProps.currentGroup;
        }
        UserGroup ug = groupService.registerUserGroup(new UserGroup(user, group, RegisterType.REGISTERED, "online"), ParticipationType.ONLINE_PROCESSING);
        payService.pay(new Payment(payCallback.amount, Currency.RUB, "Online " + payCallback.orderId + '(' + user.getBonus() + "%)"), ug);
        payCallback.userGroup = ug;
        return ResponseEntity.ok("OK");
    }

    public void check(Map<String, String> requestParams) {
//        IP: 91.194.226.0/23
        if (!PayUtil.checkToken(requestParams, appProperties.getTerminalPass())) {
            throw new PaymentException("TokenMismatch", requestParams);
        }
        if (!appProperties.getTerminalKey().equals(requestParams.get("TerminalKey"))) {
            throw new PaymentException("Неверный TerminalKey", requestParams);
        }
        if (!"true".equals(requestParams.get("Success"))) {
            throw new PaymentException("NOT success", requestParams);
        }
        String status = requestParams.get("Status");
        if (!"AUTHORIZED".equals(status) && !"CONFIRMED".equals(status)) {
            throw new PaymentException("Status NOT AUTHORIZED", requestParams);
        }
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
    public ModelAndView payOnline(@RequestParam(value = "payId") String payId) {
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
}