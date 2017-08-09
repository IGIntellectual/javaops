package ru.javaops.util;

import com.google.common.collect.Maps;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppConfig;
import ru.javaops.model.Group;
import ru.javaops.model.GroupType;
import ru.javaops.model.Project;
import ru.javaops.to.AuthUser;
import ru.javaops.to.PayDetail;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * gkislin
 * 13.07.2016
 */
public class ProjectUtil {
    public static final String INTERVIEW = "interview";

    public static Props getProps(String projectName, Collection<Group> groups) {
        return new Props(
                getExistedGroupByProjectAndType(groups, projectName, GroupType.REGISTERED),
                getExistedGroupByProjectAndType(groups, projectName, GroupType.CURRENT));
    }


    public static Optional<Group> getGroupByProjectAndType(Collection<Group> groups, String projectName, GroupType type) {
        return groups.stream()
                .filter(g -> g.getProject() != null && g.getProject().getName().equals(projectName) && (g.getType() == type))
                .findFirst();
    }

    public static Group getExistedGroupByProjectAndType(Collection<Group> groups, String projectName, GroupType type) {
        return getGroupByProjectAndType(groups, projectName, type)
                .orElseThrow(() -> new IllegalStateException("В проекте " + projectName + " отсутствуют группы c типом " + type));
    }

    public static PayDetail getPayDetails(String payId) {
        return AppConfig.payDetails.get(payId);
    }

    public static Map<String, PayDetail> getProjectPayDetails(String project) {
        AuthUser authUser = AuthorizedUser.authUser();
        if (authUser.isPresent(project)) {
            Map<String, PayDetail> payDetailMap = AppConfig.projectPayDetails.get(project);
            if (authUser.isCurrent(project) || authUser.isFinished(project)) {
                payDetailMap = Maps.filterKeys(payDetailMap, payId -> !payId.contains("P"));
            } else {
                payDetailMap = Maps.filterKeys(payDetailMap, payId -> payId.contains("P"));
            }
            if (authUser.isFinishedOrHWReview(project)) {
                payDetailMap = Maps.filterKeys(payDetailMap, payId -> !payId.contains("HW"));
            }
            return payDetailMap;
        }
        return Collections.emptyMap();
    }

    public static class Props {
        public final Group registeredGroup;
        public final Group currentGroup;
        public final Project project;

        public Props(Group registeredGroup, Group currentGroup) {
            this.registeredGroup = registeredGroup;
            this.currentGroup = currentGroup;
            this.project = registeredGroup.getProject();
        }
    }
}
