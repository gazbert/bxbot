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

import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Market Order Book impl behaves as expected.
 * 测试 Market Order Book impl 的行为是否符合预期。
 *
 * @author gazbert
 */
class TestMarketOrderBookImpl {

  private static final String MARKET_ID = "BTC_USD";

  private List<MarketOrder> sellOrders;
  private List<MarketOrder> buyOrders;

  private static final BigDecimal ORDER_1_PRICE = new BigDecimal("111.11");
  private static final BigDecimal ORDER_1_QUANTITY = new BigDecimal("0.01614453");
  private static final BigDecimal ORDER_1_TOTAL = ORDER_1_PRICE.multiply(ORDER_1_QUANTITY);

  private static final BigDecimal ORDER_2_PRICE = new BigDecimal("222.22");
  private static final BigDecimal ORDER_2_QUANTITY = new BigDecimal("0.02423424");
  private static final BigDecimal ORDER_2_TOTAL = ORDER_2_PRICE.multiply(ORDER_2_QUANTITY);

  private static final BigDecimal ORDER_3_PRICE = new BigDecimal("333.33");
  private static final BigDecimal ORDER_3_QUANTITY = new BigDecimal("0.03435344");
  private static final BigDecimal ORDER_3_TOTAL = ORDER_3_PRICE.multiply(ORDER_3_QUANTITY);

  private MarketOrder sellOrder1;
  private MarketOrder sellOrder2;
  private MarketOrder sellOrder3;

  private MarketOrder buyOrder1;
  private MarketOrder buyOrder2;
  private MarketOrder buyOrder3;

  /** Sets up some MarketOrders for the tests.
   * 为测试设置一些 MarketOrders。 */
  @BeforeEach
  void setupOrdersBeforeEachTest() {
    sellOrder1 =
        new MarketOrderImpl(OrderType.SELL, ORDER_1_PRICE, ORDER_1_QUANTITY, ORDER_1_TOTAL);
    sellOrder2 =
        new MarketOrderImpl(OrderType.SELL, ORDER_2_PRICE, ORDER_2_QUANTITY, ORDER_2_TOTAL);
    sellOrder3 =
        new MarketOrderImpl(OrderType.SELL, ORDER_3_PRICE, ORDER_3_QUANTITY, ORDER_3_TOTAL);

    sellOrders = new ArrayList<>();
    sellOrders.add(sellOrder1);
    sellOrders.add(sellOrder2);
    sellOrders.add(sellOrder3);

    buyOrder1 = new MarketOrderImpl(OrderType.BUY, ORDER_1_PRICE, ORDER_1_QUANTITY, ORDER_1_TOTAL);
    buyOrder2 = new MarketOrderImpl(OrderType.BUY, ORDER_2_PRICE, ORDER_2_QUANTITY, ORDER_2_TOTAL);
    buyOrder3 = new MarketOrderImpl(OrderType.BUY, ORDER_3_PRICE, ORDER_3_QUANTITY, ORDER_3_TOTAL);

    buyOrders = new ArrayList<>();
    buyOrders.add(buyOrder1);
    buyOrders.add(buyOrder2);
    buyOrders.add(buyOrder3);
  }

  @Test
  void testMarketOrderBookIsInitialisedAsExpected() {
    final MarketOrderBookImpl marketOrderBook =
        new MarketOrderBookImpl(MARKET_ID, sellOrders, buyOrders);
    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    assertEquals(sellOrders, marketOrderBook.getSellOrders());
    assertEquals(3, sellOrders.size());
    assertTrue(sellOrders.contains(sellOrder1));
    assertTrue(sellOrders.contains(sellOrder2));
    assertTrue(sellOrders.contains(sellOrder3));

    assertEquals(buyOrders, marketOrderBook.getBuyOrders());
    assertEquals(3, buyOrders.size());
    assertTrue(buyOrders.contains(buyOrder1));
    assertTrue(buyOrders.contains(buyOrder2));
    assertTrue(buyOrders.contains(buyOrder3));
  }

  @Test
  void testSettersWorkAsExpected() {
    final MarketOrderBookImpl marketOrderBook = new MarketOrderBookImpl(null, null, null);
    assertNull(marketOrderBook.getMarketId());
    assertNull(marketOrderBook.getSellOrders());
    assertNull(marketOrderBook.getBuyOrders());

    marketOrderBook.setMarketId(MARKET_ID);
    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    marketOrderBook.setSellOrders(sellOrders);
    assertEquals(sellOrders, marketOrderBook.getSellOrders());

    marketOrderBook.setBuyOrders(buyOrders);
    assertEquals(buyOrders, marketOrderBook.getBuyOrders());
  }

  @Test
  void testToStringWorksAsExpected() {
    final MarketOrderBookImpl marketOrderBook =
        new MarketOrderBookImpl(MARKET_ID, sellOrders, buyOrders);
    assertTrue(marketOrderBook.toString().contains(MARKET_ID));
  }
}
