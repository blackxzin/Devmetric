package com.devmetrics.achievement.repository;

import com.devmetrics.achievement.domain.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    @Query("""
            select ua from UserAchievement ua
            join fetch ua.achievement
            where ua.user.id = :userId
            order by ua.unlockedAt desc
            """)
    List<UserAchievement> findAllByUser(@Param("userId") Long userId);

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    long countByUserId(Long userId);
}
