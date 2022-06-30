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

package com.gazbert.bxbot.trading.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests default impl methods of TradingApi interface.
 * * 测试 TradingApi 接口的默认 impl 方法。
 *
 * @author gazbert
 */
class TestTradingApi {

  @Test
  void testGetVersion() {
    final MyApiImpl myApi = new MyApiImpl();
    assertEquals("1.1", myApi.getVersion());
  }

  @Test
  void testGetTicker() throws Exception {
    final MyApiImpl myApi = new MyApiImpl();
    final Ticker ticker = myApi.getTicker("market-123");
    assertNotNull(ticker);

    assertNull(ticker.getLast());
    assertNull(ticker.getBid());
    assertNull(ticker.getAsk());
    assertNull(ticker.getLow());
    assertNull(ticker.getHigh());
    assertNull(ticker.getOpen());
    assertNull(ticker.getVolume());
    assertNull(ticker.getVwap());
    assertNull(ticker.getTimestamp());
  }

  /** Test class.
   * 测试类 */
  class MyApiImpl implements TradingApi {

    @Override
    public String getImplName() {
      return null;
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) {
      return null;
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) {
      return null;
    }

    @Override
    public String createOrder(
        String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) {
      return null;
    }

    @Override
    public boolean cancelOrder(String orderId, String marketId) {
      return false;
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) {
      return null;
    }

    @Override
    public BalanceInfo getBalanceInfo() {
      return null;
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
      return null;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
      return null;
    }
  }
}
