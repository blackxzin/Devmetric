package com.devmetrics.scoring;

import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.activity.repository.projection.DailyAggregate;
import com.devmetrics.scoring.domain.DailyStat;
import com.devmetrics.scoring.repository.DailyStatRepository;
import com.devmetrics.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mantem a tabela daily_stats, que e o que o calendario consulta.
 * A tabela e derivada: pode ser apagada e reconstruida a partir das atividades.
 */
@Service
public class DailyStatsService {

    private static final int INTENSITY_WINDOW_DAYS = 90;

    private final ActivityRepository activityRepository;
    private final DailyStatRepository dailyStatRepository;
    private final IntensityCalculator intensityCalculator;

    public DailyStatsService(ActivityRepository activityRepository,
                             DailyStatRepository dailyStatRepository,
                             IntensityCalculator intensityCalculator) {
        this.activityRepository = activityRepository;
        this.dailyStatRepository = dailyStatRepository;
        this.intensityCalculator = intensityCalculator;
    }

    @Transactional
    public void refreshDay(User user, LocalDate day) {
        refreshRange(user, day, day);
    }

    @Transactional
    public void refreshRange(User user, LocalDate from, LocalDate to) {
        LocalDate start = from.isAfter(to) ? to : from;
        LocalDate end = from.isAfter(to) ? from : to;

        Map<LocalDate, DailyAggregate> aggregates = new HashMap<>();
        for (DailyAggregate aggregate : activityRepository.aggregateByDay(user.getId(), start, end)) {
            aggregates.put(aggregate.getDay(), aggregate);
        }

        Map<LocalDate, DailyStat> existing = new HashMap<>();
        for (DailyStat stat : dailyStatRepository
                .findByUserIdAndStatDateBetweenOrderByStatDateAsc(user.getId(), start, end)) {
            existing.put(stat.getStatDate(), stat);
        }

        List<DailyStat> toSave = new ArrayList<>();
        List<DailyStat> toDelete = new ArrayList<>();

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            DailyAggregate aggregate = aggregates.get(day);
            DailyStat stat = existing.get(day);
            if (aggregate == null) {
                if (stat != null) {
                    toDelete.add(stat);
                }
                continue;
            }
            if (stat == null) {
                stat = DailyStat.create(user, day);
            }
            stat.apply((int) aggregate.getTotal(), aggregate.getPoints(),
                    (short) aggregate.getDistinctTypes(), (short) aggregate.getDistinctTechnologies());
            toSave.add(stat);
        }

        if (!toDelete.isEmpty()) {
            dailyStatRepository.deleteAll(toDelete);
        }
        if (!toSave.isEmpty()) {
            dailyStatRepository.saveAll(toSave);
        }

        refreshIntensity(user, end);
    }

    /**
     * Recalcula o nivel de cor de cada dia da janela. Como o nivel e relativo a
     * distribuicao do proprio usuario, um dia muito produtivo muda a escala de todos.
     */
    @Transactional
    public void refreshIntensity(User user, LocalDate reference) {
        LocalDate windowStart = reference.minusDays(INTENSITY_WINDOW_DAYS - 1L);
        List<DailyStat> window = dailyStatRepository
                .findByUserIdAndStatDateBetweenOrderByStatDateAsc(user.getId(), windowStart, reference);
        if (window.isEmpty()) {
            return;
        }
        List<Double> positives = window.stream()
                .map(DailyStat::getRawPoints)
                .filter(points -> points != null && points.signum() > 0)
                .map(BigDecimal::doubleValue)
                .toList();
        IntensityCalculator.Thresholds thresholds = intensityCalculator.thresholds(positives);
        for (DailyStat stat : window) {
            double points = stat.getRawPoints() == null ? 0 : stat.getRawPoints().doubleValue();
            stat.setIntensityLevel(intensityCalculator.level(points, thresholds));
        }
        dailyStatRepository.saveAll(window);
    }

    @Transactional(readOnly = true)
    public List<DailyStat> statsBetween(Long userId, LocalDate from, LocalDate to) {
        return dailyStatRepository.findByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, from, to);
    }
}
