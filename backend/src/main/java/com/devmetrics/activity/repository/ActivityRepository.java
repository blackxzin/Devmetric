package com.devmetrics.activity.repository;

import com.devmetrics.activity.domain.Activity;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.repository.projection.DailyAggregate;
import com.devmetrics.activity.repository.projection.MonthlyAggregate;
import com.devmetrics.activity.repository.projection.TypeAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    Optional<Activity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndSourceAndExternalId(Long userId, ActivitySource source, String externalId);

    long countByUserId(Long userId);

    long countByUserIdAndType(Long userId, ActivityType type);

    long countByUserIdAndTypeAndActivityDate(Long userId, ActivityType type, LocalDate activityDate);

    long countByUserIdAndTypeAndActivityDateBetween(Long userId, ActivityType type, LocalDate from, LocalDate to);

    long countByUserIdAndActivityDateBetween(Long userId, LocalDate from, LocalDate to);

    List<Activity> findByUserIdAndActivityDateBetweenOrderByOccurredAtAsc(Long userId, LocalDate from, LocalDate to);

    Optional<Activity> findFirstByUserIdOrderByOccurredAtDesc(Long userId);

    @Query("""
            select coalesce(sum(a.points), 0) from Activity a
            where a.user.id = :userId and a.activityDate between :from and :to
            """)
    BigDecimal sumPointsBetween(@Param("userId") Long userId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    @Query("""
            select a.type as type, count(a) as total, coalesce(sum(a.points), 0) as points
            from Activity a
            where a.user.id = :userId and a.activityDate between :from and :to
            group by a.type
            order by count(a) desc
            """)
    List<TypeAggregate> aggregateByType(@Param("userId") Long userId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    @Query("""
            select a.activityDate as day,
                   count(a) as total,
                   coalesce(sum(a.points), 0) as points,
                   count(distinct a.type) as distinctTypes,
                   count(distinct a.technology.id) as distinctTechnologies
            from Activity a
            where a.user.id = :userId and a.activityDate between :from and :to
            group by a.activityDate
            order by a.activityDate
            """)
    List<DailyAggregate> aggregateByDay(@Param("userId") Long userId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    @Query("""
            select distinct a.activityDate from Activity a
            where a.user.id = :userId and a.activityDate <= :until
            order by a.activityDate desc
            """)
    List<LocalDate> findActiveDatesDesc(@Param("userId") Long userId, @Param("until") LocalDate until);

    @Query("""
            select count(distinct a.technology.id) from Activity a
            where a.user.id = :userId and a.activityDate between :from and :to
                  and a.technology is not null
            """)
    long countDistinctTechnologiesBetween(@Param("userId") Long userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    @Query("""
            select count(distinct t.category) from Activity a join a.technology t
            where a.user.id = :userId and a.activityDate between :from and :to
            """)
    long countDistinctTechnologyCategoriesBetween(@Param("userId") Long userId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    @Query(value = """
            select extract(month from activity_date)::int as "monthNumber",
                   count(*) as "total",
                   coalesce(sum(points), 0) as "points",
                   count(distinct type) as "distinctTypes",
                   count(distinct technology_id) as "distinctTechnologies"
            from activities
            where user_id = :userId and extract(year from activity_date) = :year
            group by 1
            order by 1
            """, nativeQuery = true)
    List<MonthlyAggregate> aggregateByMonth(@Param("userId") Long userId, @Param("year") int year);

    @Query("""
            select a from Activity a
            where a.user.id = :userId and a.type = :type
            order by a.occurredAt desc
            limit 5
            """)
    List<Activity> findRecentByType(@Param("userId") Long userId, @Param("type") ActivityType type);

    @Query("""
            select count(distinct a.type) from Activity a
            where a.user.id = :userId and a.activityDate between :from and :to
            """)
    long countDistinctTypesBetween(@Param("userId") Long userId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    long countByProjectId(Long projectId);

    @Query("select coalesce(sum(a.points), 0) from Activity a where a.project.id = :projectId")
    BigDecimal sumPointsByProject(@Param("projectId") Long projectId);
}
