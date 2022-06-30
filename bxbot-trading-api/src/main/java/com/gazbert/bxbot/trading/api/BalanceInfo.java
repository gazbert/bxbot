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

import java.math.BigDecimal;
import java.util.Map;

/**
 * Encapsulates wallet balance info held on the exchange.
 * 封装交易所持有的钱包余额信息。
 *
 * @author gazbert
 * @since 1.0
 */
public interface BalanceInfo {

  /**
   * Returns map of available balances.
   *  返回可用余额的映射。
   *
   * <p>The key is the currency id in UPPERCASE, e.g. LTC, BTC, USD
   *  * <p>键是大写的货币ID，例如 LTC, BTC, USD
   *
   * @return map of available balances.
   *  * @return 可用余额的映射。
   */
  Map<String, BigDecimal> getBalancesAvailable();

  /**
   * Returns map of balances on hold.
   *  返回搁置余额的地图。
   *
   * <p>Some exchanges do not provide this information and the returned map will be empty.
   *  <p>某些交易所不提供此信息，返回的地图将为空。
   *
   * <p>The key is the currency id in UPPERCASE, e.g. LTC, BTC, USD
   *  关键是大写的货币ID，e.g. LTC, BTC, USD
   *
   * @return map of balances on hold.
   *  @return 保留的余额地图。
   */
  Map<String, BigDecimal> getBalancesOnHold();
}
