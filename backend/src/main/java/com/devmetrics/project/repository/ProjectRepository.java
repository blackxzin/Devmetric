package com.devmetrics.project.repository;

import com.devmetrics.project.domain.Project;
import com.devmetrics.project.domain.ProjectSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    Optional<Project> findByUserIdAndSourceAndExternalId(Long userId, ProjectSource source, String externalId);

    Page<Project> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Page<Project> findByUserIdAndArchivedAtIsNullOrderByStartedAtDesc(Long userId, Pageable pageable);

    @Query("select p from Project p left join fetch p.technologies where p.user.id = :userId")
    List<Project> findAllWithTechnologies(@Param("userId") Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndArchivedAtIsNull(Long userId);

    long countByUserIdAndStartedAtGreaterThanEqual(Long userId, LocalDate since);

    long countByUserIdAndStartedAtBetween(Long userId, LocalDate from, LocalDate to);
}
