package ru.javaops.web;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.javaops.model.GroupType;
import ru.javaops.model.RegisterType;
import ru.javaops.model.User;
import ru.javaops.service.GroupService;
import ru.javaops.service.MailService;
import ru.javaops.service.MailService.GroupResult;
import ru.javaops.service.UserService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * GKislin
 */

@RestController
@RequestMapping(value = "/api/mail", produces = MediaType.APPLICATION_JSON_VALUE)
public class MailController {
    private static final Logger LOG = LoggerFactory.getLogger(MailController.class);

    @Autowired
    private MailService mailService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/test", method = POST)
    public ResponseEntity<String> sendToUser(@Param("template") String template) {
        String result = mailService.sendTest(template);
        return new ResponseEntity<>(result, MailService.isOk(result) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/to-user", method = POST)
    public ResponseEntity<String> sendToUser(@Param("template") String template, @Param("email") String email) {
        String result = mailService.sendToUser(template, email);
        return new ResponseEntity<>(result, MailService.isOk(result) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/by-location", method = POST)
    public ResponseEntity<GroupResult> sendToUsersByLocation(@Param("template") String template, @Param("location") String location) {
        return sendToGroup(template, userService.findByLocation(location));
    }

    @RequestMapping(value = "/to-users", method = POST)
    public ResponseEntity<GroupResult> sendToUsers(@Param("template") String template, @Param("emails") String emails) {
        GroupResult groupResult = mailService.sendToEmailList(template, Splitter.on(',').trimResults().omitEmptyStrings().splitToList(emails));
        return getGroupResultResponseEntity(groupResult);
    }

    @RequestMapping(value = "/to-groups", method = POST)
    public ResponseEntity<GroupResult> sendToGroup(@Param("template") String template, @Param("includes") String includes,
                                                   @RequestParam(value = "excludes", required = false) String excludes,
                                                   @RequestParam(value = "reg-type", required = false) RegisterType registerType,
                                                   @RequestParam(value = "group-types", required = false) GroupType[] groupTypes,
                                                   @RequestParam(value = "startRegisteredDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startRegisteredDate,
                                                   @RequestParam(value = "endRegisteredDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endRegisteredDate) {
        return sendToGroup(template, groupService.filterUserByGroupNames(includes, excludes, registerType, groupTypes, startRegisteredDate, endRegisteredDate));
    }

    @RequestMapping(value = "/resend", method = POST)
    public ResponseEntity<GroupResult> resend(@Param("template") String template) {
        GroupResult groupResult = mailService.resendTodayFailed(template);
        return getGroupResultResponseEntity(groupResult);
    }

    private ResponseEntity<GroupResult> sendToGroup(String template, Set<User> users) {
        if (users.isEmpty()) {
            return getGroupResultResponseEntity(new GroupResult(0, Collections.emptyList(), null));
        }
        GroupResult groupResult = mailService.sendToUserList(template, users);
        return getGroupResultResponseEntity(groupResult);
    }

    private ResponseEntity<GroupResult> getGroupResultResponseEntity(GroupResult groupResult) {
        LOG.info(groupResult.toString());
        return new ResponseEntity<>(groupResult, groupResult.isOk() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
