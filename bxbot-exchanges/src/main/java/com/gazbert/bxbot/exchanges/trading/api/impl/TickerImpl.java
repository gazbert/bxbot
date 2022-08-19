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
 * Exchange 适配器可以使用的 Ticker 实现。
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
   * 创建一个新的 Ticket Impl。
   *
   * @param last the last trade price.
   *             * @param last 最后的交易价格。
   *
   * @param bid the current bid.
   *            * @param bid 当前出价。
   *
   * @param ask the current ask.
   *            * @param 当前询问/期望。
   *
   * @param low the current low.
   *                目前的低点
   *
   * @param high the current high.
   *             * @param high 当前高点。
   *
   * @param open the current open.
   *             * @param 当前打开。
   *
   * @param volume the current volume.
   *               * @param volume 当前总量?。
   *
   * @param vwap the current vwap.
   *             当前的 vwap。
   *
   * @param timestamp the timestamp for the tick.
   *                  tick的时间戳
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

  public void setLast(BigDecimal last) {
    this.last = last;
  }

  @Override
  public BigDecimal getBid() {
    return bid;
  }

  public void setBid(BigDecimal bid) {
    this.bid = bid;
  }

  @Override
  public BigDecimal getAsk() {
    return ask;
  }

  public void setAsk(BigDecimal ask) {
    this.ask = ask;
  }

  @Override
  public BigDecimal getLow() {
    return low;
  }

  public void setLow(BigDecimal low) {
    this.low = low;
  }

  @Override
  public BigDecimal getHigh() {
    return high;
  }

  public void setHigh(BigDecimal high) {
    this.high = high;
  }

  @Override
  public BigDecimal getOpen() {
    return open;
  }

  public void setOpen(BigDecimal open) {
    this.open = open;
  }

  @Override
  public BigDecimal getVolume() {
    return volume;
  }

  public void setVolume(BigDecimal volume) {
    this.volume = volume;
  }

  @Override
  public BigDecimal getVwap() {
    return vwap;
  }

  public void setVwap(BigDecimal vwap) {
    this.vwap = vwap;
  }

  @Override
  public Long getTimestamp() {
    return timestamp;
  }

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
