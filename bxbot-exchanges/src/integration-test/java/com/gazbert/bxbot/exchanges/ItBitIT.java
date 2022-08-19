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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.Ticker;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Basic integration testing with ItBit exchange.
 * 与 ItBit 交换的基本集成测试。
 *
 * @author gazbert
 */
public class ItBitIT {

  private static final String MARKET_ID = "XBTUSD";
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("10000.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.01");

  private static final String USERID = "userId123";
  private static final String KEY = "key123";
  private static final String SECRET = "notGonnaTellYa";
  private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
  private static final List<String> nonFatalNetworkErrorMessages =
      Arrays.asList(
          "Connection refused 连接被拒绝",
          "Connection reset 连接重置",
          "Remote host closed connection during handshake 握手期间远程主机关闭连接");

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;
  private OtherConfig otherConfig;

  /**
   * Create some exchange config - the TradingEngine would normally do this.
   * * 创建一些交换配置 - TradingEngine 通常会这样做。
   */
  @Before
  public void setupForEachTest() {
    authenticationConfig = createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("userId")).andReturn(USERID);
    expect(authenticationConfig.getItem("key")).andReturn(KEY);
    expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

    networkConfig = createMock(NetworkConfig.class);
    expect(networkConfig.getConnectionTimeout()).andReturn(30);
    expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
    expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

    otherConfig = createMock(OtherConfig.class);
    expect(otherConfig.getItem("buy-fee")).andReturn("0.25");
    expect(otherConfig.getItem("sell-fee")).andReturn("0.25");
    expect(otherConfig.getItem("keep-alive-during-maintenance")).andReturn("false");

    exchangeConfig = createMock(ExchangeConfig.class);
    expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
    expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
    expect(exchangeConfig.getOtherConfig()).andReturn(otherConfig);
  }

  @Test
  public void testPublicApiCalls() throws Exception {
    replay(authenticationConfig, networkConfig, otherConfig, exchangeConfig);

    final ExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    assertNotNull(exchangeAdapter.getLatestMarketPrice(MARKET_ID));

    final MarketOrderBook orderBook = exchangeAdapter.getMarketOrders(MARKET_ID);
    assertFalse(orderBook.getBuyOrders().isEmpty());
    assertFalse(orderBook.getSellOrders().isEmpty());

    final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);
    assertNotNull(ticker.getLast());
    assertNotNull(ticker.getAsk());
    assertNotNull(ticker.getBid());
    assertNotNull(ticker.getHigh());
    assertNotNull(ticker.getLow());
    assertNotNull(ticker.getOpen());
    assertNotNull(ticker.getVolume());
    assertNotNull(ticker.getVwap());
    assertNotNull(ticker.getTimestamp());

    verify(authenticationConfig, networkConfig, otherConfig, exchangeConfig);
  }

  /**
   * You'll need to change the USERID, KEY, SECRET, constants to real-world values.
   * * 您需要将 USERID、KEY、SECRET、常量更改为实际值。
   */
  @Ignore("Disabled. Integration testing authenticated API calls requires your secret credentials!")
  @Test
  public void testAuthenticatedApiCalls() throws Exception {
    replay(authenticationConfig, networkConfig, otherConfig, exchangeConfig);

    final ExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();
    assertNotNull(balanceInfo.getBalancesAvailable().get("BTC"));

    // Careful here - make sure the SELL_ORDER_PRICE is sensible!
    // 注意这里 - 确保 SELL_ORDER_PRICE 是合理的！
    // final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL,
    // SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    // final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);
    // assertTrue(openOrders.stream().anyMatch(o -> o.getId().equals(orderId)));
    // assertTrue(exchangeAdapter.cancelOrder(orderId, MARKET_ID));

    verify(authenticationConfig, networkConfig, otherConfig, exchangeConfig);
  }
}
