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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;


/**
 * Tests default impl methods of TradingApi interface.
 *
 * @author gazbert
 */
public class TestTradingApi {

  @Test
  public void testGetVersion() {
    final MyApiImpl myApi = new MyApiImpl();
    assertEquals("1.2", myApi.getVersion());
  }

  @Test
  public void testGetTicker() throws Exception {
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

  @Test
  public void testGetOhlcHasDefaultImpl() throws Exception {
    final MyApiImpl myApi = new MyApiImpl();
    final Ohlc ohlc = myApi.getOhlc("market-123", OhlcInterval.OneWeek);

    assertNotNull(ohlc);
    assertNull(ohlc.getResumeID());
    assertTrue(ohlc.getFrames().isEmpty());
  }

  @Test
  public void testGetOhlcResumeHasDefaultImpl() throws Exception {
    final MyApiImpl myApi = new MyApiImpl();
    final Ohlc ohlc = myApi.getOhlc("market-123", OhlcInterval.OneWeek, 5);

    assertNotNull(ohlc);
    assertNull(ohlc.getResumeID());
    assertTrue(ohlc.getFrames().isEmpty());
  }

  /** Test class. */
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
