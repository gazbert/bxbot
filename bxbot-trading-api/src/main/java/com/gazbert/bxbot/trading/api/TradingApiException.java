/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

/**
 * This exception is thrown from Exchange Adapter implementations when there is a problem making an
 * API call to the exchange.
 *
 * <p>If your Trading Strategy receives this exception, this means something bad as happened; you
 * would probably want to wrap this exception in a StrategyException and let the Trading Engine
 * shutdown the bot immediately to prevent unexpected losses.
 *
 * <p>If the Trading Engine receives one of these exceptions from directly calling an Exchange
 * Adapter method, it shuts down the bot immediately.
 *
 * @author gazbert
 * @since 1.0
 */
public class TradingApiException extends Exception {

  private static final long serialVersionUID = -8279304672615688060L;

  /**
   * Constructor builds exception with error message.
   *
   * @param msg the error message.
   */
  public TradingApiException(String msg) {
    super(msg);
  }

  /**
   * Constructor builds exception with error message and original throwable.
   *
   * @param msg the error message.
   * @param e the original exception.
   */
  public TradingApiException(String msg, Throwable e) {
    super(msg, e);
  }
}
