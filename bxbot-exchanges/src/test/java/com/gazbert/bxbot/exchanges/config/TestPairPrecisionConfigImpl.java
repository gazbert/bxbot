/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 gazbert
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the PairPrecisionConfigImpl behaves as expected.
 * 测试 PairPrecisionConfigImpl 的行为是否符合预期。
 *
 * @author gazbert
 */
class TestPairPrecisionConfigImpl {

  private static final String BTC_USD_PAIR_ID = "BTC/USD";
  private static final Integer BTC_USD_PRICE = Integer.valueOf("1000");
  private static final Integer BTC_USD_VOLUME = Integer.valueOf("34234234");

  private static final String ETH_USD_PAIR_ID = "ETC/USD";
  private static final Integer ETH_USD_PRICE = Integer.valueOf("4545345");
  private static final Integer ETH_USD_VOLUME = Integer.valueOf("77777");

  private Map<String, Integer> prices;
  private Map<String, Integer> volumes;

  @BeforeEach
  void setup() {
    prices = new HashMap<>();
    prices.put(BTC_USD_PAIR_ID, BTC_USD_PRICE);
    prices.put(ETH_USD_PAIR_ID, ETH_USD_PRICE);

    volumes = new HashMap<>();
    volumes.put(BTC_USD_PAIR_ID, BTC_USD_VOLUME);
    volumes.put(ETH_USD_PAIR_ID, ETH_USD_VOLUME);
  }

  @Test
  void getPricePrecision() {
    final PairPrecisionConfigImpl pairPrecisionConfig =
        new PairPrecisionConfigImpl(prices, volumes);
    assertEquals(BTC_USD_PRICE, pairPrecisionConfig.getPricePrecision(BTC_USD_PAIR_ID));
    assertEquals(ETH_USD_PRICE, pairPrecisionConfig.getPricePrecision(ETH_USD_PAIR_ID));
  }

  @Test
  void getVolumePrecision() {
    final PairPrecisionConfigImpl pairPrecisionConfig =
        new PairPrecisionConfigImpl(prices, volumes);
    assertEquals(BTC_USD_VOLUME, pairPrecisionConfig.getVolumePrecision(BTC_USD_PAIR_ID));
    assertEquals(ETH_USD_VOLUME, pairPrecisionConfig.getVolumePrecision(ETH_USD_PAIR_ID));
  }
}
