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

    @Autowired
    private RefService refService;

    @GetMapping(value = "/ref/{channel}")
    public ModelAndView rootReference(@PathVariable(value = "channel") String channel, HttpServletResponse response) {
        setCookie(response, channel, "root");
        return new ModelAndView("util/redirectToUrl", "redirectUrl", "/");
    }

    @GetMapping(value = "/ref/{project}/{channel}")
    public ModelAndView projectReference(@PathVariable(value = "channel") String channel,
                                         @PathVariable(value = "project") String project,
                                         HttpServletResponse response) {
        setCookie(response, channel, project);
        return new ModelAndView("util/redirectToUrl", "redirectUrl", "/reg/" + project);
    }

    @GetMapping(value = "/reg/{project}")
    public ModelAndView registration(@PathVariable(value = "project") String project,
                                     @RequestParam(value = "ch", required = false) String channel,
                                     HttpServletResponse response) {
        setCookie(response, "channel", channel, project);
        return new ModelAndView(project);
    }

    private void setCookie(HttpServletResponse response, String channel, String entry) {
        User user = refService.decryptUser(channel);
        if (user == null) {
            setCookie(response, "channel", channel, entry);
        } else {
            log.info("+++ Reference from user {}", user.getEmail());
            setCookie(response, "ref", user.getId().toString(), entry);
        }
    }

    private void setCookie(HttpServletResponse response, String name, String value, String entry) {
        if (value != null) {
            log.info("+++ set Cookie '{}' : '{}' for entry {}", name, value, entry);
            Cookie cookie = new Cookie(name, value);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
            response.addCookie(cookie);
        }
    }
}