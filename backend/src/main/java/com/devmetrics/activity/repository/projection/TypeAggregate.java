package com.devmetrics.activity.repository.projection;

import java.math.BigDecimal;

public interface TypeAggregate {

    String getType();

    long getTotal();

    BigDecimal getPoints();
}
