package com.devmetrics.dashboard;

import com.devmetrics.activity.domain.Activity;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.dto.ActivityResponse;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.activity.repository.projection.TypeAggregate;
import com.devmetrics.dashboard.dto.ActivityBreakdownItem;
import com.devmetrics.dashboard.dto.CalendarDay;
import com.devmetrics.dashboard.dto.CalendarResponse;
import com.devmetrics.dashboard.dto.DashboardSummary;
import com.devmetrics.dashboard.dto.StreakInfo;
import com.devmetrics.dashboard.dto.TopTechnology;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.StreakCalculator;
import com.devmetrics.scoring.ScoreService;
import com.devmetrics.scoring.domain.DailyStat;
import com.devmetrics.scoring.repository.DailyStatRepository;
import com.devmetrics.technology.domain.UserTechnology;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int RECENT_ACTIVITIES = 8;
    private static final int TOP_TECHNOLOGIES = 8;
    private static final int NEW_TECHNOLOGY_DAYS = 30;

    private final ActivityRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final DailyStatRepository dailyStatRepository;
    private final StreakCalculator streakCalculator;
    private final ScoreService scoreService;

    public DashboardService(ActivityRepository activityRepository,
                            ProjectRepository projectRepository,
                            UserTechnologyRepository userTechnologyRepository,
                            DailyStatRepository dailyStatRepository,
                            StreakCalculator streakCalculator,
                            ScoreService scoreService) {
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.userTechnologyRepository = userTechnologyRepository;
        this.dailyStatRepository = dailyStatRepository;
        this.streakCalculator = streakCalculator;
        this.scoreService = scoreService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(User user) {
        Long userId = user.getId();
        LocalDate today = LocalDate.now(user.zoneId());
        LocalDate weekStart = today.minusDays(6);

        long weekActivities = activityRepository.countByUserIdAndActivityDateBetween(userId, weekStart, today);
        BigDecimal weekPoints = orZero(activityRepository.sumPointsBetween(userId, weekStart, today));
        int weekActiveDays = dailyStatRepository
                .findByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, weekStart, today).size();

        double goalProgress = user.getWeeklyGoalPoints() <= 0 ? 0
                : Math.min(1.0, weekPoints.doubleValue() / user.getWeeklyGoalPoints());

        DashboardSummary.WeekSummary week = new DashboardSummary.WeekSummary(
                weekActivities, weekPoints, weekActiveDays, user.getWeeklyGoalPoints(),
                Math.round(goalProgress * 100.0) / 100.0);

        DashboardSummary.Totals totals = new DashboardSummary.Totals(
                activityRepository.countByUserId(userId),
                projectRepository.countByUserId(userId),
                projectRepository.countByUserIdAndArchivedAtIsNull(userId),
                userTechnologyRepository.countByUser(userId));

        List<ActivityResponse> recent = activityRepository
                .findAll((root, query, builder) -> builder.equal(root.get("user").get("id"), userId),
                        PageRequest.of(0, RECENT_ACTIVITIES,
                                org.springframework.data.domain.Sort.by("occurredAt").descending()))
                .map(ActivityResponse::from)
                .getContent();

        return new DashboardSummary(
                scoreService.current(user),
                streak(user),
                week,
                totals,
                topTechnologies(user, today),
                breakdown(userId, today.minusDays(29), today),
                recent);
    }

    @Transactional(readOnly = true)
    public StreakInfo streak(User user) {
        LocalDate today = LocalDate.now(user.zoneId());
        List<LocalDate> activeDates = activityRepository.findActiveDatesDesc(user.getId(), today);
        StreakCalculator.Streak streak = streakCalculator.calculate(activeDates, today, today.minusDays(29));
        LocalDate lastActive = activeDates.isEmpty() ? null : activeDates.get(0);
        return new StreakInfo(streak.current(), streak.longest(), streak.activeDaysInWindow(), lastActive);
    }

    @Transactional(readOnly = true)
    public CalendarResponse calendar(User user, Integer year) {
        LocalDate today = LocalDate.now(user.zoneId());
        int targetYear = year == null ? today.getYear() : year;
        return calendarBetween(user, LocalDate.of(targetYear, 1, 1), LocalDate.of(targetYear, 12, 31));
    }

    /** Ultimos 365 dias terminando hoje: usado pelo perfil publico e pelo badge. */
    @Transactional(readOnly = true)
    public CalendarResponse lastYear(User user) {
        LocalDate today = LocalDate.now(user.zoneId());
        return calendarBetween(user, today.minusDays(364), today);
    }

    private CalendarResponse calendarBetween(User user, LocalDate from, LocalDate to) {
        Map<LocalDate, DailyStat> statsByDate = new HashMap<>();
        for (DailyStat stat : dailyStatRepository
                .findByUserIdAndStatDateBetweenOrderByStatDateAsc(user.getId(), from, to)) {
            statsByDate.put(stat.getStatDate(), stat);
        }

        List<CalendarDay> days = new ArrayList<>((int) ChronoUnit.DAYS.between(from, to) + 1);
        int totalActivities = 0;
        BigDecimal totalPoints = BigDecimal.ZERO;

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DailyStat stat = statsByDate.get(date);
            if (stat == null) {
                days.add(new CalendarDay(date, 0, BigDecimal.ZERO, 0));
                continue;
            }
            days.add(new CalendarDay(date, stat.getActivityCount(), stat.getRawPoints(), stat.getIntensityLevel()));
            totalActivities += stat.getActivityCount();
            totalPoints = totalPoints.add(orZero(stat.getRawPoints()));
        }

        return new CalendarResponse(to.getYear(), from, to, totalActivities, totalPoints,
                statsByDate.size(), days);
    }

    @Transactional(readOnly = true)
    public List<ActivityBreakdownItem> breakdown(Long userId, LocalDate from, LocalDate to) {
        List<TypeAggregate> aggregates = activityRepository.aggregateByType(userId, from, to);
        long total = aggregates.stream().mapToLong(TypeAggregate::getTotal).sum();

        List<ActivityBreakdownItem> items = new ArrayList<>(aggregates.size());
        for (TypeAggregate aggregate : aggregates) {
            ActivityType type = ActivityType.valueOf(aggregate.getType());
            double share = total == 0 ? 0 : (double) aggregate.getTotal() / total;
            items.add(new ActivityBreakdownItem(type.name(), type.label(), aggregate.getTotal(),
                    orZero(aggregate.getPoints()), Math.round(share * 1000.0) / 1000.0));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<TopTechnology> topTechnologies(User user, LocalDate today) {
        Instant newLimit = today.minusDays(NEW_TECHNOLOGY_DAYS).atStartOfDay(user.zoneId()).toInstant();
        List<UserTechnology> technologies = userTechnologyRepository.findAllByUser(user.getId());
        return technologies.stream()
                .limit(TOP_TECHNOLOGIES)
                .map(userTechnology -> new TopTechnology(
                        userTechnology.getTechnology().getName(),
                        userTechnology.getTechnology().getCategory().name(),
                        userTechnology.getUsageCount(),
                        userTechnology.getFirstUsedAt(),
                        userTechnology.getFirstUsedAt().isAfter(newLimit)))
                .toList();
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
