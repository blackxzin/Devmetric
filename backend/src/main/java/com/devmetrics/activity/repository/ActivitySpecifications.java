package com.devmetrics.activity.repository;

import com.devmetrics.activity.domain.Activity;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Filtros da listagem de atividades. O filtro por usuario e sempre obrigatorio:
 * nenhuma consulta de atividade existe sem ele.
 */
public final class ActivitySpecifications {

    private ActivitySpecifications() {
    }

    public static Specification<Activity> ownedBy(Long userId) {
        return (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Activity> ofType(ActivityType type) {
        return (root, query, builder) -> type == null ? null : builder.equal(root.get("type"), type);
    }

    public static Specification<Activity> ofSource(ActivitySource source) {
        return (root, query, builder) -> source == null ? null : builder.equal(root.get("source"), source);
    }

    public static Specification<Activity> ofProject(Long projectId) {
        return (root, query, builder) -> projectId == null
                ? null
                : builder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Activity> from(LocalDate from) {
        return (root, query, builder) -> from == null
                ? null
                : builder.greaterThanOrEqualTo(root.get("activityDate"), from);
    }

    public static Specification<Activity> to(LocalDate to) {
        return (root, query, builder) -> to == null
                ? null
                : builder.lessThanOrEqualTo(root.get("activityDate"), to);
    }
}
