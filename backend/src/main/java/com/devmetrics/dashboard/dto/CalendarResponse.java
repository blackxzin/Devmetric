package com.devmetrics.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalendarResponse(
        int year,
        LocalDate from,
        LocalDate to,
        int totalActivities,
        BigDecimal totalPoints,
        int activeDays,
        List<CalendarDay> days
) {
}
