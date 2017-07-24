package ru.javaops.web;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.AuthorizedUser;
import ru.javaops.model.Project;
import ru.javaops.model.User;
import ru.javaops.service.CachedGroups;
import ru.javaops.service.UserService;
import ru.javaops.to.AuthUser;
import ru.javaops.util.ProjectUtil.Detail;

/**
 * gkislin
 * 19.07.2017
 */
@Controller
public class PayOnlineController {

    @Autowired
    private UserService userService;

    @Autowired
    private CachedGroups cachedGroups;

    @PostMapping("/payonline/callback")
//    https://oplata.tinkoff.ru/documentation/?section=notification
    public ResponseEntity<String> callback(@RequestParam("TerminalKey") String terminalKey,
                                           @RequestParam("OrderId") String orderId,
                                           @RequestParam("Success") boolean success,
                                           @RequestParam("Status") boolean status,
                                           @RequestParam("PaymentId") String paymentId,
                                           @RequestParam("ErrorCode") String errorCode,
                                           @RequestParam("Amount") int amount,
                                           @RequestParam("Pan") String pan,
                                           @RequestParam("Token") String token) {

        String[] split = orderId.split("_");
        Project project = cachedGroups.getProject(Integer.valueOf(split[0]));
        Detail detail = cachedGroups.getProjectItemDetails(project.getName() + split[1]);
        User user = userService.get(Integer.valueOf(split[3]));
        int calculatedAmount = getAmount(detail, user.getBonus());
        if (amount != calculatedAmount) {

        }
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/payonline")
    public ModelAndView payOnlineFromMail(@RequestParam("email") String email, @RequestParam("key") String key, @RequestParam("projectItem") String item) {
        return new ModelAndView("/util/postRedirect",
                ImmutableMap.of("redirectUrl", "/auth/payonline", "projectItem", item));
    }

    @PostMapping("/auth/payonline")
    public ModelAndView payOnline(@RequestParam("projectItem") String item) {
        AuthUser authUser = AuthorizedUser.authUser();

        Detail detail = cachedGroups.getProjectItemDetails(item);
        String order = String.format("%d_%s_%d", detail.getProject().getId(), detail.getItem(), authUser.getId());

        return new ModelAndView("payOnline",
                ImmutableMap.of("project", detail.getProject().getName(),
                        "info", detail.getInfo(), "amount", getAmount(detail, authUser.getBonus()), "order", order));
    }

    private int getAmount(Detail detail, int bonus) {
        Integer amount = detail.getPrice();
        Integer bonusAmount = detail.getBonusPrice();
        return (amount == null ? 0 : amount) +
                (bonusAmount == null ? 0 : bonusAmount * Math.max((100 - bonus), 0));
    }
}
