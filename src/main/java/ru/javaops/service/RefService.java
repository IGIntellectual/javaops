package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.javaops.config.AppProperties;
import ru.javaops.model.User;
import ru.javaops.util.RefUtil;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
@Slf4j
public class RefService {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    private SecretKey secretKey;

    @PostConstruct
    private void postConstruct() throws NoSuchAlgorithmException {
//        http://stackoverflow.com/questions/10303767/encrypt-and-decrypt-in-java
//        http://sakthipriyan.com/2015/07/21/encryption-and-decrption-in-java.html

        this.secretKey = new SecretKeySpec(appProperties.getSecretKey().getBytes(), "AES");
    }

    public String encrypt(String value) {
        return RefUtil.markRef(RefUtil.encrypt0(value, secretKey));
    }

    private String decrypt(String value) {
        return RefUtil.isRef(value) ? RefUtil.decrypt0(value.substring(1), secretKey) : null;
    }

    public User decryptUser(String value) {
        User user = null;
        String email = decrypt(value);
        if (email != null) {
            user = userService.findByEmail(email);
            if (user == null) {
                log.error("!!! Error user decrypted email '{}'", email);
            }
        }
        return user;
    }

    public User getRefUser(User user) {
        String source = user.getSource();
        if (RefUtil.isRef(source)) {
            User refUser = userService.findByEmail(source.substring(1));
            if (refUser == null) {
                log.error("!!! Error source '{}' of {}", source, user);
            }
            return refUser;
        }
        return null;
    }

    public String getRefUrl(String project, String email) {
        return project == null ?
                String.format("http://javaops.ru/ref/%s", encrypt(email)) :
                String.format("http://javaops.ru/ref/%s/%s", project, encrypt(email));
    }

    public void sendAsyncMail(User refUser, String template, Map<String, ?> params) {
        Map<String, Object> refParams = ImmutableMap.<String, Object>builder()
                .putAll(params)
                .put("javaopsRef", getRefUrl(null, refUser.getEmail())).build();
        mailService.sendWithTemplateAsync(refUser, template, refParams);
    }

    public String findChannel(String refUserId, String cookieChannel, String channel) {
        if (!StringUtils.isEmpty(refUserId)) {
            try {
                User refUser = userService.get(Integer.parseInt(refUserId));
                if (refUser != null) {
                    channel = RefUtil.markRef(refUser.getEmail());
                } else {
                    channel = "UnknownUserId_" + refUserId;
                }
            } catch (Exception e) {
                channel = "UnknownUserId_" + refUserId;
            }
        } else if (!StringUtils.isEmpty(cookieChannel)) {
            channel = cookieChannel;
        }
        return channel;
    }
}