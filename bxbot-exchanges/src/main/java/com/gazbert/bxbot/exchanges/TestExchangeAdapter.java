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

package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This test adapter is now deprecated. It has been superseded by the {@link
  TryModeExchangeAdapter}.
 此测试适配器现已弃用。它已被 {@link
TryModeExchangeAdapter}。
 *
 * <p>Dummy Exchange adapter used to keep the bot up and running for engine and strategy testing.
 * <p>虚拟交换适配器用于保持机器人正常运行以进行引擎和策略测试。
 *
 * <p>Makes public calls to the Bitstamp exchange. It does not trade. All private (authenticated)
  requests are stubbed.
 <p>向 Bitstamp 交易所进行公开调用。它不交易。所有私有（经过身份验证）
 请求被存根。
 *
 * <p>Might be handy for 'dry testing' your algos.
 * <p>对于“干式测试”您的算法可能会很方便。
 *
 * @author gazbert
 * @since 1.0 {@code @deprecated}
 */
@Deprecated(since = "1.4.0")
public final class TestExchangeAdapter extends BitstampExchangeAdapter {

  private static final String DUMMY_BALANCE = "100.00";

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId) {
    return new ArrayList<>();
  }

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) {
    return "DUMMY_ORDER_ID: " + UUID.randomUUID().toString();
  }

  /* marketId is not needed for cancelling orders on this exchange.*/
  @Override
  public boolean cancelOrder(String orderId, String marketIdNotNeeded) {
    return true;
  }

  @Override
  public BalanceInfo getBalanceInfo() {
    final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
    balancesAvailable.put("BTC", new BigDecimal(DUMMY_BALANCE));
    balancesAvailable.put("USD", new BigDecimal(DUMMY_BALANCE));
    balancesAvailable.put("EUR", new BigDecimal(DUMMY_BALANCE));
    balancesAvailable.put("LTC", new BigDecimal(DUMMY_BALANCE));
    balancesAvailable.put("XRP", new BigDecimal(DUMMY_BALANCE));

    final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
    balancesOnOrder.put("BTC", new BigDecimal(DUMMY_BALANCE));
    balancesOnOrder.put("USD", new BigDecimal(DUMMY_BALANCE));
    balancesOnOrder.put("EUR", new BigDecimal(DUMMY_BALANCE));
    balancesOnOrder.put("LTC", new BigDecimal(DUMMY_BALANCE));
    balancesOnOrder.put("XRP", new BigDecimal(DUMMY_BALANCE));

    return new BalanceInfoImpl(balancesAvailable, balancesOnOrder);
  }

  @Override
  public String getImplName() {
    return "Dummy Test Adapter - based on Bitstamp HTTP API v2";
  }
}
