package com.devmetrics.scoring;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.scoring.domain.ScoreComponent;
import com.devmetrics.scoring.domain.ScoreInput;
import com.devmetrics.scoring.domain.ScoreResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dev Score: 0 a 1000, calculado sobre uma janela movel (padrao 90 dias).
 *
 *   devScore = 400*Volume + 200*Consistencia + 150*Variedade
 *            + 150*Aprendizado + 100*Diversidade tecnologica
 *
 * Volume satura na meta do usuario, entao acumular volume nunca passa de 40% do total.
 * Variedade usa entropia de Shannon: quem so faz commit perde 150 pontos inteiros.
 * Essa combinacao e o que impede o sistema de premiar commit inutil.
 *
 * Funcao pura: mesma entrada, mesma saida. Sem banco, sem relogio.
 */
@Component
public class DevScoreCalculator {

    public static final int MAX_SCORE = 1000;

    private static final int VOLUME_MAX = 400;
    private static final int CONSISTENCY_MAX = 200;
    private static final int VARIETY_MAX = 150;
    private static final int LEARNING_MAX = 150;
    private static final int DIVERSITY_MAX = 100;

    /** Numero de tipos de atividade equilibrados que rende nota maxima em variedade. */
    private static final double VARIETY_TARGET_TYPES = 5.0;
    private static final int STREAK_TARGET_DAYS = 30;
    private static final double NEW_TECH_TARGET = 4.0;
    private static final double STUDY_TARGET = 12.0;
    private static final double NEW_PROJECT_TARGET = 3.0;
    private static final double TECH_TARGET = 8.0;
    private static final double CATEGORY_TARGET = 5.0;

    public ScoreResult calculate(ScoreInput input) {
        List<ScoreComponent> components = new ArrayList<>(5);

        components.add(volume(input));
        components.add(consistency(input));
        components.add(variety(input));
        components.add(learning(input));
        components.add(diversity(input));

        int total = components.stream().mapToInt(ScoreComponent::points).sum();
        total = Math.max(0, Math.min(MAX_SCORE, total));
        return new ScoreResult(total, ScoreResult.levelFor(total), components);
    }

    private ScoreComponent volume(ScoreInput input) {
        double weeks = Math.max(1.0, input.windowDays() / 7.0);
        double target = Math.max(1.0, input.weeklyGoalPoints() * weeks);
        double raw = input.rawPoints() == null ? 0 : input.rawPoints().doubleValue();
        double normalized = clamp(raw / target);
        return component("VOLUME", "Volume", VOLUME_MAX, raw, normalized,
                String.format(Locale.ROOT,
                        "%.0f de %.0f pontos na meta de %d dias. Satura na meta: acumular volume nao passa de 40%% do score.",
                        raw, target, input.windowDays()));
    }

    private ScoreComponent consistency(ScoreInput input) {
        double window = Math.max(1, input.windowDays());
        double spread = clamp(input.activeDays() / window);
        double streak = clamp(input.currentStreak() / (double) STREAK_TARGET_DAYS);
        double normalized = clamp(0.6 * spread + 0.4 * streak);
        return component("CONSISTENCY", "Consistencia", CONSISTENCY_MAX, input.activeDays(), normalized,
                String.format(Locale.ROOT,
                        "%d dias ativos em %d e sequencia atual de %d dias. Distribuir vale mais que concentrar.",
                        input.activeDays(), input.windowDays(), input.currentStreak()));
    }

    private ScoreComponent variety(ScoreInput input) {
        double entropy = shannonEntropy(input.pointsByType());
        double normalized = clamp(entropy / Math.log(VARIETY_TARGET_TYPES));
        int distinctTypes = input.pointsByType() == null ? 0 : (int) input.pointsByType().values().stream()
                .filter(value -> value != null && value.signum() > 0)
                .count();
        return component("VARIETY", "Variedade", VARIETY_MAX, distinctTypes, normalized,
                String.format(Locale.ROOT,
                        "%d tipos de atividade com pontos. Nota maxima com 5 tipos equilibrados; so commit zera este componente.",
                        distinctTypes));
    }

    private ScoreComponent learning(ScoreInput input) {
        double newTech = clamp(input.newTechnologies() / NEW_TECH_TARGET);
        double study = clamp(input.studyActivities() / STUDY_TARGET);
        double projects = clamp(input.newProjects() / NEW_PROJECT_TARGET);
        double normalized = clamp(0.5 * newTech + 0.3 * study + 0.2 * projects);
        return component("LEARNING", "Aprendizado", LEARNING_MAX, input.newTechnologies(), normalized,
                String.format(Locale.ROOT,
                        "%d tecnologias novas, %d estudos registrados e %d projetos iniciados na janela.",
                        input.newTechnologies(), input.studyActivities(), input.newProjects()));
    }

    private ScoreComponent diversity(ScoreInput input) {
        double technologies = clamp(input.distinctTechnologies() / TECH_TARGET);
        double categories = clamp(input.distinctTechnologyCategories() / CATEGORY_TARGET);
        double normalized = clamp(0.6 * technologies + 0.4 * categories);
        return component("TECH_DIVERSITY", "Diversidade tecnologica", DIVERSITY_MAX,
                input.distinctTechnologies(), normalized,
                String.format(Locale.ROOT,
                        "%d tecnologias em %d categorias. Categorias diferentes valem mais que varios frameworks da mesma familia.",
                        input.distinctTechnologies(), input.distinctTechnologyCategories()));
    }

    /**
     * Entropia de Shannon sobre a distribuicao de pontos por tipo de atividade.
     * Retorna 0 quando ha um unico tipo (ou nenhum).
     */
    public double shannonEntropy(Map<ActivityType, BigDecimal> pointsByType) {
        if (pointsByType == null || pointsByType.isEmpty()) {
            return 0;
        }
        double total = pointsByType.values().stream()
                .filter(value -> value != null && value.signum() > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        if (total <= 0) {
            return 0;
        }
        double entropy = 0;
        for (BigDecimal value : pointsByType.values()) {
            if (value == null || value.signum() <= 0) {
                continue;
            }
            double probability = value.doubleValue() / total;
            entropy -= probability * Math.log(probability);
        }
        return entropy;
    }

    private ScoreComponent component(String code, String label, int maxPoints,
                                     double raw, double normalized, String explanation) {
        int points = (int) Math.round(normalized * maxPoints);
        double weight = maxPoints / (double) MAX_SCORE;
        return new ScoreComponent(code, label, weight, round2(raw), round2(normalized),
                points, maxPoints, explanation);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0;
        }
        return Math.min(1.0, value);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
