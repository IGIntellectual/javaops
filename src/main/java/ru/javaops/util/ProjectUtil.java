package ru.javaops.util;

import com.google.common.collect.Maps;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppConfig;
import ru.javaops.model.Group;
import ru.javaops.model.GroupType;
import ru.javaops.model.Project;
import ru.javaops.to.AuthUser;
import ru.javaops.to.pay.ProjectPayDetail;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

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


    public static Map<String, ProjectPayDetail.PayDetail> getProjectPayDetails(String project) {
        AuthUser authUser = AuthorizedUser.authUser();
        ProjectPayDetail projectPayDetail = AppConfig.projectPayDetails.get(project);
        if (INTERVIEW.equals(project)) {
            return projectPayDetail.getPayIds();
        }
        if (authUser.isPresent(project)) {
            Map<String, ProjectPayDetail.PayDetail> payIds = projectPayDetail.getPayIds();
            payIds = Maps.filterKeys(payIds, payId ->
                    (authUser.isCurrent(project) || authUser.isFinished(project)) != payId.contains("P"));
            if (authUser.isFinishedOrHWReview(project)) {
                payIds = Maps.filterKeys(payIds, payId -> !payId.contains("HW"));
            }
            if (authUser.isMember("topjava")) {
                payIds = Maps.filterKeys(payIds, payId -> !payId.contains("TP"));
            }
            if (authUser.isMember("masterjava")) {
                payIds = Maps.filterKeys(payIds, payId -> !payId.contains("MP"));
            }
            payIds = new LinkedHashMap<>(payIds);
            payIds.entrySet().forEach(
                    entry -> {
                        ProjectPayDetail.PayDetail payDetail = entry.getValue();
                        if (payDetail.getPrice() == 0) {
                            entry.setValue(new ProjectPayDetail.PayDetail(
                                    calculatePrice(entry.getKey(), projectPayDetail.getPrice(), authUser), payDetail.getInfo(), payDetail.getTemplate()));
                        }
                    });
            return payIds;
        }
        return Collections.emptyMap();
    }

    private static int calculatePrice(String payId, Map<String, Object> price, AuthUser authUser) {
        Map<String, Object> result = isMember("topjava", price, authUser);
        if (result == null) {
            result = isMember("masterjava", price, authUser);
        }
        if (result == null) {
            result = isMember("member", price, authUser);
        }
        if (result == null) {
            result = price;
        }
        int participantPrice = (Integer) checkNotNull(result.get("participantPrice"), "For %s missed participantPrice", payId);
        int reviewHWPrice = (Integer) checkNotNull(result.get("reviewHWPrice"), "For %s missed reviewHWPrice", payId);

        int resultPrice = payId.contains("HW") ? reviewHWPrice : 0;
        if (payId.contains("P")) {
            resultPrice += ((participantPrice * Math.max(100 - authUser.getBonus(), 0) + 500) / 1000) * 10;
        }
        return resultPrice;
    }

    private static Map<String, Object> isMember(String project, Map<String, Object> price, AuthUser authUser) {
        return authUser.isMember(project) && price.containsKey(project) ? (Map<String, Object>) price.get(project) : null;
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
