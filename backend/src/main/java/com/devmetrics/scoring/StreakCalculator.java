package com.devmetrics.scoring;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sequencia de dias ativos. Funcao pura sobre a lista de datas com atividade.
 *
 * O dia de hoje ainda sem atividade nao quebra a sequencia: ela e contada a partir
 * de ontem. Quebrar a sequencia as 00h01 seria punir o usuario por nada.
 */
@Component
public class StreakCalculator {

    public record Streak(int current, int longest, int activeDaysInWindow) {
    }

    public Streak calculate(List<LocalDate> activeDates, LocalDate today, LocalDate windowStart) {
        if (activeDates == null || activeDates.isEmpty()) {
            return new Streak(0, 0, 0);
        }
        Set<LocalDate> dates = new TreeSet<>(activeDates);

        int current = 0;
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        while (dates.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }

        int longest = 0;
        int running = 0;
        LocalDate previous = null;
        for (LocalDate date : dates) {
            if (previous != null && previous.plusDays(1).equals(date)) {
                running++;
            } else {
                running = 1;
            }
            longest = Math.max(longest, running);
            previous = date;
        }

        int activeInWindow = (int) dates.stream()
                .filter(date -> !date.isBefore(windowStart) && !date.isAfter(today))
                .count();

        return new Streak(current, longest, activeInWindow);
    }
}
