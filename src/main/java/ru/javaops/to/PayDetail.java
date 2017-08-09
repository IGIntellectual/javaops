package ru.javaops.to;

import lombok.Getter;
import lombok.Setter;
import ru.javaops.model.ParticipationType;
import ru.javaops.model.Project;
import ru.javaops.util.ProjectUtil;

/**
 * gkislin
 * 27.07.2017
 */
@Getter
@Setter
public class PayDetail {
    private Project project;
    private Integer price;
    private Integer bonusPrice;
    private String info;
    private String template;

    public void setProject(Project project) {
        this.project = project;
    }

    public ParticipationType findParticipationType(String orderId, int amount, int bonus) {
        boolean isPaid = (amount > calculateAmount(bonus) - 30);
        return isPaid ? (orderId.contains("HW") ? ParticipationType.HW_REVIEW : ParticipationType.REGULAR) : ParticipationType.PREPAID;
    }

    public int calculateAmount(int bonus) {
        return (price == null ? 0 : price) +
                (bonusPrice == null ? 0 : (bonusPrice * Math.max(100 - bonus, 0) + 50) / 100);
    }

    public boolean isInterview() {
        return ProjectUtil.INTERVIEW.equals(project.getName());
    }
}
