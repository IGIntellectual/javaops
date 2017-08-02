package ru.javaops.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.model.User;
import ru.javaops.service.RefService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Controller
@Slf4j
public class RefController {

    public static final String COOKIE_REF ="ref";
    public static final String COOKIE_CHANNEL ="channel";

    @Autowired
    private RefService refService;

    @GetMapping(value = "/ref/{channel}")
    public ModelAndView rootReference(@PathVariable(value = "channel") String channel, HttpServletResponse response) {
        setCookie(response, channel);
        return new ModelAndView("util/redirectToUrl", "redirectUrl", "/");
    }

    @GetMapping(value = "/ref/{project}/{channel}")
    public ModelAndView projectReference(@PathVariable(value = "channel") String channel,
                                         @PathVariable(value = "project") String project,
                                         HttpServletResponse response) {
        setCookie(response, channel);
        return new ModelAndView("util/redirectToUrl", "redirectUrl", "/reg/" + project);
    }

    @GetMapping(value = "/reg/{project}")
    public ModelAndView registration(@PathVariable(value = "project") String project,
                                     @RequestParam(value = "ch", required = false) String channel,
                                     HttpServletResponse response) {
        setCookie(response, COOKIE_CHANNEL, channel);
        return new ModelAndView(project);
    }

    private void setCookie(HttpServletResponse response, String channel) {
        User user = refService.decryptUser(channel);
        if (user == null) {
            setCookie(response, COOKIE_CHANNEL, channel);
        } else {
            log.info("+++ Reference from user {}", user.getEmail());
            setCookie(response, COOKIE_REF, user.getId().toString());
        }
    }

    private void setCookie(HttpServletResponse response, String name, String value) {
        if (value != null) {
            log.info("+++ set Cookie '{}' : '{}'", name, value);
            Cookie cookie = new Cookie(name, value);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
            response.addCookie(cookie);
        }
    }
}