package ru.javaops.web;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.AuthorizedUser;
import ru.javaops.model.IdeaCoupon;
import ru.javaops.model.User;
import ru.javaops.repository.UserRepository;
import ru.javaops.service.*;
import ru.javaops.to.AuthUser;
import ru.javaops.to.UserStat;
import ru.javaops.to.UserToExt;
import ru.javaops.util.UserUtil;
import ru.javaops.util.Util;
import ru.javaops.util.exception.NotMemberException;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.util.List;
import java.util.Map;

/**
 * gkislin
 * 19.05.2017
 */

@Controller
@Slf4j
public class ProfileController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private RefService refService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private IdeaCouponService ideaCouponService;

    @Autowired
    private AuthService authService;

    @GetMapping("/auth/profile")
    public ModelAndView profile() {
        return getProfileView(null);
    }

    @GetMapping("/auth/profileER")
    public ModelAndView profileER(@RequestParam(value = "project", required = false) String projectName) {
        return new ModelAndView("profileER", "project", projectName);
    }

    @GetMapping("/participate")
    public ModelAndView participate(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("project") String projectName) {
        return getProfileView(ImmutableMap.of("project", projectName));
    }

    @GetMapping("/profile")
    public String profile(@RequestParam("email") String email, @RequestParam("key") String key) {
        return "redirect:/auth/profile";
    }

    @GetMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam("email") String email, @RequestParam("key") String key) {
        return new ResponseEntity<>(userService.deleteByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/auth/update")
    public String update() {
        authService.updateAuth(AuthorizedUser.authUser());
        return "redirect:/auth/profile";
    }

    @PostMapping("/auth/registerSlack")
    public ModelAndView registerSlack() {
        AuthUser authUser = AuthorizedUser.authUser();
        if (!authUser.isMember()) {
            throw new NotMemberException(authUser.getEmail());
        }
        return subscriptionService.grantGoogleAndSendSlack(authUser, SubscriptionService.JAVAOPS);
    }

    @PostMapping("/auth/save")
    public ModelAndView save(@RequestParam(value = "project", required = false) String project, @Valid UserToExt userToExt, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(Util.getErrorMessage(result));
        }
        User user = userService.update(userToExt);
        AuthorizedUser.authUser().update(user);
        if (StringUtils.isEmpty(project)) {
            partnerService.checkAndProcessNewCandidate(user);
            return new ModelAndView("message/saveProfile");
        } else {
            groupService.checkParticipation(user, project);
            return subscriptionService.grantGoogleAndSendSlack(user, project);
        }
    }

    @GetMapping(value = "/auth/users")
    public ModelAndView usersInfo() {
        List<UserStat> users = userRepository.findAllForStats();
        return (users.stream().anyMatch(u -> u.getEmail().equals(AuthorizedUser.user().getEmail()))) ?
                new ModelAndView("users", "users", users) :
                new ModelAndView("message/statsForbidden");
    }

    private ModelAndView getProfileView(Map<String, ?> params) {
        AuthUser authUser = AuthorizedUser.authUser();
        String aboutMe = UserUtil.normalize(authUser.getAboutMe());

        String refUrl = refService.getRefUrl(null, authUser.getEmail());

        String ideaCoupon = "";
        if (authUser.isMember()) {
            IdeaCoupon coupon = ideaCouponService.getByUser(authUser);
            if (coupon != null) {
                ideaCoupon = coupon.getCoupon();
            }
        }
        ImmutableMap.Builder<String, Object> builder =
                new ImmutableMap.Builder<String, Object>()
                        .put("aboutMe", aboutMe)
                        .put("refUrl", refUrl)
                        .put("ideaCoupon", ideaCoupon);

        if (params != null) {
            builder = builder.putAll(params);
        }
        return new ModelAndView("profile", builder.build());
    }
}