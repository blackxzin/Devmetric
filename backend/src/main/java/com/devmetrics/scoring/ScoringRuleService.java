package com.devmetrics.scoring;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.scoring.domain.EffectiveRule;
import com.devmetrics.scoring.domain.ScoringRule;
import com.devmetrics.scoring.dto.ScoringRuleResponse;
import com.devmetrics.scoring.dto.UpdateScoringRuleRequest;
import com.devmetrics.scoring.repository.ScoringRuleRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolve a regra efetiva de cada tipo de atividade: o override do usuario quando existe,
 * senao a regra global. Nenhum valor de pontuacao fica hardcoded no codigo de negocio.
 */
@Service
public class ScoringRuleService {

    private final ScoringRuleRepository scoringRuleRepository;
    private final UserRepository userRepository;

    public ScoringRuleService(ScoringRuleRepository scoringRuleRepository, UserRepository userRepository) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<ActivityType, EffectiveRule> effectiveRules(Long userId) {
        Map<ActivityType, EffectiveRule> rules = new EnumMap<>(ActivityType.class);
        for (ScoringRule global : scoringRuleRepository.findGlobalRules()) {
            rules.put(global.getActivityType(), global.toEffectiveRule());
        }
        for (ScoringRule override : scoringRuleRepository.findByUserId(userId)) {
            rules.put(override.getActivityType(), override.toEffectiveRule());
        }
        for (ActivityType type : ActivityType.values()) {
            rules.putIfAbsent(type, EffectiveRule.disabled(type));
        }
        return rules;
    }

    @Transactional(readOnly = true)
    public EffectiveRule ruleFor(Long userId, ActivityType type) {
        return scoringRuleRepository.findByUserIdAndActivityType(userId, type)
                .or(() -> scoringRuleRepository.findGlobalRule(type))
                .map(ScoringRule::toEffectiveRule)
                .orElseGet(() -> EffectiveRule.disabled(type));
    }

    @Transactional(readOnly = true)
    public List<ScoringRuleResponse> list(Long userId) {
        Map<ActivityType, EffectiveRule> rules = effectiveRules(userId);
        return List.of(ActivityType.values()).stream()
                .map(rules::get)
                .map(ScoringRuleResponse::from)
                .toList();
    }

    @Transactional
    public ScoringRuleResponse update(Long userId, ActivityType type, UpdateScoringRuleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        ScoringRule rule = scoringRuleRepository.findByUserIdAndActivityType(userId, type)
                .orElseGet(() -> {
                    EffectiveRule current = ruleFor(userId, type);
                    return scoringRuleRepository.save(ScoringRule.override(user, type,
                            current.basePoints(), current.dailyCap(), current.diminishing(), current.active()));
                });
        rule.update(request.basePoints(), request.dailyCap(), request.diminishing(), request.active());
        return ScoringRuleResponse.from(rule.toEffectiveRule());
    }

    @Transactional
    public ScoringRuleResponse reset(Long userId, ActivityType type) {
        scoringRuleRepository.findByUserIdAndActivityType(userId, type)
                .ifPresent(scoringRuleRepository::delete);
        return ScoringRuleResponse.from(scoringRuleRepository.findGlobalRule(type)
                .map(ScoringRule::toEffectiveRule)
                .orElseGet(() -> EffectiveRule.disabled(type)));
    }
}
