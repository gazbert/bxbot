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
 * 封装 Exchange 适配器的配置。
 *
 * @author gazbert
 * @since 1.0
 */
public interface ExchangeConfig {

  /**
   * Returns the name of the exchange.
   * 返回交易所的名称。
   *
   * @return the exchange name.
   * @return 交换名称。
   */
  String getExchangeName();

  /**
   * Returns the fully qualified class name of the Exchange Adapter.
   *返回 Exchange 适配器的完全限定类名。
   *
   * @return the full class name (includes packages) of the Exchange Adapter.
   * @return 交换适配器的完整类名（包括包）。
   */
  String getExchangeAdapter();

  /**
   * Returns the authentication config.
   * 返回身份验证配置。
   *
   * @return authentication config if present, null otherwise.
   * @return 身份验证配置（如果存在），否则为 null。
   */
  AuthenticationConfig getAuthenticationConfig();

  /**
   * Returns the network config.
   * 返回网络配置。
   *
   * @return network config if present, null otherwise.
   * @return 网络配置（如果存在），否则为 null。
   */
  NetworkConfig getNetworkConfig();

  /**
   * Returns the other config.
   * 返回其他配置。
   *
   * @return other config if present, null otherwise.
   * @return 其他配置（如果存在），否则为 null。
   */
  OtherConfig getOtherConfig();
}
