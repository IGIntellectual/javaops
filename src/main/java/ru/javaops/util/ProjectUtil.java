package ru.javaops.util;

import org.springframework.util.CollectionUtils;
import ru.javaops.model.Group;
import ru.javaops.model.GroupType;
import ru.javaops.model.Project;

import java.util.*;
import java.util.stream.Collectors;

/**
 * gkislin
 * 13.07.2016
 */
public class ProjectUtil {

    public static Props getProps(String projectName, Collection<Group> groups) {
        return new Props(
                getExistedGroupByProjectAndType(groups, projectName, GroupType.REGISTERED),
                getExistedGroupByProjectAndType(groups, projectName, GroupType.CURRENT));
    }


    public static Map<String, Set<GroupType>> getParticipation(Collection<Group> groups) {
        return CollectionUtils.isEmpty(groups) ?
                Collections.emptyMap() :
                groups.stream()
                        .filter(g -> g.getProject() != null)
                        .collect(Collectors.toMap(
                                g -> g.getProject().getName(),
                                group -> EnumSet.of(group.getType()),
                                (set1, set2) -> {
                                    set1.addAll(set2);
                                    return set1;
                                }));
    }

    public static Optional<Group> getGroupByProjectAndType(Collection<Group> groups, String projectName, GroupType type) {
        return groups.stream()
                .filter(g -> g.getProject() != null && g.getProject().getName().equals(projectName) && (g.getType() == type))
                .findFirst();
    }

    public static Group getExistedGroupByProjectAndType(Collection<Group> groups, String projectName, GroupType type) {
        return getGroupByProjectAndType(groups, projectName, type)
                .orElseThrow(() -> new IllegalStateException("В проекте " + projectName + " отсутствуют группы c типом " + type));
    }

    public static class Props {
        public final Group registeredGroup;
        public final Group currentGroup;
        public final Project project;

        public Props(Group registeredGroup, Group currentGroup) {
            this.registeredGroup = registeredGroup;
            this.currentGroup = currentGroup;
            this.project = registeredGroup.getProject();
        }
    }
}
