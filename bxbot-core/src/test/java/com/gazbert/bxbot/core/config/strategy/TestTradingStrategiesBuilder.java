/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.core.config.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Test;

/**
 * Tests the Trading Strategies Builder works as expected.
 *
 * @author gazbert
 */
public class TestTradingStrategiesBuilder {

  private static final String STRATEGY_ID = "MyMacdStrategy_v3";
  private static final String UNKNOWN_STRATEGY_ID = "unknown-strategy-id";
  private static final String STRATEGY_NAME = "MACD Shorting algo";
  private static final String STRATEGY_DESCRIPTION = "MACD Shorting algo description";
  private static final String STRATEGY_CLASSNAME =
      "com.gazbert.bxbot.core.config.strategy.strategies.TradingStrategyForClassnameInstantiation";
  private static final String STRATEGY_BEAN = null;
  private static final String STRATEGY_CONFIG_ITEM_NAME = "btc-sell-order-amount";
  private static final String STRATEGY_CONFIG_ITEM_VALUE = "0.2";

  private static final String MARKET_1_NAME = "BTC/USD";
  private static final String MARKET_1_ID = "btc_usd";
  private static final String MARKET_1_BASE_CURRENCY = "BTC";
  private static final String MARKET_1_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_1_IS_ENABLED = true;

  private static final String MARKET_2_NAME = "LTC/USD";
  private static final String MARKET_2_ID = "ltc_usd";
  private static final String MARKET_2_BASE_CURRENCY = "LTC";
  private static final String MARKET_2_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_2_NOT_ENABLED = false;

  @Test
  public void testBuildingStrategiesSuccessfully() {
    final ExchangeAdapter exchangeAdapter = EasyMock.createMock(ExchangeAdapter.class);
    final List<TradingStrategy> strategies =
        TradingStrategiesBuilder.buildStrategies(
            someStrategiesConfig(), someMarketsConfig(), exchangeAdapter);
    assertThat(strategies.size()).isEqualTo(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildingStrategiesFailsForUnknownStrategyId() {
    final ExchangeAdapter exchangeAdapter = EasyMock.createMock(ExchangeAdapter.class);
    TradingStrategiesBuilder.buildStrategies(
        someStrategiesConfig(), someMarketsConfigUsingUnknownStrategyId(), exchangeAdapter);
  }

  private static List<StrategyConfig> someStrategiesConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(STRATEGY_CONFIG_ITEM_NAME, STRATEGY_CONFIG_ITEM_VALUE);

    final StrategyConfig strategyConfig1 =
        new StrategyConfig(
            STRATEGY_ID,
            STRATEGY_NAME,
            STRATEGY_DESCRIPTION,
            STRATEGY_CLASSNAME,
            STRATEGY_BEAN,
            configItems);

    final List<StrategyConfig> allStrategies = new ArrayList<>();
    allStrategies.add(strategyConfig1);
    return allStrategies;
  }

  private static List<MarketConfig> someMarketsConfig() {
    final MarketConfig marketConfig1 =
        new MarketConfig(
            MARKET_1_ID,
            MARKET_1_NAME,
            MARKET_1_BASE_CURRENCY,
            MARKET_1_COUNTER_CURRENCY,
            MARKET_1_IS_ENABLED,
            STRATEGY_ID);

    final MarketConfig marketConfig2 =
        new MarketConfig(
            MARKET_2_ID,
            MARKET_2_NAME,
            MARKET_2_BASE_CURRENCY,
            MARKET_2_COUNTER_CURRENCY,
            MARKET_2_NOT_ENABLED,
            STRATEGY_ID);

    final List<MarketConfig> allMarkets = new ArrayList<>();
    allMarkets.add(marketConfig1);
    allMarkets.add(marketConfig2);
    return allMarkets;
  }

  private static List<MarketConfig> someMarketsConfigUsingUnknownStrategyId() {
    final MarketConfig marketConfig1 =
        new MarketConfig(
            MARKET_1_ID,
            MARKET_1_NAME,
            MARKET_1_BASE_CURRENCY,
            MARKET_1_COUNTER_CURRENCY,
            MARKET_1_IS_ENABLED,
            UNKNOWN_STRATEGY_ID);

    final List<MarketConfig> allMarkets = new ArrayList<>();
    allMarkets.add(marketConfig1);
    return allMarkets;
  }
}
