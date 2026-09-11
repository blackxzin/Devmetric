package com.devmetrics.achievement.domain;

import com.devmetrics.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "achievements")
public class Achievement extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(length = 16)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AchievementCategory category;

    @Column(nullable = false)
    private int threshold;

    protected Achievement() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public int getThreshold() {
        return threshold;
    }
}
