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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;

/**
 * Foto diaria do Dev Score. Guarda o breakdown completo para que o historico
 * continue explicavel mesmo que a formula mude no futuro.
 */
@Entity
@Table(name = "score_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_score_snapshot",
                columnNames = {"user_id", "snapshot_date"}))
public class ScoreSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "dev_score", nullable = false)
    private int devScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> breakdown;

    protected ScoreSnapshot() {
    }

    public static ScoreSnapshot create(User user, LocalDate snapshotDate, int devScore,
                                       Map<String, Object> breakdown) {
        ScoreSnapshot snapshot = new ScoreSnapshot();
        snapshot.user = user;
        snapshot.snapshotDate = snapshotDate;
        snapshot.devScore = devScore;
        snapshot.breakdown = breakdown;
        return snapshot;
    }

    public void update(int devScore, Map<String, Object> breakdown) {
        this.devScore = devScore;
        this.breakdown = breakdown;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public int getDevScore() {
        return devScore;
    }

    public Map<String, Object> getBreakdown() {
        return breakdown;
    }
}
