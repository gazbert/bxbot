/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.exchange.api;

import com.gazbert.bxbot.trading.api.TradingApi;

/**
 * All Exchange Adapters must implement this interface. It's main purpose is for the Trading Engine
 * to pass the adapter its configuration on startup.
 *
 * <p>The Trading Engine will send only 1 thread through the Exchange Adapter code at a time - you
 * do not have to code for concurrency.
 *
 * @author gazbert
 * @since 1.0
 */
public interface ExchangeAdapter extends TradingApi {

  /**
   * Called once by the Trading Engine when it starts up.
   *
   * @param config configuration for the Exchange Adapter.
   */
  void init(ExchangeConfig config);
}
