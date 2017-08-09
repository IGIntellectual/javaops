package ru.javaops.service;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.javaops.model.Project;
import ru.javaops.repository.ProjectRepository;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
@Slf4j
public class CachedProjects {
    @Autowired
    private ProjectRepository projectRepository;

    public static Map<Character, Project> projectMap;

    @PostConstruct
    void postConstruct() {
        projectMap = ImmutableMap.of(
                'I', getByName("interview"),
                'T', getByName("topjava"),
                'M', getByName("masterjava"),
                'B', getByName("basejava")
        );
    }

    @Cacheable("project")
    public Project getByName(String name) {
        return projectRepository.getByName(name);
    }

}