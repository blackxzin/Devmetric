package com.devmetrics.scoring.dto;

import java.time.LocalDate;

public record ScoreHistoryPoint(LocalDate date, int devScore) {
}
