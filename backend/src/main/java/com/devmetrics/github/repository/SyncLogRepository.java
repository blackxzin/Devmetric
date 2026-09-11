package com.devmetrics.github.repository;

import com.devmetrics.github.domain.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    Optional<SyncLog> findByIdAndUserId(Long id, Long userId);

    List<SyncLog> findTop10ByUserIdOrderByStartedAtDesc(Long userId);
}
