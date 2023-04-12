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
import com.google.common.base.MoreObjects;
import java.util.List;

/**
 * A MarketOrderBook implementation that can be used by Exchange Adapters.
 *
 * @author gazbert
 */
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

  @Override
  public String getMarketId() {
    return marketId;
  }

  /**
   * Sets the market id.
   *
   * @param marketId the market id.
   */
  public void setMarketId(String marketId) {
    this.marketId = marketId;
  }

  @Override
  public List<MarketOrder> getSellOrders() {
    return sellOrders;
  }

  /**
   * Sets the sell orders.
   *
   * @param sellOrders the sell orders.
   */
  public void setSellOrders(List<MarketOrder> sellOrders) {
    this.sellOrders = sellOrders;
  }

  @Override
  public List<MarketOrder> getBuyOrders() {
    return buyOrders;
  }

  /**
   * Sets the buy orders.
   *
   * @param buyOrders the buy orders.
   */
  public void setBuyOrders(List<MarketOrder> buyOrders) {
    this.buyOrders = buyOrders;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("marketId", marketId)
        .add("sellOrders", sellOrders)
        .add("buyOrders", buyOrders)
        .toString();
  }
}
