package ru.javaops.to;

import org.springframework.beans.BeanUtils;
import ru.javaops.model.GroupType;
import ru.javaops.model.User;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * gkislin
 * 19.07.2017
 */
public class AuthUser extends User {
    private Map<String, Set<GroupType>> projectGroupTypes = Collections.emptyMap();
    private Map<String, Boolean> projectHWReview = Collections.emptyMap();

    public AuthUser(User user) {
        update(user);
    }

    public void update(User user) {
        BeanUtils.copyProperties(user, this);
    }

    public void update(Map<String, Set<GroupType>> projectGroupTypes, Map<String, Boolean> projectHWReview) {
        this.projectGroupTypes = projectGroupTypes;
        this.projectHWReview = projectHWReview;
    }

    public boolean isRegistered(String project) {
        return hasType(project, GroupType.REGISTERED);
    }

    public boolean isCurrent(String project) {
        return hasType(project, GroupType.CURRENT);
    }

    public boolean isFinishedOrHWReview(String project) {
        return isFinished(project) || (projectHWReview.get(project) != null && projectHWReview.get(project));
    }

    public boolean isFinished(String project) {
        return hasType(project, GroupType.FINISHED);
    }

    private boolean hasType(String project, GroupType type) {
        Set<GroupType> groupTypes = projectGroupTypes.get(project);
        return groupTypes != null && groupTypes.contains(type);
    }

    public boolean isPresent(String project) {
        return projectGroupTypes.get(project) != null;
    }
}
