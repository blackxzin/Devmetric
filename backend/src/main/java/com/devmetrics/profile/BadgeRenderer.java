package com.devmetrics.profile;

import com.devmetrics.dashboard.dto.CalendarDay;
import com.devmetrics.dashboard.dto.CalendarResponse;
import com.devmetrics.dashboard.dto.StreakInfo;
import com.devmetrics.scoring.dto.ScoreResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Desenha o card SVG para colar no README do GitHub: Dev Score, streak e o calendario
 * dos ultimos 365 dias. SVG puro, sem fonte externa nem script (o GitHub remove ambos).
 */
@Component
public class BadgeRenderer {

    private static final String[] LEVEL_COLORS = {"#1a1f2e", "#123a3a", "#146356", "#0f9d78", "#37e29a"};
    private static final int CELL = 10;
    private static final int GAP = 2;
    private static final int PAD = 16;
    private static final int TOP = 56;

    public String render(String username, ScoreResponse score, StreakInfo streak, CalendarResponse calendar) {
        LocalDate gridStart = calendar.from().minusDays(calendar.from().getDayOfWeek().getValue() % 7);
        int weeks = (int) (ChronoUnit.DAYS.between(gridStart, calendar.to()) / 7) + 1;
        int width = PAD * 2 + weeks * (CELL + GAP) - GAP;
        int height = TOP + 7 * (CELL + GAP) - GAP + PAD + 14;

        StringBuilder svg = new StringBuilder(32_000);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\" role=\"img\" aria-label=\"DevMetrics de ").append(escape(username))
                .append(": Dev Score ").append(score.devScore()).append("\">")
                .append("<style>text{font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif}</style>")
                .append("<rect width=\"100%\" height=\"100%\" rx=\"12\" fill=\"#0a0d16\"/>")
                .append(text(PAD, 26, 15, "#f4f6fb", "700", "@" + escape(username)))
                .append(text(PAD, 44, 11, "#8890a8", "400",
                        calendar.totalActivities() + " atividades em " + calendar.activeDays() + " dias no ultimo ano"))
                .append(text(width - PAD, 28, 22, "#7c5cff", "800", String.valueOf(score.devScore()), "end"))
                .append(text(width - PAD, 44, 11, "#8890a8", "400",
                        "Dev Score · " + escape(score.level()) + " · streak " + streak.current() + "d", "end"));

        for (CalendarDay day : calendar.days()) {
            long offset = ChronoUnit.DAYS.between(gridStart, day.date());
            int x = PAD + (int) (offset / 7) * (CELL + GAP);
            int y = TOP + (int) (offset % 7) * (CELL + GAP);
            int level = Math.max(0, Math.min(4, day.level()));
            svg.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(CELL)
                    .append("\" height=\"").append(CELL).append("\" rx=\"2\" fill=\"").append(LEVEL_COLORS[level])
                    .append("\"><title>").append(day.date()).append(": ").append(day.count())
                    .append(" atividades</title></rect>");
        }

        svg.append(text(PAD, height - 10, 10, "#5b6178", "400", "DevMetrics"))
                .append("</svg>");
        return svg.toString();
    }

    private String text(int x, int y, int size, String color, String weight, String content) {
        return text(x, y, size, color, weight, content, "start");
    }

    private String text(int x, int y, int size, String color, String weight, String content, String anchor) {
        return "<text x=\"" + x + "\" y=\"" + y + "\" font-size=\"" + size + "\" fill=\"" + color
                + "\" font-weight=\"" + weight + "\" text-anchor=\"" + anchor + "\">" + content + "</text>";
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
