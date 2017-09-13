package ru.javaops.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import org.springframework.util.StringUtils;
import ru.javaops.model.User;
import ru.javaops.to.UserTo;
import ru.javaops.to.UserToExt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.javaops.util.Util.*;

/**
 * GKislin
 * 16.02.2016
 */
public class UserUtil {
    static final Pattern GMAIL_EXP = Pattern.compile("\\@gmail\\.");

    public static User createFromTo(UserTo userTo) {
        User user = new User(userTo.getEmail(), userTo.getNameSurname(), userTo.getLocation(), userTo.getSkype());
        tryFillGmail(user);
        return user;
    }

    public static User createFromToExt(UserToExt userToExt) {
        User user = createFromTo(userToExt);
        user.setGithub(userToExt.getGithub());
        return user;
    }

    public static void updateFromToExt(User user, UserToExt userToExt) {
        user.setActive(true);
        assignNotEmpty(userToExt.getNameSurname(), user::setFullName);
        assignNotEmpty(userToExt.getInfoSource(), user::setInfoSource);
        assign(userToExt.getLocation(), user::setLocation);
        assign(userToExt.getSkype(), user::setSkype);
        if (user.isMember()) {
            user.setStatsAgree(userToExt.isStatsAgree());
            user.setJobThroughTopjava(userToExt.isJobThroughTopjava());
        } else if (StringUtils.hasText(userToExt.getGmail())) {
            if (!GMAIL_EXP.matcher(userToExt.getGmail()).find()) {
                throw new IllegalArgumentException("Неверный формат gmail");
            }
            // gmail could be changed only if not member!
            user.setGmail(userToExt.getGmail());
        }
        if ((user.getHrUpdate() == null || user.getHrUpdate().isBefore(LocalDate.now())) // not switched back
                && userToExt.isConsiderJobOffers() && !Strings.isNullOrEmpty(userToExt.getResumeUrl())  // visible for HR
                && (user.getConsiderJobOffers() == null || !user.getConsiderJobOffers() || Strings.isNullOrEmpty(user.getResumeUrl()))) {  // was not visible for HR
            user.setHrUpdate(LocalDate.now());
            user.setNewCandidate(true);

        } else if (!userToExt.isConsiderJobOffers() && user.getConsiderJobOffers() != null && user.getConsiderJobOffers()) {  // stop job considering
            user.setHrUpdate(LocalDate.now().plus(90, ChronoUnit.DAYS));
        }
        if (user.isPartner()) {
            user.setPartnerCandidateNotify(userToExt.isPartnerCandidateNotify());
            user.setPartnerCorporateStudy(userToExt.isPartnerCorporateStudy());
        }
        user.setConsiderJobOffers(userToExt.isConsiderJobOffers());
        assign(userToExt.getResumeUrl(), user::setResumeUrl);
        user.setUnderRecruitment(userToExt.isUnderRecruitment());

        user.setRelocationReady(userToExt.isRelocationReady());
        assign(userToExt.getRelocation(), user::setRelocation);

        user.setUnderRecruitment(userToExt.isUnderRecruitment());
        assign(userToExt.getCompany(), user::setCompany);

        assign(userToExt.getAboutMe(), user::setAboutMe);
    }

    public static boolean updateFromAuth(User user, UserToExt userToExt) {
        boolean assign = assignNotOverride(userToExt.getNameSurname(), user.getFullName(), user::setFullName);
        assign |= assignNotEmpty(userToExt.getGithub(), user::setGithub);
        assign |= tryFillGmail(user);
        return assign;
    }

    private static boolean tryFillGmail(User user) {
        if (user.getGmail() == null && GMAIL_EXP.matcher(user.getEmail()).find()) {
            user.setGmail(user.getEmail());
            return true;
        }
        return false;
    }

    public static String normalize(String aboutMe) {
        return aboutMe == null ? "" : aboutMe.replace("\r\n", "<br/>")
                .replace("\n", "<br/>")
                .replace("\r", "<br/>");
    }

    public static int getPostpaidPriceFromAux(String payId, User user) {
        Integer postpaidPrice = getPostpaidMapFromAux(user).get(payId);
        return checkNotNull(postpaidPrice, "Prepaid %s has no aux mapping for %s. Aux=%s", user, payId, user.getAux());
    }

    public static Map<String, Integer> getPostpaidMapFromAux(User user) {
        return StringUtils.isEmpty(user.getAux()) ? Collections.emptyMap() :
                JsonUtil.readValue(user.getAux(), new TypeReference<Map<String, Integer>>() {
                });
    }
}
