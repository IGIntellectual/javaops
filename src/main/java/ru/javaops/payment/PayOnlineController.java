package ru.javaops.payment;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static ru.javaops.payment.PayUtil.getProjectName;

/**
 * gkislin
 * 19.07.2017
 */
@Controller
public class PayOnlineController {
    private static Logger log = LoggerFactory.getLogger("payment");

    LoadingCache<Integer, String> paymentStatuses = CacheBuilder.newBuilder()
            .weakKeys()
            .maximumSize(100)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, String>() {
                public String load(Integer id) {
                    return "";
                }
            });

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

    private enum Status {
        AUTHORIZED("Ожидается подтверждение операции"),
        CONFIRMED("Операция подтверждена"),
        REVERSED("Операция отменена"),
        REFUNDED("Произведён возврат"),
        PARTIAL_REFUNDED("Произведён частичный возврат"),
        REJECTED("Списание денежных средств закончилась ошибкой");

        public String descr;

        Status(String descr) {
            this.descr = descr;
        }
    }

    @PostMapping("/api/payonline")
    public ResponseEntity<String> activate(@RequestParam("activate") boolean activate) {
        log.warn(activate ? "Activate" : "Deactivate");
        this.activate = activate;
        return ResponseEntity.ok(String.valueOf(activate));
    }

    @GetMapping("/auth/payonline/success")
    public ModelAndView success(PayNotify payNotify) {
        AuthUser authUser = AuthorizedUser.authUser();
        log.info("Payment Success for {}: {}", authUser, payNotify);
        parse(payNotify);
        if (payNotify.userId != authUser.getId()) {
            log.error("Пользователь id={} не совпедает с {}. PayNotify: {}", payNotify.userId, authUser, payNotify);
            return new ModelAndView("message/payFailed");
        }
        String project = PayUtil.getProjectName(payNotify.payId);
        PayDetail payDetail = PayUtil.getPayDetail(payNotify.payId, project, authUser);
        ImmutableMap<String, Object> params = ImmutableMap.of("payNotify", payNotify, "payDetail", payDetail, "project", project);
        if (PayUtil.INTERVIEW.equals(project)) {
            return new ModelAndView("message/payManual", params);
        } else {
            return new ModelAndView("message/paySuccess",
                    ImmutableMap.of("payNotify", payNotify, "payDetail", payDetail, "project", project));
        }
    }

    @GetMapping("/auth/payonline/checkStatus")
    public ModelAndView checkStatus() throws ExecutionException {
        AuthUser authUser = AuthorizedUser.authUser();
        String status = paymentStatuses.get(authUser.getId());
        if (StringUtils.isEmpty(status)) {
            status = "Ожидается ответ от платежной сисетмы";
        }
        return new ModelAndView("message/checkPaymentStatus", ImmutableMap.of("status", status));
    }

    @GetMapping("/auth/payonline/failed")
    public ModelAndView failed(PayNotify payNotify) {
        log.error("Payment Failed from {}\n{}", AuthorizedUser.user(), payNotify);
        return new ModelAndView("message/payFailed", ImmutableMap.of("payNotify", payNotify));
    }

    @PostMapping("/payonline/callback")
//    https://oplata.tinkoff.ru/documentation/?section=notification
    public ResponseEntity<String> callback(PayNotify payNotify, @RequestParam Map<String, String> requestParams) {
        log.info("Pay callback: {}", requestParams);
        parse(payNotify);
        User user = userService.get(payNotify.userId);

        if (user == null) {
            log.error("Не найден пользователь id={}, params: {}", payNotify.userId, requestParams);
        } else if (!appProperties.getTerminalKey().equals(requestParams.get("TerminalKey"))) {
            log.error("!!! Wrong TerminalKey, user {}, params {}", user, requestParams);
        } else if (!PayUtil.checkToken(requestParams, appProperties.getTerminalPass())) {
            log.error("!!! Token mismatch, user {}, params {}", user, requestParams);
        } else if (!"true".equals(requestParams.get("Success"))) {
            log.warn("Unsuccess pay, user {}, params {}", user, requestParams);
        } else {
            Status status = Status.valueOf(requestParams.get("Status"));
            if (status != Status.CONFIRMED) {
                paymentStatuses.put(user.getId(), "Статус операции: " + status.descr);
                log.info("Status changed {}, user {}, params {}", status, user, requestParams);
            } else {
                paymentStatuses.put(user.getId(), "Статус операции: " + status.descr);
                String project = getProjectName(payNotify.getPayId());

                AuthUser authUser = new AuthUser(user);
                groupService.updateAuthParticipation(authUser);
                PayDetail payDetail = PayUtil.getPayDetail(payNotify.payId, project, authUser);

                Group group;
                if (PayUtil.INTERVIEW.equals(project)) {
                    group = cachedGroups.findByName(PayUtil.INTERVIEW);
                } else {
                    ProjectUtil.Props projectProps = groupService.getProjectProps(project);
                    group = projectProps.currentGroup;
                }
                UserGroup userGroup = groupService.registerUserGroup(new UserGroup(user, group, RegisterType.REGISTERED, "online"), ParticipationType.ONLINE_PROCESSING);
                ParticipationType type = PayUtil.getParticipation(payNotify.payId, payDetail, payNotify.amount, userGroup.getRegisterType());
                if (type != null) {
                    userGroup.setParticipationType(type);
                    groupService.save(userGroup);
                    if (user.getBonus() != 0) {
                        user.setBonus(0); // clear after use
                        userService.save(user);
                    }
                    payService.sendPaymentRefMail(userGroup);
                    String templates = payDetail.getTemplate();
                    String mailResult = "";
                    if (templates != null) {
                        String[] array = templates.split(",");
                        for (String template : array) {
                            mailResult = mailService.sendToUser(project + '/' + template, user);
                            paymentStatuses.put(user.getId(), "Результат отправки письма: " + mailResult);
                        }
                    }
                }
                payService.pay(new Payment(payNotify.amount, Currency.RUB, "Online " + payNotify.orderId + '(' + user.getBonus() + "%)"), userGroup);
            }
        }
        return ResponseEntity.ok("OK");
    }

    private void parse(PayNotify payNotify) {
        String[] split = payNotify.orderId.split("-");
        payNotify.payId = split[0];
        payNotify.userId = Integer.parseInt(split[1]);
        payNotify.amount /= 100;
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