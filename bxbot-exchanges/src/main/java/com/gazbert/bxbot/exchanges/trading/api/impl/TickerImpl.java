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

package com.gazbert.bxbot.exchanges.trading.api.impl;

import com.gazbert.bxbot.trading.api.Ticker;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;

/**
 * A Ticker implementation that can be used by Exchange Adapters.
 *
 * @author gazbert
 */
public final class TickerImpl implements Ticker {

  private BigDecimal last;
  private BigDecimal bid;
  private BigDecimal ask;
  private BigDecimal low;
  private BigDecimal high;
  private BigDecimal open;
  private BigDecimal volume;
  private BigDecimal vwap;
  private Long timestamp;

  /**
   * Creates a new TicketImpl.
   *
   * @param last the last trade price.
   * @param bid the current bid.
   * @param ask the current ask.
   * @param low the current low.
   * @param high the current high.
   * @param open the current open.
   * @param volume the current volume.
   * @param vwap the current vwap.
   * @param timestamp the timestamp for the tick.
   */
  public TickerImpl(
      BigDecimal last,
      BigDecimal bid,
      BigDecimal ask,
      BigDecimal low,
      BigDecimal high,
      BigDecimal open,
      BigDecimal volume,
      BigDecimal vwap,
      Long timestamp) {

    this.last = last;
    this.bid = bid;
    this.ask = ask;
    this.low = low;
    this.high = high;
    this.open = open;
    this.volume = volume;
    this.vwap = vwap;
    this.timestamp = timestamp;
  }

  @Override
  public BigDecimal getLast() {
    return last;
  }

  /**
   * Set last price.
   *
   * @param last the last price.
   */
  public void setLast(BigDecimal last) {
    this.last = last;
  }

  @Override
  public BigDecimal getBid() {
    return bid;
  }

  /**
   * Set bid price.
   *
   * @param bid the bid price.
   */
  public void setBid(BigDecimal bid) {
    this.bid = bid;
  }

  @Override
  public BigDecimal getAsk() {
    return ask;
  }

  /**
   * Set ask price.
   *
   * @param ask the ask price.
   */
  public void setAsk(BigDecimal ask) {
    this.ask = ask;
  }

  @Override
  public BigDecimal getLow() {
    return low;
  }

  /**
   * Set low price.
   *
   * @param low the low price.
   */
  public void setLow(BigDecimal low) {
    this.low = low;
  }

  @Override
  public BigDecimal getHigh() {
    return high;
  }

  /**
   * Set high price.
   *
   * @param high the high price.
   */
  public void setHigh(BigDecimal high) {
    this.high = high;
  }

  @Override
  public BigDecimal getOpen() {
    return open;
  }

  /**
   * Set open price.
   *
   * @param open the open price.
   */
  public void setOpen(BigDecimal open) {
    this.open = open;
  }

  @Override
  public BigDecimal getVolume() {
    return volume;
  }

  /**
   * Set volume.
   *
   * @param volume the volume.
   */
  public void setVolume(BigDecimal volume) {
    this.volume = volume;
  }

  @Override
  public BigDecimal getVwap() {
    return vwap;
  }

  /**
   * Set vwap.
   *
   * @param vwap the vwap.
   */
  public void setVwap(BigDecimal vwap) {
    this.vwap = vwap;
  }

  @Override
  public Long getTimestamp() {
    return timestamp;
  }

  /**
   * Set timestamp.
   *
   * @param timestamp the timestamp.
   */
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("last", last)
        .add("bid", bid)
        .add("ask", ask)
        .add("low", low)
        .add("high", high)
        .add("open", open)
        .add("volume", volume)
        .add("vwap", vwap)
        .add("timestamp", timestamp)
        .toString();
  }
}
