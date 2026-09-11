package com.devmetrics.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntensityCalculatorTest {

    private final IntensityCalculator calculator = new IntensityCalculator();

    @Test
    @DisplayName("com amostra pequena usa os cortes fixos")
    void smallSampleFallsBackToFixedThresholds() {
        IntensityCalculator.Thresholds thresholds = calculator.thresholds(List.of(10.0, 20.0));

        assertThat(thresholds.fallback()).isTrue();
        assertThat(calculator.level(3, thresholds)).isEqualTo((short) 1);
        assertThat(calculator.level(10, thresholds)).isEqualTo((short) 2);
        assertThat(calculator.level(25, thresholds)).isEqualTo((short) 3);
        assertThat(calculator.level(80, thresholds)).isEqualTo((short) 4);
    }

    @Test
    @DisplayName("dia sem pontos e sempre nivel zero")
    void zeroPointsIsAlwaysLevelZero() {
        IntensityCalculator.Thresholds thresholds = calculator.thresholds(List.of());

        assertThat(calculator.level(0, thresholds)).isZero();
    }

    @Test
    @DisplayName("com amostra suficiente os niveis vem dos percentis do proprio usuario")
    void largeSampleUsesPercentiles() {
        List<Double> values = new ArrayList<>();
        for (int day = 1; day <= 100; day++) {
            values.add((double) day);
        }

        IntensityCalculator.Thresholds thresholds = calculator.thresholds(values);

        assertThat(thresholds.fallback()).isFalse();
        assertThat(calculator.level(5, thresholds)).isEqualTo((short) 1);
        assertThat(calculator.level(50, thresholds)).isEqualTo((short) 2);
        assertThat(calculator.level(80, thresholds)).isEqualTo((short) 3);
        assertThat(calculator.level(100, thresholds)).isEqualTo((short) 4);
    }

    @Test
    @DisplayName("a escala e relativa: quem produz pouco tambem alcanca o nivel maximo")
    void scaleIsRelativeToTheUser() {
        List<Double> lowVolume = new ArrayList<>();
        for (int day = 1; day <= 20; day++) {
            lowVolume.add(day <= 19 ? 1.0 : 6.0);
        }

        IntensityCalculator.Thresholds thresholds = calculator.thresholds(lowVolume);

        assertThat(calculator.level(6, thresholds)).isEqualTo((short) 4);
    }
}
