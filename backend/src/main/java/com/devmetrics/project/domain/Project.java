package com.devmetrics.project.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects",
        uniqueConstraints = @UniqueConstraint(name = "uk_project_external",
                columnNames = {"user_id", "source", "external_id"}))
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectSource source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "archived_at")
    private LocalDate archivedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "project_technologies",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id"))
    private Set<Technology> technologies = new HashSet<>();

    protected Project() {
    }

    public static Project manual(User user, String name, String description, LocalDate startedAt) {
        Project project = new Project();
        project.user = user;
        project.name = name.trim();
        project.description = description;
        project.source = ProjectSource.MANUAL;
        project.startedAt = startedAt == null ? LocalDate.now() : startedAt;
        return project;
    }

    public static Project fromGitHub(User user, String name, String description,
                                     String externalId, String repoUrl, LocalDate startedAt) {
        return imported(user, ProjectSource.GITHUB, name, description, externalId, repoUrl, startedAt);
    }

    public static Project imported(User user, ProjectSource source, String name, String description,
                                   String externalId, String repoUrl, LocalDate startedAt) {
        Project project = new Project();
        project.user = user;
        project.name = name.trim();
        project.description = description;
        project.source = source;
        project.externalId = externalId;
        project.repoUrl = repoUrl;
        project.startedAt = startedAt == null ? LocalDate.now() : startedAt;
        return project;
    }

    public void update(String name, String description, LocalDate startedAt, String repoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.description = description;
        if (startedAt != null) {
            this.startedAt = startedAt;
        }
        if (repoUrl != null) {
            this.repoUrl = repoUrl;
        }
    }

    public void refreshFromGitHub(String description, String repoUrl) {
        this.description = description;
        this.repoUrl = repoUrl;
    }

    public void archive() {
        this.archivedAt = LocalDate.now();
    }

    public void unarchive() {
        this.archivedAt = null;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isReadOnly() {
        return source != ProjectSource.MANUAL;
    }

    public void replaceTechnologies(Set<Technology> newTechnologies) {
        this.technologies.clear();
        this.technologies.addAll(newTechnologies);
    }

    public void addTechnology(Technology technology) {
        this.technologies.add(technology);
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProjectSource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public LocalDate getStartedAt() {
        return startedAt;
    }

    public LocalDate getArchivedAt() {
        return archivedAt;
    }

    public Set<Technology> getTechnologies() {
        return technologies;
    }
}
