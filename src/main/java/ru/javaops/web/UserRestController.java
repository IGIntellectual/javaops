package ru.javaops.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.javaops.model.ParticipationType;
import ru.javaops.model.Payment;
import ru.javaops.model.UserGroup;
import ru.javaops.payment.PayService;
import ru.javaops.service.*;
import ru.javaops.to.UserTo;

import javax.validation.Valid;

/**
 * GKislin
 */

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.TEXT_PLAIN_VALUE)
@Slf4j
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private RefService refService;

    @Autowired
    private MailService mailService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PayService payService;

    @DeleteMapping
    public ResponseEntity<String> delete(@RequestParam("email") String email) {
        return new ResponseEntity<>(userService.deleteByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/key")
    public String getKey(@RequestParam("email") String email) {
        return subscriptionService.generateActivationKey(email.toLowerCase());
    }

    @PostMapping("/pay")
    public String pay(@RequestParam("group") String group,
                      @Valid UserTo userTo,
                      @Valid Payment payment,
                      @RequestParam(value = "type", defaultValue = "REGULAR") ParticipationType participationType,
                      @RequestParam(value = "template", required = false) String template) {

        log.info("Pay from {} for {}", userTo, group);
        UserGroup ug = groupService.registerAtGroup(userTo, group, null, participationType);
        payService.pay(payment, ug);
        String refInfo = payService.sendPaymentRefMail(ug);
        return refInfo + ug.toString() + '\n' + (template == null ? "No template" : mailService.sendToUser(template, ug.getUser()));
    }
}