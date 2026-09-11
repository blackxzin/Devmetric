package com.devmetrics.scoring.repository;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.scoring.domain.ScoringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScoringRuleRepository extends JpaRepository<ScoringRule, Long> {

    @Query("select r from ScoringRule r where r.user is null")
    List<ScoringRule> findGlobalRules();

    @Query("select r from ScoringRule r where r.user is null and r.activityType = :type")
    Optional<ScoringRule> findGlobalRule(@Param("type") ActivityType type);

    List<ScoringRule> findByUserId(Long userId);

    Optional<ScoringRule> findByUserIdAndActivityType(Long userId, ActivityType activityType);
}
