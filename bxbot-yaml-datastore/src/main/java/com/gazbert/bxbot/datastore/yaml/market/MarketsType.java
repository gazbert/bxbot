/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.datastore.yaml.market;

import com.gazbert.bxbot.domain.market.MarketConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a list of Market configs for dumping to and loading from YAML.
 *包装用于转储到 YAML 和从 YAML 加载的市场配置列表。
 * @author gazbert
 */
public class MarketsType {

  private List<MarketConfig> markets;

  /**
   * Returns the Market configs.
   *  返回市场配置。
   * @return a list of Market configs.
   * 市场配置列表。
   */
  public List<MarketConfig> getMarkets() {
    if (markets == null) {
      markets = new ArrayList<>();
    }
    return markets;
  }

  public void setMarkets(List<MarketConfig> markets) {
    this.markets = markets;
  }
}
