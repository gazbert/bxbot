/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 gazbert
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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.TickerImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.Ticker;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Tests the behaviour of the Try-Mode Exchange Adapter.
 * * 测试 Try-Mode Exchange Adapter 的行为。
 *
 * <p>It has been configured to use Bitstamp for the public API calls.
 * * <p>它已被配置为使用 Bitstamp 进行公共 API 调用。
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
@PrepareForTest(TryModeExchangeAdapter.class)
public class TestTryModeExchangeAdapter extends AbstractExchangeAdapter {

  // --------------------------------------------------------------------------
  // Canned test data
  // 罐头测试数据
  // --------------------------------------------------------------------------

  private static final String MARKET_ID = "btcusd";
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
  private static final BigDecimal INSTANT_FILL_BUY_ORDER_PRICE = new BigDecimal("18800.14");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("19789.12");
  private static final BigDecimal INSTANT_FILL_SELL_ORDER_PRICE = new BigDecimal("1000.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final String UNRECOGNISED_ORDER_ID = "80894263";

  private static final BigDecimal PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE =
      new BigDecimal("0.0025");
  private static final BigDecimal PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE =
      new BigDecimal("0.0024");

  private static final BigDecimal LAST = new BigDecimal("18789.58");
  private static final BigDecimal BID = new BigDecimal("18778.25");
  private static final BigDecimal ASK = new BigDecimal("18783.33");
  private static final BigDecimal LOW = new BigDecimal("17111.00");
  private static final BigDecimal HIGH = new BigDecimal("18790.76");
  private static final BigDecimal OPEN = new BigDecimal("17477.98");
  private static final BigDecimal VOLUME = new BigDecimal("10231.12911572");
  private static final BigDecimal VWAP = new BigDecimal("17756.56");
  private static final Long TIMESTAMP = 1513439945L;

  private static final BigDecimal ORDER_1_PRICE = new BigDecimal("111.11");
  private static final BigDecimal ORDER_1_QUANTITY = new BigDecimal("0.01614453");
  private static final BigDecimal ORDER_1_TOTAL = ORDER_1_PRICE.multiply(ORDER_1_QUANTITY);
  private static final BigDecimal ORDER_2_PRICE = new BigDecimal("50.22");
  private static final BigDecimal ORDER_2_QUANTITY = new BigDecimal("0.02423424");
  private static final BigDecimal ORDER_2_TOTAL = ORDER_2_PRICE.multiply(ORDER_2_QUANTITY);
  private static final BigDecimal ORDER_3_PRICE = new BigDecimal("333.33");
  private static final BigDecimal ORDER_3_QUANTITY = new BigDecimal("0.03435344");
  private static final BigDecimal ORDER_3_TOTAL = ORDER_3_PRICE.multiply(ORDER_3_QUANTITY);

  private static final BigDecimal LATEST_MARKET_PRICE = new BigDecimal("20789.58");

  private static final String OPEN_ORDER_ID = "abc_123_def_456_ghi_789";
  private static final Date OPEN_ORDER_CREATION_DATE = new Date();
  private static final BigDecimal OPEN_SELL_ORDER_PRICE = new BigDecimal("99971.91");
  private static final BigDecimal CLOSED_SELL_ORDER_PRICE = new BigDecimal("100.91");
  private static final BigDecimal OPEN_BUY_ORDER_PRICE = new BigDecimal("100.91");
  private static final BigDecimal CLOSED_BUY_ORDER_PRICE = new BigDecimal("99999.91");
  private static final BigDecimal OPEN_ORDER_ORIGINAL_QUANTITY = new BigDecimal("0.01433434");
  private static final BigDecimal OPEN_ORDER_TOTAL =
      OPEN_SELL_ORDER_PRICE.multiply(OPEN_ORDER_ORIGINAL_QUANTITY);
  private static final BigDecimal OPEN_ORDER_QUANTITY =
      OPEN_ORDER_ORIGINAL_QUANTITY.subtract(new BigDecimal("0.00112112"));

  // --------------------------------------------------------------------------
  // Mocked API Ops
  // 模拟 API 操作
  // --------------------------------------------------------------------------

  private static final String MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER =
      "createDelegateExchangeAdapter";

  private static final String MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE =
      "getPercentageOfSellOrderTakenForExchangeFee";
  private static final String MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE =
      "getPercentageOfBuyOrderTakenForExchangeFee";
  private static final String MOCKED_GET_TICKER_METHOD = "getTicker";
  private static final String MOCKED_GET_BALANCE_INFO = "getBalanceInfo";
  private static final String MOCKED_GET_MARKET_ORDERS = "getMarketOrders";
  private static final String MOCKED_GET_LATEST_MARKET_PRICE = "getLatestMarketPrice";

  // --------------------------------------------------------------------------
  // Delegate Exchange Adapter config
  // 委托交换适配器配置
  // --------------------------------------------------------------------------

  private static final String DELEGATE_ADAPTER =
      "com.gazbert.bxbot.exchanges.BitstampExchangeAdapter";

  private static final String BASE_CURRENCY = "BTC";
  private static final String BASE_CURRENCY_STARTING_BALANCE = "1.0";
  private static final String COUNTER_CURRENCY = "USD";
  private static final String COUNTER_CURRENCY_STARTING_BALANCE = "100.0";

  private static final String CLIENT_ID = "clientId123";
  private static final String KEY = "key123";
  private static final String SECRET = "notGonnaTellYa";
  private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
  private static final List<String> nonFatalNetworkErrorMessages =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake");

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;

  /** Create some exchange config - the TradingEngine would normally do this.
   * 创建一些交换配置 - TradingEngine 通常会这样做。 */
  @Before
  public void setupForEachTest() {

    networkConfig = PowerMock.createMock(NetworkConfig.class);
    expect(networkConfig.getConnectionTimeout()).andReturn(30);
    expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
    expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

    OtherConfig otherConfig = PowerMock.createMock(OtherConfig.class);
    expect(otherConfig.getItem("simulatedBaseCurrency")).andReturn(BASE_CURRENCY).atLeastOnce();
    expect(otherConfig.getItem("baseCurrencyStartingBalance"))
        .andReturn(BASE_CURRENCY_STARTING_BALANCE)
        .atLeastOnce();

    expect(otherConfig.getItem("simulatedCounterCurrency"))
        .andReturn(COUNTER_CURRENCY)
        .atLeastOnce();
    expect(otherConfig.getItem("counterCurrencyStartingBalance"))
        .andReturn(COUNTER_CURRENCY_STARTING_BALANCE)
        .atLeastOnce();

    expect(otherConfig.getItem("delegateAdapter")).andReturn(DELEGATE_ADAPTER).atLeastOnce();

    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("client-id")).andReturn(CLIENT_ID);
    expect(authenticationConfig.getItem("key")).andReturn(KEY);
    expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

    exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
    expect(exchangeConfig.getOtherConfig()).andReturn(otherConfig).atLeastOnce();
    expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
    expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
  }

  // --------------------------------------------------------------------------
  //  Create Orders tests
  // 创建订单测试
  // --------------------------------------------------------------------------

  @Test(expected = TradingApiException.class)
  public void testCreateOrderWhenOneAlreadyExists() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            OPEN_SELL_ORDER_PRICE,
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch! // 哎哟！
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    tryModeExchangeAdapter.createOrder(
        MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);

    PowerMock.verifyAll();
  }

  @Test
  public void testCreateBuyOrder() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    final String orderId =
        tryModeExchangeAdapter.createOrder(
            MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    assertNotNull(orderId);

    PowerMock.verifyAll();
  }

  @Test
  public void testCreateBuyOrderThatFillsInstantly() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_GET_TICKER_METHOD,
            MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    PowerMock.expectPrivate(
            delegateExchangeAdapter,
            MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE,
            eq(MARKET_ID))
        .andReturn(PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    final String orderId =
        tryModeExchangeAdapter.createOrder(
            MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, INSTANT_FILL_BUY_ORDER_PRICE);
    assertNotNull(orderId);

    PowerMock.verifyAll();
  }

  @Test
  public void testCreateSellOrder() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    final String orderId =
        tryModeExchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertNotNull(orderId);

    PowerMock.verifyAll();
  }

  @Test
  public void testCreateSellOrderThatFillsInstantly() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_GET_TICKER_METHOD,
            MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    PowerMock.expectPrivate(
            delegateExchangeAdapter,
            MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE,
            eq(MARKET_ID))
        .andReturn(PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    final String orderId =
        tryModeExchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, INSTANT_FILL_SELL_ORDER_PRICE);
    assertNotNull(orderId);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Cancel Order tests
  // 取消订单测试
  // --------------------------------------------------------------------------

  @Test
  public void testCancelOrderIsSuccessful() throws Exception {

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            OPEN_SELL_ORDER_PRICE,
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_MARKET_ORDERS);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch!
    // 哎哟！
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    // Order ID will match the open order
    // 订单 ID 将匹配未结订单
    assertTrue(tryModeExchangeAdapter.cancelOrder(OPEN_ORDER_ID, MARKET_ID));

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderWhenNoneExist() throws Exception {

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_MARKET_ORDERS);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    tryModeExchangeAdapter.cancelOrder(OPEN_ORDER_ID, MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderWhenOrderIdDoesNotMatch() throws Exception {

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            OPEN_SELL_ORDER_PRICE,
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_MARKET_ORDERS);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch!
    // 哎哟！
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    // Order ID will not match the open order
    // 订单ID将与未结订单不匹配
    tryModeExchangeAdapter.cancelOrder(UNRECOGNISED_ORDER_ID, MARKET_ID);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Your Open Orders tests
  // 获取您的未结订单测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingYourOpenOrdersWhenNoneExist() throws Exception {

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_MARKET_ORDERS);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final List<OpenOrder> openOrders = tryModeExchangeAdapter.getYourOpenOrders(MARKET_ID);

    assertEquals(0, openOrders.size());

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingYourOpenOrdersWhenSellOrderNotFilled() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            OPEN_SELL_ORDER_PRICE,
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch!
    // 哎哟！
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final List<OpenOrder> openOrders = tryModeExchangeAdapter.getYourOpenOrders(MARKET_ID);

    assertEquals(1, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals(OPEN_ORDER_ID, openOrders.get(0).getId());
    assertSame(OrderType.SELL, openOrders.get(0).getType());
    assertEquals(openOrders.get(0).getCreationDate(), OPEN_ORDER_CREATION_DATE);
    assertEquals(0, openOrders.get(0).getPrice().compareTo(OPEN_SELL_ORDER_PRICE));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(OPEN_ORDER_QUANTITY));
    assertEquals(0, openOrders.get(0).getTotal().compareTo(OPEN_ORDER_TOTAL));
    assertEquals(OPEN_ORDER_ORIGINAL_QUANTITY, openOrders.get(0).getOriginalQuantity());

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingYourOpenOrdersWhenSellOrderFilled() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.SELL,
            CLOSED_SELL_ORDER_PRICE, // this will result in an empty open order list  // 这将导致一个空的未结订单列表
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_GET_TICKER_METHOD,
            MOCKED_GET_MARKET_ORDERS,
            MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    PowerMock.expectPrivate(
            delegateExchangeAdapter,
            MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE,
            eq(MARKET_ID))
        .andReturn(PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch! 哎哟!
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final List<OpenOrder> openOrders = tryModeExchangeAdapter.getYourOpenOrders(MARKET_ID);

    assertEquals(0, openOrders.size());

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingYourOpenOrdersWhenBuyOrderNotFilled() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.BUY,
            OPEN_BUY_ORDER_PRICE,
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch! 哎哟!
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final List<OpenOrder> openOrders = tryModeExchangeAdapter.getYourOpenOrders(MARKET_ID);

    assertEquals(1, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals(OPEN_ORDER_ID, openOrders.get(0).getId());
    assertSame(OrderType.BUY, openOrders.get(0).getType());
    assertEquals(openOrders.get(0).getCreationDate(), OPEN_ORDER_CREATION_DATE);
    assertEquals(0, openOrders.get(0).getPrice().compareTo(OPEN_BUY_ORDER_PRICE));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(OPEN_ORDER_QUANTITY));
    assertEquals(0, openOrders.get(0).getTotal().compareTo(OPEN_ORDER_TOTAL));
    assertEquals(OPEN_ORDER_ORIGINAL_QUANTITY, openOrders.get(0).getOriginalQuantity());

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingYourOpenOrdersWhenBuyOrderFilled() throws Exception {

    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final OpenOrderImpl openOrder =
        new OpenOrderImpl(
            OPEN_ORDER_ID,
            OPEN_ORDER_CREATION_DATE,
            MARKET_ID,
            OrderType.BUY,
            CLOSED_BUY_ORDER_PRICE, // this will result in an empty open order list 这将导致一个空的未结订单列表
            OPEN_ORDER_QUANTITY,
            OPEN_ORDER_ORIGINAL_QUANTITY,
            OPEN_ORDER_TOTAL);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_GET_TICKER_METHOD,
            MOCKED_GET_MARKET_ORDERS,
            MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    PowerMock.expectPrivate(
            delegateExchangeAdapter,
            MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE,
            eq(MARKET_ID))
        .andReturn(PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    // Ouch!
    Whitebox.setInternalState(tryModeExchangeAdapter, "currentOpenOrder", openOrder);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final List<OpenOrder> openOrders = tryModeExchangeAdapter.getYourOpenOrders(MARKET_ID);

    assertEquals(0, openOrders.size());

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Market Orders tests
  // 获取市价单测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingMarketOrdersSuccessfully() throws Exception {

    final MarketOrder sellOrder1 =
        new MarketOrderImpl(OrderType.SELL, ORDER_1_PRICE, ORDER_1_QUANTITY, ORDER_1_TOTAL);
    final MarketOrder sellOrder2 =
        new MarketOrderImpl(OrderType.SELL, ORDER_2_PRICE, ORDER_2_QUANTITY, ORDER_2_TOTAL);
    final MarketOrder sellOrder3 =
        new MarketOrderImpl(OrderType.SELL, ORDER_3_PRICE, ORDER_3_QUANTITY, ORDER_3_TOTAL);

    final List<MarketOrder> sellOrders = new ArrayList<>();
    sellOrders.add(sellOrder1);
    sellOrders.add(sellOrder2);
    sellOrders.add(sellOrder3);

    final MarketOrder buyOrder1 =
        new MarketOrderImpl(OrderType.BUY, ORDER_1_PRICE, ORDER_1_QUANTITY, ORDER_1_TOTAL);
    final MarketOrder buyOrder2 =
        new MarketOrderImpl(OrderType.BUY, ORDER_2_PRICE, ORDER_2_QUANTITY, ORDER_2_TOTAL);
    final MarketOrder buyOrder3 =
        new MarketOrderImpl(OrderType.BUY, ORDER_3_PRICE, ORDER_3_QUANTITY, ORDER_3_TOTAL);

    final List<MarketOrder> buyOrders = new ArrayList<>();
    buyOrders.add(buyOrder1);
    buyOrders.add(buyOrder2);
    buyOrders.add(buyOrder3);

    final MarketOrderBookImpl mockedMarketOrderBook =
        new MarketOrderBookImpl(MARKET_ID, sellOrders, buyOrders);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_MARKET_ORDERS);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_MARKET_ORDERS, eq(MARKET_ID))
        .andReturn(mockedMarketOrderBook);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final MarketOrderBook marketOrderBook = tryModeExchangeAdapter.getMarketOrders(MARKET_ID);

    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    assertEquals(3, marketOrderBook.getBuyOrders().size());
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(ORDER_1_PRICE));
    assertEquals(
        0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(ORDER_1_QUANTITY));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(ORDER_1_TOTAL));
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(1).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(1).getPrice().compareTo(ORDER_2_PRICE));
    assertEquals(
        0, marketOrderBook.getBuyOrders().get(1).getQuantity().compareTo(ORDER_2_QUANTITY));
    assertEquals(0, marketOrderBook.getBuyOrders().get(1).getTotal().compareTo(ORDER_2_TOTAL));
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(2).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(2).getPrice().compareTo(ORDER_3_PRICE));
    assertEquals(
        0, marketOrderBook.getBuyOrders().get(2).getQuantity().compareTo(ORDER_3_QUANTITY));
    assertEquals(0, marketOrderBook.getBuyOrders().get(2).getTotal().compareTo(ORDER_3_TOTAL));

    assertEquals(3, marketOrderBook.getSellOrders().size());
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getPrice().compareTo(ORDER_1_PRICE));
    assertEquals(
        0, marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(ORDER_1_QUANTITY));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getTotal().compareTo(ORDER_1_TOTAL));
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(1).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(1).getPrice().compareTo(ORDER_2_PRICE));
    assertEquals(
        0, marketOrderBook.getSellOrders().get(1).getQuantity().compareTo(ORDER_2_QUANTITY));
    assertEquals(0, marketOrderBook.getSellOrders().get(1).getTotal().compareTo(ORDER_2_TOTAL));
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(2).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(2).getPrice().compareTo(ORDER_3_PRICE));
    assertEquals(
        0, marketOrderBook.getSellOrders().get(2).getQuantity().compareTo(ORDER_3_QUANTITY));
    assertEquals(0, marketOrderBook.getSellOrders().get(2).getTotal().compareTo(ORDER_3_TOTAL));

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Latest Market Price tests
  // 获取最新的市场价格测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingLatestMarketPriceSuccessfully() throws Exception {

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_LATEST_MARKET_PRICE);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_LATEST_MARKET_PRICE, eq(MARKET_ID))
        .andReturn(LATEST_MARKET_PRICE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();
    tryModeExchangeAdapter.init(exchangeConfig);

    final BigDecimal latestMarketPrice =
        tryModeExchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, RoundingMode.HALF_UP);
    assertEquals(0, latestMarketPrice.compareTo(LATEST_MARKET_PRICE));

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Balance Info tests
  // 获取余额信息测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingBalanceInfoSuccessfully() throws Exception {

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_BALANCE_INFO);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final BalanceInfo balanceInfo = tryModeExchangeAdapter.getBalanceInfo();

    assertEquals(
        0,
        balanceInfo
            .getBalancesAvailable()
            .get(BASE_CURRENCY)
            .compareTo(new BigDecimal(BASE_CURRENCY_STARTING_BALANCE)));
    assertEquals(
        0,
        balanceInfo
            .getBalancesAvailable()
            .get(COUNTER_CURRENCY)
            .compareTo(new BigDecimal(COUNTER_CURRENCY_STARTING_BALANCE)));

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Exchange Fees for Buy orders tests
  // 获取买单测试的交易所费用
  // --------------------------------------------------------------------------

  @Test
  public void testGettingExchangeBuyingFeeSuccessfully() throws Exception {
    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    PowerMock.expectPrivate(
            delegateExchangeAdapter,
            MOCKED_GET_PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE,
            eq(MARKET_ID))
        .andReturn(PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        delegateExchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(PERCENTAGE_OF_BUY_ORDER_TAKEN_FOR_EXCHANGE_FEE));

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Exchange Fees for Sell orders tests
  // 获取卖单测试的交易所费用
  // --------------------------------------------------------------------------

  @Test
  public void testGettingExchangeSellingFeeSuccessfully() throws Exception {
    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    PowerMock.expectPrivate(
            delegateExchangeAdapter,
            MOCKED_GET_PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE,
            eq(MARKET_ID))
        .andReturn(PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);

    final BigDecimal sellPercentageFee =
        tryModeExchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, sellPercentageFee.compareTo(PERCENTAGE_OF_SELL_ORDER_TAKEN_FOR_EXCHANGE_FEE));

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Ticker tests
  // 获取 Ticker 测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingTickerSuccessfully() throws Exception {
    final Ticker tickerResponse =
        new TickerImpl(LAST, BID, ASK, LOW, HIGH, OPEN, VOLUME, VWAP, TIMESTAMP);

    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);
    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andReturn(tickerResponse);

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();

    tryModeExchangeAdapter.init(exchangeConfig);
    final Ticker ticker = tryModeExchangeAdapter.getTicker(MARKET_ID);

    assertEquals(0, ticker.getLast().compareTo(LAST));
    assertEquals(0, ticker.getAsk().compareTo(ASK));
    assertEquals(0, ticker.getBid().compareTo(BID));
    assertEquals(0, ticker.getHigh().compareTo(HIGH));
    assertEquals(0, ticker.getLow().compareTo(LOW));
    assertEquals(0, ticker.getOpen().compareTo(OPEN));
    assertEquals(0, ticker.getVolume().compareTo(VOLUME));
    assertEquals(0, ticker.getVwap().compareTo(VWAP));
    assertEquals(1513439945L, (long) ticker.getTimestamp());

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingTickerHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andThrow(
            new ExchangeNetworkException(
                "Dehydrated turkey, with dehydrated oyster "
                    + "stuffing. Also dehydrated cranberry sauce, dehydrated gravy and giblets, "
                    + "dehydrated sweet potatoes in dehydrated orange sauce, dehydrated vegetable "
                    + "salad, dehydrated mince pie, dehydrated..."));

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();
    tryModeExchangeAdapter.init(exchangeConfig);
    tryModeExchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter delegateExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_GET_TICKER_METHOD);

    PowerMock.expectPrivate(delegateExchangeAdapter, MOCKED_GET_TICKER_METHOD, eq(MARKET_ID))
        .andThrow(
            new TradingApiException("There is nothing in the desert and no man needs nothing."));

    final TryModeExchangeAdapter tryModeExchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            TryModeExchangeAdapter.class, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER);

    PowerMock.expectPrivate(tryModeExchangeAdapter, MOCKED_CREATE_DELEGATE_EXCHANGE_ADAPTER)
        .andReturn(delegateExchangeAdapter);

    PowerMock.replayAll();
    tryModeExchangeAdapter.init(exchangeConfig);
    tryModeExchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Non Exchange visiting tests
  // 非交易所访问测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();
    final TryModeExchangeAdapter exchangeAdapter = new TryModeExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertEquals(
        "Try-Mode Test Adapter: configurable exchange public API delegation & simulated orders",
        exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Initialisation tests assume config property files are located under  src/test/resources
  // 初始化测试假定配置属性文件位于 src/test/resources 下
  // --------------------------------------------------------------------------

  @Test
  public void testExchangeAdapterInitialisesSuccessfully() {
    PowerMock.replayAll();
    final TryModeExchangeAdapter exchangeAdapter = new TryModeExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertNotNull(exchangeAdapter);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfClientIdConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("client-id")).andReturn(null);
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    final TryModeExchangeAdapter exchangeAdapter = new TryModeExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("client-id")).andReturn("your-client-id");
    expect(authenticationConfig.getItem("key")).andReturn(null);
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    final TryModeExchangeAdapter exchangeAdapter = new TryModeExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("client-id")).andReturn("your-client-id");
    expect(authenticationConfig.getItem("key")).andReturn("your-client-key");
    expect(authenticationConfig.getItem("secret")).andReturn(null);
    PowerMock.replayAll();

    final TryModeExchangeAdapter exchangeAdapter = new TryModeExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() {
    PowerMock.reset(networkConfig);
    expect(networkConfig.getConnectionTimeout()).andReturn(0);
    PowerMock.replayAll();

    final TryModeExchangeAdapter exchangeAdapter = new TryModeExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }
}
