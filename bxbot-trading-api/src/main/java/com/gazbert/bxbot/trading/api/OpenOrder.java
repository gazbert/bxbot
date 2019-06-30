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

import java.math.BigDecimal;
import java.util.Date;

/**
 * Represents an Open Order (active order) on the exchange.
 *
 * @author gazbert
 * @since 1.0
 */
public interface OpenOrder {

  /**
   * Returns the ID for this order.
   *
   * @return the ID of the order.
   */
  String getId();

  /**
   * Returns the exchange date/time the order was created.
   *
   * @return The exchange date/time.
   */
  Date getCreationDate();

  /**
   * Returns the id of the market this order was placed on.
   *
   * @return the id of the market.
   */
  String getMarketId();

  /**
   * Returns the type of order. Value will be {@link OrderType#BUY} or {@link OrderType#SELL}.
   *
   * @return the type of order.
   */
  OrderType getType();

  /**
   * Returns the price per unit for this order. This is usually in BTC or USD.
   *
   * @return the price per unit for this order.
   */
  BigDecimal getPrice();

  /**
   * Returns the Quantity remaining for this order. This is usually the amount of the other currency
   * you want to trade for BTC/USD.
   *
   * @return the Quantity remaining for this order.
   */
  BigDecimal getQuantity();

  /**
   * Returns the Original total order quantity. If the Exchange does not provide this information,
   * the value will be null. This is usually the amount of the other currency you want to trade for
   * BTC/USD.
   *
   * @return the Original total order quantity if the Exchange provides this information, null
   *     otherwise.
   */
  BigDecimal getOriginalQuantity();

  /**
   * Returns the Total value of order (price * quantity). This is usually in BTC or USD.
   *
   * @return the Total value of order (price * quantity).
   */
  BigDecimal getTotal();
}
