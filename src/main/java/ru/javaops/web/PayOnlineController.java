package ru.javaops.web;

import com.google.common.collect.ImmutableMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * gkislin
 * 19.07.2017
 */
public class PayOnlineController {

    @RequestMapping(value = "/payonline", method = RequestMethod.GET)
    public String profile(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("project") String project) {
        return "redirect:/util/postRedirect";
    }

    @PostMapping("/auth/payonline")
    public ModelAndView profile(@RequestParam("project") String project) {
        return getView(project);
    }

    private ModelAndView getView(String project) {
        ImmutableMap<String, ?> map = ImmutableMap.of();
        return new ModelAndView("payOnline", map);
    }
}
