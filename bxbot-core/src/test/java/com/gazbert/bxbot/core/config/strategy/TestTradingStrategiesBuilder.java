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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

/**
 * Tests the Trading Strategies Builder works as expected.
 * 测试交易策略生成器是否按预期工作。
 *
 * @author gazbert
 */
class TestTradingStrategiesBuilder {

  private static final String STRATEGY_1_ID = "MyMacdStrategy_v3";
  private static final String STRATEGY_1_NAME = "MACD Shorting algo";
  private static final String STRATEGY_1_DESCRIPTION = "MACD Shorting algo description";
  private static final String STRATEGY_1_CLASSNAME =
      "com.gazbert.bxbot.core.config.strategy.strategies.TradingStrategyForClassnameInstantiation";
  private static final String STRATEGY_1_BEAN = null;
  private static final String STRATEGY_1_CONFIG_ITEM_NAME = "btc-sell-order-amount";
  private static final String STRATEGY_1_CONFIG_ITEM_VALUE = "0.2";

  private static final String STRATEGY_2_ID = "EMA Strat";
  private static final String STRATEGY_2_NAME = "EMA algo";
  private static final String STRATEGY_2_DESCRIPTION = "EMA algo description";
  private static final String STRATEGY_2_CLASSNAME =
      "com.gazbert.bxbot.core.config.strategy.strategies.TradingStrategyForClassnameInstantiation";
  private static final String STRATEGY_2_BEAN = null;

  private static final String UNKNOWN_STRATEGY_ID = "unknown-strategy-id";

  private static final String MARKET_1_NAME = "BTC/USD";
  private static final String MARKET_1_ID = "btc_usd";
  private static final String MARKET_1_BASE_CURRENCY = "BTC";
  private static final String MARKET_1_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_1_IS_ENABLED = true;

  private static final String MARKET_2_NAME = "LTC/USD";
  private static final String MARKET_2_ID = "ltc_usd";
  private static final String MARKET_2_BASE_CURRENCY = "LTC";
  private static final String MARKET_2_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_2_IS_ENABLED = true;

  private static final String MARKET_3_NAME = "ETH/USD";
  private static final String MARKET_3_ID = "eth_usd";
  private static final String MARKET_3_BASE_CURRENCY = "ETC";
  private static final String MARKET_3_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_3_NOT_ENABLED = false;

  @Test
  void testBuildingStrategiesSuccessfully() {
    final ExchangeAdapter exchangeAdapter = EasyMock.createMock(ExchangeAdapter.class);
    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    final TradingStrategiesBuilder tradingStrategiesBuilder = new TradingStrategiesBuilder();
    tradingStrategiesBuilder.setTradingStrategyFactory(tradingStrategyFactory);
    final List<TradingStrategy> strategies =
        tradingStrategiesBuilder.buildStrategies(
            someStrategiesConfig(), someMarketsConfig(), exchangeAdapter);
    assertThat(strategies).hasSize(2);
  }

  @Test
  void testBuildingStrategiesFailsForUnknownStrategyId() {
    final ExchangeAdapter exchangeAdapter = EasyMock.createMock(ExchangeAdapter.class);
    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    final TradingStrategiesBuilder tradingStrategiesBuilder = new TradingStrategiesBuilder();
    tradingStrategiesBuilder.setTradingStrategyFactory(tradingStrategyFactory);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            tradingStrategiesBuilder.buildStrategies(
                someStrategiesConfig(),
                someMarketsConfigUsingUnknownStrategyId(),
                exchangeAdapter));
  }

  @Test
  void testBuildingStrategiesFailsDuplicateMarket() {
    final ExchangeAdapter exchangeAdapter = EasyMock.createMock(ExchangeAdapter.class);
    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    final TradingStrategiesBuilder tradingStrategiesBuilder = new TradingStrategiesBuilder();
    tradingStrategiesBuilder.setTradingStrategyFactory(tradingStrategyFactory);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            tradingStrategiesBuilder.buildStrategies(
                someStrategiesConfig(), someMarketsConfigWithDuplicateMarket(), exchangeAdapter));
  }

  private static List<StrategyConfig> someStrategiesConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(STRATEGY_1_CONFIG_ITEM_NAME, STRATEGY_1_CONFIG_ITEM_VALUE);

    final StrategyConfig strategyConfig1 =
        new StrategyConfig(
            STRATEGY_1_ID,
            STRATEGY_1_NAME,
            STRATEGY_1_DESCRIPTION,
            STRATEGY_1_CLASSNAME,
            STRATEGY_1_BEAN,
            configItems);

    final StrategyConfig strategyConfig2 =
        new StrategyConfig(
            STRATEGY_2_ID,
            STRATEGY_2_NAME,
            STRATEGY_2_DESCRIPTION,
            STRATEGY_2_CLASSNAME,
            STRATEGY_2_BEAN,
            new HashMap<>()); // no optional config for this strat // 此策略没有可选配置

    final List<StrategyConfig> allStrategies = new ArrayList<>();
    allStrategies.add(strategyConfig1);
    allStrategies.add(strategyConfig2);
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
            STRATEGY_1_ID);

    final MarketConfig marketConfig2 =
        new MarketConfig(
            MARKET_2_ID,
            MARKET_2_NAME,
            MARKET_2_BASE_CURRENCY,
            MARKET_2_COUNTER_CURRENCY,
            MARKET_2_IS_ENABLED,
            STRATEGY_2_ID);

    final MarketConfig marketConfig3 =
        new MarketConfig(
            MARKET_3_ID,
            MARKET_3_NAME,
            MARKET_3_BASE_CURRENCY,
            MARKET_3_COUNTER_CURRENCY,
            MARKET_3_NOT_ENABLED,
            STRATEGY_1_ID);

    final List<MarketConfig> allMarkets = new ArrayList<>();
    allMarkets.add(marketConfig1);
    allMarkets.add(marketConfig2);
    allMarkets.add(marketConfig3);
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

  private static List<MarketConfig> someMarketsConfigWithDuplicateMarket() {
    final MarketConfig marketConfig1 =
        new MarketConfig(
            MARKET_1_ID,
            MARKET_1_NAME,
            MARKET_1_BASE_CURRENCY,
            MARKET_1_COUNTER_CURRENCY,
            MARKET_1_IS_ENABLED,
            STRATEGY_1_ID);

    final MarketConfig marketConfig2 =
        new MarketConfig(
            MARKET_1_ID,
            MARKET_1_NAME,
            MARKET_1_BASE_CURRENCY,
            MARKET_1_COUNTER_CURRENCY,
            MARKET_1_IS_ENABLED,
            STRATEGY_1_ID);

    final List<MarketConfig> allMarkets = new ArrayList<>();
    allMarkets.add(marketConfig1);
    allMarkets.add(marketConfig2);
    return allMarkets;
  }
}
