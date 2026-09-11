package com.devmetrics.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 11);

    private final StreakCalculator calculator = new StreakCalculator();

    @Test
    @DisplayName("sem datas ativas a sequencia e zero")
    void noActivityMeansNoStreak() {
        StreakCalculator.Streak streak = calculator.calculate(List.of(), TODAY, TODAY.minusDays(29));

        assertThat(streak.current()).isZero();
        assertThat(streak.longest()).isZero();
        assertThat(streak.activeDaysInWindow()).isZero();
    }

    @Test
    @DisplayName("dias consecutivos terminando hoje contam na sequencia atual")
    void consecutiveDaysEndingTodayCount() {
        List<LocalDate> dates = List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

        StreakCalculator.Streak streak = calculator.calculate(dates, TODAY, TODAY.minusDays(29));

        assertThat(streak.current()).isEqualTo(3);
        assertThat(streak.longest()).isEqualTo(3);
    }

    @Test
    @DisplayName("hoje ainda sem atividade nao quebra a sequencia que vem de ontem")
    void todayWithoutActivityDoesNotBreakStreak() {
        List<LocalDate> dates = List.of(TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3));

        StreakCalculator.Streak streak = calculator.calculate(dates, TODAY, TODAY.minusDays(29));

        assertThat(streak.current()).isEqualTo(3);
    }

    @Test
    @DisplayName("um dia de intervalo zera a sequencia atual mas preserva o recorde")
    void gapResetsCurrentButKeepsLongest() {
        List<LocalDate> dates = List.of(
                TODAY,
                TODAY.minusDays(3), TODAY.minusDays(4), TODAY.minusDays(5), TODAY.minusDays(6));

        StreakCalculator.Streak streak = calculator.calculate(dates, TODAY, TODAY.minusDays(29));

        assertThat(streak.current()).isEqualTo(1);
        assertThat(streak.longest()).isEqualTo(4);
    }

    @Test
    @DisplayName("dias ativos da janela ignoram datas fora do periodo")
    void activeDaysAreLimitedToWindow() {
        List<LocalDate> dates = List.of(TODAY, TODAY.minusDays(10), TODAY.minusDays(60));

        StreakCalculator.Streak streak = calculator.calculate(dates, TODAY, TODAY.minusDays(29));

        assertThat(streak.activeDaysInWindow()).isEqualTo(2);
    }

    @Test
    @DisplayName("datas duplicadas nao inflam a sequencia")
    void duplicatedDatesAreCollapsed() {
        List<LocalDate> dates = List.of(TODAY, TODAY, TODAY.minusDays(1), TODAY.minusDays(1));

        StreakCalculator.Streak streak = calculator.calculate(dates, TODAY, TODAY.minusDays(29));

        assertThat(streak.current()).isEqualTo(2);
    }
}
