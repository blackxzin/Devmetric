package com.devmetrics.scoring;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.scoring.domain.ScoreComponent;
import com.devmetrics.scoring.domain.ScoreInput;
import com.devmetrics.scoring.domain.ScoreResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevScoreCalculatorTest {

    private static final int WINDOW = 90;
    private static final int WEEKLY_GOAL = 150;

    private final DevScoreCalculator calculator = new DevScoreCalculator();

    private int componentPoints(ScoreResult result, String component) {
        return result.components().stream()
                .filter(item -> item.component().equals(component))
                .mapToInt(ScoreComponent::points)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("usuario sem nenhuma atividade tem score zero")
    void emptyUserScoresZero() {
        ScoreResult result = calculator.calculate(ScoreInput.empty(WINDOW, WEEKLY_GOAL));

        assertThat(result.devScore()).isZero();
        assertThat(result.level()).isEqualTo("Iniciando");
        assertThat(result.components()).hasSize(5);
    }

    @Test
    @DisplayName("usuario que so faz commit zera o componente de variedade")
    void singleActivityTypeZeroesVariety() {
        Map<ActivityType, BigDecimal> pointsByType = new EnumMap<>(ActivityType.class);
        pointsByType.put(ActivityType.COMMIT, new BigDecimal("1000"));

        ScoreResult result = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                new BigDecimal("1000"), 45, 5, pointsByType, 0, 0, 0, 1, 1));

        assertThat(componentPoints(result, "VARIETY")).isZero();
    }

    @Test
    @DisplayName("mesmo volume distribuido em cinco tipos pontua mais que concentrado em um")
    void distributedActivityScoresHigherThanConcentrated() {
        BigDecimal total = new BigDecimal("1000");

        Map<ActivityType, BigDecimal> concentrated = new EnumMap<>(ActivityType.class);
        concentrated.put(ActivityType.COMMIT, total);

        Map<ActivityType, BigDecimal> distributed = new EnumMap<>(ActivityType.class);
        distributed.put(ActivityType.COMMIT, new BigDecimal("200"));
        distributed.put(ActivityType.TEST, new BigDecimal("200"));
        distributed.put(ActivityType.BUG_FIX, new BigDecimal("200"));
        distributed.put(ActivityType.DOCUMENTATION, new BigDecimal("200"));
        distributed.put(ActivityType.FEATURE, new BigDecimal("200"));

        ScoreResult concentratedResult = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                total, 45, 5, concentrated, 0, 0, 0, 1, 1));
        ScoreResult distributedResult = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                total, 45, 5, distributed, 0, 0, 0, 1, 1));

        assertThat(distributedResult.devScore()).isGreaterThan(concentratedResult.devScore());
        assertThat(componentPoints(distributedResult, "VARIETY")).isEqualTo(150);
    }

    @Test
    @DisplayName("volume satura na meta: dez vezes a meta nao vale dez vezes os pontos")
    void volumeSaturatesAtGoal() {
        Map<ActivityType, BigDecimal> pointsByType = new EnumMap<>(ActivityType.class);
        pointsByType.put(ActivityType.COMMIT, new BigDecimal("19500"));

        ScoreResult atGoal = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                new BigDecimal("1950"), 45, 5, pointsByType, 0, 0, 0, 1, 1));
        ScoreResult tenTimesGoal = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                new BigDecimal("19500"), 45, 5, pointsByType, 0, 0, 0, 1, 1));

        assertThat(componentPoints(atGoal, "VOLUME")).isEqualTo(400);
        assertThat(componentPoints(tenTimesGoal, "VOLUME")).isEqualTo(400);
    }

    @Test
    @DisplayName("90 dias ativos com sequencia de 30 dias dao consistencia maxima")
    void fullConsistencyScoresMaximum() {
        ScoreResult result = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                new BigDecimal("500"), 90, 30, Map.of(ActivityType.COMMIT, new BigDecimal("500")),
                0, 0, 0, 1, 1));

        assertThat(componentPoints(result, "CONSISTENCY")).isEqualTo(200);
    }

    @Test
    @DisplayName("o score nunca passa de 1000 mesmo com todos os componentes no maximo")
    void scoreNeverExceedsMaximum() {
        Map<ActivityType, BigDecimal> pointsByType = new EnumMap<>(ActivityType.class);
        pointsByType.put(ActivityType.COMMIT, new BigDecimal("200"));
        pointsByType.put(ActivityType.TEST, new BigDecimal("200"));
        pointsByType.put(ActivityType.BUG_FIX, new BigDecimal("200"));
        pointsByType.put(ActivityType.DOCUMENTATION, new BigDecimal("200"));
        pointsByType.put(ActivityType.FEATURE, new BigDecimal("200"));

        ScoreResult result = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                new BigDecimal("999999"), 90, 90, pointsByType, 99, 99, 99, 99, 99));

        assertThat(result.devScore()).isEqualTo(1000);
        assertThat(result.level()).isEqualTo("Em evolucao continua");
    }

    @Test
    @DisplayName("todo componente devolve peso, valor bruto, normalizado e explicacao")
    void everyComponentIsExplainable() {
        ScoreResult result = calculator.calculate(new ScoreInput(WINDOW, WEEKLY_GOAL,
                new BigDecimal("300"), 20, 3, Map.of(ActivityType.COMMIT, new BigDecimal("300")),
                1, 2, 1, 3, 2));

        assertThat(result.components()).allSatisfy(component -> {
            assertThat(component.weight()).isPositive();
            assertThat(component.maxPoints()).isPositive();
            assertThat(component.normalized()).isBetween(0.0, 1.0);
            assertThat(component.explanation()).isNotBlank();
        });
        assertThat(result.components().stream().mapToInt(ScoreComponent::maxPoints).sum())
                .isEqualTo(DevScoreCalculator.MAX_SCORE);
    }

    @Test
    @DisplayName("entropia e zero com um tipo unico e positiva com tipos equilibrados")
    void entropyBehavesAsExpected() {
        assertThat(calculator.shannonEntropy(Map.of(ActivityType.COMMIT, BigDecimal.TEN))).isZero();
        assertThat(calculator.shannonEntropy(Map.of(
                ActivityType.COMMIT, BigDecimal.TEN,
                ActivityType.TEST, BigDecimal.TEN))).isGreaterThan(0.6);
        assertThat(calculator.shannonEntropy(Map.of())).isZero();
    }
}
