package com.devmetrics.scoring.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agregado diario materializado. O calendario le desta tabela em uma unica consulta,
 * em vez de agregar a tabela de atividades a cada carregamento do dashboard.
 */
@Entity
@Table(name = "daily_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_stat", columnNames = {"user_id", "stat_date"}))
public class DailyStat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "activity_count", nullable = false)
    private int activityCount;

    @Column(name = "raw_points", nullable = false, precision = 8, scale = 2)
    private BigDecimal rawPoints = BigDecimal.ZERO;

    @Column(name = "intensity_level", nullable = false)
    private short intensityLevel;

    @Column(name = "distinct_types", nullable = false)
    private short distinctTypes;

    @Column(name = "distinct_technologies", nullable = false)
    private short distinctTechnologies;

    protected DailyStat() {
    }

    public static DailyStat create(User user, LocalDate statDate) {
        DailyStat stat = new DailyStat();
        stat.user = user;
        stat.statDate = statDate;
        return stat;
    }

    public void apply(int activityCount, BigDecimal rawPoints, short distinctTypes, short distinctTechnologies) {
        this.activityCount = activityCount;
        this.rawPoints = rawPoints == null ? BigDecimal.ZERO : rawPoints;
        this.distinctTypes = distinctTypes;
        this.distinctTechnologies = distinctTechnologies;
    }

    public void setIntensityLevel(short intensityLevel) {
        this.intensityLevel = intensityLevel;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public int getActivityCount() {
        return activityCount;
    }

    public BigDecimal getRawPoints() {
        return rawPoints;
    }

    public short getIntensityLevel() {
        return intensityLevel;
    }

    public short getDistinctTypes() {
        return distinctTypes;
    }

    public short getDistinctTechnologies() {
        return distinctTechnologies;
    }
}
