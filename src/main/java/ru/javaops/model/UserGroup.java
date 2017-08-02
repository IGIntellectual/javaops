package ru.javaops.model;

import com.google.common.base.MoreObjects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

/**
 * GKislin
 * 02.09.2015.
 */
@Entity
@Table(name = "user_group", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "group_id"}, name = "user_group_unique_idx")})
@Getter
@Setter
public class UserGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @Column(name = "registered_date", columnDefinition = "TIMESTAMP DEFAULT NOW()")
    private Date registeredDate = new Date();

    @Enumerated(EnumType.STRING)
    @Column(name = "register_type")
    private RegisterType registerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_type", nullable = true)
    private ParticipationType participationType;

    @Column(name = "channel")
    private String channel;

    @Transient
    private boolean alreadyExist = false;

    public UserGroup() {
    }

    public UserGroup(User user, Group group, RegisterType type, String channel) {
        this.user = user;
        this.group = group;
        this.registerType = type;
        this.channel = channel;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("channel", channel)
                .add("id", getId())
                .add("user.email", user.getEmail())
                .add("group.name", group.getName())
                .add("registerType", registerType)
                .add("participationType", participationType)
                .add("alreadyExist", alreadyExist)
                .toString();
    }
}
