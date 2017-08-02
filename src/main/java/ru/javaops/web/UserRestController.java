package ru.javaops.web;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.javaops.model.*;
import ru.javaops.service.*;
import ru.javaops.to.UserTo;

import javax.validation.Valid;

/**
 * GKislin
 */

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @DeleteMapping
    public ResponseEntity<String> delete(@RequestParam("email") String email) {
        return new ResponseEntity<>(userService.deleteByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/key")
    public String getKey(@RequestParam("email") String email) {
        return subscriptionService.generateActivationKey(email.toLowerCase());
    }

    @PostMapping("/pay")
    public String pay(@RequestParam("group") String group, @Valid UserTo userTo,
                      @RequestParam("sum") int sum, @RequestParam("currency") Currency currency, @RequestParam("comment") String comment,
                      @RequestParam(value = "type", required = false) ParticipationType participationType,
                      @RequestParam(value = "template", required = false) String template) {
        UserGroup ug = groupService.pay(userTo, group, new Payment(sum, currency, comment), participationType);
        User refUser = null;
        if (ug.isAlreadyExist()) {
            log.info("User {} already exist in {}", userTo.getEmail(), group);
        } else {
            refUser = refService.getRefUser(ug.getUser());
            if (refUser != null) {
                String project = ug.getGroup().getProject().getName();
                int addBonus = "topjava".equals(project) || "masterjava".equals(project) ? 25 : 10;
                refUser.addBonus(addBonus);
                log.info("!!! Ref Participation from user {}, bonus {}", refUser.getEmail(), refUser.getBonus());
                userService.save(refUser);
                refService.sendMail(refUser, "ref/refParticipation", ImmutableMap.of("project", project, "email", userTo.getEmail(), "addBonus", addBonus));
            }
        }
        return (refUser == null ? "" : "Reference from " + refUser.getEmail() + ", bonus=" + refUser.getBonus() + "\n") +
                ug.toString() + '\n' + (template == null ? "No template" : mailService.sendToUser(template, ug.getUser()));
    }
}