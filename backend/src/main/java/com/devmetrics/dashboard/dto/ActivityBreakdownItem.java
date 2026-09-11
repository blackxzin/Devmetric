package com.devmetrics.dashboard.dto;

import java.math.BigDecimal;

public record ActivityBreakdownItem(String type, String label, long count, BigDecimal points, double share) {
}
