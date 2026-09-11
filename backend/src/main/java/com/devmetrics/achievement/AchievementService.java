package com.devmetrics.achievement;

import com.devmetrics.achievement.domain.Achievement;
import com.devmetrics.achievement.domain.UserAchievement;
import com.devmetrics.achievement.dto.AchievementResponse;
import com.devmetrics.achievement.repository.AchievementRepository;
import com.devmetrics.achievement.repository.UserAchievementRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final AchievementEvaluator evaluator;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              UserRepository userRepository,
                              AchievementEvaluator evaluator) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userRepository = userRepository;
        this.evaluator = evaluator;
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> catalog(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        Map<String, Long> progress = evaluator.progressByCode(user);
        Map<Long, Instant> unlocked = new HashMap<>();
        for (UserAchievement userAchievement : userAchievementRepository.findAllByUser(userId)) {
            unlocked.put(userAchievement.getAchievement().getId(), userAchievement.getUnlockedAt());
        }

        List<AchievementResponse> responses = new ArrayList<>();
        for (Achievement achievement : achievementRepository.findAllByOrderByCategoryAscThresholdAsc()) {
            long current = progress.getOrDefault(achievement.getCode(), 0L);
            int threshold = Math.max(1, achievement.getThreshold());
            Instant unlockedAt = unlocked.get(achievement.getId());
            double ratio = Math.min(1.0, current / (double) threshold);
            responses.add(new AchievementResponse(
                    achievement.getCode(), achievement.getName(), achievement.getDescription(),
                    achievement.getIcon(), achievement.getCategory().name(), threshold,
                    Math.min(current, threshold), Math.round(ratio * 100.0) / 100.0,
                    unlockedAt != null, unlockedAt));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> unlocked(Long userId) {
        return catalog(userId).stream().filter(AchievementResponse::unlocked).toList();
    }

    /**
     * Desbloqueia tudo que o usuario ja alcancou. Idempotente: rodar duas vezes
     * nao cria conquista duplicada (a constraint unica tambem garante isso).
     *
     * @return codigos desbloqueados nesta chamada
     */
    @Transactional
    public List<String> evaluate(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        Map<String, Long> progress = evaluator.progressByCode(user);
        List<String> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : achievementRepository.findAllByOrderByCategoryAscThresholdAsc()) {
            long current = progress.getOrDefault(achievement.getCode(), 0L);
            if (current < achievement.getThreshold()) {
                continue;
            }
            if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
                continue;
            }
            userAchievementRepository.save(UserAchievement.unlock(user, achievement));
            newlyUnlocked.add(achievement.getCode());
        }
        if (!newlyUnlocked.isEmpty()) {
            log.info("Usuario {} desbloqueou {}", userId, newlyUnlocked);
        }
        return newlyUnlocked;
    }
}
