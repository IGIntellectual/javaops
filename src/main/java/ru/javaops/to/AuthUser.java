package ru.javaops.to;

import org.springframework.beans.BeanUtils;
import ru.javaops.model.GroupType;
import ru.javaops.model.ParticipationType;
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
    private Map<String, ParticipationType> currentParticipationTypes = Collections.emptyMap();

    public AuthUser(User user) {
        update(user);
    }

    public void update(User user) {
        BeanUtils.copyProperties(user, this);
    }

    public void update(Map<String, Set<GroupType>> projectGroupTypes, Map<String, ParticipationType> currentParticipationTypes) {
        this.projectGroupTypes = projectGroupTypes;
        this.currentParticipationTypes = currentParticipationTypes;
    }

    public boolean isRegistered(String project) {
        return hasType(project, GroupType.REGISTERED);
    }

    public boolean isCurrent(String project) {
        return hasType(project, GroupType.CURRENT);
    }

    public boolean isFinished(String project) {
        return hasType(project, GroupType.FINISHED);
    }

    public boolean isMember(String project) {
        if ("member".equals(project)) {
            return isMember();
        }
        return isFinished(project) || isCurrent(project);
    }

    public boolean isPresent(String project) {
        return projectGroupTypes.get(project) != null;
    }

    public boolean isPrepaid(String project) {
        ParticipationType pt = currentParticipationTypes.get(project);
        return pt != null && pt == ParticipationType.PREPAID;
    }

    public boolean isFinishedOrHWReview(String project) {
        ParticipationType pt = currentParticipationTypes.get(project);
        return isFinished(project) || (pt != null && pt == ParticipationType.HW_REVIEW);
    }

    private boolean hasType(String project, GroupType type) {
        Set<GroupType> groupTypes = projectGroupTypes.get(project);
        return groupTypes != null && groupTypes.contains(type);
    }

}
