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
import com.google.common.base.Objects;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A OpenOrder implementation that can be used by Exchange Adapters.
 *
 * @author gazbert
 */
@Data
public final class OpenOrderImpl implements OpenOrder {

  private String id;
  private String marketId;
  private OrderType type;

  @EqualsAndHashCode.Exclude private Date creationDate;
  @EqualsAndHashCode.Exclude private BigDecimal price;
  @EqualsAndHashCode.Exclude private BigDecimal quantity;
  @EqualsAndHashCode.Exclude private BigDecimal originalQuantity;
  @EqualsAndHashCode.Exclude private BigDecimal total;

  /**
   * Creates a new Open Order.
   *
   * @param id the order ID.
   * @param creationDate the creation date.
   * @param marketId the market ID.
   * @param type the order type.
   * @param price the price.
   * @param quantity the quantity remaining.
   * @param originalQuantity the original quantity.
   * @param total the total cost.
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

  @Override
  public Date getCreationDate() {
    if (creationDate != null) {
      return new Date(creationDate.getTime());
    }
    return null;
  }

  /**
   * Sets the order creation date.
   *
   * @param creationDate the order creation date.
   */
  void setCreationDate(Date creationDate) {
    if (creationDate != null) {
      this.creationDate = new Date(creationDate.getTime());
    }
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
}
