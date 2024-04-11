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

package com.gazbert.bxbot.datastore.yaml.exchange;

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;

/**
 * Wraps Exchange config for dumping to and loading from YAML.
 *
 * @author gazbert
 */
public class ExchangeType {

  private ExchangeConfig exchange;

  /** Creates the Exchange type. */
  public ExchangeType() {
    // No extra init needed.
  }

  /**
   * Returns the exchange config.
   *
   * @return the exchange config.
   */
  public ExchangeConfig getExchange() {
    return exchange;
  }

  /**
   * Sets the exchange config.
   *
   * @param exchange the exchange config.
   */
  public void setExchange(ExchangeConfig exchange) {
    this.exchange = exchange;
  }
}
