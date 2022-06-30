/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
 * Copyright (c) 2021 maiph
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

import static java.util.Collections.emptyMap;
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
import com.gazbert.bxbot.exchanges.AbstractExchangeAdapter.ExchangeHttpResponse;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.Ticker;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
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
 * Tests the behaviour of the Kraken Exchange Adapter.
 * 测试 Kraken 交换适配器的行为。
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
@PrepareForTest(KrakenExchangeAdapter.class)
public class TestKrakenExchangeAdapter extends AbstractExchangeAdapterTest {

  private static final String ASSET_PAIR_JSON_RESPONSE
      = "./src/test/exchange-data/kraken/AssetPairs.json";
  private static final String DEPTH_JSON_RESPONSE = "./src/test/exchange-data/kraken/Depth.json";
  private static final String DEPTH_ERROR_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/Depth-error.json";
  private static final String BALANCE_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/Balance.json";
  private static final String BALANCE_ERROR_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/Balance-error.json";
  private static final String TICKER_JSON_RESPONSE = "./src/test/exchange-data/kraken/Ticker.json";
  private static final String TICKER_ERROR_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/Ticker-error.json";
  private static final String OPEN_ORDERS_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/OpenOrders.json";
  private static final String OPEN_ORDERS_ERROR_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/OpenOrders-error.json";
  private static final String ADD_ORDER_BUY_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/AddOrder-buy.json";
  private static final String ADD_ORDER_SELL_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/AddOrder-sell.json";
  private static final String ADD_ORDER_ERROR_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/AddOrder-error.json";
  private static final String CANCEL_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/CancelOrder.json";
  private static final String CANCEL_ORDER_ERROR_JSON_RESPONSE =
      "./src/test/exchange-data/kraken/CancelOrder-error.json";

  private static final String DEPTH = "Depth";
  private static final String BALANCE = "Balance";
  private static final String TICKER = "Ticker";
  private static final String ASSET_PAIRS = "AssetPairs";
  private static final String OPEN_ORDERS = "OpenOrders";
  private static final String ADD_ORDER = "AddOrder";
  private static final String CANCEL_ORDER = "CancelOrder";

  // Market id must be the same as the Asset Pair id. See:
  // 市场 id 必须与资产对 id 相同。看：
  // https://www.kraken.com/help/api#get-tradable-pairs
  private static final String MARKET_ID = "XBTUSD";
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("456.41");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.001");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("758.17");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.001");
  private static final String ORDER_ID_TO_CANCEL = "OLD2Z4-L4C7H-MKH5BW";

  private static final String MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD = "createRequestParamMap";
  private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD =
      "sendAuthenticatedRequestToExchange";
  private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD =
      "sendPublicRequestToExchange";
  private static final String MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD = "createHeaderParamMap";
  private static final String MOCKED_MAKE_NETWORK_REQUEST_METHOD = "makeNetworkRequest";

  private static final String KEY = "key123";
  private static final String SECRET = "notGonnaTellYa";
  private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
  private static final List<String> nonFatalNetworkErrorMessages =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake");

  private static final String KRAKEN_BASE_URI = "https://api.kraken.com/";
  private static final String KRAKEN_API_VERSION = "0";
  private static final String KRAKEN_PUBLIC_PATH = "/public/";
  private static final String KRAKEN_PRIVATE_PATH = "/private/";
  private static final String PUBLIC_API_BASE_URL =
      KRAKEN_BASE_URI + KRAKEN_API_VERSION + KRAKEN_PUBLIC_PATH;
  private static final String AUTHENTICATED_API_URL =
      KRAKEN_BASE_URI + KRAKEN_API_VERSION + KRAKEN_PRIVATE_PATH;

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;

  /**
   * Create some exchange config - the TradingEngine would normally do this.
   * * 创建一些交换配置 - TradingEngine 通常会这样做。
   */
  @Before
  public void setupForEachTest() {
    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("key")).andReturn(KEY);
    expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

    networkConfig = PowerMock.createMock(NetworkConfig.class);
    expect(networkConfig.getConnectionTimeout()).andReturn(30);
    expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
    expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

    OtherConfig otherConfig = PowerMock.createMock(OtherConfig.class);
    expect(otherConfig.getItem("buy-fee")).andReturn("0.1");
    expect(otherConfig.getItem("sell-fee")).andReturn("0.2");
    expect(otherConfig.getItem("keep-alive-during-maintenance")).andReturn("false");

    exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
    expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
    expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
    expect(exchangeConfig.getOtherConfig()).andReturn(otherConfig);
  }

  // --------------------------------------------------------------------------
  //  Get Market Orders tests
  // 获取市价单测试
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testGettingMarketOrdersSuccessfully() throws Exception {
    // Load the canned response from the exchange
    final byte[] encoded = Files.readAllBytes(Paths.get(DEPTH_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
    // 模拟出参数映射，因此我们可以断言传递给传输层的内容是我们所期望的。
    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);

    // Partial mock so we do not send stuff down the wire
    // 部分模拟，所以我们不会通过网络发送东西
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(DEPTH),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    // 断言一些关键的东西；我们不是在这里测试 GSON。
    // assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));

    final BigDecimal buyPrice = new BigDecimal("662.55000");
    final BigDecimal buyQuantity = new BigDecimal("5.851");
    final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

    assertEquals(100, marketOrderBook.getBuyOrders().size());
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal));

    final BigDecimal sellPrice = new BigDecimal("664.53600");
    final BigDecimal sellQuantity = new BigDecimal("0.888");
    final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

    assertEquals(100, marketOrderBook.getSellOrders().size());
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal));

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  @SuppressWarnings("unchecked")
  public void testGettingMarketOrdersHandlesErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(DEPTH_ERROR_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(DEPTH),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(DEPTH),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException("You're not giving orders. You're in my world now."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(DEPTH),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "Down time is the worst, isn’t it? "
                    + "Adrenaline leaves and the mind starts to wander..."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Your Open Orders tests
  // 获取您的未结订单测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingYourOpenOrdersSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(OPEN_ORDERS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    // 断言一些关键的东西；我们不是在这里测试 GSON。
    assertEquals(2, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals("OZUVVF-XEJUB-BOWOFO", openOrders.get(0).getId());
    assertSame(OrderType.SELL, openOrders.get(0).getType());
    assertEquals(1469653618L, openOrders.get(0).getCreationDate().getTime());
    assertEquals(0, openOrders.get(0).getPrice().compareTo(new BigDecimal("699.100")));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.006")));
    assertEquals(
        0,
        openOrders
            .get(0)
            .getTotal()
            .compareTo(
                openOrders.get(0).getPrice().multiply(openOrders.get(0).getOriginalQuantity())));
    assertEquals(
        0, openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.01000000")));

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingYourOpenOrdersExchangeErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(OPEN_ORDERS_ERROR_JSON_RESPONSE));
    final KrakenExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new KrakenExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS),
            anyObject(Map.class))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders("junk_market_id");
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingYourOpenOrdersHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "Yes... yes. This is a fertile land, & we will thrive. We will rule over all "
                    + "this land, & we will call it... This Land"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS),
            anyObject(Map.class))
        .andThrow(new IllegalStateException("Ah! Curse your sudden but inevitable betrayal!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Create Orders tests
  // 创建订单测试
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToBuyIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ADD_ORDER_BUY_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("type", "buy")).andStubReturn(null);
    expect(requestParamMap.put("ordertype", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.#", getDecimalFormatSymbols()).format(BUY_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "volume",
                new DecimalFormat("#.###", getDecimalFormatSymbols())
                    .format(BUY_ORDER_QUANTITY)))
        .andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ADD_ORDER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    assertEquals("OLD2Z4-L4C9H-MKH5BX", orderId);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToSellIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ADD_ORDER_SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("ordertype", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.#", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "volume",
                new DecimalFormat("#.###", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ADD_ORDER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("OLD2Z4-L4C7H-MKH5BW", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCreateOrderExchangeErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ADD_ORDER_ERROR_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ADD_ORDER),
            anyObject(Map.class))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCreateOrderHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ADD_ORDER),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "This is your last chance. After this, there is no turning back. "
                    + "You take the blue pill - the story ends, you wake up in your bed and "
                    + "believe whatever you want to believe. You take the red pill - you stay "
                    + "in Wonderland and I show you how deep the rabbit-hole goes"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCreateOrderHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ADD_ORDER),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "Have you ever had a dream, Neo, that you were so sure was real? "
                    + "What if you were unable to wake from that dream? How would you know the "
                    + "difference between the dream world and the real world?"));

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
  @SuppressWarnings("unchecked")
  public void testCancelOrderIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("txid", ORDER_ID_TO_CANCEL)).andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(CANCEL_ORDER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, MARKET_ID);
    assertTrue(success);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderExchangeErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_ERROR_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(CANCEL_ORDER),
            anyObject(Map.class))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCancelOrderHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(CANCEL_ORDER),
            anyObject(Map.class))
        .andThrow(new ExchangeNetworkException("Never send a human to do a machine's job."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(CANCEL_ORDER),
            anyObject(Map.class))
        .andThrow(
            new IllegalStateException(
                "You know, I know this steak doesn't exist."
                    + "I know that when I put it in my mouth, the Matrix is telling my brain "
                    + "that it is juicy and delicious. After nine years, you know what I "
                    + "realize? Ignorance is bliss."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Balance Info tests
  // 获取余额信息测试
  // --------------------------------------------------------------------------

  @Test
  public void testGettingBalanceInfoSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BALANCE_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

    // assert some key stuff; we're not testing GSON here.
    // 断言一些关键的东西；我们不是在这里测试 GSON。
    assertEquals(
        0,
        balanceInfo.getBalancesAvailable().get("XXBT").compareTo(new BigDecimal("1.1000000000")));
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("ZUSD").compareTo(new BigDecimal("1000.12")));

    // Kraken does not provide on-hold balances
    assertNull(balanceInfo.getBalancesOnHold().get("XXBT"));
    assertNull(balanceInfo.getBalancesOnHold().get("ZUSD"));

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingBalanceInfoHandlesExchangeErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BALANCE_ERROR_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    // Partial mock so we do not send stuff down the wire
    // 部分模拟，所以我们不会通过网络发送东西
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            anyObject(Map.class))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "All the gods, all the heavens, all the hells, are within you."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(new IllegalStateException("Are those friendlies? I hope they're friendlies..."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Latest Market Price tests
  // 获取最新的市场价格测试
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testGettingLatestMarketPriceSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal latestMarketPrice =
        exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, RoundingMode.HALF_UP);
    assertEquals(0, latestMarketPrice.compareTo(new BigDecimal("657.99900")));
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingLatestMarketPriceHandlesExchangeErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_ERROR_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            anyObject(Map.class))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "As long as you can still grab a breath, you fight. You breathe. "
                    + "Keep breathing. When there is a storm and you stand in front of a tree, "
                    + "if you look at its branches, you swear it will fall. But if you watch the "
                    + "trunk, you will see its stability."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "Yes, you have information. You can find out all about a man, "
                    + "track him down, keep an eye on him. But you have to look him in the eye. "
                    + "All the tech you have can't help you with that. A license to kill also "
                    + "means a license NOT to kill."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Latest Market Price tests
  // 获取最新的市场价格测试
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testGettingTickerSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);
    assertEquals(0, ticker.getLast().compareTo(new BigDecimal("657.99900")));
    assertEquals(0, ticker.getAsk().compareTo(new BigDecimal("657.99900")));
    assertEquals(0, ticker.getBid().compareTo(new BigDecimal("655.20100")));
    assertEquals(0, ticker.getHigh().compareTo(new BigDecimal("659.13000")));
    assertEquals(0, ticker.getLow().compareTo(new BigDecimal("642.50000")));
    assertEquals(0, ticker.getOpen().compareTo(new BigDecimal("651.73600")));
    assertEquals(0, ticker.getVolume().compareTo(new BigDecimal("1152.99666422")));
    assertEquals(0, ticker.getVwap().compareTo(new BigDecimal("652.64807")));
    assertNull(ticker.getTimestamp()); // timestamp not supplied by Kraken  // Kraken 不提供时间戳

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesExchangeErrorResponse() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_ERROR_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            anyObject(Map.class))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingTickerHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            anyObject(Map.class))
        .andThrow(new ExchangeNetworkException("KHAAANNN!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesUnexpectedException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    mockAssetPairsPublicRequest(exchangeAdapter);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(TICKER),
            anyObject(Map.class))
        .andThrow(new IllegalArgumentException("Resistance is futile."));

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
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();
    final ExchangeAdapter exchangeAdapter = new KrakenExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertEquals("Kraken API v1", exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }

  @Test
  public void testGettingExchangeSellingFeeIsAsExpected() throws Exception {
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new KrakenExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal sellPercentageFee =
        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, sellPercentageFee.compareTo(new BigDecimal("0.002")));

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingExchangeBuyingFeeIsAsExpected() throws Exception {
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new KrakenExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(new BigDecimal("0.001")));

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Initialisation tests
  // 初始化测试
  // --------------------------------------------------------------------------

  @Test
  public void testExchangeAdapterInitialisesSuccessfully() {
    PowerMock.replayAll();
    final ExchangeAdapter exchangeAdapter = new KrakenExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertNotNull(exchangeAdapter);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("key")).andReturn(null);
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    new KrakenExchangeAdapter().init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn(null);
    PowerMock.replayAll();

    new KrakenExchangeAdapter().init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() {
    PowerMock.reset(networkConfig);
    expect(networkConfig.getConnectionTimeout()).andReturn(0);
    PowerMock.replayAll();

    new KrakenExchangeAdapter().init(exchangeConfig);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Request sending tests
  // 请求发送测试
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testSendingPublicRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    mockAssetPairsNetworkRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

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
    assertEquals(0, lastMarketPrice.compareTo(new BigDecimal("657.99900")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  @SuppressWarnings("unchecked")
  public void testSendingPublicRequestToExchangeHandlesExchangeNetworkException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    mockAssetPairsNetworkRequest(exchangeAdapter);

    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(PUBLIC_API_BASE_URL + TICKER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(
            new ExchangeNetworkException(
                "And from his own flesh Hades gave birth to an"
                    + " unspeakable horror... the Kraken."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  @SuppressWarnings("unchecked")
  public void testSendingPublicRequestToExchangeHandlesTradingApiException() throws Exception {
    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    mockAssetPairsNetworkRequest(exchangeAdapter);

    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(PUBLIC_API_BASE_URL + TICKER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(
            new TradingApiException(
                "In 10 days, when the sun is eclipsed, I will unleash the Kraken. "
                    + "Argos will be swept from the earth and all of you with it. Unless you "
                    + "sacrifice the princess you so foolish compare to the gods. Only her blood "
                    + "will sate the Kraken and Zeus, who you have so offended. Choose your "
                    + "penance, Argos. Destruction of sacrifice."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ADD_ORDER_SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("ordertype", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.#", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "volume",
                new DecimalFormat("#.###", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/x-www-form-urlencoded"))
        .andStubReturn(null);
    expect(requestHeaderMap.put(eq("API-Key"), eq(KEY))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("API-Sign"), anyString())).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early  // map需要尽早上线

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    mockAssetPairsNetworkRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(emptyMap());
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(AUTHENTICATED_API_URL + ADD_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("OLD2Z4-L4C7H-MKH5BW", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesExchangeNetworkException()
      throws Exception {
    final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("ordertype", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.#", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "volume",
                new DecimalFormat("#.###", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/x-www-form-urlencoded"))
        .andStubReturn(null);
    expect(requestHeaderMap.put(eq("API-Key"), eq(KEY))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("API-Sign"), anyString())).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    mockAssetPairsNetworkRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(emptyMap());
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(AUTHENTICATED_API_URL + ADD_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(new ExchangeNetworkException("How may a mortal man face and defeat the Kraken?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesTradingApiException()
      throws Exception {
    final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("pair", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("ordertype", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.#", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "volume",
                new DecimalFormat("#.###", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/x-www-form-urlencoded"))
        .andStubReturn(null);
    expect(requestHeaderMap.put(eq("API-Key"), eq(KEY))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("API-Sign"), anyString())).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early  map需要尽早上场

    final KrakenExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            KrakenExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    mockAssetPairsNetworkRequest(exchangeAdapter);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(emptyMap());
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(AUTHENTICATED_API_URL + ADD_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(
            new TradingApiException(
                "Below the thunders of the upper deep;\n"
                    + "Far far beneath in the abysmal sea,\n"
                    + "His ancient, dreamless, uninvaded sleep\n"
                    + "The Kraken sleepeth: faintest sunlights flee\n"
                    + "About his shadowy sides; above him swell..."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }

  private void mockAssetPairsPublicRequest(Object exchangeAdapter) throws Exception {
    final ExchangeHttpResponse assetsResponse = createMockAssetPairs();
    PowerMock.expectPrivate(
        exchangeAdapter,
        MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
        eq(ASSET_PAIRS),
        anyObject()
    ).andReturn(assetsResponse);
  }

  private void mockAssetPairsNetworkRequest(Object exchangeAdapter) throws Exception {
    final ExchangeHttpResponse assetsResponse = createMockAssetPairs();
    final URL url = new URL(PUBLIC_API_BASE_URL + ASSET_PAIRS);
    PowerMock.expectPrivate(
        exchangeAdapter,
        MOCKED_MAKE_NETWORK_REQUEST_METHOD,
        eq(url),
        eq("GET"),
        anyObject(),
        anyObject()
    ).andReturn(assetsResponse);
  }

  private ExchangeHttpResponse createMockAssetPairs() throws IOException {
    final byte[] assetsMsg = Files.readAllBytes(Paths.get(
        TestKrakenExchangeAdapter.ASSET_PAIR_JSON_RESPONSE));

    return new ExchangeHttpResponse(200, "OK",
        new String(assetsMsg, StandardCharsets.UTF_8));
  }
}
