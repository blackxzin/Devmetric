package com.devmetrics.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalendarDay(LocalDate date, int count, BigDecimal points, int level) {
}
