package com.devmetrics.activity.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.project.domain.Project;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activity_user_date", columnList = "user_id, activity_date"),
        @Index(name = "idx_activity_user_type_date", columnList = "user_id, type, activity_date")
})
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivitySource source;

    @Column(name = "external_id", length = 120)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_id")
    private Technology technology;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    protected Activity() {
    }

    public static Activity create(User user, ActivityType type, ActivitySource source, String title,
                                  Instant occurredAt, LocalDate activityDate) {
        Activity activity = new Activity();
        activity.user = user;
        activity.type = type;
        activity.source = source;
        activity.title = truncate(title, 255);
        activity.occurredAt = occurredAt;
        activity.activityDate = activityDate;
        return activity;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    public void attachProject(Project project) {
        this.project = project;
    }

    public void attachTechnology(Technology technology) {
        this.technology = technology;
    }

    public void describe(String description) {
        this.description = description;
    }

    public void withExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void putMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public void applyPoints(BigDecimal points) {
        this.points = points == null ? BigDecimal.ZERO : points;
    }

    public void updateContent(ActivityType type, String title, String description,
                              Instant occurredAt, LocalDate activityDate) {
        this.type = type;
        this.title = truncate(title, 255);
        this.description = description;
        this.occurredAt = occurredAt;
        this.activityDate = activityDate;
    }

    public boolean isReadOnly() {
        return source == ActivitySource.GITHUB;
    }

    public User getUser() {
        return user;
    }

    public Project getProject() {
        return project;
    }

    public ActivityType getType() {
        return type;
    }

    public ActivitySource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public Technology getTechnology() {
        return technology;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
