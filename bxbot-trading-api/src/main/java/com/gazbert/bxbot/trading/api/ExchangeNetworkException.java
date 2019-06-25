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

package com.gazbert.bxbot.trading.api;

/**
 * This exception is thrown by the Exchange Adapter when there is a network error when attempting to
 * connect to the exchange to make an API call.
 *
 * <p>The non-fatal error response codes and messages specified in the exchange.yaml config file
 * determine whether this exception is thrown by the Exchange Adapter.
 *
 * <p>If your Trading Strategy catches this exception, you could retry the API call, or exit from
 * your Trading Strategy and let the Trading Engine execute your Trading Strategy at the next trade
 * cycle. This allows the you to recover from temporary network issues.
 *
 * <p>If the Trading Engine receives these exceptions from directly calling an Exchange Adapter
 * method, it will log the event and sleep until the next trade cycle.
 *
 * @author gazbert
 * @since 1.0
 */
public final class ExchangeNetworkException extends Exception {

  private static final long serialVersionUID = 1090595894948829893L;

  /**
   * Constructor builds exception with error message.
   *
   * @param msg the error message.
   */
  public ExchangeNetworkException(String msg) {
    super(msg);
  }

  /**
   * Constructor builds exception with error message and original throwable.
   *
   * @param msg the error message.
   * @param e the original exception.
   */
  public ExchangeNetworkException(String msg, Throwable e) {
    super(msg, e);
  }
}
