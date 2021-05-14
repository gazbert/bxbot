/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 maiph
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.exchanges.config;

import com.gazbert.bxbot.exchange.api.PairPrecisionConfig;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Default implementation of {@link PairPrecisionConfig} backed by {@link Map}s.
 *
 * @author maiph
 */
public class PairPrecisionConfigImpl implements PairPrecisionConfig {

  private final Map<String, Integer> prices;
  private final Map<String, Integer> volumes;
  private final Map<String, BigDecimal> orderMins;

  /**
   * Default implementation of {@link PairPrecisionConfig} backed by {@link Map}s.
   */
  public PairPrecisionConfigImpl(
      Map<String, Integer> prices,
      Map<String, Integer> volumes,
      Map<String, BigDecimal> orderMins) {
    this.prices = prices;
    this.volumes = volumes;
    this.orderMins = orderMins;
  }

  @Override
  public int getPricePrecision(String pair) {
    return prices.getOrDefault(pair, -1);
  }

  @Override
  public int getVolumePrecision(String pair) {
    return volumes.getOrDefault(pair, -1);
  }

  @Override
  public BigDecimal getMinimalOrderVolume(String pair) {
    return orderMins.getOrDefault(pair, null);
  }
}
