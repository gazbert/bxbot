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

package com.gazbert.bxbot.core.config.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Test the StrategyConfigItems behaves as expected.
 * 测试 StrategyConfigItems 的行为是否符合预期。
 *
 * @author gazbert
 */
class TestStrategyConfigItems {

  private static final String BUY_PRICE_CONFIG_ITEM_KEY = "buyPrice";
  private static final String BUY_PRICE_CONFIG_ITEM_VALUE = "671.15";

  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_KEY = "amountToBuy";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_VALUE = "0.5";

  @Test
  void testAddingAndFetchingConfigItems() {

    final Map<String, String> items = new HashMap<>();
    items.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    items.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

    final StrategyConfigItems strategyConfig = new StrategyConfigItems();
    strategyConfig.setItems(items);

    assertEquals(2, strategyConfig.getNumberOfConfigItems());

    assertTrue(strategyConfig.getConfigItemKeys().contains(BUY_PRICE_CONFIG_ITEM_KEY));
    assertTrue(strategyConfig.getConfigItemKeys().contains(AMOUNT_TO_BUY_CONFIG_ITEM_KEY));

    assertTrue(strategyConfig.getItems().containsValue(BUY_PRICE_CONFIG_ITEM_VALUE));
    assertTrue(strategyConfig.getItems().containsValue(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE));

    assertEquals(
        BUY_PRICE_CONFIG_ITEM_VALUE, strategyConfig.getConfigItem(BUY_PRICE_CONFIG_ITEM_KEY));
    assertEquals(
        AMOUNT_TO_BUY_CONFIG_ITEM_VALUE,
        strategyConfig.getConfigItem(AMOUNT_TO_BUY_CONFIG_ITEM_KEY));
  }

  @Test
  void testToStringWorksAsExpected() {
    final StrategyConfigItems strategyConfig = new StrategyConfigItems();
    strategyConfig.getItems().put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    assertTrue(strategyConfig.toString().contains(BUY_PRICE_CONFIG_ITEM_VALUE));
  }
}
