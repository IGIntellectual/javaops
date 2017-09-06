package ru.javaops.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * GKislin
 * 02.09.2015.
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment extends BaseEntity {

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_group_id", nullable = false)
    @ManyToOne
    private UserGroup userGroup;

    @Column(name = "date")
    private LocalDate date = LocalDate.now();

    @Column(name = "sum")
    private int sum;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    @NotNull
    private Currency currency;

    @Column(name = "comment")
    @NotNull
    private String comment;

    @Override
    public String toString() {
        return "Payment(" + sum + ' ' + currency + ':' + comment + ')';
    }

    public Payment() {
    }

    public Payment(int sum, Currency currency, String comment) {
        this.sum = sum;
        this.currency = currency;
        this.comment = comment;
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }
}
