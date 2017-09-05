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
            Map<String, Object> priceMap = projectPayDetail.getPrice();
            Map<String, Object> discountPriceMap = isPriceMember("topjava", priceMap, authUser);
            if (discountPriceMap == null) {
                discountPriceMap = isPriceMember("masterjava", priceMap, authUser);
            }
            if (discountPriceMap == null) {
                discountPriceMap = isPriceMember("member", priceMap, authUser);
            }
            if (discountPriceMap == null) {
                discountPriceMap = isPrice("early", priceMap);
            }
            if (discountPriceMap == null) {
                discountPriceMap = priceMap;
            }
            int price = calculatePrice(priceMap, 0, payId);
            int discountPrice = calculatePrice(discountPriceMap, authUser.getBonus(), payId);
            payDetail = new PayDetail(price, discountPrice, payDetail.getInfo(), payDetail.getTemplate());
        } else {
            payDetail.setDiscountPrice(payDetail.getPrice());
        }
        return payDetail;
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
        return authUser.isMember(project) ? isPrice(project, price) : null;
    }

    private static Map<String, Object> isPrice(String project, Map<String, Object> price) {
        return price.containsKey(project) ? (Map<String, Object>) price.get(project) : null;
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

    static boolean checkToken(PayCallback payCallback, String terminalPass) {
/*
        final Map<String, String> sortedParameters = new TreeMap<String, String>() {
            {
                put("Password", terminalPass);
                put("TerminalKey", payCallback.terminalKey);
                put("OrderId", payCallback.orderId);
                put("Success", String.valueOf(payCallback.success));
                put("Status", payCallback.status.name());
                put("PaymentId", String.valueOf(payCallback.paymentId));
                put("ErrorCode", String.valueOf(payCallback.errorCode));
                put("Amount", String.valueOf(payCallback.amount));
                put("Pan", payCallback.pan);
                put("CardId", payCallback.cardId);
            }
        };
*/
        final String paramString =
                Joiner.on("").skipNulls().join(new Object[]{
                        payCallback.amount,
                        payCallback.cardId,
                        payCallback.errorCode,
                        payCallback.orderId,
                        payCallback.pan,
                        terminalPass,
                        payCallback.paymentId,
                        payCallback.status,
                        payCallback.success,
                        payCallback.terminalKey});

        final String expectedToken = Hashing.sha256()
                .hashString(paramString, StandardCharsets.UTF_8)
                .toString();

        return expectedToken.equals(payCallback.token);
    }
}
