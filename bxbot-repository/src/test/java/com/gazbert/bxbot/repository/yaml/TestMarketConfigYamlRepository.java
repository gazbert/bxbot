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

package com.gazbert.bxbot.repository.yaml;

import static com.gazbert.bxbot.datastore.yaml.FileLocations.MARKETS_CONFIG_YAML_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.market.MarketsType;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.MarketConfigRepository;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests YAML backed Market configuration repository behaves as expected.
 *
 * @author gazbert
 */
class TestMarketConfigYamlRepository {

  private static final String UNKNOWN_MARKET_ID = "unknown-or-new-market-id";

  private static final String MARKET_1_ID = "gemini_usd/btc";
  private static final String MARKET_1_NAME = "BTC/USD";
  private static final String MARKET_1_BASE_CURRENCY = "BTC";
  private static final String MARKET_1_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_1_IS_ENABLED = true;
  private static final String MARKET_1_TRADING_STRATEGY_ID = "macd_trend_follower";

  private static final String MARKET_2_ID = "coinbasepro_gbp/btc";
  private static final String MARKET_2_NAME = "BTC/GBP";
  private static final String MARKET_2_BASE_CURRENCY = "BTC";
  private static final String MARKET_2_COUNTER_CURRENCY = "GBP";
  private static final boolean MARKET_2_IS_ENABLED = false;
  private static final String MARKET_2_TRADING_STRATEGY_ID = "scalper";

  private static final String NEW_MARKET_NAME = "BTC/ETH";
  private static final String NEW_MARKET_BASE_CURRENCY = "BTC";
  private static final String NEW_MARKET_COUNTER_CURRENCY = "ETH";
  private static final boolean NEW_MARKET_IS_ENABLED = false;
  private static final String NEW_MARKET_TRADING_STRATEGY_ID = "macd-jobby";

  private ConfigurationManager configurationManager;

  @BeforeEach
  void setup() {
    configurationManager = EasyMock.createMock(ConfigurationManager.class);
  }

  @Test
  void whenFindAllCalledThenExpectServiceToReturnAllMarketConfigs() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final List<MarketConfig> marketConfigItems = marketConfigRepository.findAll();

    assertThat(marketConfigItems.size()).isEqualTo(2);

    assertThat(marketConfigItems.get(0).getId()).isEqualTo(MARKET_1_ID);
    assertThat(marketConfigItems.get(0).getName()).isEqualTo(MARKET_1_NAME);
    assertThat(marketConfigItems.get(0).isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
    assertThat(marketConfigItems.get(0).getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
    assertThat(marketConfigItems.get(0).getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
    assertThat(marketConfigItems.get(0).getTradingStrategyId())
        .isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

    assertThat(marketConfigItems.get(1).getId()).isEqualTo(MARKET_2_ID);
    assertThat(marketConfigItems.get(1).getName()).isEqualTo(MARKET_2_NAME);
    assertThat(marketConfigItems.get(1).isEnabled()).isEqualTo(MARKET_2_IS_ENABLED);
    assertThat(marketConfigItems.get(1).getBaseCurrency()).isEqualTo(MARKET_2_BASE_CURRENCY);
    assertThat(marketConfigItems.get(1).getCounterCurrency()).isEqualTo(MARKET_2_COUNTER_CURRENCY);
    assertThat(marketConfigItems.get(1).getTradingStrategyId())
        .isEqualTo(MARKET_2_TRADING_STRATEGY_ID);

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenFindByIdCalledWithKnownIdThenReturnMatchingMarketConfig() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final MarketConfig marketConfig = marketConfigRepository.findById(MARKET_1_ID);

    assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
    assertThat(marketConfig.getName()).isEqualTo(MARKET_1_NAME);
    assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
    assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
    assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
    assertThat(marketConfig.getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenFindByIdCalledWithUnknownIdThenReturnNullMarketConfig() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final MarketConfig marketConfig = marketConfigRepository.findById(UNKNOWN_MARKET_ID);

    assertThat(marketConfig).isNull();
    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledWithKnownIdThenReturnUpdatedMarketConfig() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    configurationManager.saveConfig(
        eq(MarketsType.class), anyObject(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME));

    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final MarketConfig marketConfig = marketConfigRepository.save(someExternalMarketConfig());

    assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
    assertThat(marketConfig.getName()).isEqualTo(MARKET_1_NAME);
    assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
    assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
    assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
    assertThat(marketConfig.getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledWithUnknownIdThenReturnEmptyMarketConfig() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final MarketConfig marketConfig =
        marketConfigRepository.save(someExternalMarketConfigWithUnknownId());

    assertThat(marketConfig).isEqualTo(null);
    EasyMock.verify(configurationManager);
  }

  @Test
  void whenDeleteCalledWithKnownIdThenReturnMatchingMarketConfig() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    configurationManager.saveConfig(
        eq(MarketsType.class), anyObject(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME));

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final MarketConfig marketConfig = marketConfigRepository.delete(MARKET_1_ID);

    assertThat(marketConfig.getId()).isEqualTo(MARKET_1_ID);
    assertThat(marketConfig.getName()).isEqualTo(MARKET_1_NAME);
    assertThat(marketConfig.isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
    assertThat(marketConfig.getBaseCurrency()).isEqualTo(MARKET_1_BASE_CURRENCY);
    assertThat(marketConfig.getCounterCurrency()).isEqualTo(MARKET_1_COUNTER_CURRENCY);
    assertThat(marketConfig.getTradingStrategyId()).isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenSaveCalledWithEmptyIdThenExpectCreatedMarketConfigToBeReturned() {

    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    configurationManager.saveConfig(
        eq(MarketsType.class), anyObject(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME));

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);

    final MarketConfig marketConfig = marketConfigRepository.save(someNewExternalMarketConfig());

    assertThat(marketConfig.getId()).isNotBlank();
    assertThat(marketConfig.getName()).isEqualTo(NEW_MARKET_NAME);
    assertThat(marketConfig.isEnabled()).isEqualTo(NEW_MARKET_IS_ENABLED);
    assertThat(marketConfig.getBaseCurrency()).isEqualTo(NEW_MARKET_BASE_CURRENCY);
    assertThat(marketConfig.getCounterCurrency()).isEqualTo(NEW_MARKET_COUNTER_CURRENCY);
    assertThat(marketConfig.getTradingStrategyId()).isEqualTo(NEW_MARKET_TRADING_STRATEGY_ID);

    EasyMock.verify(configurationManager);
  }

  @Test
  void whenDeleteCalledWithUnknownIdThenReturnEmptyMarket() {
    expect(configurationManager.loadConfig(eq(MarketsType.class), eq(MARKETS_CONFIG_YAML_FILENAME)))
        .andReturn(allTheInternalMarketsConfig());

    EasyMock.replay(configurationManager);

    final MarketConfigRepository marketConfigRepository =
        new MarketConfigYamlRepository(configurationManager);
    final MarketConfig marketConfig = marketConfigRepository.delete(UNKNOWN_MARKET_ID);

    assertThat(marketConfig).isNull();
    EasyMock.verify(configurationManager);
  }

  // --------------------------------------------------------------------------
  // Private utils
  // --------------------------------------------------------------------------

  private static MarketsType allTheInternalMarketsConfig() {
    final MarketConfig marketConfig1 = new MarketConfig();
    marketConfig1.setId(MARKET_1_ID);
    marketConfig1.setName(MARKET_1_NAME);
    marketConfig1.setEnabled(MARKET_1_IS_ENABLED);
    marketConfig1.setBaseCurrency(MARKET_1_BASE_CURRENCY);
    marketConfig1.setCounterCurrency(MARKET_1_COUNTER_CURRENCY);
    marketConfig1.setTradingStrategyId(MARKET_1_TRADING_STRATEGY_ID);

    final MarketConfig marketConfig2 = new MarketConfig();
    marketConfig2.setId(MARKET_2_ID);
    marketConfig2.setName(MARKET_2_NAME);
    marketConfig2.setEnabled(MARKET_2_IS_ENABLED);
    marketConfig2.setBaseCurrency(MARKET_2_BASE_CURRENCY);
    marketConfig2.setCounterCurrency(MARKET_2_COUNTER_CURRENCY);
    marketConfig2.setTradingStrategyId(MARKET_2_TRADING_STRATEGY_ID);

    final MarketsType marketsType = new MarketsType();
    marketsType.getMarkets().add(marketConfig1);
    marketsType.getMarkets().add(marketConfig2);

    return marketsType;
  }

  private static MarketConfig someExternalMarketConfig() {
    return new MarketConfig(
        MARKET_1_ID,
        MARKET_1_NAME,
        MARKET_1_BASE_CURRENCY,
        MARKET_1_COUNTER_CURRENCY,
        MARKET_1_IS_ENABLED,
        MARKET_1_TRADING_STRATEGY_ID);
  }

  private static MarketConfig someNewExternalMarketConfig() {
    return new MarketConfig(
        null,
        NEW_MARKET_NAME,
        NEW_MARKET_BASE_CURRENCY,
        NEW_MARKET_COUNTER_CURRENCY,
        NEW_MARKET_IS_ENABLED,
        NEW_MARKET_TRADING_STRATEGY_ID);
  }

  private static MarketConfig someExternalMarketConfigWithUnknownId() {
    return new MarketConfig(
        UNKNOWN_MARKET_ID,
        MARKET_1_NAME,
        MARKET_1_BASE_CURRENCY,
        MARKET_1_COUNTER_CURRENCY,
        MARKET_1_IS_ENABLED,
        MARKET_1_TRADING_STRATEGY_ID);
  }
}
