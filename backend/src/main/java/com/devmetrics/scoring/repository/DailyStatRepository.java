package com.devmetrics.scoring.repository;

import com.devmetrics.scoring.domain.DailyStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStatRepository extends JpaRepository<DailyStat, Long> {

    Optional<DailyStat> findByUserIdAndStatDate(Long userId, LocalDate statDate);

    List<DailyStat> findByUserIdAndStatDateBetweenOrderByStatDateAsc(Long userId, LocalDate from, LocalDate to);

    void deleteByUserIdAndStatDateBetween(Long userId, LocalDate from, LocalDate to);
}
