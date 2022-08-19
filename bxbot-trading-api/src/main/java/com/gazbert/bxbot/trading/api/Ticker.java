/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Gareth Jon Lynch
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
 * Holds Exchange Ticker information.
 *  保存 Exchange Ticker 信息。
 *
 * <p>Not all exchanges provide the information returned in the Ticker methods - you'll need to
  check the relevant Exchange Adapter code/Javadoc and online Exchange API documentation.
 <p>并非所有交易所都提供在 Ticker 方法中返回的信息 - 您需要
 检查相关的 Exchange 适配器代码/Javadoc 和在线 Exchange API 文档。
 *
 * <p>If the exchange does not provide the information, a null value is returned.
 *  <p>如果交易所不提供信息，则返回空值。
 *
 * @author gazbert
 * @since 1.1
 */
public interface Ticker {

  /**
   * Returns the last trade price.
   *  返回最后的交易价格。
   *
   * @return the last trade price if the exchange provides it, null otherwise.
   *  @return 交易所提供的最后交易价格，否则返回 null。
   */
  BigDecimal getLast();

  /**
   * Returns the highest buy order price.
   *  返回最高买单价格。
   *
   * @return the highest but order price if the exchange provides it, null otherwise.
   *  @return 如果交易所提供，则返回最高订单价格，否则返回 null。
   */
  BigDecimal getBid();

  /**
   * Returns the lowest sell order price.
   * 返回最低卖单价格。
   *
   * @return the lowest sell order price if the exchange provides it, null otherwise.
   *  @return 交易所提供的最低卖单价格，否则为空。
   */
  BigDecimal getAsk();

  /**
   * Returns the last 24 hours price low.
   *  返回过去 24 小时的最低价。
   *
   * @return the last 24 hours price low if the exchange provides it, null otherwise.
   *  @return 如果交易所提供，则返回最近 24 小时的最低价格，否则返回 null。
   */
  BigDecimal getLow();

  /**
   * Returns the last 24 hours price high.
   *  返回过去 24 小时的最高价。
   *
   * @return the last 24 hours price high if the exchange provides it, null otherwise.
   *  @return 如果交易所提供，则返回最近 24 小时的最高价格，否则返回 null。
   */
  BigDecimal getHigh();

  /**
   * Returns the first trade price of the day.
   *  返回当天的第一笔交易价格。
   *
   * @return the first trade price of the day if the exchange provides it, null otherwise.
   *  @return 如果交易所提供，则返回当天的第一个交易价格，否则返回 null。
   */
  BigDecimal getOpen();

  /**
   * Returns the last 24 hours volume.
   *  返回过去 24 小时的交易量。
   *
   * @return the last 24 hours volume if the exchange provides it, null otherwise.
   *  @return 如果交易所提供了过去 24 小时的交易量，否则返回 null。
   */
  BigDecimal getVolume();

  /**
   * Returns the last 24 hours volume weighted average - https://en.wikipedia.org/wiki/Volume-weighted_average_price
   * * 返回过去 24 小时的交易量加权平均值 - https://en.wikipedia.org/wiki/Volume-weighted_average_price
   *
   * @return the last 24 hours volume weighted average if the exchange provides it, null otherwise.
   * @return 如果交易所提供，则返回过去 24 小时的交易量加权平均值，否则返回 null。
   */
  BigDecimal getVwap();

  /**
   * Returns the current time on the exchange in UNIX time format.
   * 以 UNIX 时间格式返回交易所的当前时间。
   *
   * @return the current time on the exchange if provided, null otherwise.
   * @return 如果提供，则返回交易所的当前时间，否则返回 null。
   */
  Long getTimestamp();
}
