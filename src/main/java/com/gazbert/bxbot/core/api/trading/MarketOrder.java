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

package com.gazbert.bxbot.core.api.trading;

import com.google.common.base.MoreObjects;

import java.math.BigDecimal;

/**
 * <p>
 * Domain class representing a Market Order on the exchange.
 * </p>
 *
 * <p>
 * The type of order (buy/sell) is determined by the {@link OrderType}.
 * </p>
 *
 * @author gazbert
 */
public final class MarketOrder {

    /**
     * The type of order.
     * Value will be {@link OrderType#BUY} or {@link OrderType#SELL}.
     */
    private OrderType type;

    /**
     * The price of the order. This is usually in BTC or USD.
     */
    private BigDecimal price;

    /**
     * The quantity of the order. This is usually the amount of the other currency you want to trade for BTC/USD.
     */
    private BigDecimal quantity;

    /**
     * The total value of the order (price * quantity). This is usually in BTC or USD.
     */
    private BigDecimal total;

    /**
     * Constructor builds a Market Order.
     *
     * @param type      Type of order. Value must be {@link OrderType#BUY} or {@link OrderType#SELL}.
     * @param price     Price of the order. This is usually in BTC or USD.
     * @param quantity  Quantity of the order. This is usually the amount of the other currency you want to trade for BTC/USD.
     * @param total     Total value of order (price * quantity). This is usually in BTC or USD.
     */
    public MarketOrder(OrderType type, BigDecimal price, BigDecimal quantity, BigDecimal total) {
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.total = total;
    }

    /**
     * Returns the type of order. Value will be {@link OrderType#BUY} or {@link OrderType#SELL}.
     * @return the type of order.
     */
    public OrderType getType() {
        return type;
    }

    /**
     * Sets the type of order. Value must be {@link OrderType#BUY} or {@link OrderType#SELL}.
     * @param type the order type.
     */
    public void setType(OrderType type) {
        this.type = type;
    }

    /**
     * Returns the price of the order. This is usually in BTC or USD.
     *
     * @return Price of the order.
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the price of the order. This is usually in BTC or USD.
     *
     * @param price the price of the order.
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Returns the quantity of the order. This is usually the amount of the other currency you want to trade for BTC/USD.
     *
     * @return Quantity of the order.
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Sets the quantity of the order. This is usually the amount of the other currency you want to trade for BTC/USD.
     *
     * @param quantity the quantity of the order.
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the total value of order (price * quantity). This is usually in BTC or USD.
     *
     * @return Total value of order (price * quantity).
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Sets the total value of order (price * quantity). This is usually in BTC or USD.
     *
     * @param total the total value of the order.
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("price", price)
                .add("quantity", quantity)
                .add("total", total)
                .toString();
    }
}