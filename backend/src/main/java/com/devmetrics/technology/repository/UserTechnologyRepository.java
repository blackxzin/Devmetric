package com.devmetrics.technology.repository;

import com.devmetrics.technology.domain.UserTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserTechnologyRepository extends JpaRepository<UserTechnology, Long> {

    Optional<UserTechnology> findByUserIdAndTechnologyId(Long userId, Long technologyId);

    @Query("""
            select ut from UserTechnology ut
            join fetch ut.technology
            where ut.user.id = :userId
            order by ut.usageCount desc, ut.firstUsedAt desc
            """)
    List<UserTechnology> findAllByUser(@Param("userId") Long userId);

    long countByUserIdAndFirstUsedAtAfter(Long userId, Instant since);

    @Query("select count(ut) from UserTechnology ut where ut.user.id = :userId")
    long countByUser(@Param("userId") Long userId);

    @Query("""
            select count(ut) from UserTechnology ut
            where ut.user.id = :userId and ut.technology.category = :category
            """)
    long countByUserAndCategory(@Param("userId") Long userId,
                                @Param("category") com.devmetrics.technology.domain.TechnologyCategory category);
}
