package com.gazbert.bxbot.exchanges.config;

import com.gazbert.bxbot.exchange.api.PairPrecisionConfig;
import java.util.Map;

/**
 * Default implementation of {@link PairPrecisionConfig} backed by {@link Map}s.
 *
 * @author maiph
 */
public class PairPrecisionConfigImpl implements PairPrecisionConfig {

  private final Map<String, Integer> prices;
  private final Map<String, Integer> volumes;

  public PairPrecisionConfigImpl(Map<String, Integer> prices, Map<String, Integer> volumes) {
    this.prices = prices;
    this.volumes = volumes;
  }

  @Override
  public int getPricePrecision(String pair) {
    return prices.getOrDefault(pair, -1);
  }

  @Override
  public int getVolumePrecision(String pair) {
    return volumes.getOrDefault(pair, -1);
  }
}
