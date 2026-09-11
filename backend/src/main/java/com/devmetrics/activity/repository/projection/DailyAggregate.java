package com.devmetrics.activity.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyAggregate {

    LocalDate getDay();

    long getTotal();

    BigDecimal getPoints();

    long getDistinctTypes();

    long getDistinctTechnologies();
}
