package com.devmetrics.scoring;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.scoring.domain.EffectiveRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PointsCalculatorTest {

    private static final double NEW_TECH_BONUS = 0.5;

    private final PointsCalculator calculator = new PointsCalculator();

    private EffectiveRule commitRule() {
        return new EffectiveRule(ActivityType.COMMIT, new BigDecimal("5.00"), 10, true, true, false);
    }

    @Test
    @DisplayName("primeira atividade do dia vale os pontos base cheios")
    void firstActivityOfDayScoresBasePoints() {
        BigDecimal points = calculator.calculate(commitRule(), 1, false, NEW_TECH_BONUS);

        assertThat(points).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("quinta atividade do mesmo tipo no mesmo dia vale base dividido por cinco")
    void fifthActivityOfDayScoresBaseDividedByFive() {
        BigDecimal points = calculator.calculate(commitRule(), 5, false, NEW_TECH_BONUS);

        assertThat(points).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("dez commits no mesmo dia somam 14,65 pontos, nao 50")
    void tenCommitsInOneDayDoNotScoreLinearly() {
        BigDecimal total = BigDecimal.ZERO;
        for (int position = 1; position <= 10; position++) {
            total = total.add(calculator.calculate(commitRule(), position, false, NEW_TECH_BONUS));
        }

        assertThat(total).isEqualByComparingTo("14.65");
        assertThat(total).isLessThan(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("um commit, um teste e uma documentacao valem mais que dez commits")
    void varietyBeatsRepetition() {
        BigDecimal tenCommits = BigDecimal.ZERO;
        for (int position = 1; position <= 10; position++) {
            tenCommits = tenCommits.add(calculator.calculate(commitRule(), position, false, NEW_TECH_BONUS));
        }

        BigDecimal mixedDay = calculator.calculate(commitRule(), 1, false, NEW_TECH_BONUS)
                .add(calculator.calculate(new EffectiveRule(ActivityType.TEST,
                        new BigDecimal("8.00"), 8, true, true, false), 1, false, NEW_TECH_BONUS))
                .add(calculator.calculate(new EffectiveRule(ActivityType.DOCUMENTATION,
                        new BigDecimal("6.00"), 4, true, true, false), 1, false, NEW_TECH_BONUS));

        assertThat(mixedDay).isEqualByComparingTo("19.00");
        assertThat(mixedDay).isGreaterThan(tenCommits);
    }

    @Test
    @DisplayName("atividade acima do limite diario nao pontua")
    void activityAboveDailyCapScoresZero() {
        BigDecimal points = calculator.calculate(commitRule(), 11, false, NEW_TECH_BONUS);

        assertThat(points).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("regra sem rendimento decrescente mantem os pontos base")
    void ruleWithoutDiminishingKeepsBasePoints() {
        EffectiveRule newProject = new EffectiveRule(ActivityType.NEW_PROJECT,
                new BigDecimal("25.00"), 2, false, true, false);

        assertThat(calculator.calculate(newProject, 2, false, NEW_TECH_BONUS))
                .isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("bonus de tecnologia nova multiplica os pontos")
    void newTechnologyAppliesBonus() {
        BigDecimal points = calculator.calculate(commitRule(), 1, true, NEW_TECH_BONUS);

        assertThat(points).isEqualByComparingTo("7.50");
    }

    @Test
    @DisplayName("regra inativa nao pontua")
    void inactiveRuleScoresZero() {
        EffectiveRule inactive = new EffectiveRule(ActivityType.COMMIT,
                new BigDecimal("5.00"), 10, true, false, false);

        assertThat(calculator.calculate(inactive, 1, false, NEW_TECH_BONUS)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("regra nula nao quebra o calculo")
    void nullRuleScoresZero() {
        assertThat(calculator.calculate(null, 1, false, NEW_TECH_BONUS)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("posicao zero ou negativa e tratada como primeira do dia")
    void nonPositivePositionIsTreatedAsFirst() {
        assertThat(calculator.calculate(commitRule(), 0, false, NEW_TECH_BONUS))
                .isEqualByComparingTo("5.00");
    }
}
