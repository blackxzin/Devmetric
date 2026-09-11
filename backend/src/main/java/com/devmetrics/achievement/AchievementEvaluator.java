package com.devmetrics.achievement;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.StreakCalculator;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula o progresso de cada conquista.
 *
 * Uma unica leitura do estado do usuario alimenta todos os codigos de conquista,
 * em vez de uma consulta por regra. O catalogo (nome, descricao, limite) vive no
 * banco; aqui fica so o "como medir" de cada codigo.
 */
@Component
public class AchievementEvaluator {

    private final ActivityRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final TechnologyService technologyService;
    private final StreakCalculator streakCalculator;

    public AchievementEvaluator(ActivityRepository activityRepository,
                                ProjectRepository projectRepository,
                                UserTechnologyRepository userTechnologyRepository,
                                TechnologyService technologyService,
                                StreakCalculator streakCalculator) {
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.userTechnologyRepository = userTechnologyRepository;
        this.technologyService = technologyService;
        this.streakCalculator = streakCalculator;
    }

    public Map<String, Long> progressByCode(User user) {
        Long userId = user.getId();
        LocalDate today = LocalDate.now(user.zoneId());

        List<LocalDate> activeDates = activityRepository.findActiveDatesDesc(userId, today);
        StreakCalculator.Streak streak = streakCalculator.calculate(activeDates, today, today.minusDays(29));
        long bestStreak = Math.max(streak.current(), streak.longest());

        Map<String, Long> progress = new HashMap<>();
        progress.put("FIRST_PROJECT", projectRepository.countByUserId(userId));
        progress.put("TEN_PROJECTS", projectRepository.countByUserId(userId));
        progress.put("FIRST_PR", activityRepository.countByUserIdAndType(userId, ActivityType.PULL_REQUEST));
        progress.put("FIRST_ISSUE", activityRepository.countByUserIdAndType(userId, ActivityType.ISSUE));
        progress.put("FIRST_TEST", activityRepository.countByUserIdAndType(userId, ActivityType.TEST));
        progress.put("TEST_MASTER", activityRepository.countByUserIdAndType(userId, ActivityType.TEST));
        progress.put("FIRST_DOC", activityRepository.countByUserIdAndType(userId, ActivityType.DOCUMENTATION));
        progress.put("FIRST_BUG_FIX", activityRepository.countByUserIdAndType(userId, ActivityType.BUG_FIX));
        progress.put("FIRST_STUDY", activityRepository.countByUserIdAndType(userId, ActivityType.STUDY));
        progress.put("HUNDRED_ACTIVITIES", activityRepository.countByUserId(userId));
        progress.put("STREAK_7", bestStreak);
        progress.put("STREAK_30", bestStreak);
        progress.put("FIRST_NEW_TECH", userTechnologyRepository.countByUser(userId));
        progress.put("POLYGLOT", userTechnologyRepository
                .countByUserAndCategory(userId, TechnologyCategory.LANGUAGE));
        progress.put("TOOLBELT", userTechnologyRepository
                .countByUserAndCategory(userId, TechnologyCategory.TOOL));
        progress.put("FIRST_DOCKER", technologyService.hasUsed(userId, "docker") ? 1L : 0L);
        progress.put("FULL_WEEK", activityRepository
                .countDistinctTypesBetween(userId, today.minusDays(6), today));
        return progress;
    }
}
