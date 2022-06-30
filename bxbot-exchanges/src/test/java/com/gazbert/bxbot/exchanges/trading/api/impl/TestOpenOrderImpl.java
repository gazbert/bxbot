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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gazbert.bxbot.trading.api.OrderType;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * Tests the Open Order impl behaves as expected.
 * 测试 Open Order impl 的行为是否符合预期。
 *
 * @author gazbert
 */
class TestOpenOrderImpl {

  private static final String ID = "abc_123_def_456_ghi_789";
  private static final Date CREATION_DATE = new Date();
  private static final String MARKET_ID = "BTC_USD";
  private static final BigDecimal PRICE = new BigDecimal("671.91");
  private static final BigDecimal ORIGINAL_QUANTITY = new BigDecimal("0.01433434");
  private static final BigDecimal TOTAL = PRICE.multiply(ORIGINAL_QUANTITY);
  private static final BigDecimal QUANTITY =
      ORIGINAL_QUANTITY.subtract(new BigDecimal("0.00112112"));

  @Test
  void testOpenOrderIsInitialisedAsExpected() {
    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            ID,
            CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            PRICE,
            QUANTITY,
            ORIGINAL_QUANTITY,
            TOTAL);

    assertEquals(ID, openOrder.getId());
    assertEquals(CREATION_DATE, openOrder.getCreationDate());
    assertEquals(MARKET_ID, openOrder.getMarketId());
    assertEquals(OrderType.SELL, openOrder.getType());
    assertEquals(PRICE, openOrder.getPrice());
    assertEquals(QUANTITY, openOrder.getQuantity());
    assertEquals(ORIGINAL_QUANTITY, openOrder.getOriginalQuantity());
    assertEquals(TOTAL, openOrder.getTotal());
  }

  @Test
  void testSettersWorkAsExpected() {
    final OpenOrderImpl openOrder =
        new OpenOrderImpl(null, null, null, null, null, null, null, null);
    assertNull(openOrder.getId());
    assertNull(openOrder.getCreationDate());
    assertNull(openOrder.getMarketId());
    assertNull(openOrder.getType());
    assertNull(openOrder.getPrice());
    assertNull(openOrder.getQuantity());
    assertNull(openOrder.getOriginalQuantity());
    assertNull(openOrder.getTotal());

    openOrder.setId(ID);
    assertEquals(ID, openOrder.getId());

    openOrder.setCreationDate(CREATION_DATE);
    assertEquals(CREATION_DATE, openOrder.getCreationDate());

    openOrder.setMarketId(MARKET_ID);
    assertEquals(MARKET_ID, openOrder.getMarketId());

    openOrder.setType(OrderType.BUY);
    assertEquals(OrderType.BUY, openOrder.getType());

    openOrder.setPrice(PRICE);
    assertEquals(PRICE, openOrder.getPrice());

    openOrder.setQuantity(QUANTITY);
    assertEquals(QUANTITY, openOrder.getQuantity());

    openOrder.setOriginalQuantity(ORIGINAL_QUANTITY);
    assertEquals(ORIGINAL_QUANTITY, openOrder.getOriginalQuantity());

    openOrder.setTotal(TOTAL);
    assertEquals(TOTAL, openOrder.getTotal());
  }

  @Test
  void testEqualsWorksAsExpected() {
    final OpenOrderImpl openOrder1 =
        new OpenOrderImpl(
            ID,
            CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            PRICE,
            QUANTITY,
            ORIGINAL_QUANTITY,
            TOTAL);

    final OpenOrderImpl openOrder2 =
        new OpenOrderImpl(
            "different-id",
            CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            PRICE,
            QUANTITY,
            ORIGINAL_QUANTITY,
            TOTAL);

    final OpenOrderImpl openOrder3 =
        new OpenOrderImpl(
            ID,
            CREATION_DATE,
            "diff-market",
            OrderType.SELL,
            PRICE,
            QUANTITY,
            ORIGINAL_QUANTITY,
            TOTAL);

    final OpenOrderImpl openOrder4 =
        new OpenOrderImpl(
            ID, CREATION_DATE, MARKET_ID, OrderType.BUY, PRICE, QUANTITY, ORIGINAL_QUANTITY, TOTAL);

    assertEquals(openOrder1, openOrder1);

    assertNotEquals(openOrder1, openOrder2);
    assertNotEquals(openOrder1, openOrder3);
    assertNotEquals(openOrder1, openOrder4);
  }

  @Test
  void testToStringWorksAsExpected() {
    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            ID,
            CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            PRICE,
            QUANTITY,
            ORIGINAL_QUANTITY,
            TOTAL);

    assertTrue(openOrder.toString().contains(ID));
  }
}
