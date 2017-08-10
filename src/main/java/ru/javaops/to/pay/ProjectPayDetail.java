package ru.javaops.to.pay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.javaops.model.ParticipationType;
import ru.javaops.model.Project;
import ru.javaops.to.AuthUser;

import java.util.Map;

/**
 * gkislin
 * 27.07.2017
 */
@Getter
@Setter
public class ProjectPayDetail {
    private Map<String, PayDetail> payIds;
    private Map<String, Object> price;

    @Getter
    @Setter
    public static class PayDetail {
        private Project project;
        private final int price;
        private final String info;
        private final String template;

        public PayDetail(@JsonProperty(value = "price", defaultValue = "0") int price,
                         @JsonProperty("info") String info,
                         @JsonProperty(value = "template", defaultValue = "") String template) {
            this.price = price;
            this.info = info;
            this.template = template;
        }
    }

    public ParticipationType findParticipationType(String payId, int amount, AuthUser authUser) {
/*
        boolean isPaid = (amount > calculateAmount(py) - 30);
        return isPaid ? (orderId.contains("HW") ? ParticipationType.HW_REVIEW : ParticipationType.REGULAR) : ParticipationType.PREPAID;
*/
        return ParticipationType.REGULAR;
    }

    public int calculateAmount(String orderId, AuthUser authUser) {
        return 0;
/*
                (price == null ? 0 : price) +
                (bonusPrice == null ? 0 : (bonusPrice * Math.max(100 - bonus, 0) + 50) / 100);
*/
    }
}