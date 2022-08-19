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

package com.gazbert.bxbot.trading.api;

import java.util.List;

/**
 * Represents a Market Order Book.
 * 代表市场订单簿。
 *
 * <p>The Market Order Book SELL orders are ordered price ascending - <em>lowest</em> ASK price is first in list.
 * * <p>Market Order Book SELL 订单按价格升序排列 - <em>最低</em> ASK 价格在列表中排在首位。
 *
 * <p>The Market Order Book BUY orders are ordered price descending - <em>highest</em> BID price is first in list.
 * * <p>市场订单簿买入订单按价格降序排列 - <em>最高</em> BID 价格在列表中排在首位。
 *
 * @author gazbert
 * @since 1.0
 */
public interface MarketOrderBook {

  /**
   * Returns the market id for this Market Order Book.
   *  返回此市场订单簿的市场 ID。
   *
   * @return The market id.
   *  @return 市场编号。
   */
  String getMarketId();

  /**
   * Returns current SELL orders for the market. Ordered price ascending - <em>lowest</em> ASK price is first in list.
   *  * 返回当前市场的卖出订单。订购价格升序 - <em>最低</em> ASK 价格在列表中排在首位。
   *
   * @return current SELL orders for the market.
   *  @return 当前市场的卖出订单。
   */
  List<MarketOrder> getSellOrders();

  /**
   * Return the current BUY orders for the market. Ordered price descending - <em>highest</em> BID price is first in list.
   *  * 返回当前市场的买单。订购价格降序 - <em>最高</em> BID 价格在列表中排在首位。
   *
   * @return current BUY orders for the market.
   *  @return 当前市场的买单。
   */
  List<MarketOrder> getBuyOrders();
}
