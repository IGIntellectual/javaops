package ru.javaops.web;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ru.javaops.SqlResult;
import ru.javaops.model.Group;
import ru.javaops.model.Project;
import ru.javaops.model.User;
import ru.javaops.repository.UserRepository;
import ru.javaops.service.CachedGroups;
import ru.javaops.service.CachedProjects;
import ru.javaops.service.PartnerService;
import ru.javaops.service.SqlService;
import ru.javaops.to.UserAdminsInfo;
import ru.javaops.to.UserJobWanted;
import ru.javaops.to.UserJobWantedBrief;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * gkislin
 * 15.07.2017
 */
@Controller
@Slf4j
public class PartnerController {
    @Autowired
    private PartnerService partnerService;

    @Autowired
    private SqlService sqlService;

    @Autowired
    private CachedGroups cachedGroups;

    @Autowired
    private CachedProjects cachedProjects;

    @Autowired
    private UserRepository userRepository;

    @GetMapping(value = "/user")
    public ModelAndView userInfo(@RequestParam("email") String email,
                                 @RequestParam("partnerKey") String partnerKey) {

        User partner = partnerService.checkPartner(partnerKey);
        User user = userRepository.findByEmailWithGroup(email);
        List<Project> projects = getProjects(user).collect(Collectors.toList());
        return new ModelAndView("userInfo",
                ImmutableMap.of("user", user, "projects", projects, "partner", partner));
    }

    @PostMapping(value = "/saveAdminInfo")
    public String saveComment(@RequestParam("email") String email,
                              @RequestParam("adminKey") String adminKey,
                              UserAdminsInfo uaInfo) {
//        userRepository.saveAdminInfo(email, uaInfo);
        userRepository.saveAdminInfo(email, uaInfo.getComment(), uaInfo.getMark(), uaInfo.getBonus());
        return "util/closeWindow";
    }

    @GetMapping(value = "/sql")
    public ModelAndView sqlExecute(@RequestParam("sql_key") String sqlKey,
                                   @RequestParam(value = "limit", required = false) Integer limit,
                                   @RequestParam(value = "csv", required = false, defaultValue = "false") boolean csv,
                                   @RequestParam("partnerKey") String partnerKey,
                                   @RequestParam(value = "fromDate", required = false) String fromDate,
                                   @RequestParam Map<String, String> params) {

        User partner = partnerService.checkPartner(partnerKey);
        params.put("partnerKey", partnerKey);
        params.put("partnerMark", partner.getMark());
        params.put("fromDate", fromDate == null ? "01-01-01" : fromDate);
        SqlResult result = sqlService.execute(sqlKey, limit, params);
        return new ModelAndView("sqlResult",
                ImmutableMap.of("result", result, "csv", csv));
    }

    @PostMapping(value = "/memberList")
    @ResponseBody
    public List<UserJobWantedBrief> memberList(@RequestParam("channel") String channel, @RequestParam("channelKey") String channelKey,
                                               @RequestParam(value = "project", required = false) String projectName) {
        if (StringUtils.isEmpty(projectName)) {
            return userRepository.findAllJobWanted();
        } else {
            Project project = Preconditions.checkNotNull(cachedProjects.getByName(projectName), "Project %s not found", projectName);
            return userRepository.findProjectJobWanted(project.getId());
        }
    }

    @PostMapping(value = "/member")
    @ResponseBody
    public UserJobWanted member(@RequestParam("channel") String channel, @RequestParam("channelKey") String channelKey,
                                @RequestParam(value = "github", required = false) String github,
                                @RequestParam(value = "email", required = false) String email) {
        if (StringUtils.isEmpty(github) == StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("must be defined github or email");
        }
        User u = Preconditions.checkNotNull(StringUtils.isEmpty(github) ?
                userRepository.findByEmailWithGroup(email) : userRepository.findByGitHubWithGroup(github), "User not found");
        String projects = getProjects(u).map(Project::getName).collect(Collectors.joining(","));
        return new UserJobWanted(
                u.getFullName(), u.getEmail(), u.getLocation(), u.getSkype(), u.getResumeUrl(), u.getRelocationReady(), u.getRelocation(), u.getGithub(), u.getHrUpdate(),
                u.getAboutMe(), projects);
    }

    private Stream<Project> getProjects(User user) {
        Map<Integer, Group> groupMembers = cachedGroups.getMembers();
        return user.getUserGroups().stream()
                .filter(ug -> groupMembers.containsKey(ug.getGroup().getId()))
                .map(ug -> groupMembers.get(ug.getGroup().getId()).getProject())
                .distinct();
    }
}