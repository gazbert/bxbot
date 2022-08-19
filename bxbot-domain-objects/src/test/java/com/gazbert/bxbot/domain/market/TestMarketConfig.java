/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.domain.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests a MarketConfig domain object behaves as expected.
 * 测试 MarketConfig 域对象的行为是否符合预期。
 *
 * @author gazbert
 */
class TestMarketConfig {

  private static final String ID = "gemini_usd/btc";
  private static final String NAME = "BTC/USD";
  private static final String BASE_CURRENCY = "BTC";
  private static final String COUNTER_CURRENCY = "USD";
  private static final boolean IS_ENABLED = true;
  private static final String TRADING_STRATEGY = "macd_trend_follower";

  @Test
  void testInitialisationWorksAsExpected() {
    final MarketConfig marketConfig =
        new MarketConfig(ID, NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);

    assertEquals(NAME, marketConfig.getName());
    assertEquals(ID, marketConfig.getId());
    assertEquals(BASE_CURRENCY, marketConfig.getBaseCurrency());
    assertEquals(COUNTER_CURRENCY, marketConfig.getCounterCurrency());
    assertEquals(IS_ENABLED, marketConfig.isEnabled());
    assertEquals(TRADING_STRATEGY, marketConfig.getTradingStrategyId());
  }

  @Test
  void testSettersWorkAsExpected() {
    final MarketConfig marketConfig = new MarketConfig();
    assertNull(marketConfig.getId());
    assertNull(marketConfig.getName());
    assertNull(marketConfig.getBaseCurrency());
    assertNull(marketConfig.getCounterCurrency());
    assertFalse(marketConfig.isEnabled());
    assertNull(marketConfig.getTradingStrategyId());

    marketConfig.setId(ID);
    assertEquals(ID, marketConfig.getId());

    marketConfig.setName(NAME);
    assertEquals(NAME, marketConfig.getName());

    marketConfig.setBaseCurrency(BASE_CURRENCY);
    assertEquals(BASE_CURRENCY, marketConfig.getBaseCurrency());

    marketConfig.setCounterCurrency(COUNTER_CURRENCY);
    assertEquals(COUNTER_CURRENCY, marketConfig.getCounterCurrency());

    marketConfig.setEnabled(IS_ENABLED);
    assertEquals(IS_ENABLED, marketConfig.isEnabled());

    marketConfig.setTradingStrategyId(TRADING_STRATEGY);
    assertEquals(TRADING_STRATEGY, marketConfig.getTradingStrategyId());
  }

  @Test
  void testCloningWorksAsExpected() {
    final MarketConfig marketConfig =
        new MarketConfig(ID, NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);
    final MarketConfig clonedMarketConfig = new MarketConfig(marketConfig);

    assertEquals(clonedMarketConfig, marketConfig);
  }

  @Test
  void testEqualsWorksAsExpected() {
    final MarketConfig market1 =
        new MarketConfig(ID, NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);
    final MarketConfig market2 =
        new MarketConfig(
            "different-id", NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);
    final MarketConfig market3 =
        new MarketConfig(
            ID, "different-name", BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);

    assertEquals(market1, market1);
    assertNotEquals(market1, market2);
    assertEquals(market1, market3);
  }

  @Test
  void testHashCodeWorksAsExpected() {
    final MarketConfig market1 =
        new MarketConfig(ID, NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);
    final MarketConfig market2 =
        new MarketConfig(
            "different-id", NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);
    final MarketConfig market3 =
        new MarketConfig(
            ID, "different-name", BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);

    assertEquals(market1.hashCode(), market1.hashCode());
    assertNotEquals(market1.hashCode(), market2.hashCode());
    assertEquals(market1.hashCode(), market3.hashCode());
  }

  @Test
  void testToStringWorksAsExpected() {
    final MarketConfig market1 =
        new MarketConfig(ID, NAME, BASE_CURRENCY, COUNTER_CURRENCY, IS_ENABLED, TRADING_STRATEGY);

    assertEquals(
        "MarketConfig{id=gemini_usd/btc, name=BTC/USD, baseCurrency=BTC,"
            + " counterCurrency=USD, enabled=true, tradingStrategyId=macd_trend_follower}",
        market1.toString());
  }
}
