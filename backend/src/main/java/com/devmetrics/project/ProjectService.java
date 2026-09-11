package com.devmetrics.project;

import com.devmetrics.activity.ActivityService;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.common.event.ActivityRecordedEvent;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.project.domain.Project;
import com.devmetrics.project.dto.CreateProjectRequest;
import com.devmetrics.project.dto.ProjectResponse;
import com.devmetrics.project.dto.SetTechnologiesRequest;
import com.devmetrics.project.dto.UpdateProjectRequest;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final TechnologyService technologyService;
    private final ApplicationEventPublisher eventPublisher;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ActivityRepository activityRepository,
                          ActivityService activityService,
                          TechnologyService technologyService,
                          ApplicationEventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityService = activityService;
        this.technologyService = technologyService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Long userId, boolean onlyActive, Pageable pageable) {
        Page<Project> page = onlyActive
                ? projectRepository.findByUserIdAndArchivedAtIsNullOrderByStartedAtDesc(userId, pageable)
                : projectRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
        return page.map(ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long userId, Long projectId) {
        Project project = requireProject(userId, projectId);
        return ProjectResponse.from(project,
                activityRepository.countByProjectId(projectId),
                activityRepository.sumPointsByProject(projectId));
    }

    @Transactional
    public ProjectResponse create(Long userId, CreateProjectRequest request) {
        User user = requireUser(userId);
        Project project = Project.manual(user, request.name(), request.description(), request.startedAt());
        if (request.repoUrl() != null) {
            project.update(null, request.description(), request.startedAt(), request.repoUrl());
        }
        applyTechnologies(project, request.technologyIds());
        projectRepository.save(project);

        // Um projeto novo tambem e uma atividade: entra no calendario e no score.
        Technology mainTechnology = project.getTechnologies().stream().findFirst().orElse(null);
        Instant startedAtInstant = project.getStartedAt().atStartOfDay(user.zoneId()).toInstant();
        Instant occurredAt = startedAtInstant.isAfter(Instant.now()) ? Instant.now() : startedAtInstant;
        activityService.record(user, ActivityType.NEW_PROJECT, ActivitySource.MANUAL,
                "Projeto criado: " + project.getName(), project.getDescription(), occurredAt,
                project, mainTechnology, null, Map.of("projectId", project.getId()));

        registerProjectTechnologies(user, project, occurredAt);
        eventPublisher.publishEvent(new ActivityRecordedEvent(userId));
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(Long userId, Long projectId, UpdateProjectRequest request) {
        Project project = requireProject(userId, projectId);
        if (project.isReadOnly()) {
            throw new BusinessException(ErrorCode.READ_ONLY_RESOURCE,
                    "Projeto importado do GitHub e somente leitura");
        }
        project.update(request.name(), request.description(), request.startedAt(), request.repoUrl());
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse setTechnologies(Long userId, Long projectId, SetTechnologiesRequest request) {
        Project project = requireProject(userId, projectId);
        applyTechnologies(project, request.technologyIds());
        registerProjectTechnologies(project.getUser(), project, Instant.now());
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse archive(Long userId, Long projectId) {
        Project project = requireProject(userId, projectId);
        project.archive();
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse unarchive(Long userId, Long projectId) {
        Project project = requireProject(userId, projectId);
        project.unarchive();
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long userId, Long projectId) {
        Project project = requireProject(userId, projectId);
        if (project.isReadOnly()) {
            throw new BusinessException(ErrorCode.READ_ONLY_RESOURCE,
                    "Projeto importado do GitHub nao pode ser removido");
        }
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public Project requireProject(Long userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROJECT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Project> allWithTechnologies(Long userId) {
        return projectRepository.findAllWithTechnologies(userId);
    }

    /**
     * Usado pelo sync do GitHub para anexar tecnologias detectadas sem apagar as existentes.
     */
    @Transactional
    public void attachTechnologies(Long projectId, java.util.Collection<Technology> technologies) {
        projectRepository.findById(projectId).ifPresent(project -> {
            technologies.forEach(project::addTechnology);
            projectRepository.save(project);
        });
    }

    private void applyTechnologies(Project project, List<Long> technologyIds) {
        if (technologyIds == null) {
            return;
        }
        Set<Technology> technologies = new HashSet<>();
        for (Long technologyId : technologyIds) {
            technologies.add(technologyService.requireTechnology(technologyId));
        }
        project.replaceTechnologies(technologies);
    }

    private void registerProjectTechnologies(User user, Project project, Instant moment) {
        for (Technology technology : project.getTechnologies()) {
            technologyService.registerUsage(user, technology, moment);
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Usado pelo sync do GitHub: cria o projeto se ainda nao existir, senao atualiza.
     */
    @Transactional
    public Project upsertFromGitHub(User user, String externalId, String name, String description,
                                    String repoUrl, LocalDate createdAt) {
        return projectRepository
                .findByUserIdAndSourceAndExternalId(user.getId(),
                        com.devmetrics.project.domain.ProjectSource.GITHUB, externalId)
                .map(existing -> {
                    existing.refreshFromGitHub(description, repoUrl);
                    return existing;
                })
                .orElseGet(() -> projectRepository.save(
                        Project.fromGitHub(user, name, description, externalId, repoUrl, createdAt)));
    }
}
