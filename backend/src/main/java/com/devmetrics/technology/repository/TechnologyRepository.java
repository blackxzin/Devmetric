package com.devmetrics.technology.repository;

import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.domain.TechnologyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {

    Optional<Technology> findBySlug(String slug);

    List<Technology> findByCategoryOrderByNameAsc(TechnologyCategory category);

    List<Technology> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
