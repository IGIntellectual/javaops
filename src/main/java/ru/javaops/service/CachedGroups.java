package ru.javaops.service;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.javaops.model.Group;
import ru.javaops.model.Project;
import ru.javaops.repository.GroupRepository;
import ru.javaops.repository.ProjectRepository;
import ru.javaops.util.ProjectUtil.Detail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * gkislin
 * 17.03.2017
 */
@Service
@Slf4j
public class CachedGroups {
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Cacheable("groups")
    public List<Group> getAll() {
        log.debug("getAll");
        List<Group> groups = groupRepository.findAll();
        Cache cache = cacheManager.getCache("group");
        groups.forEach(g -> cache.put(g.getName(), g));
        return groups;
    }

    @Cacheable("member_groups")
    public Map<Integer, Group> getMembers() {
        List<Group> groups = getAll();
        return groups.stream().filter(Group::isMembers).collect(Collectors.toMap(Group::getId, g -> g));
    }

    @Cacheable("project")
    public Project getProject(String name) {
        return projectRepository.getByName(name);
    }

    @Cacheable("project")
    public Project getProject(int id) {
        return projectRepository.findOne(id);
    }

    @Cacheable("project_item_detail")
    public Detail getProjectItemDetails(String projectItem) {
        String[] split = projectItem.split("_");
        String projectName = split[0];
        String item = split[1];
        Project project = Preconditions.checkNotNull(getProject(projectName), "Отсутствует проект %s", projectName);
        return new Detail(project, item, 1450, null, "");
    }

    @Cacheable("group")
    public Group findByName(String name) {
        List<Group> groups = getAll();
        return groups.stream()
                .filter(g -> name.equals(g.getName()))
                .findAny().orElseThrow(() -> new IllegalArgumentException("Не найдена группа '" + name + '\''));
    }
}
