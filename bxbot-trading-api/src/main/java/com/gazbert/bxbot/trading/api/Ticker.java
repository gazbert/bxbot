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
 *
 * <p>Not all exchanges provide the information returned in the Ticker methods - you'll need to
 * check the relevant Exchange Adapter code/Javadoc and online Exchange API documentation.
 *
 * <p>If the exchange does not provide the information, a null value is returned.
 *
 * @author gazbert
 * @since 1.1
 */
public interface Ticker {

  /**
   * Returns the last trade price.
   *
   * @return the last trade price if the exchange provides it, null otherwise.
   */
  BigDecimal getLast();

  /**
   * Returns the highest buy order price.
   *
   * @return the highest but order price if the exchange provides it, null otherwise.
   */
  BigDecimal getBid();

  /**
   * Returns the lowest sell order price.
   *
   * @return the lowest sell order price if the exchange provides it, null otherwise.
   */
  BigDecimal getAsk();

  /**
   * Returns the last 24 hours price low.
   *
   * @return the last 24 hours price low if the exchange provides it, null otherwise.
   */
  BigDecimal getLow();

  /**
   * Returns the last 24 hours price high.
   *
   * @return the last 24 hours price high if the exchange provides it, null otherwise.
   */
  BigDecimal getHigh();

  /**
   * Returns the first trade price of the day.
   *
   * @return the first trade price of the day if the exchange provides it, null otherwise.
   */
  BigDecimal getOpen();

  /**
   * Returns the last 24 hours volume.
   *
   * @return the last 24 hours volume if the exchange provides it, null otherwise.
   */
  BigDecimal getVolume();

  /**
   * Returns the last 24 hours volume weighted average -
   * https://en.wikipedia.org/wiki/Volume-weighted_average_price
   *
   * @return the last 24 hours volume weighted average if the exchange provides it, null otherwise.
   */
  BigDecimal getVwap();

  /**
   * Returns the current time on the exchange in UNIX time format.
   *
   * @return the current time on the exchange if provided, null otherwise.
   */
  Long getTimestamp();
}
