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

package com.gazbert.bxbot.exchanges.trading.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gazbert.bxbot.trading.api.OrderType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests the Market Order impl behaves as expected.
 * 测试 Market Order impl 的行为是否符合预期。
 *
 * @author gazbert
 */
class TestMarketOrderImpl {

  private static final BigDecimal PRICE = new BigDecimal("671.91");
  private static final BigDecimal QUANTITY = new BigDecimal("0.01345453");
  private static final BigDecimal TOTAL = PRICE.multiply(QUANTITY);

  @Test
  void testMarketOrderIsInitialisedAsExpected() {
    final MarketOrderImpl marketOrder = new MarketOrderImpl(OrderType.BUY, PRICE, QUANTITY, TOTAL);

    assertEquals(OrderType.BUY, marketOrder.getType());
    assertEquals(PRICE, marketOrder.getPrice());
    assertEquals(QUANTITY, marketOrder.getQuantity());
    assertEquals(TOTAL, marketOrder.getTotal());
  }

  @Test
  void testSettersWorkAsExpected() {
    final MarketOrderImpl marketOrder = new MarketOrderImpl(null, null, null, null);
    assertNull(marketOrder.getType());
    assertNull(marketOrder.getPrice());
    assertNull(marketOrder.getQuantity());
    assertNull(marketOrder.getTotal());

    marketOrder.setType(OrderType.BUY);
    assertEquals(OrderType.BUY, marketOrder.getType());

    marketOrder.setPrice(PRICE);
    assertEquals(PRICE, marketOrder.getPrice());

    marketOrder.setQuantity(QUANTITY);
    assertEquals(QUANTITY, marketOrder.getQuantity());

    marketOrder.setTotal(TOTAL);
    assertEquals(TOTAL, marketOrder.getTotal());
  }

  @Test
  void testToStringWorksAsExpected() {
    final MarketOrderImpl marketOrder = new MarketOrderImpl(OrderType.BUY, PRICE, QUANTITY, TOTAL);
    assertTrue(marketOrder.toString().contains(TOTAL.toString()));
  }
}
