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
 *  代表交易所的未结订单（活动订单）。
 *
 * @author gazbert
 * @since 1.0
 */
public interface OpenOrder {

  /**
   * Returns the ID for this order.
   *  返回此订单的 ID。
   *
   * @return the ID of the order.
   *  @return 订单ID。
   */
  String getId();

  /**
   * Returns the exchange date/time the order was created.
   *  * 返回创建订单的交换日期/时间。
   *
   * @return The exchange date/time.
   *  * @return 交换日期/时间。
   */
  Date getCreationDate();

  /**
   * Returns the id of the market this order was placed on.
   *  返回此订单所在市场的 ID。
   *
   * @return the id of the market.
   *  @return 市场的 id。
   */
  String getMarketId();

  /**
   * Returns the type of order. Value will be {@link OrderType#BUY} or {@link OrderType#SELL}.
   *  * 返回订单的类型。值为 {@link OrderType#BUY} 或 {@link OrderType#SELL}。
   *
   * @return the type of order.
   *  @return 订单类型。
   */
  OrderType getType();

  /**
   * Returns the price per unit for this order. This is usually in BTC or USD.
   *  * 返回此订单的每单位价格。这通常是比特币或美元。
   *
   * @return the price per unit for this order.
   *  @return 此订单的单位价格。
   */
  BigDecimal getPrice();

  /**
   * Returns the Quantity remaining for this order. This is usually the amount of the other currency you want to trade for BTC/USD.
   *    * 返回此订单的剩余数量。这通常是您想要交易 BTC/USD 的其他货币的金额。
   *
   * @return the Quantity remaining for this order.
   *  @return 此订单的剩余数量。
   */
  BigDecimal getQuantity();

  /**
   * Returns the Original total order quantity. If the Exchange does not provide this information,
    the value will be null. This is usually the amount of the other currency you want to trade for
    BTC/USD.
   * 返回原始总订单数量。如果联交所不提供此信息，
   该值将为空。这通常是您想要交易的其他货币的金额
   比特币/美元。
   *
   * @return the Original total order quantity if the Exchange provides this information, null  otherwise.
   * @return 如果交易所提供此信息，则返回原始总订单数量，否则返回 null。
   */
  BigDecimal getOriginalQuantity();

  /**
   * Returns the Total value of order (price * quantity). This is usually in BTC or USD.
   *  * 返回订单的总价值（价格 * 数量）。这通常是比特币或美元。
   *
   * @return the Total value of order (price * quantity).
   *  @return 订单总价值（价格 * 数量）。
   */
  BigDecimal getTotal();
}
