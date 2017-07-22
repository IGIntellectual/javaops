package ru.javaops.web;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.service.GroupService;
import ru.javaops.service.UserService;

/**
 * gkislin
 * 19.07.2017
 */
@Controller
public class PayOnlineController {

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @GetMapping("/payonline/callback")
    public void callback(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("project") String project) {
    }

    @GetMapping("/payonline")
    public ModelAndView payOnlineFromMail(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("project") String project) {
        return new ModelAndView("/util/postRedirect",
                ImmutableMap.of("redirectUrl", "/auth/payonline", "project", project));
    }

    @PostMapping("/auth/payonline")
    public ModelAndView payOnline(@RequestParam("project") String project) {
        return new ModelAndView("payOnline", ImmutableMap.of("project", project));
    }
}
