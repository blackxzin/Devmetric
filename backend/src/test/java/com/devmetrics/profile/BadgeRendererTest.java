package com.devmetrics.profile;

import com.devmetrics.dashboard.dto.CalendarDay;
import com.devmetrics.dashboard.dto.CalendarResponse;
import com.devmetrics.dashboard.dto.StreakInfo;
import com.devmetrics.scoring.dto.ScoreResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeRendererTest {

    private final BadgeRenderer renderer = new BadgeRenderer();

    @Test
    @DisplayName("desenha um quadrado por dia com a cor do nivel e o score no topo")
    void rendersOneCellPerDay() {
        LocalDate to = LocalDate.of(2026, 10, 6);
        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date = to.minusDays(364); !date.isAfter(to); date = date.plusDays(1)) {
            days.add(new CalendarDay(date, date.equals(to) ? 5 : 0, BigDecimal.ZERO, date.equals(to) ? 4 : 0));
        }
        CalendarResponse calendar = new CalendarResponse(2026, to.minusDays(364), to, 5, BigDecimal.TEN, 1, days);

        String svg = renderer.render("dev", score(742), new StreakInfo(3, 10, 12, to), calendar);

        assertThat(svg).startsWith("<svg").endsWith("</svg>");
        assertThat(svg).contains(">742<", "@dev", "streak 3d");
        assertThat(svg.split("<rect x=").length - 1).isEqualTo(365);
        assertThat(svg).containsOnlyOnce("#37e29a");
    }

    @Test
    @DisplayName("escapa texto vindo do usuario (sem injecao de SVG)")
    void escapesUserText() {
        assertThat(BadgeRenderer.escape("<script>&\"'")).isEqualTo("&lt;script&gt;&amp;&quot;&apos;");
    }

    private ScoreResponse score(int value) {
        return new ScoreResponse(value, "Avancado", 90, Instant.now(), List.of(), new ScoreResponse.Trend(0, "FLAT"));
    }
}
