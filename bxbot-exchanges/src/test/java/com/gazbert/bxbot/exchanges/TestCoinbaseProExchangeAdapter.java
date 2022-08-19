/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
 * Copyright (c) 2019 David Huertas
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.Ticker;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests the behaviour of the COINBASE PRO Exchange Adapter.
 * 测试 COINBASE PRO 交换适配器的行为。
 *
 * @author davidhuertas
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*",
    "javax.xml.datatype.*"
})
@PrepareForTest(CoinbaseProExchangeAdapter.class)
public class TestCoinbaseProExchangeAdapter extends AbstractExchangeAdapterTest {

  private static final String BOOK_JSON_RESPONSE = "./src/test/exchange-data/coinbasepro/book.json";
  private static final String ORDERS_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/orders.json";
  private static final String ACCOUNTS_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/accounts.json";
  private static final String TICKER_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/ticker.json";
  private static final String NEW_BUY_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/new_buy_order.json";
  private static final String NEW_SELL_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/new_sell_order.json";
  private static final String CANCEL_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/cancel.json";
  private static final String STATS_JSON_RESPONSE =
      "./src/test/exchange-data/coinbasepro/stats.json";

  private static final String MARKET_ID = "BTC-GBP";
  private static final String ORDER_BOOK_DEPTH_LEVEL =
      "2"; //  "2" = Top 50 bids and asks (aggregated) "2" = 前 50 名出价和要价（汇总）
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.01");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.01");
  private static final String ORDER_ID_TO_CANCEL = "3ecf7a12-fc89-4d3d-baef-f158f80b3bd3";

  private static final String BOOK = "products/" + MARKET_ID + "/book";
  private static final String ORDERS = "orders";
  private static final String ACCOUNTS = "accounts";
  private static final String TICKER = "products/" + MARKET_ID + "/ticker";
  private static final String NEW_ORDER = "orders";
  private static final String CANCEL_ORDER = "orders/" + ORDER_ID_TO_CANCEL;
  private static final String STATS = "products/" + MARKET_ID + "/stats";

  private static final String MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD = "createRequestParamMap";
  private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD =
      "sendAuthenticatedRequestToExchange";
  private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD =
      "sendPublicRequestToExchange";
  private static final String MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD = "createHeaderParamMap";
  private static final String MOCKED_MAKE_NETWORK_REQUEST_METHOD = "makeNetworkRequest";

  private static final String PASSPHRASE = "lePassPhrase";
  private static final String KEY = "key123";
  private static final String SECRET = "notGonnaTellYa";
  private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
  private static final List<String> nonFatalNetworkErrorMessages =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake");

  private static final String PUBLIC_API_BASE_URL = "https://api.pro.coinbase.com/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;
  private OtherConfig otherConfig;

  /** Create some exchange config - the TradingEngine would normally do this.
   * 创建一些交换配置 - TradingEngine 通常会这样做。*/
  @Before
  public void setupForEachTest() {
    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("passphrase")).andReturn(PASSPHRASE);
    expect(authenticationConfig.getItem("key")).andReturn(KEY);
    expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

    networkConfig = PowerMock.createMock(NetworkConfig.class);
    expect(networkConfig.getConnectionTimeout()).andReturn(30);
    expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
    expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

    otherConfig = PowerMock.createMock(OtherConfig.class);
    expect(otherConfig.getItem("buy-fee")).andReturn("0.25");
    expect(otherConfig.getItem("sell-fee")).andReturn("0.25");
    expect(otherConfig.getItem("time-server-bias")).andReturn("82");

    exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
    expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
    expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
    expect(exchangeConfig.getOtherConfig()).andReturn(otherConfig);
  }

  // --------------------------------------------------------------------------
  //  Create Orders tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToBuyIsSuccessful() throws Exception {
    // Load the canned response from the exchange
    // 从交易所加载预设响应
    final byte[] encoded = Files.readAllBytes(Paths.get(NEW_BUY_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
    // 模拟出参数映射，因此我们可以断言传递给传输层的内容是我们所期望的。
    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(
            requestParamMap.put(
                "size",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(BUY_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(BUY_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("side", "buy")).andStubReturn(null);
    expect(requestParamMap.put("product_id", MARKET_ID)).andStubReturn(null);

    // Partial mock so we do not send stuff down the wire
    // 部分模拟，所以我们不会通过网络发送东西
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("POST"),
            eq(NEW_ORDER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    assertEquals("193d2ad9-e671-4d66-9211-7f75f6380231", orderId);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToSellIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(NEW_SELL_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(
            requestParamMap.put(
                "size",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);
    expect(requestParamMap.put("product_id", MARKET_ID)).andStubReturn(null);

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("POST"),
            eq(NEW_ORDER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("693d7ad9-e671-4d66-9911-7f75f6380134", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCreateOrderHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("POST"),
            eq(NEW_ORDER),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                " When it comes to the safety of these people, there's me and "
                    + "then there's God, understand?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCreateOrderHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("POST"),
            eq(NEW_ORDER),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                " 我们都看到了我们想看到的。科菲看了看，他看到了俄罗斯人。他看到了仇恨”\n" +
                        "                    +“和恐惧。你必须用比那更好的眼睛看"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Cancel Order tests
  // 取消订单测试
  // --------------------------------------------------------------------------

  @Test
  public void testCancelOrderIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("DELETE"),
            eq(CANCEL_ORDER),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    // 取消此交易所的订单不需要 marketId arg。
    final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    assertTrue(success);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCancelOrderHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("DELETE"),
            eq(CANCEL_ORDER),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "我们不需要它们。我们不能相信他们。我们可能不得不采取措施。”\n" +
                        "                    + \" 我们将不得不采取措施。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    // 取消此交易所的订单不需要 marketId arg。
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("DELETE"),
            eq(CANCEL_ORDER),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "Fluid breathing system, we just got it. You use it when you go really deep.流体呼吸系统，我们刚刚得到它。当你真正深入时使用它。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    // 取消此交易所的订单不需要 marketId arg。
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Your Open Orders tests
  // 获取您的未结订单测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingYourOpenOrdersSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDERS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(ORDERS),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(2, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals("cdad7602-f290-41e5-a64d-42a1a20fd02", openOrders.get(0).getId());
    assertSame(OrderType.SELL, openOrders.get(0).getType());
    assertEquals(
        openOrders.get(0).getCreationDate(), Date.from(Instant.parse("2015-10-15T21:10:38.193Z")));
    assertEquals(0, openOrders.get(0).getPrice().compareTo(new BigDecimal("275.00000000")));
    assertEquals(
        0, openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.01000000")));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.00500000")));
    assertEquals(
        0,
        openOrders
            .get(0)
            .getTotal()
            .compareTo(
                openOrders.get(0).getPrice().multiply(openOrders.get(0).getOriginalQuantity())));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingYourOpenOrdersHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(ORDERS),
            eq(null))
        .andThrow(new ExchangeNetworkException("Bond. James Bond."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(ORDERS),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "All those moments will be lost in time... like tears in rain. 所有这些瞬间都会消失在时间里……就像雨中的泪水。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Market Orders tests
  // 获取市价单测试
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testGettingMarketOrders() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BOOK_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("level", ORDER_BOOK_DEPTH_LEVEL)).andStubReturn(null);

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(BOOK),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    // 断言一些关键的东西；我们不是在这里测试 GSON。
    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    final BigDecimal buyPrice = new BigDecimal("165.87");
    final BigDecimal buyQuantity = new BigDecimal("16.2373");
    final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

    assertEquals(50, marketOrderBook.getBuyOrders().size());
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal));

    final BigDecimal sellPrice = new BigDecimal("165.96");
    final BigDecimal sellQuantity = new BigDecimal("24.31");
    final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

    assertEquals(50, marketOrderBook.getSellOrders().size());
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(BOOK),
            anyObject(Map.class))
        .andThrow(new ExchangeNetworkException("Re-verify our range to target... one ping only."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(BOOK),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "大使先生，您有近百艘军舰在“\n" +
                        "                    + “现在是北大西洋。你的飞机已经掉落了足够多的声纳浮标”\n" +
                        "                    + “这样一个人就可以从格陵兰步行到冰岛再到苏格兰，而无需”\n" +
                        "                    +“弄湿他的脚。现在，我们应该放弃公牛吗？"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Latest Market Price tests
  // 获取最新的市场价格测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingLatestMarketPriceSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal latestMarketPrice =
        exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, RoundingMode.HALF_UP);
    assertEquals(0, latestMarketPrice.compareTo(new BigDecimal("14744.9")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), eq(null))
        .andThrow(
            new ExchangeNetworkException("I need your clothes, your boots and your motorcycle. 我需要你的衣服、靴子和摩托车。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), eq(null))
        .andThrow(new IllegalArgumentException("Come with me if you want to live. 如果你想活下去，跟我来。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Balance Info tests
  // 获取余额信息测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingBalanceInfoSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ACCOUNTS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(ACCOUNTS),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

    // assert some key stuff; we're not testing GSON here.
    // 断言一些关键的东西；我们不是在这里测试 GSON。
    assertEquals(
        0,
        balanceInfo
            .getBalancesAvailable()
            .get("BTC")
            .compareTo(new BigDecimal("100.0000000000000004")));
    assertEquals(
        0,
        balanceInfo
            .getBalancesAvailable()
            .get("GBP")
            .compareTo(new BigDecimal("501.0100000000000001")));
    assertEquals(0, balanceInfo.getBalancesAvailable().get("EUR").compareTo(new BigDecimal("0")));

    assertEquals(
        0,
        balanceInfo
            .getBalancesOnHold()
            .get("BTC")
            .compareTo(new BigDecimal("100.0000000000000005")));
    assertEquals(
        0,
        balanceInfo
            .getBalancesOnHold()
            .get("GBP")
            .compareTo(new BigDecimal("499.9900000000000002")));
    assertEquals(0, balanceInfo.getBalancesOnHold().get("EUR").compareTo(new BigDecimal("0")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(ACCOUNTS),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "对于你想做的任何事情来说，三点钟总是太晚或太早。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(ACCOUNTS),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "话多有时间，睡觉也有时间。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Ticker tests
  // 获取 Ticker 测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingTickerSuccessfully() throws Exception {
    final byte[] encodedTicker = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse tickerExchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encodedTicker, StandardCharsets.UTF_8));

    final byte[] encodedStats = Files.readAllBytes(Paths.get(STATS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse statsExchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encodedStats, StandardCharsets.UTF_8));

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), eq(null))
        .andReturn(tickerExchangeResponse);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(STATS), eq(null))
        .andReturn(statsExchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);

    assertEquals(0, ticker.getLast().compareTo(new BigDecimal("14744.9")));
    assertEquals(0, ticker.getAsk().compareTo(new BigDecimal("14744.81")));
    assertEquals(0, ticker.getBid().compareTo(new BigDecimal("14744.8")));
    assertEquals(0, ticker.getHigh().compareTo(new BigDecimal("14899.00000000")));
    assertEquals(0, ticker.getLow().compareTo(new BigDecimal("13409.97000000")));
    assertEquals(0, ticker.getOpen().compareTo(new BigDecimal("13609.53000000")));
    assertEquals(0, ticker.getVolume().compareTo(new BigDecimal("607.54445656")));
    assertNull(ticker.getVwap()); // not provided by COINBASE PRO
    assertEquals(1508008776604L, (long) ticker.getTimestamp());

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingTickerHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "听着，麦克先生，我不知道你习惯与什么样的人打交道，”\n" +
                        "                    +“但是没有人告诉我该做什么。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesUnexpectedException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), eq(null))
        .andThrow(
            new IllegalArgumentException(
                "印第安纳琼斯。我一直都知道有一天你会来”\n" +
                        "                    +“从我的门走回来。我从不怀疑这一点。有些东西成功了”\n" +
                        "                    +“不可避免。那么，你在尼泊尔做什么？"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Non Exchange visiting tests
  // 非交易所访问测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingExchangeSellingFeeIsAsExpected() {
    PowerMock.replayAll();
    final CoinbaseProExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal sellPercentageFee =
        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, sellPercentageFee.compareTo(new BigDecimal("0.0025")));
    PowerMock.verifyAll();
  }

  @Test
  public void testGettingExchangeBuyingFeeIsAsExpected() {
    PowerMock.replayAll();
    final CoinbaseProExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(new BigDecimal("0.0025")));
    PowerMock.verifyAll();
  }

  @Test
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();
    final CoinbaseProExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    assertEquals("COINBASE PRO REST API v1", exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Initialisation tests
  // --------------------------------------------------------------------------

  @Test
  public void testExchangeAdapterInitialisesSuccessfully() {
    PowerMock.replayAll();

    final CoinbaseProExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertNotNull(exchangeAdapter);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfPassphraseConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("passphrase")).andReturn(null);
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("passphrase")).andReturn("your_passphrase");
    expect(authenticationConfig.getItem("key")).andReturn(null);
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("passphrase")).andReturn("your_passphrase");
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn(null);
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfBuyFeeIsMissing() {
    PowerMock.reset(otherConfig);
    expect(otherConfig.getItem("buy-fee")).andReturn("");
    expect(otherConfig.getItem("sell-fee")).andReturn("0.25");
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfSellFeeIsMissing() {
    PowerMock.reset(otherConfig);
    expect(otherConfig.getItem("buy-fee")).andReturn("0.25");
    expect(otherConfig.getItem("sell-fee")).andReturn("");

    PowerMock.replayAll();
    final ExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() {
    PowerMock.reset(networkConfig);
    expect(networkConfig.getConnectionTimeout()).andReturn(0);
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new CoinbaseProExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Request sending tests
  // 请求发送测试
  /**
    "The rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down,
    so suddenly that Alice had not a moment to think about stopping herself before she found
     herself falling down what seemed to be a very deep well...
   “兔子洞像隧道一样笔直向前走了一段，然后突然下降，
   来得太突然，爱丽丝来不及想停下来就发现
   自己掉进了似乎很深的井里……"
   */
//  --------------------------------------------------------------------------

  @Test
  public void testSendingPublicRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + TICKER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal lastMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    assertEquals(0, lastMarketPrice.compareTo(new BigDecimal("14744.9")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testSendingPublicRequestToExchangeHandlesExchangeNetworkException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + TICKER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(
            new ExchangeNetworkException("One wrong note eventually ruins the entire symphony.一个错误的音符最终会毁掉整部交响曲。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testSendingPublicRequestToExchangeHandlesTradingApiException() throws Exception {
    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + TICKER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(new TradingApiException("Look on my works, ye Mighty, and despair.看看我的作品，你这强大的，绝望的。"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(NEW_SELL_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = new HashMap<>();
    requestParamMap.put(
        "size",
        new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY));
    requestParamMap.put(
        "price", new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE));
    requestParamMap.put("side", "sell");
    requestParamMap.put("product_id", MARKET_ID);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-KEY"), eq(KEY))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-SIGN"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-TIMESTAMP"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-PASSPHRASE"), eq(PASSPHRASE))).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);

    final URL url = new URL(AUTHENTICATED_API_URL + NEW_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            eq(new GsonBuilder().create().toJson(requestParamMap)),
            eq(requestHeaderMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("693d7ad9-e671-4d66-9911-7f75f6380134", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesExchangeNetworkException()
      throws Exception {
    final Map<String, String> requestParamMap = new HashMap<>();
    requestParamMap.put(
        "size",
        new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY));
    requestParamMap.put(
        "price", new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE));
    requestParamMap.put("side", "sell");
    requestParamMap.put("product_id", MARKET_ID);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-KEY"), eq(KEY))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-SIGN"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-TIMESTAMP"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-PASSPHRASE"), eq(PASSPHRASE))).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);

    final URL url = new URL(AUTHENTICATED_API_URL + NEW_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            eq(new GsonBuilder().create().toJson(requestParamMap)),
            eq(requestHeaderMap))
        .andThrow(
            new ExchangeNetworkException(
                "Allow me then a moment to consider. You seek your creator. "
                    + "I am looking at mine. I will serve you, yet you're human. "
                    + "You will die, I will not."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesTradingApiException()
      throws Exception {
    final Map<String, String> requestParamMap = new HashMap<>();
    requestParamMap.put(
        "size",
        new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY));
    requestParamMap.put(
        "price", new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE));
    requestParamMap.put("side", "sell");
    requestParamMap.put("product_id", MARKET_ID);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-KEY"), eq(KEY))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-SIGN"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-TIMESTAMP"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("CB-ACCESS-PASSPHRASE"), eq(PASSPHRASE))).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final CoinbaseProExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            CoinbaseProExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);

    final URL url = new URL(AUTHENTICATED_API_URL + NEW_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            eq(new GsonBuilder().create().toJson(requestParamMap)),
            eq(requestHeaderMap))
        .andThrow(new TradingApiException("When you close your eyes do you dream of me?当你闭上眼睛时，你会梦见我吗？"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }
}
