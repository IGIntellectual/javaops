package ru.javaops.payment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppConfig;
import ru.javaops.payment.ProjectPayDetail.PayDetail;
import ru.javaops.to.AuthUser;
import ru.javaops.util.UserUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
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

    public static String getInfo(String payId) {
        String project = getProjectName(payId);
        return AppConfig.projectPayDetails.get(project).getPayIds().get(payId).getInfo();
    }

    public static Map<String, Integer> getPostpaidDetails(String project, String payId, AuthUser authUser) {
        ProjectPayDetail projectPayDetail = AppConfig.projectPayDetails.get(project);
        Map<String, PayDetail> payIds = projectPayDetail.getPayIds();
        int prepaidAmount = payIds.get(payId).getPrice();
        checkArgument(prepaidAmount != 0, "prepaidAmount must not be 0");

        Character firstChar = payId.charAt(0);
        return Arrays.stream(new String[]{firstChar + "P", firstChar + "PHW"}).collect(
                Collectors.toMap(
                        pid -> pid,
                        pid -> calculatePayDetail(pid, projectPayDetail, payIds.get(pid), authUser).getDiscountPrice() - prepaidAmount));

    }

    public static Map<String, PayDetail> getPayDetails(String project) {
        AuthUser authUser = AuthorizedUser.authUser();
        ProjectPayDetail projectPayDetail = AppConfig.projectPayDetails.get(project);
        final Map<String, PayDetail> payIds = projectPayDetail.getPayIds();

        if (INTERVIEW.equals(project)) {
            return payIds;
        } else if (authUser.isPrepaid(project)) {
            Map<String, Integer> map = UserUtil.getPrepaidFromAux(authUser);
            return map.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new PayDetail(e.getValue(), e.getValue(), payIds.get(e.getKey()).getInfo(), null)));

        } else if (authUser.isPresent(project)) {
            Map<String, PayDetail> filteredPayIds = Maps.filterKeys(payIds, payId ->
                    (authUser.isCurrent(project) || authUser.isFinished(project)) != payId.contains("P"));
            if (authUser.isFinishedOrHWReview(project)) {
                filteredPayIds = Maps.filterKeys(filteredPayIds, payId -> !payId.contains("HW"));
            }
            if (authUser.isMember("topjava")) {
                filteredPayIds = Maps.filterKeys(filteredPayIds, payId -> !payId.contains("TP"));
            }
            if (authUser.isMember("masterjava")) {
                filteredPayIds = Maps.filterKeys(filteredPayIds, payId -> !payId.contains("MP"));
            }
            filteredPayIds = new LinkedHashMap<>(filteredPayIds);
            filteredPayIds.entrySet().forEach(
                    entry -> entry.setValue(calculatePayDetail(entry.getKey(), projectPayDetail, entry.getValue(), authUser)));
            return filteredPayIds;
        }
        return Collections.emptyMap();
    }

    public static PayDetail getPayDetail(String payId, String project, AuthUser authUser) {
        ProjectPayDetail projectPayDetail = AppConfig.projectPayDetails.get(project);
        PayDetail payDetail = checkNotNull(projectPayDetail.getPayIds().get(payId), "Неверный payId=%s", payId);
        return calculatePayDetail(payId, projectPayDetail, payDetail, authUser);
    }

    private static PayDetail calculatePayDetail(String payId,
                                                ProjectPayDetail projectPayDetail, PayDetail payDetail, AuthUser authUser) {
        int price = payDetail.getPrice();
        if (price == 0) {
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
            payDetail = new PayDetail(price, price, payDetail.getInfo(), payDetail.getTemplate());
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

    public static boolean isPrepaid(String payId) {
        return payId.contains("PR");
    }
}
