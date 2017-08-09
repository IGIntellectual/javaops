package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.config.AppProperties;
import ru.javaops.model.Role;
import ru.javaops.model.User;
import ru.javaops.util.PasswordUtil;

@Service
@Slf4j
public class SubscriptionService {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private GoogleAdminSDKDirectoryService googleAdminSDKDirectoryService;

    public String getSubscriptionUrl(String email, String activationKey, boolean active) {
        return appProperties.getHostUrl() + "/activate?email=" + email + "&key=" + activationKey + "&activate=" + active;
    }

    public String generateActivationKey(String email) {
        return PasswordUtil.getPasswordEncoder().encode(getSalted(email));
    }

    public void checkActivationKey(String value, String key) {
        if (!PasswordUtil.isMatch(getSalted(value), key)) {
            throw new IllegalArgumentException("!!! Неверный ключ активации:" + value);
        }
    }

    private String getSalted(String value) {
        return value + appProperties.getActivationSecretSalt();
    }

    public void checkAdminKey(String adminKey) {
        if (!userService.findExistedByEmail(adminKey).isAdmin()) {
            throw new IllegalArgumentException("Неверный ключ");
        }
    }

    public ModelAndView grantGoogleAndSendSlack(User user, String project) {
        log.info("grantAllAccess to {}", user);
        IntegrationService.SlackResponse response = integrationService.sendSlackInvitation(user.getEmail(), project);
        String accessResponse = "";
        if (!project.equals("javaops")) {
            accessResponse = grantGoogleDrive(user, project);
        }
        return new ModelAndView("message/registration",
                ImmutableMap.of("response", response, "accessResponse", accessResponse, "project", project));
    }

    public String grantGoogleDrive(User user, String project) {
        String resp = googleAdminSDKDirectoryService.insertMember(project + "@javaops.ru", user.getGmail());
        if (GoogleAdminSDKDirectoryService.OK.equals(resp) && !user.isMember()) {
            // become member after access to GD
            user.getRoles().add(Role.ROLE_MEMBER);
            userService.save(user);
        }
        return resp;
    }
}
