package com.gazbert.bxbot.trading.api;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public interface OhlcFrame {

  ZonedDateTime getTime();

  BigDecimal getOpen();

  BigDecimal getHigh();

  BigDecimal getLow();

  BigDecimal getClose();

  BigDecimal getVwap();

  BigDecimal getVolume();

  Integer getCount();
}
