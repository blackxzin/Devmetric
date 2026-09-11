package com.devmetrics.scoring;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nivel de intensidade do calendario (0 a 4), relativo ao proprio usuario.
 *
 * Usar percentis em vez de cortes fixos evita dois problemas: quem produz pouco
 * nunca sairia do nivel 1, e quem produz muito ficaria no nivel 4 todos os dias.
 * Com menos de 14 dias de amostra ainda nao ha distribuicao confiavel, entao
 * cai para cortes fixos.
 */
@Component
public class IntensityCalculator {

    private static final int MIN_SAMPLE = 14;
    private static final double FIXED_LEVEL_1 = 5;
    private static final double FIXED_LEVEL_2 = 15;
    private static final double FIXED_LEVEL_3 = 30;

    public record Thresholds(double level2, double level3, double level4, boolean fallback) {
    }

    public Thresholds thresholds(List<Double> positiveDailyPoints) {
        if (positiveDailyPoints == null || positiveDailyPoints.size() < MIN_SAMPLE) {
            return new Thresholds(FIXED_LEVEL_1, FIXED_LEVEL_2, FIXED_LEVEL_3, true);
        }
        List<Double> sorted = new ArrayList<>(positiveDailyPoints);
        Collections.sort(sorted);
        return new Thresholds(percentile(sorted, 0.40), percentile(sorted, 0.70),
                percentile(sorted, 0.90), false);
    }

    public short level(double dailyPoints, Thresholds thresholds) {
        if (dailyPoints <= 0) {
            return 0;
        }
        if (dailyPoints <= thresholds.level2()) {
            return 1;
        }
        if (dailyPoints <= thresholds.level3()) {
            return 2;
        }
        if (dailyPoints <= thresholds.level4()) {
            return 3;
        }
        return 4;
    }

    private static double percentile(List<Double> sortedAscending, double percentile) {
        if (sortedAscending.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile * sortedAscending.size()) - 1;
        index = Math.max(0, Math.min(sortedAscending.size() - 1, index));
        return sortedAscending.get(index);
    }
}
