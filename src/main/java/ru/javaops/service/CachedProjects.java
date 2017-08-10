package ru.javaops.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.javaops.model.Project;
import ru.javaops.repository.ProjectRepository;

@Service
@Slf4j
public class CachedProjects {
    @Autowired
    private ProjectRepository projectRepository;

    @Cacheable("project")
    public Project getByName(String name) {
        return projectRepository.getByName(name);
    }
}