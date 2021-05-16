package com.gazbert.bxbot.exchanges.trading.api.impl;

import com.gazbert.bxbot.trading.api.Ohlc;
import com.gazbert.bxbot.trading.api.OhlcFrame;
import java.util.List;

public class OhlcImpl implements Ohlc {
  private final Integer resumeID;
  private final List<OhlcFrame> frames;

  /**
   * This class represents the OHLC result. It contains all frames and an ID from which subsequent
   * calls can resume fetching OHLC data
   *
   * @param resumeID the ID which can be used to resume OHLC download by ignoring all order results
   * @param frames the OHLC frames in the requested packaging interval
   */
  public OhlcImpl(Integer resumeID, List<OhlcFrame> frames) {
    this.resumeID = resumeID;
    this.frames = frames;
  }

  @Override
  public Integer getResumeID() {
    return resumeID;
  }

  @Override
  public List<OhlcFrame> getFrames() {
    return frames;
  }
}
