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

package com.gazbert.bxbot.datastore.yaml.market;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.domain.market.MarketConfig;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import org.junit.Test;

/**
 * Tests the Market configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestMarketConfigurationManagement {

  private static final String VALID_YAML_CONFIG_FILENAME =
      "src/test/config/markets/valid-markets.yaml";
  private static final String INVALID_YAML_CONFIG_FILENAME =
      "src/test/config/markets/invalid-markets.yaml";
  private static final String MISSING_YAML_CONFIG_FILENAME =
      "src/test/config/markets/missing-markets.yaml";
  private static final String YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/markets/saved-markets.yaml";
  private static final String INVALID_YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/not-there/saved-markets.yaml";

  private static final String MARKET_1_ID = "gemini_usd/btc";
  private static final String MARKET_1_NAME = "BTC/USD";
  private static final String MARKET_1_BASE_CURRENCY = "BTC";
  private static final String MARKET_1_COUNTER_CURRENCY = "USD";
  private static final boolean MARKET_1_IS_ENABLED = true;
  private static final String MARKET_1_TRADING_STRATEGY_ID = "macd_trend_follower";

  private static final String MARKET_2_ID = "gdax_gbp/btc";
  private static final String MARKET_2_NAME = "BTC/GBP";
  private static final String MARKET_2_BASE_CURRENCY = "BTC";
  private static final String MARKET_2_COUNTER_CURRENCY = "GBP";
  private static final boolean MARKET_2_IS_ENABLED = false;
  private static final String MARKET_2_TRADING_STRATEGY_ID = "scalper";

  @Test
  public void testLoadingValidYamlConfigFileIsSuccessful() {
    final MarketsType marketsType =
        ConfigurationManager.loadConfig(MarketsType.class, VALID_YAML_CONFIG_FILENAME);

    assertEquals(2, marketsType.getMarkets().size());

    assertEquals("btc_usd", marketsType.getMarkets().get(0).getId());
    assertEquals("BTC/USD", marketsType.getMarkets().get(0).getName());
    assertEquals("BTC", marketsType.getMarkets().get(0).getBaseCurrency());
    assertEquals("USD", marketsType.getMarkets().get(0).getCounterCurrency());
    assertTrue(marketsType.getMarkets().get(0).isEnabled());
    assertEquals("scalping-strategy", marketsType.getMarkets().get(0).getTradingStrategyId());

    assertEquals("ltc_usd", marketsType.getMarkets().get(1).getId());
    assertEquals("LTC/BTC", marketsType.getMarkets().get(1).getName());
    assertEquals("LTC", marketsType.getMarkets().get(1).getBaseCurrency());
    assertEquals("BTC", marketsType.getMarkets().get(1).getCounterCurrency());
    assertFalse(marketsType.getMarkets().get(1).isEnabled());
    assertEquals("scalping-strategy", marketsType.getMarkets().get(1).getTradingStrategyId());
  }

  @Test(expected = IllegalStateException.class)
  public void testLoadingMissingYamlConfigFileThrowsException() {
    ConfigurationManager.loadConfig(MarketsType.class, MISSING_YAML_CONFIG_FILENAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLoadingInvalidYamlConfigFileThrowsException() {
    ConfigurationManager.loadConfig(MarketsType.class, INVALID_YAML_CONFIG_FILENAME);
  }

  @Test
  public void testSavingConfigToYamlIsSuccessful() throws Exception {
    final MarketConfig market1 = new MarketConfig();
    market1.setEnabled(MARKET_1_IS_ENABLED);
    market1.setId(MARKET_1_ID);
    market1.setName(MARKET_1_NAME);
    market1.setBaseCurrency(MARKET_1_BASE_CURRENCY);
    market1.setCounterCurrency(MARKET_1_COUNTER_CURRENCY);
    market1.setTradingStrategyId(MARKET_1_TRADING_STRATEGY_ID);

    final MarketConfig market2 = new MarketConfig();
    market2.setEnabled(MARKET_2_IS_ENABLED);
    market2.setId(MARKET_2_ID);
    market2.setName(MARKET_2_NAME);
    market2.setBaseCurrency(MARKET_2_BASE_CURRENCY);
    market2.setCounterCurrency(MARKET_2_COUNTER_CURRENCY);
    market2.setTradingStrategyId(MARKET_2_TRADING_STRATEGY_ID);

    final MarketsType marketsConfig = new MarketsType();
    marketsConfig.getMarkets().add(market1);
    marketsConfig.getMarkets().add(market2);

    ConfigurationManager.saveConfig(MarketsType.class, marketsConfig, YAML_CONFIG_TO_SAVE_FILENAME);

    // Read it back in
    final MarketsType marketsReloaded =
        ConfigurationManager.loadConfig(MarketsType.class, YAML_CONFIG_TO_SAVE_FILENAME);

    assertThat(marketsReloaded.getMarkets().get(0).isEnabled()).isEqualTo(MARKET_1_IS_ENABLED);
    assertThat(marketsReloaded.getMarkets().get(0).getId()).isEqualTo(MARKET_1_ID);
    assertThat(marketsReloaded.getMarkets().get(0).getName()).isEqualTo(MARKET_1_NAME);
    assertThat(marketsReloaded.getMarkets().get(0).getBaseCurrency())
        .isEqualTo(MARKET_1_BASE_CURRENCY);
    assertThat(marketsReloaded.getMarkets().get(0).getCounterCurrency())
        .isEqualTo(MARKET_1_COUNTER_CURRENCY);
    assertThat(marketsReloaded.getMarkets().get(0).getTradingStrategyId())
        .isEqualTo(MARKET_1_TRADING_STRATEGY_ID);

    assertThat(marketsReloaded.getMarkets().get(1).isEnabled()).isEqualTo(MARKET_2_IS_ENABLED);
    assertThat(marketsReloaded.getMarkets().get(1).getId()).isEqualTo(MARKET_2_ID);
    assertThat(marketsReloaded.getMarkets().get(1).getName()).isEqualTo(MARKET_2_NAME);
    assertThat(marketsReloaded.getMarkets().get(1).getBaseCurrency())
        .isEqualTo(MARKET_2_BASE_CURRENCY);
    assertThat(marketsReloaded.getMarkets().get(1).getCounterCurrency())
        .isEqualTo(MARKET_2_COUNTER_CURRENCY);
    assertThat(marketsReloaded.getMarkets().get(1).getTradingStrategyId())
        .isEqualTo(MARKET_2_TRADING_STRATEGY_ID);

    // cleanup
    Files.delete(FileSystems.getDefault().getPath(YAML_CONFIG_TO_SAVE_FILENAME));
  }

  @Test(expected = IllegalStateException.class)
  public void testSavingConfigToInvalidYamlFileIsHandled() {
    final MarketConfig market1 = new MarketConfig();
    market1.setEnabled(MARKET_1_IS_ENABLED);
    market1.setId(MARKET_1_ID);
    market1.setName(MARKET_1_NAME);
    market1.setBaseCurrency(MARKET_1_BASE_CURRENCY);
    market1.setCounterCurrency(MARKET_1_COUNTER_CURRENCY);
    market1.setTradingStrategyId(MARKET_1_TRADING_STRATEGY_ID);

    final MarketConfig market2 = new MarketConfig();
    market2.setEnabled(MARKET_2_IS_ENABLED);
    market2.setId(MARKET_2_ID);
    market2.setName(MARKET_2_NAME);
    market2.setBaseCurrency(MARKET_2_BASE_CURRENCY);
    market2.setCounterCurrency(MARKET_2_COUNTER_CURRENCY);
    market2.setTradingStrategyId(MARKET_2_TRADING_STRATEGY_ID);

    final MarketsType marketsConfig = new MarketsType();
    marketsConfig.getMarkets().add(market1);
    marketsConfig.getMarkets().add(market2);

    ConfigurationManager.saveConfig(MarketsType.class, marketsConfig, INVALID_YAML_CONFIG_TO_SAVE_FILENAME);
  }
}
