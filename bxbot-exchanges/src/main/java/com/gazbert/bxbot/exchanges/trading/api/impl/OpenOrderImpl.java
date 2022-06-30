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

import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.math.BigDecimal;
import java.util.Date;

/**
 * A OpenOrder implementation that can be used by Exchange Adapters.
 * 可由 Exchange 适配器使用的 OpenOrder 实现。
 *
 * @author gazbert
 */
public final class OpenOrderImpl implements OpenOrder {

  private String id;
  private Date creationDate;
  private String marketId;
  private OrderType type;
  private BigDecimal price;
  private BigDecimal quantity;
  private BigDecimal originalQuantity;
  private BigDecimal total;

  /**
   * Creates a new Open Order.
   * 创建一个新的未结订单。
   *
   * @param id the order ID.
   *           订单 ID。
   *
   * @param creationDate the creation date.
   *                     创建日期。
   *
   * @param marketId the market ID.
   *                 市场编号。
   *
   * @param type the order type.
   *             订单类型。
   *
   * @param price the price.
   *              价格。
   *
   * @param quantity the quantity remaining.
   *                 剩余的数量。
   *
   * @param originalQuantity the original quantity.
   *                         原始数量。
   *
   * @param total the total cost.
   *              总成本。
   */
  public OpenOrderImpl(
      String id,
      Date creationDate,
      String marketId,
      OrderType type,
      BigDecimal price,
      BigDecimal quantity,
      BigDecimal originalQuantity,
      BigDecimal total) {

    this.id = id;
    if (creationDate != null) {
      this.creationDate = new Date(creationDate.getTime());
    }
    this.marketId = marketId;
    this.type = type;
    this.price = price;
    this.quantity = quantity;
    this.originalQuantity = originalQuantity;
    this.total = total;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /** Returns the Order creation date. 返回订单创建日期。 */
  public Date getCreationDate() {
    if (creationDate != null) {
      return new Date(creationDate.getTime());
    }
    return null;
  }

  void setCreationDate(Date creationDate) {
    if (creationDate != null) {
      this.creationDate = new Date(creationDate.getTime());
    }
  }

  public String getMarketId() {
    return marketId;
  }

  public void setMarketId(String marketId) {
    this.marketId = marketId;
  }

  public OrderType getType() {
    return type;
  }

  public void setType(OrderType type) {
    this.type = type;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getOriginalQuantity() {
    return originalQuantity;
  }

  void setOriginalQuantity(BigDecimal originalQuantity) {
    this.originalQuantity = originalQuantity;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenOrderImpl openOrder = (OpenOrderImpl) o;
    return Objects.equal(id, openOrder.id)
        && Objects.equal(marketId, openOrder.marketId)
        && type == openOrder.type;
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
