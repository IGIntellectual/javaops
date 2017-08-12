package ru.javaops.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.javaops.AuthorizedUser;
import ru.javaops.model.*;
import ru.javaops.repository.GroupRepository;
import ru.javaops.repository.UserGroupRepository;
import ru.javaops.to.AuthUser;
import ru.javaops.to.UserMail;
import ru.javaops.to.UserTo;
import ru.javaops.util.ProjectUtil;
import ru.javaops.util.ProjectUtil.Props;
import ru.javaops.util.TimeUtil;
import ru.javaops.util.exception.NotMemberException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * GKislin
 * 15.02.2016
 */
@Service
public class GroupService {
    private final Logger log = LoggerFactory.getLogger(GroupService.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private CachedGroups cachedGroups;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserService userService;

    public boolean isProjectMember(int userId, String projectName) {
        return getGroupsByUserId(userId).stream()
                .anyMatch(g -> g.isMembers() && projectName.equals(g.getProject().getName()));
    }

    public Set<Group> getGroupsByUserId(int userId) {
        log.debug("getGroupsByUserId {}", userId);
        return groupRepository.findByUser(userId);
    }

    @Transactional
    public UserGroup registerAtProject(UserTo userTo, String projectName, String channel) {
        log.info("add{} to project {}", userTo, projectName);

        Props projectProps = getProjectProps(projectName);
        return registerAtGroup(userTo, channel, projectProps.registeredGroup, null,
                user -> {
                    RegisterType registerType = isProjectMember(user.getId(), projectProps.project.getName()) ?
                            RegisterType.REPEAT : RegisterType.REGISTERED;

                    return new UserGroup(user,
                            registerType == RegisterType.REGISTERED ? projectProps.registeredGroup : projectProps.currentGroup,
                            registerType, channel);
                });
    }

    @Transactional
    public UserGroup registerAtGroup(UserTo userTo, String groupName, String channel, ParticipationType participationType) {
        log.info("add{} to group {}", userTo, groupName);
        Group group = cachedGroups.findByName(groupName);
        UserGroup userGroup = registerAtGroup(userTo, channel, group, participationType,
                user -> new UserGroup(user, group, RegisterType.REGISTERED, channel));
        userGroup.setGroup(group);
        return userGroup;
    }

    private UserGroup registerAtGroup(UserTo userTo, String channel, Group newUserGroup, ParticipationType type, Function<User, UserGroup> existedUserGroupProvider) {
        User user = userService.findByEmailOrGmail(userTo.getEmail());
        final UserGroup ug;
        if (user == null) {
            user = userService.create(userTo, channel);
            ug = new UserGroup(user, newUserGroup, RegisterType.FIRST_REGISTERED, channel);
        } else {
            ug = existedUserGroupProvider.apply(user);
        }
        return registerUserGroup(ug, type);
    }

    public UserGroup registerUserGroup(UserGroup ug, ParticipationType type) {
        UserGroup oldUserGroup = userGroupRepository.findByUserIdAndGroupId(ug.getUser().getId(), ug.getGroup().getId());
        if (oldUserGroup != null) {
            oldUserGroup.setRegisterType(RegisterType.DUPLICATED);
            if (type == null || Objects.equals(oldUserGroup.getParticipationType(), type)) {
                return oldUserGroup;
            }
            oldUserGroup.setParticipationType(type);
            return save(oldUserGroup);
        }
        if (ug.getGroup().isMembers() && ug.getRegisterType() == RegisterType.REGISTERED) {
            ug = checkRemoveFromRegistered(ug);
        }
        ug.setParticipationType(type);
        ug = save(ug);
        updateAuthParticipation(AuthorizedUser.user());
        return ug;
    }

    public UserGroup save(UserGroup userGroup) {
        return userGroupRepository.save(userGroup);
    }

    private UserGroup checkRemoveFromRegistered(UserGroup ug) {
        Props projectProps = getProjectProps(ug.getGroup().getProject().getName());
        UserGroup registeredUserGroup = userGroupRepository.findByUserIdAndGroupId(ug.getUser().getId(), projectProps.registeredGroup.getId());
        if (registeredUserGroup == null) {
            return ug;
        }
        registeredUserGroup.setRegisterType(RegisterType.REGISTERED);
        registeredUserGroup.setGroup(ug.getGroup());
        if (ug.getChannel() != null) {
            registeredUserGroup.setChannel(ug.getChannel());
        }
        registeredUserGroup.setRegisteredDate(new Date());
        return registeredUserGroup;
    }

    public Props getProjectProps(String projectName) {
        return ProjectUtil.getProps(projectName, cachedGroups.getAll());
    }

    public Set<UserMail> filterUserByGroupNames(String includes, String excludes, LocalDate startRegisteredDate, LocalDate endRegisteredDate) {
        final List<Group> groups = cachedGroups.getAll();
        final Set<UserMail> includeUsers = filterUserByGroupNames(groups, includes, startRegisteredDate, endRegisteredDate);
        if (StringUtils.isNoneEmpty(excludes)) {
            Set<UserMail> excludeUsers = filterUserByGroupNames(groups, excludes, startRegisteredDate, endRegisteredDate);
            includeUsers.removeAll(excludeUsers);
        }
        return includeUsers;
    }

    public void checkParticipation(User user, String projectName) {
        if (projectName.equals("javaops")) {
            if (!user.isMember()) {
                throw new NotMemberException(user.getEmail());
            }
        } else {
            Props projectProps = getProjectProps(projectName);
            UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(user.getId(), projectProps.currentGroup.getId());
            checkNotNull(userGroup, "Пользователь <b>%s</b> не найден в группе <b>%s</b>", user.getEmail(), projectProps.currentGroup.getName());
            checkArgument(ParticipationType.isParticipant(userGroup.getParticipationType()),
                    "Пожалуйста свяжитесь со мной (<b>skype:grigory.kislin, <a href='mailto:admin@javaops.ru'>admin@javaops.ru</a></b>) по поводу оплаты");
        }
    }

    private Set<UserMail> filterUserByGroupNames(List<Group> groups, String groupNames, LocalDate startRegisteredDate, LocalDate endRegisteredDate) {
        List<Predicate<String>> predicates = getMatcher(groupNames);
        Date startDate = TimeUtil.toDate(startRegisteredDate);
        Date endDate = TimeUtil.toDate(endRegisteredDate);

        // filter users by predicates
        return groups.stream().filter(group -> predicates.stream().anyMatch(p -> p.test(group.getName())))
                .flatMap(group -> userService.findByGroupName(group.getName()).stream())
                .filter(um -> startDate == null || um.getRegisteredDate().compareTo(startDate) >= 0)
                .filter(um -> endDate == null || um.getRegisteredDate().compareTo(endDate) <= 0)
                .collect(Collectors.toSet());
    }

    // group matches predicates
    private List<Predicate<String>> getMatcher(String groupNames) {
        return Arrays.stream(groupNames.split(","))
                .map(String::trim)
                .map(paramName -> paramName.charAt(paramName.length() - 1) == '*' ?
                        new Predicate<String>() {
                            final String startWith = StringUtils.chop(paramName);

                            @Override
                            public boolean test(String name) {
                                return name.startsWith(startWith);
                            }
                        } :
                        (Predicate<String>) paramName::equals
                ).collect(Collectors.toList());
    }

    public void setAuthorized(String email, HttpServletRequest request) {
        setAuthorized(userService.findExistedByEmail(email), request);
    }

    @Transactional(readOnly = true)
    public void updateAuthParticipation(AuthUser user) {
        if (user != null) {
            Set<Group> groups = getGroupsByUserId(user.getId());
            if (!CollectionUtils.isEmpty(groups)) {
                user.update(
                        groups.stream()
                                .filter(g -> g.getProject() != null)
                                .collect(Collectors.toMap(
                                        g -> g.getProject().getName(),
                                        group -> EnumSet.of(group.getType()),
                                        (set1, set2) -> {
                                            set1.addAll(set2);
                                            return set1;
                                        })),
                        groups.stream()
                                .filter(g -> g.getType() == GroupType.CURRENT)
                                .collect(
                                        Collectors.toMap(
                                                g -> g.getProject().getName(),
                                                g -> userGroupRepository.findByUserIdAndGroupId(user.getId(), g.getId()).getParticipationType() == ParticipationType.HW_REVIEW
                                        )
                                )
                );
            }
        }
    }

    public void setAuthorized(User user, HttpServletRequest request) {
        log.info("setAuthorized for '{}', '{}'", user.getEmail(), user.getFullName());
        AuthUser authUser = new AuthUser(user);
        updateAuthParticipation(authUser);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, authUser.getRoles()));
        // Create a new session and add the security context.
        HttpSession session = request.getSession(true);
        session.removeAttribute(AuthorizedUser.PRE_AUTHORIZED);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
