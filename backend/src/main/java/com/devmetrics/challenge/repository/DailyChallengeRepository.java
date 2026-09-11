package com.devmetrics.challenge.repository;

import com.devmetrics.challenge.domain.ChallengeStatus;
import com.devmetrics.challenge.domain.DailyChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    @Query("""
            select c from DailyChallenge c join fetch c.template
            where c.user.id = :userId and c.challengeDate = :date
            """)
    Optional<DailyChallenge> findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    Optional<DailyChallenge> findByIdAndUserId(Long id, Long userId);

    @Query(value = """
            select c from DailyChallenge c join fetch c.template
            where c.user.id = :userId
            order by c.challengeDate desc
            """,
            countQuery = "select count(c) from DailyChallenge c where c.user.id = :userId")
    Page<DailyChallenge> findHistory(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndStatus(Long userId, ChallengeStatus status);

    long countByUserIdAndStatusAndChallengeDateBetween(Long userId, ChallengeStatus status,
                                                       LocalDate from, LocalDate to);
}
