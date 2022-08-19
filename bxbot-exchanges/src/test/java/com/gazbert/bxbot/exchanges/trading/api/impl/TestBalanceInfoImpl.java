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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the Balance Info impl behaves as expected.
 * 测试 Balance Info impl 的行为是否符合预期。
 *
 * @author gazbert
 */
class TestBalanceInfoImpl {

  private static final String BTC_CURRENCY_ID = "BTC";
  private static final BigDecimal BTC_BALANCE_AVAILABLE = new BigDecimal("1000.24546282");
  private static final BigDecimal BTC_BALANCE_ON_HOLD = new BigDecimal("100.63825573");

  private static final String USD_CURRENCY_ID = "USD";
  private static final BigDecimal USD_BALANCE_AVAILABLE = new BigDecimal("2000.57573495");
  private static final BigDecimal USD_BALANCE_ON_HOLD = new BigDecimal("200.45834593");

  /**
   * Map of wallet balances available to trade.
    Key is currency id, e.g. BTC, USD
   可用于交易的钱包余额地图。
   键是货币 ID，例如比特币、美元
   */
  private Map<String, BigDecimal> balancesAvailable;

  /**
   * Map of wallet balances currently on-hold for open orders.
    Key is currency id, e.g. BTC, USD
   当前保留未结订单的钱包余额地图。
   键是货币 ID，例如比特币、美元
   */
  private Map<String, BigDecimal> balancesOnHold;

  /** Sets up some test balances.
   * 设置一些测试天平。 */
  @BeforeEach
  void setupBalancesBeforeEachTest() {
    balancesAvailable = new HashMap<>();
    balancesAvailable.put(BTC_CURRENCY_ID, BTC_BALANCE_AVAILABLE);
    balancesAvailable.put(USD_CURRENCY_ID, USD_BALANCE_AVAILABLE);

    balancesOnHold = new HashMap<>();
    balancesOnHold.put(BTC_CURRENCY_ID, BTC_BALANCE_ON_HOLD);
    balancesOnHold.put(USD_CURRENCY_ID, USD_BALANCE_ON_HOLD);
  }

  @Test
  void testBalanceInfoIsInitialisedAsExpected() {
    final BalanceInfoImpl balanceInfo = new BalanceInfoImpl(balancesAvailable, balancesOnHold);

    assertEquals(balancesAvailable, balanceInfo.getBalancesAvailable());
    assertEquals(BTC_BALANCE_AVAILABLE, balancesAvailable.get(BTC_CURRENCY_ID));
    assertEquals(USD_BALANCE_AVAILABLE, balancesAvailable.get(USD_CURRENCY_ID));

    assertEquals(balancesOnHold, balanceInfo.getBalancesOnHold());
    assertEquals(BTC_BALANCE_ON_HOLD, balancesOnHold.get(BTC_CURRENCY_ID));
    assertEquals(USD_BALANCE_ON_HOLD, balancesOnHold.get(USD_CURRENCY_ID));
  }

  @Test
  void testSettersWorkAsExpected() {
    final BalanceInfoImpl balanceInfo = new BalanceInfoImpl(null, null);
    assertNull(balanceInfo.getBalancesAvailable());
    assertNull(balanceInfo.getBalancesOnHold());

    balanceInfo.setBalancesAvailable(balancesAvailable);
    assertEquals(balancesAvailable, balanceInfo.getBalancesAvailable());
    assertEquals(BTC_BALANCE_AVAILABLE, balancesAvailable.get(BTC_CURRENCY_ID));
    assertEquals(USD_BALANCE_AVAILABLE, balancesAvailable.get(USD_CURRENCY_ID));

    balanceInfo.setBalancesOnHold(balancesOnHold);
    assertEquals(balancesOnHold, balanceInfo.getBalancesOnHold());
    assertEquals(BTC_BALANCE_ON_HOLD, balancesOnHold.get(BTC_CURRENCY_ID));
    assertEquals(USD_BALANCE_ON_HOLD, balancesOnHold.get(USD_CURRENCY_ID));
  }

  @Test
  void testToStringWorksAsExpected() {
    final BalanceInfoImpl balanceInfo = new BalanceInfoImpl(balancesAvailable, balancesOnHold);
    assertTrue(balanceInfo.toString().contains(balancesAvailable.toString()));
  }
}
