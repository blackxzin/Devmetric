package com.devmetrics.scoring.repository;

import com.devmetrics.scoring.domain.ScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScoreSnapshotRepository extends JpaRepository<ScoreSnapshot, Long> {

    Optional<ScoreSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    List<ScoreSnapshot> findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long userId, LocalDate from, LocalDate to);

    Optional<ScoreSnapshot> findFirstByUserIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(
            Long userId, LocalDate date);
}
