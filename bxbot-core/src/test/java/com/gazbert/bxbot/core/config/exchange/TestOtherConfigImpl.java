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

package com.gazbert.bxbot.core.config.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests Other Config exchange API config object behaves as expected.
 * 测试其他配置交换 API 配置对象的行为符合预期。
 *
 * @author gazbert
 */
class TestOtherConfigImpl {

  private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
  private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";

  private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
  private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";

  @Test
  void testAddingAndFetchingOtherConfigItems() {

    final Map<String, String> items = new HashMap<>();
    items.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    items.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    final OtherConfigImpl otherConfig = new OtherConfigImpl();
    otherConfig.setItems(items);

    assertEquals(2, otherConfig.getItems().size());
    assertEquals(BUY_FEE_CONFIG_ITEM_VALUE, otherConfig.getItem(BUY_FEE_CONFIG_ITEM_KEY));
    assertEquals(SELL_FEE_CONFIG_ITEM_VALUE, otherConfig.getItem(SELL_FEE_CONFIG_ITEM_KEY));
  }

  @Test
  void testToStringWorksAsExpected() {
    final Map<String, String> items = new HashMap<>();
    items.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    items.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    final OtherConfigImpl otherConfig = new OtherConfigImpl();
    otherConfig.setItems(items);
    assertTrue(otherConfig.toString().contains(items.toString()));
  }
}
