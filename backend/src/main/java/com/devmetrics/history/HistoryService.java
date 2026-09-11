package com.devmetrics.history;

import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.activity.repository.projection.MonthlyAggregate;
import com.devmetrics.challenge.ChallengeService;
import com.devmetrics.history.dto.MonthlyHistoryItem;
import com.devmetrics.history.dto.MonthlyHistoryResponse;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.domain.ScoreSnapshot;
import com.devmetrics.scoring.repository.DailyStatRepository;
import com.devmetrics.scoring.repository.ScoreSnapshotRepository;
import com.devmetrics.technology.domain.UserTechnology;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HistoryService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ActivityRepository activityRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final ProjectRepository projectRepository;
    private final DailyStatRepository dailyStatRepository;
    private final ChallengeService challengeService;

    public HistoryService(ActivityRepository activityRepository,
                          ScoreSnapshotRepository scoreSnapshotRepository,
                          UserTechnologyRepository userTechnologyRepository,
                          ProjectRepository projectRepository,
                          DailyStatRepository dailyStatRepository,
                          ChallengeService challengeService) {
        this.activityRepository = activityRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
        this.userTechnologyRepository = userTechnologyRepository;
        this.projectRepository = projectRepository;
        this.dailyStatRepository = dailyStatRepository;
        this.challengeService = challengeService;
    }

    @Transactional(readOnly = true)
    public MonthlyHistoryResponse monthly(User user, Integer year) {
        Long userId = user.getId();
        int targetYear = year == null ? LocalDate.now(user.zoneId()).getYear() : year;

        Map<Integer, MonthlyAggregate> aggregates = new HashMap<>();
        for (MonthlyAggregate aggregate : activityRepository.aggregateByMonth(userId, targetYear)) {
            aggregates.put(aggregate.getMonthNumber(), aggregate);
        }

        Map<Integer, Integer> scoreByMonth = lastScorePerMonth(userId, targetYear);
        Map<Integer, Long> newTechnologiesByMonth = newTechnologiesPerMonth(user, targetYear);

        List<MonthlyHistoryItem> months = new ArrayList<>(12);
        long totalActivities = 0;
        BigDecimal totalPoints = BigDecimal.ZERO;
        int bestScore = 0;

        for (int month = 1; month <= 12; month++) {
            LocalDate from = LocalDate.of(targetYear, month, 1);
            LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

            MonthlyAggregate aggregate = aggregates.get(month);
            long activities = aggregate == null ? 0 : aggregate.getTotal();
            BigDecimal points = aggregate == null || aggregate.getPoints() == null
                    ? BigDecimal.ZERO : aggregate.getPoints();
            int score = scoreByMonth.getOrDefault(month, 0);

            totalActivities += activities;
            totalPoints = totalPoints.add(points);
            bestScore = Math.max(bestScore, score);

            months.add(new MonthlyHistoryItem(
                    month,
                    Month.of(month).getDisplayName(TextStyle.SHORT, PT_BR),
                    activities,
                    points,
                    score,
                    aggregate == null ? 0 : aggregate.getDistinctTypes(),
                    aggregate == null ? 0 : aggregate.getDistinctTechnologies(),
                    newTechnologiesByMonth.getOrDefault(month, 0L),
                    projectRepository.countByUserIdAndStartedAtBetween(userId, from, to),
                    challengeService.completedBetween(userId, from, to),
                    dailyStatRepository.findByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, from, to)
                            .size()));
        }

        int currentScore = scoreSnapshotRepository
                .findFirstByUserIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(
                        userId, LocalDate.now(user.zoneId()))
                .map(ScoreSnapshot::getDevScore)
                .orElse(0);

        return new MonthlyHistoryResponse(targetYear, totalActivities, totalPoints,
                bestScore, currentScore, months);
    }

    private Map<Integer, Integer> lastScorePerMonth(Long userId, int year) {
        Map<Integer, Integer> scoreByMonth = new HashMap<>();
        List<ScoreSnapshot> snapshots = scoreSnapshotRepository
                .findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(userId,
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        // A lista vem em ordem crescente: a ultima gravacao do mes sobrescreve as anteriores.
        for (ScoreSnapshot snapshot : snapshots) {
            scoreByMonth.put(snapshot.getSnapshotDate().getMonthValue(), snapshot.getDevScore());
        }
        return scoreByMonth;
    }

    private Map<Integer, Long> newTechnologiesPerMonth(User user, int year) {
        Map<Integer, Long> byMonth = new HashMap<>();
        for (UserTechnology userTechnology : userTechnologyRepository.findAllByUser(user.getId())) {
            LocalDate firstUsed = userTechnology.getFirstUsedAt().atZone(user.zoneId()).toLocalDate();
            if (firstUsed.getYear() == year) {
                byMonth.merge(firstUsed.getMonthValue(), 1L, Long::sum);
            }
        }
        return byMonth;
    }
}
