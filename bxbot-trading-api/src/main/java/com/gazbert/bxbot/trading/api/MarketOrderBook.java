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

import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * <p>
 * Represents a Market Order Book.
 * </p>
 * <p>
 * The Market Order Book SELL orders are ordered price ascending - <em>lowest</em> ASK price is first in list.
 * </p>
 * <p>
 * The Market Order Book BUY orders are ordered price descending - <em>highest</em> BID price is first in list.
 * </p>
 *
 * @author gazbert
 * @since 1.0
 */
public final class MarketOrderBook {

    /**
     * The market id for this Market Order Book.
     */
    private String marketId;

    /**
     * Current SELL orders for the market.
     * Ordered price ascending - <em>lowest</em> ASK price is first in list.
     */
    private List<MarketOrder> sellOrders;

    /**
     * Current BUY orders for the market.
     * Ordered price descending - <em>highest</em> BID price is first in list.
     */
    private List<MarketOrder> buyOrders;


    /**
     * Constructor builds a Market Order Book.
     *
     * @param marketId   The market id for this Order Book.
     * @param sellOrders Current SELL orders for the market.
     * @param buyOrders  Current BUY orders for the market.
     */
    public MarketOrderBook(String marketId, List<MarketOrder> sellOrders, List<MarketOrder> buyOrders) {
        this.marketId = marketId;
        this.sellOrders = sellOrders;
        this.buyOrders = buyOrders;
    }

    /**
     * Returns the market id for this Market Order Book.
     *
     * @return The market id.
     */
    public String getMarketId() {
        return marketId;
    }

    /**
     * Sets The market id for this Market Order Book.
     *
     * @param marketId the Market ID.
     */
    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    /**
     * Returns current SELL orders for the market.
     * Ordered price ascending - <em>lowest</em> ASK price is first in list.
     *
     * @return current SELL orders for the market.
     */
    public List<MarketOrder> getSellOrders() {
        return sellOrders;
    }

    /**
     * Sets the current SELL orders for the market.
     *
     * @param sellOrders the current SELL orders for the Market.
     */
    public void setSellOrders(List<MarketOrder> sellOrders) {
        this.sellOrders = sellOrders;
    }

    /**
     * Return the current BUY orders for the market.
     * Ordered price descending - <em>highest</em> BID price is first in list.
     *
     * @return current BUY orders for the market.
     */
    public List<MarketOrder> getBuyOrders() {
        return buyOrders;
    }

    /**
     * Sets the Current BUY orders for the market.
     *
     * @param buyOrders the current BUY orders for the Market.
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
