package ru.javaops.payment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppConfig;
import ru.javaops.model.ParticipationType;
import ru.javaops.model.RegisterType;
import ru.javaops.payment.ProjectPayDetail.PayDetail;
import ru.javaops.to.AuthUser;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * gkislin
 * 05.09.2017
 */
public class PayUtil {
    public static final String INTERVIEW = "interview";

    public static final Map<Character, String> PROJECT_MAP = ImmutableMap.of(
            'I', INTERVIEW,
            'T', "topjava",
            'M', "masterjava",
            'B', "basejava"
    );

    public static Map<String, PayDetail> getPayDetails(String project) {
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
                    entry -> entry.setValue(calculatePayDetail(entry.getKey(), projectPayDetail, entry.getValue(), authUser)));
            return payIds;
        }
        return Collections.emptyMap();
    }

    public static PayDetail getPayDetail(String payId, String project) {
        ProjectPayDetail projectPayDetail = AppConfig.projectPayDetails.get(project);
        PayDetail payDetail = checkNotNull(projectPayDetail.getPayIds().get(payId), "Неверный payId=%s", payId);
        return calculatePayDetail(payId, projectPayDetail, payDetail, AuthorizedUser.authUser());
    }

    private static PayDetail calculatePayDetail(String payId,
                                                ProjectPayDetail projectPayDetail, PayDetail payDetail, AuthUser authUser) {
        if (payDetail.getPrice() == 0) {
            Map<String, Integer> priceMap = projectPayDetail.getPrices();
            int participant = checkNotNull(priceMap.get("participant"), "For %s missed participant", payId);
            int reviewHW = checkNotNull(priceMap.get("reviewHW"), "For %s missed reviewHW", payId);

            Integer discount = isPriceMember("topjava", priceMap, authUser);
            if (discount == null) {
                discount = isPriceMember("masterjava", priceMap, authUser);
            }
            if (discount == null) {
                discount = isPriceMember("member", priceMap, authUser);
            }
            if (discount == null) {
                discount = priceMap.get("early");
            }
            int reviewPrice = payId.contains("HW") ? reviewHW : 0;
            int participantPrice = reviewPrice + calculatePrice(participant, 0, payId);
            int discountPrice = (discount == null ? participantPrice : reviewPrice + calculatePrice(discount, authUser.getBonus(), payId));
            payDetail = new PayDetail(participantPrice, discountPrice, payDetail.getInfo(), payDetail.getTemplate());
        } else {
            payDetail.setDiscountPrice(payDetail.getPrice());
        }
        return payDetail;
    }

    private static int calculatePrice(int participant, int bonus, String payId) {
        return payId.contains("P") ? ((participant * Math.max(100 - bonus, 0) + 500) / 1000) * 10 : 0;
    }

    private static Integer isPriceMember(String project, Map<String, Integer> price, AuthUser authUser) {
        return authUser.isMember(project) ? price.get(project) : null;
    }

    public static String getProjectName(String payId) {
        return checkNotNull(PROJECT_MAP.get(payId.charAt(0)));
    }

    public static ParticipationType getParticipation(String payId, PayDetail payDetail, int amount, RegisterType registerType) {
        if (amount + 30 >= payDetail.getDiscountPrice()) {
            if (payId.contains("HW")) {
                if (payId.contains("P") || registerType == RegisterType.DUPLICATED) {
                    return ParticipationType.HW_REVIEW;
                }
            } else if (payId.contains("P")) {
                return ParticipationType.REGULAR;
            }
        }
        return null;
    }

    static boolean checkToken(Map<String, String> requestParams, String terminalPass) {
        final Map<String, String> sortedParams = new TreeMap<>(requestParams);
        sortedParams.put("Password", terminalPass);
        String token = sortedParams.remove("Token");
        String paramString = Joiner.on("").skipNulls().join(sortedParams.values());

        final String expectedToken = Hashing.sha256()
                .hashString(paramString, StandardCharsets.UTF_8)
                .toString();

        return expectedToken.equals(token);
    }
}
