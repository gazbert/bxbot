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
 *
 * @author gazbert
 * @since 1.0
 */
public interface BalanceInfo {

  /**
   * Returns map of available balances.
   *
   * <p>The key is the currency id in UPPERCASE, e.g. LTC, BTC, USD
   *
   * @return map of available balances.
   */
  Map<String, BigDecimal> getBalancesAvailable();

  /**
   * Returns map of balances on hold.
   *
   * <p>Some exchanges do not provide this information and the returned map will be empty.
   *
   * <p>The key is the currency id in UPPERCASE, e.g. LTC, BTC, USD
   *
   * @return map of balances on hold.
   */
  Map<String, BigDecimal> getBalancesOnHold();
}
