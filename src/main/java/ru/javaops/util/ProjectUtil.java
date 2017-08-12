package ru.javaops.util;

import com.google.common.collect.Maps;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppConfig;
import ru.javaops.model.Group;
import ru.javaops.model.GroupType;
import ru.javaops.model.Project;
import ru.javaops.to.AuthUser;
import ru.javaops.to.pay.ProjectPayDetail;
import ru.javaops.to.pay.ProjectPayDetail.PayDetail;

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


    public static Map<String, PayDetail> getProjectPayDetails(String project) {
        AuthUser authUser = AuthorizedUser.authUser();
        ProjectPayDetail projectPayDetail = AppConfig.projectPayDetails.get(project);
        if (INTERVIEW.equals(project)) {
            return projectPayDetail.getPayIds();
        }
        if (authUser.isPresent(project)) {
            Map<String, PayDetail> payIds = projectPayDetail.getPayIds();
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
                        PayDetail payDetail = entry.getValue();
                        if (payDetail.getPrice() == 0) {
                            entry.setValue(calculatePayDetail(entry.getKey(), projectPayDetail, payDetail, authUser));
                        } else {
                            payDetail.setDiscountPrice(payDetail.getPrice());
                        }
                    });
            return payIds;
        }
        return Collections.emptyMap();
    }

    private static PayDetail calculatePayDetail(String payId,
                                                ProjectPayDetail projectPayDetail, PayDetail payDetail, AuthUser authUser) {
        Map<String, Object> priceMap = projectPayDetail.getPrice();
        Map<String, Object> discountPriceMap = isPriceMember("topjava", priceMap, authUser);
        if (discountPriceMap == null) {
            discountPriceMap = isPriceMember("masterjava", priceMap, authUser);
        }
        if (discountPriceMap == null) {
            discountPriceMap = isPriceMember("member", priceMap, authUser);
        }
        if (discountPriceMap == null) {
            discountPriceMap = priceMap;
        }
        int price = calculatePrice(priceMap, 0, payId);
        int discountPrice = calculatePrice(discountPriceMap, authUser.getBonus(), payId);
        return new PayDetail(price, discountPrice, payDetail.getInfo(), payDetail.getTemplate());
    }

    private static int calculatePrice(Map<String, Object> priceMap, int bonus, String payId) {
        int participantPrice = (Integer) checkNotNull(priceMap.get("participantPrice"), "For %s missed participantPrice", payId);
        int reviewHWPrice = (Integer) checkNotNull(priceMap.get("reviewHWPrice"), "For %s missed reviewHWPrice", payId);
        int price = payId.contains("HW") ? reviewHWPrice : 0;
        if (payId.contains("P")) {
            price += ((participantPrice * Math.max(100 - bonus, 0) + 500) / 1000) * 10;
        }
        return price;
    }

    private static Map<String, Object> isPriceMember(String project, Map<String, Object> price, AuthUser authUser) {
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
