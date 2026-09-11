package com.devmetrics.technology.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.common.util.Slugifier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "technologies")
public class Technology extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TechnologyCategory category;

    protected Technology() {
    }

    public static Technology create(String name, TechnologyCategory category) {
        Technology technology = new Technology();
        technology.name = name.trim();
        technology.slug = Slugifier.slugify(name);
        technology.category = category == null ? TechnologyCategory.OTHER : category;
        return technology;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public TechnologyCategory getCategory() {
        return category;
    }
}
