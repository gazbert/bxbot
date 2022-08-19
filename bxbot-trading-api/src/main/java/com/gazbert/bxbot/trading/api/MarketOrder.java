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

/**
 * Represents a Market Order on the exchange.
 *  代表交易所的市价单。
 *
 * <p>The type of order (buy/sell) is determined by the {@link OrderType}.
 *  订单类型（买入/卖出）由 {@link OrderType} 确定。
 *
 * @author gazbert
 * @since 1.0
 */
public interface MarketOrder {

  /**
   * Returns the type of order. Value will be {@link OrderType#BUY} or {@link OrderType#SELL}.
   *  * 返回订单的类型。值为 {@link OrderType#BUY} 或 {@link OrderType#SELL}。
   *
   * @return the type of order.
   *        订单的类型。
   */
  OrderType getType();

  /**
   * Returns the price of the order. This is usually in BTC or USD.
   *  * 返回订单的价格。这通常是比特币或美元。
   *
   * @return Price of the order.
   *  订单的价格。
   */
  BigDecimal getPrice();

  /**
   * Returns the quantity of the order. This is usually the amount of the other currency you want to trade for BTC/USD.
   *  * 返回订单的数量。这通常是您想要交易 BTC/USD 的其他货币的金额。
   *
   * @return Quantity of the order.
   * @return 订单数量。
   */
  BigDecimal getQuantity();

  /**
   * Returns the total value of order (price * quantity). This is usually in BTC or USD.
   *  * 返回订单的总价值（价格 * 数量）。这通常是比特币或美元。
   *
   * @return Total value of order (price * quantity).
   *    @return 订单总价值（价格 * 数量）。
   */
  BigDecimal getTotal();
}
