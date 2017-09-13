package ru.javaops.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.javaops.AuthorizedUser;
import ru.javaops.model.Group;
import ru.javaops.model.GroupType;
import ru.javaops.model.ParticipationType;
import ru.javaops.model.User;
import ru.javaops.repository.GroupRepository;
import ru.javaops.repository.UserGroupRepository;
import ru.javaops.to.AuthUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * gkislin
 * 13.09.2017
 */
@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private GroupRepository groupRepository;

    public void setAuthorized(String email, HttpServletRequest request) {
        setAuthorized(userService.findExistedByEmail(email), request);
    }

    public void setAuthorized(User user, HttpServletRequest request) {
        log.info("setAuthorized for '{}', '{}'", user.getEmail(), user.getFullName());
        AuthUser authUser = AuthorizedUser.user();
        if (authUser != null) {
            if (authUser.equals(user)) {
                return;
            }
            request.getSession(false).invalidate();
        }
        authUser = new AuthUser(user);
        updateAuthParticipation(authUser);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, authUser.getRoles()));

        // Create a new session and add the security context.
        // https://stackoverflow.com/a/8336233/548473
        HttpSession session = request.getSession(true);
        session.removeAttribute(AuthorizedUser.PRE_AUTHORIZED);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    public Set<Group> getGroupsByUserId(int userId) {
        log.debug("getGroupsBy UserId={}", userId);
        return groupRepository.findByUser(userId);
    }

    @Transactional(readOnly = true)
    public void updateAuthParticipation(AuthUser authUser) {
        log.info("updateAuthParticipation for {}", authUser);
        if (authUser != null) {
            Set<Group> groups = getGroupsByUserId(authUser.getId());
            if (!CollectionUtils.isEmpty(groups)) {

                Map<String, Set<GroupType>> projectGroupTypes = groups.stream()
                        .filter(g -> g.getProject() != null)
                        .collect(Collectors.toMap(
                                g -> g.getProject().getName(),
                                group -> EnumSet.of(group.getType()),
                                (set1, set2) -> {
                                    set1.addAll(set2);
                                    return set1;
                                }));
                Map<String, ParticipationType> currentParticipationTypes = groups.stream()
                        .filter(g -> g.getType() == GroupType.CURRENT)
                        .collect(
                                Collectors.toMap(
                                        g -> g.getProject().getName(),
                                        g -> userGroupRepository.findByUserIdAndGroupId(authUser.getId(), g.getId()).getParticipationType()
                                )
                        );

                authUser.update(projectGroupTypes, currentParticipationTypes);
            }
        }
    }

    public void updateRoles(User user) {
        AuthUser authUser = AuthorizedUser.user();
        if (authUser != null && authUser.equals(user)) {
            authUser.setRoles(user.getRoles());
        }
    }
}