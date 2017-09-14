package ru.javaops.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Set;

/**
 * A Group.
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
public class Group extends NamedEntity {

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private GroupType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<UserGroup> groupUsers;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    public Role getRole() {
        return role;
    }

    public boolean isMembers() {
        return type == GroupType.CURRENT || type == GroupType.FINISHED;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + getId() +
                ", name=" + name +
                '}';
    }
}
