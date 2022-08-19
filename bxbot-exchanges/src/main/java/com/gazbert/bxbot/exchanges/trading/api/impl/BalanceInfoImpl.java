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

package com.gazbert.bxbot.exchanges.trading.api.impl;

import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;
import java.util.Map;

/**
 * A BalanceInfo implementation that can be used by Exchange Adapters.
 * Exchange 适配器可以使用的余额信息实现。
 *
 * @author gazbert
 */
public final class BalanceInfoImpl implements BalanceInfo {

  private Map<String, BigDecimal> balancesAvailable;
  private Map<String, BigDecimal> balancesOnHold;

  public BalanceInfoImpl(
      Map<String, BigDecimal> balancesAvailable, Map<String, BigDecimal> balancesOnHold) {
    this.balancesAvailable = balancesAvailable;
    this.balancesOnHold = balancesOnHold;
  }

  public Map<String, BigDecimal> getBalancesAvailable() {
    return balancesAvailable;
  }

  public void setBalancesAvailable(Map<String, BigDecimal> balancesAvailable) {
    this.balancesAvailable = balancesAvailable;
  }

  public Map<String, BigDecimal> getBalancesOnHold() {
    return balancesOnHold;
  }

  public void setBalancesOnHold(Map<String, BigDecimal> balancesOnHold) {
    this.balancesOnHold = balancesOnHold;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("balancesAvailable", balancesAvailable)
        .add("balancesOnHold", balancesOnHold)
        .toString();
  }
}
