package com.devmetrics.challenge.repository;

import com.devmetrics.challenge.domain.ChallengeTemplate;
import com.devmetrics.challenge.domain.ChallengeTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeTemplateRepository extends JpaRepository<ChallengeTemplate, Long> {

    List<ChallengeTemplate> findByTriggerAndActiveTrue(ChallengeTrigger trigger);

    List<ChallengeTemplate> findByActiveTrue();
}
