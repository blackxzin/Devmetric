package com.devmetrics.scoring;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.activity.repository.projection.TypeAggregate;
import com.devmetrics.config.AppProperties;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.domain.ScoreComponent;
import com.devmetrics.scoring.domain.ScoreInput;
import com.devmetrics.scoring.domain.ScoreResult;
import com.devmetrics.scoring.domain.ScoreSnapshot;
import com.devmetrics.scoring.dto.ScoreHistoryPoint;
import com.devmetrics.scoring.dto.ScoreResponse;
import com.devmetrics.scoring.repository.ScoreSnapshotRepository;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreService {

    private final ActivityRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final DevScoreCalculator devScoreCalculator;
    private final StreakCalculator streakCalculator;
    private final int windowDays;

    public ScoreService(ActivityRepository activityRepository,
                        ProjectRepository projectRepository,
                        UserTechnologyRepository userTechnologyRepository,
                        ScoreSnapshotRepository scoreSnapshotRepository,
                        DevScoreCalculator devScoreCalculator,
                        StreakCalculator streakCalculator,
                        AppProperties properties) {
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.userTechnologyRepository = userTechnologyRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
        this.devScoreCalculator = devScoreCalculator;
        this.streakCalculator = streakCalculator;
        this.windowDays = properties.scoring().windowDays();
    }

    @Transactional(readOnly = true)
    public ScoreInput buildInput(User user, LocalDate reference) {
        LocalDate windowStart = reference.minusDays(windowDays - 1L);
        Long userId = user.getId();

        BigDecimal rawPoints = activityRepository.sumPointsBetween(userId, windowStart, reference);

        Map<ActivityType, BigDecimal> pointsByType = new EnumMap<>(ActivityType.class);
        for (TypeAggregate aggregate : activityRepository.aggregateByType(userId, windowStart, reference)) {
            pointsByType.put(ActivityType.valueOf(aggregate.getType()),
                    aggregate.getPoints() == null ? BigDecimal.ZERO : aggregate.getPoints());
        }

        List<LocalDate> activeDates = activityRepository.findActiveDatesDesc(userId, reference);
        StreakCalculator.Streak streak = streakCalculator.calculate(activeDates, reference, windowStart);

        Instant windowStartInstant = windowStart.atStartOfDay(user.zoneId()).toInstant();
        long newTechnologies = userTechnologyRepository.countByUserIdAndFirstUsedAtAfter(userId, windowStartInstant);
        long studies = activityRepository.countByUserIdAndTypeAndActivityDateBetween(
                userId, ActivityType.STUDY, windowStart, reference);
        long newProjects = projectRepository.countByUserIdAndStartedAtGreaterThanEqual(userId, windowStart);
        long distinctTechnologies = activityRepository
                .countDistinctTechnologiesBetween(userId, windowStart, reference);
        long distinctCategories = activityRepository
                .countDistinctTechnologyCategoriesBetween(userId, windowStart, reference);

        return new ScoreInput(windowDays, user.getWeeklyGoalPoints(),
                rawPoints == null ? BigDecimal.ZERO : rawPoints,
                streak.activeDaysInWindow(), streak.current(), pointsByType,
                newTechnologies, studies, newProjects, distinctTechnologies, distinctCategories);
    }

    @Transactional(readOnly = true)
    public ScoreResponse current(User user) {
        LocalDate today = LocalDate.now(user.zoneId());
        ScoreResult result = devScoreCalculator.calculate(buildInput(user, today));

        int delta = scoreSnapshotRepository
                .findFirstByUserIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(
                        user.getId(), today.minusDays(30))
                .map(snapshot -> result.devScore() - snapshot.getDevScore())
                .orElse(0);

        String direction = delta > 0 ? "UP" : (delta < 0 ? "DOWN" : "FLAT");
        return new ScoreResponse(result.devScore(), result.level(), windowDays, Instant.now(),
                result.components(), new ScoreResponse.Trend(delta, direction));
    }

    @Transactional
    public ScoreSnapshot snapshot(User user, LocalDate date) {
        ScoreResult result = devScoreCalculator.calculate(buildInput(user, date));
        Map<String, Object> breakdown = new HashMap<>();
        List<Map<String, Object>> components = new ArrayList<>();
        for (ScoreComponent component : result.components()) {
            Map<String, Object> item = new HashMap<>();
            item.put("component", component.component());
            item.put("weight", component.weight());
            item.put("raw", component.raw());
            item.put("normalized", component.normalized());
            item.put("points", component.points());
            components.add(item);
        }
        breakdown.put("level", result.level());
        breakdown.put("components", components);

        return scoreSnapshotRepository.findByUserIdAndSnapshotDate(user.getId(), date)
                .map(existing -> {
                    existing.update(result.devScore(), breakdown);
                    return existing;
                })
                .orElseGet(() -> scoreSnapshotRepository.save(
                        ScoreSnapshot.create(user, date, result.devScore(), breakdown)));
    }

    @Transactional(readOnly = true)
    public List<ScoreHistoryPoint> history(Long userId, LocalDate from, LocalDate to) {
        return scoreSnapshotRepository
                .findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(userId, from, to)
                .stream()
                .map(snapshot -> new ScoreHistoryPoint(snapshot.getSnapshotDate(), snapshot.getDevScore()))
                .toList();
    }
}
