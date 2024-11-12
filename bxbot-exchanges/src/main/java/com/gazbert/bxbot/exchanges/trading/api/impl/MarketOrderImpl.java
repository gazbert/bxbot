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
import com.gazbert.bxbot.trading.api.OrderType;
import java.math.BigDecimal;
import lombok.Data;

/**
 * A MarketOrder implementation that can be used by Exchange Adapters.
 *
 * @author gazbert
 */
@Data
public final class MarketOrderImpl implements MarketOrder {

  private OrderType type;
  private BigDecimal price;
  private BigDecimal quantity;
  private BigDecimal total;

  /**
   * Creates a new Market Order.
   *
   * @param type the order type.
   * @param price the price.
   * @param quantity the quantity.
   * @param total the total cost.
   */
  public MarketOrderImpl(OrderType type, BigDecimal price, BigDecimal quantity, BigDecimal total) {
    this.type = type;
    this.price = price;
    this.quantity = quantity;
    this.total = total;
  }
}
