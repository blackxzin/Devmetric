package com.devmetrics.activity.repository.projection;

import java.math.BigDecimal;

public interface MonthlyAggregate {

    int getMonthNumber();

    long getTotal();

    BigDecimal getPoints();

    long getDistinctTypes();

    long getDistinctTechnologies();
}
