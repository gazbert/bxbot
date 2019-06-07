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

/**
 * Encapsulates configuration for an Exchange Adapter.
 *
 * @author gazbert
 * @since 1.0
 */
public interface ExchangeConfig {

  /**
   * Returns the name of the exchange.
   *
   * @return the exchange name.
   */
  String getExchangeName();

  /**
   * Returns the fully qualified class name of the Exchange Adapter.
   *
   * @return the full class name (includes packages) of the Exchange Adapter.
   */
  String getExchangeAdapter();

  /**
   * Returns the authentication config.
   *
   * @return authentication config if present, null otherwise.
   */
  AuthenticationConfig getAuthenticationConfig();

  /**
   * Returns the network config.
   *
   * @return network config if present, null otherwise.
   */
  NetworkConfig getNetworkConfig();

  /**
   * Returns the other config.
   *
   * @return other config if present, null otherwise.
   */
  OtherConfig getOtherConfig();
}
