package com.devmetrics.dashboard.dto;

import java.time.LocalDate;

public record StreakInfo(int current, int longest, int activeDaysLast30, LocalDate lastActiveDate) {
}
