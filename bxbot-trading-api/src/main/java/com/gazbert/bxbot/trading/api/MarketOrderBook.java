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
 *
 * <p>The Market Order Book SELL orders are ordered price ascending - <em>lowest</em> ASK price is
 * first in list.
 *
 * <p>The Market Order Book BUY orders are ordered price descending - <em>highest</em> BID price is
 * first in list.
 *
 * @author gazbert
 * @since 1.0
 */
public interface MarketOrderBook {

  /**
   * Returns the market id for this Market Order Book.
   *
   * @return The market id.
   */
  String getMarketId();

  /**
   * Returns current SELL orders for the market. Ordered price ascending - <em>lowest</em> ASK price
   * is first in list.
   *
   * @return current SELL orders for the market.
   */
  List<MarketOrder> getSellOrders();

  /**
   * Return the current BUY orders for the market. Ordered price descending - <em>highest</em> BID
   * price is first in list.
   *
   * @return current BUY orders for the market.
   */
  List<MarketOrder> getBuyOrders();
}
