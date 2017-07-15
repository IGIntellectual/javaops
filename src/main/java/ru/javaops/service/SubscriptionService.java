package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.config.AppProperties;
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
            throw new IllegalArgumentException("Неверный ключ активации");
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

    public ModelAndView grantGoogleAndSendSlack(String email, String gmail, String project) {
        log.info("grantAllAccess to {}/{}", email, gmail);
        IntegrationService.SlackResponse response = integrationService.sendSlackInvitation(email, project);
        String accessResponse = "";
        if (!project.equals("javaops")) {
            accessResponse = grantGoogleDrive(project, gmail);
        }
        return new ModelAndView("registration",
                ImmutableMap.of("response", response, "email", email,
                        "accessResponse", accessResponse, "project", project));
    }

    public String grantGoogleDrive(String project, String gmail) {
        return googleAdminSDKDirectoryService.insertMember(project + "@javaops.ru", gmail);
    }
}
