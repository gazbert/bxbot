package com.gazbert.bxbot.exchanges.trading.api.impl;

import com.gazbert.bxbot.trading.api.OhlcFrame;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class OhlcFrameImpl implements OhlcFrame {
  private final ZonedDateTime time;
  private final BigDecimal open;
  private final BigDecimal high;
  private final BigDecimal low;
  private final BigDecimal close;
  private final BigDecimal vwap;
  private final BigDecimal volume;
  private final Integer count;

  /**
   * Class representing a frame of the OHLC download.
   *
   * @param time the starttime of the corresponding ohlc frame
   * @param open the open price of the corresponding ohl frame
   * @param high the high price of the corresponding ohl frame
   * @param low the low price of the corresponding ohl frame
   * @param close the close price of the corresponding ohl frame
   * @param vwap the volume-weighted avaerage price of the corresponding ohl frame
   * @param volume the volume traded in the corresponding ohl frame
   * @param count the trades cound of the corresponding ohl frame
   */
  public OhlcFrameImpl(
      ZonedDateTime time,
      BigDecimal open,
      BigDecimal high,
      BigDecimal low,
      BigDecimal close,
      BigDecimal vwap,
      BigDecimal volume,
      Integer count) {
    this.time = time;
    this.open = open;
    this.high = high;
    this.low = low;
    this.close = close;
    this.vwap = vwap;
    this.volume = volume;
    this.count = count;
  }

  @Override
  public ZonedDateTime getTime() {
    return time;
  }

  @Override
  public BigDecimal getOpen() {
    return open;
  }

  @Override
  public BigDecimal getHigh() {
    return high;
  }

  @Override
  public BigDecimal getLow() {
    return low;
  }

  @Override
  public BigDecimal getClose() {
    return close;
  }

  @Override
  public BigDecimal getVwap() {
    return vwap;
  }

  @Override
  public BigDecimal getVolume() {
    return volume;
  }

  @Override
  public Integer getCount() {
    return count;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("time", time)
        .add("open", open)
        .add("high", high)
        .add("low", low)
        .add("close", close)
        .add("vwap", vwap)
        .add("volume", volume)
        .add("count", count)
        .toString();
  }
}
