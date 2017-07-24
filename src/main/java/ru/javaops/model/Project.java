package ru.javaops.model;

import com.google.common.base.CaseFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * A Group.
 */
@Entity
@Table(name = "project")
@Getter
@Setter
public class Project extends NamedEntity {

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private Set<Group> group = new HashSet<>();

    @Override
    public String toString() {
        return String.format("<a href='https://github.com/JavaOPs/%s' target='_blank'>%s</a>", name, CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }
}
