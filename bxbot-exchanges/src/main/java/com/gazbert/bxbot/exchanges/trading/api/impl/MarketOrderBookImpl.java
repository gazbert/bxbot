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

import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import java.util.List;
import lombok.Data;

/**
 * A MarketOrderBook implementation that can be used by Exchange Adapters.
 *
 * @author gazbert
 */
@Data
public final class MarketOrderBookImpl implements MarketOrderBook {

  private String marketId;
  private List<MarketOrder> sellOrders;
  private List<MarketOrder> buyOrders;

  /**
   * Creates a new Market Order Book.
   *
   * @param marketId the market ID.
   * @param sellOrders the list of sell orders.
   * @param buyOrders the list of buy orders.
   */
  public MarketOrderBookImpl(
      String marketId, List<MarketOrder> sellOrders, List<MarketOrder> buyOrders) {
    this.marketId = marketId;
    this.sellOrders = sellOrders;
    this.buyOrders = buyOrders;
  }
}
