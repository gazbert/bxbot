package com.gazbert.bxbot.trading.api;

import java.util.List;

public interface Ohlc {
  Integer getResumeID();

  List<OhlcFrame> getFrames();
}
