/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.core.config.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests Market impl behaves as expected.
 * 测试市场 impl 的行为符合预期。
 *
 * @author gazbert
 */
class TestMarketImpl {

  private static final String MARKET_NAME = "LTC_BTC";
  private static final String MARKET_ID = "3";
  private static final String BASE_CURRENCY = "LTC";
  private static final String COUNTER_CURRENCY = "BTC";

  @Test
  void testMarketIsInitialisedAsExpected() {
    final MarketImpl market =
        new MarketImpl(MARKET_NAME, MARKET_ID, BASE_CURRENCY, COUNTER_CURRENCY);
    assertEquals(MARKET_NAME, market.getName());
    assertEquals(MARKET_ID, market.getId());
    assertEquals(BASE_CURRENCY, market.getBaseCurrency());
    assertEquals(COUNTER_CURRENCY, market.getCounterCurrency());
  }

  @Test
  void testSettersWorkAsExpected() {
    final MarketImpl market = new MarketImpl(null, null, null, null);
    assertNull(market.getName());
    assertNull(market.getId());
    assertNull(market.getBaseCurrency());
    assertNull(market.getCounterCurrency());

    market.setName(MARKET_NAME);
    assertEquals(MARKET_NAME, market.getName());

    market.setId(MARKET_ID);
    assertEquals(MARKET_ID, market.getId());

    market.setBaseCurrency(BASE_CURRENCY);
    assertEquals(BASE_CURRENCY, market.getBaseCurrency());

    market.setCounterCurrency(COUNTER_CURRENCY);
    assertEquals(COUNTER_CURRENCY, market.getCounterCurrency());
  }

  @Test
  void testEqualsWorksAsExpected() {
    final MarketImpl market1 = new MarketImpl(null, "id-1", null, null);
    final MarketImpl market2 = new MarketImpl(null, "id-2", null, null);
    assertEquals(market1, market1);
    assertNotEquals(market1, market2);
  }

  @Test
  void testToStringWorksAsExpected() {
    final MarketImpl market =
        new MarketImpl(MARKET_NAME, MARKET_ID, BASE_CURRENCY, COUNTER_CURRENCY);
    market.setName(MARKET_NAME);
    assertTrue(market.toString().contains(MARKET_NAME));
  }
}
