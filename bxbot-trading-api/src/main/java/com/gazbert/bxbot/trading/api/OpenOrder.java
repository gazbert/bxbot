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
import com.google.common.base.Objects;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Domain class representing an Open (active) Order on the exchange.
 *
 * @author gazbert
 */
public final class OpenOrder {

    /**
     * ID for this order.
     */
    private String id;

    /**
     * The exchange date/time the order was created.
     */
    private Date creationDate;

    /**
     * The id of the market this order was placed on.
     */
    private String marketId;

    /**
     * Type of order.
     * Value must be {@link OrderType#BUY} or {@link OrderType#SELL}.
     */
    private OrderType type;

    /**
     * The price per unit for this order. This is usually in BTC or USD.
     */
    private BigDecimal price;

    /**
     * Quantity remaining for this order. This is usually the amount of the other currency you want to trade for BTC/USD.
     */
    private BigDecimal quantity;

    /**
     * Original total order quantity. If the Exchange does not provide this information, the value will be null.
     * This is usually the amount of the other currency you want to trade for BTC/USD.
     */
    private BigDecimal originalQuantity;

    /**
     * Total value of order (price * quantity). This is usually in BTC or USD.
     */
    private BigDecimal total;


    /**
     * Constructor builds an Open (active) Order on the exchange.
     *
     * @param id               ID for this order.
     * @param creationDate     The exchange date/time the order was created.
     * @param marketId         The id of the market this order was placed on.
     * @param type             Type of order. Value must be {@link OrderType#BUY} or {@link OrderType#SELL}.
     * @param price            The price per unit for this order. This is usually in BTC or USD.
     * @param quantity         Quantity remaining for this order. This is usually the amount of the other currency you want to trade for BTC/USD.
     * @param originalQuantity Original total order quantity. This is usually the amount of the other currency you want to trade for BTC/USD.
     * @param total            Total value of order (price * quantity). This is usually in BTC or USD.
     */
    public OpenOrder(String id, Date creationDate, String marketId, OrderType type,
                     BigDecimal price, BigDecimal quantity, BigDecimal originalQuantity,
                     BigDecimal total) {
        this.id = id;
        this.creationDate = creationDate;
        this.marketId = marketId;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.originalQuantity = originalQuantity;
        this.total = total;
    }

    /**
     * Returns the ID for this order.
     * @return the ID of the order.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID for this order.
     *
     * @param id the ID of the order.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the exchange date/time the order was created.
     * @return The exchange date/time.
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the exchange date/time the order was created.
     *
     * @param creationDate the creation date of the order.
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns the id of the market this order was placed on.
     * @return the id of the market.
     */
    public String getMarketId() {
        return marketId;
    }

    /**
     * Sets the id of the market this order was placed on.
     *
     * @param marketId the Market ID.
     */
    public void setMarketId(String marketId) {
        this.marketId = marketId;
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
     *
     * @param type the order type.
     */
    public void setType(OrderType type) {
        this.type = type;
    }

    /**
     * Returns the price per unit for this order. This is usually in BTC or USD.
     *
     * @return the price per unit for this order.
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the price per unit for this order. This is usually in BTC or USD.
     *
     * @param price the order price.
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Returns the Quantity remaining for this order.
     * This is usually the amount of the other currency you want to trade for BTC/USD.
     *
     * @return the Quantity remaining for this order.
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Sets the Quantity remaining for this order. This is usually the amount of the other currency you want to trade for BTC/USD.
     *
     * @param quantity the order quantity.
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the Original total order quantity. If the Exchange does not provide this information, the value will be null.
     * This is usually the amount of the other currency you want to trade for BTC/USD.
     *
     * @return the Original total order quantity if the Exchange provides this information, null otherwise.
     */
    public BigDecimal getOriginalQuantity() {
        return originalQuantity;
    }

    /**
     * Sets the Original total order quantity. This is usually the amount of the other currency you want to trade for BTC/USD.
     *
     * @param originalQuantity the original order quantity.
     */
    public void setOriginalQuantity(BigDecimal originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    /**
     * Returns the Total value of order (price * quantity). This is usually in BTC or USD.
     *
     * @return the Total value of order (price * quantity).
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Sets the Total value of order (price * quantity). This is usually in BTC or USD.
     *
     * @param total the total order value.
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenOrder openOrder = (OpenOrder) o;
        return Objects.equal(id, openOrder.id) &&
                Objects.equal(marketId, openOrder.marketId) &&
                type == openOrder.type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, marketId, type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("marketId", marketId)
                .add("type", type)
                .add("price", price)
                .add("quantity", quantity)
                .add("originalQuantity", originalQuantity)
                .add("total", total)
                .toString();
    }
}
