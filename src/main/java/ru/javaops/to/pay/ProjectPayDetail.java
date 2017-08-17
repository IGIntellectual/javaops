package ru.javaops.to.pay;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.javaops.model.Project;

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
    private Project project;

    @Getter
    @Setter
    public static class PayDetail {
        private int discountPrice;
        private final int price;
        private final String info;
        private final String template;

        public PayDetail(int price, int discountPrice, String info, String template) {
            this.price = price;
            this.discountPrice = discountPrice;
            this.info = info;
            this.template = template;
        }

        @JsonCreator
        public PayDetail(@JsonProperty(value = "price", defaultValue = "0") int price,
                         @JsonProperty("info") String info,
                         @JsonProperty(value = "template", defaultValue = "") String template) {
            this.price = price;
            this.info = info;
            this.template = template;
        }
    }
}