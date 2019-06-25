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

package com.gazbert.bxbot.exchanges;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
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
import org.powermock.reflect.Whitebox;

/**
 * Tests the behaviour of the itBit Exchange Adapter.
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
  "org.w3c.dom.*",
  "javax.xml.datatype.*"
})
@PrepareForTest(ItBitExchangeAdapter.class)
public class TestItBitExchangeAdapter extends AbstractExchangeAdapterTest {

  private static final String MARKET_ID = "XBTUSD";
  private static final String WALLET_ID = "62827e93-f19b-67bf-8d2f-663fa4f0f1ad";
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.01");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.0005");
  private static final String ORDER_ID_TO_CANCEL = "0be8d3d7-f710-4e1e-b0e7-91ca276b7e1a";

  private static final String WALLETS_JSON_RESPONSE = "./src/test/exchange-data/itbit/wallets.json";
  private static final String ORDER_BOOK_JSON_RESPONSE =
      "./src/test/exchange-data/itbit/order_book.json";
  private static final String OPEN_ORDERS_JSON_RESPONSE =
      "./src/test/exchange-data/itbit/orders.json";
  private static final String TICKER_JSON_RESPONSE = "./src/test/exchange-data/itbit/ticker.json";
  private static final String NEW_ORDER_BUY_JSON_RESPONSE =
      "./src/test/exchange-data/itbit/new_order_buy.json";
  private static final String NEW_ORDER_SELL_JSON_RESPONSE =
      "./src/test/exchange-data/itbit/new_order_sell.json";
  private static final String CANCEL_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/itbit/cancel_order.json";

  private static final String WALLETS = "wallets";
  private static final String ORDER_BOOK = "markets/" + MARKET_ID + "/order_book";
  private static final String OPEN_ORDERS = "wallets/" + WALLET_ID + "/orders";
  private static final String TICKER = "markets/" + MARKET_ID + "/ticker";
  private static final String NEW_ORDER =
      "wallets/" + WALLET_ID + "/orders"; // same as ORDERS but uses POST
  private static final String CANCEL_ORDER =
      "wallets/" + WALLET_ID + "/orders/" + ORDER_ID_TO_CANCEL;

  private static final String MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD = "createRequestParamMap";
  private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD =
      "sendAuthenticatedRequestToExchange";
  private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD =
      "sendPublicRequestToExchange";
  private static final String MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD = "createHeaderParamMap";
  private static final String MOCKED_MAKE_NETWORK_REQUEST_METHOD = "makeNetworkRequest";
  private static final String MOCKED_GET_BALANCE_INFO_METHOD = "getBalanceInfo";

  private static final String MOCKED_WALLET_ID_FIELD_NAME = "walletId";

  private static final String USERID = "userId123";
  private static final String KEY = "key123";
  private static final String SECRET = "notGonnaTellYa";
  private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
  private static final List<String> nonFatalNetworkErrorMessages =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake");

  private static final String ITBIT_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://api.itbit.com/" + ITBIT_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;
  private OtherConfig otherConfig;

  /** Create some exchange config - the TradingEngine would normally do this. */
  @Before
  public void setupForEachTest() {
    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("userId")).andReturn(USERID);
    expect(authenticationConfig.getItem("key")).andReturn(KEY);
    expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

    networkConfig = PowerMock.createMock(NetworkConfig.class);
    expect(networkConfig.getConnectionTimeout()).andReturn(30);
    expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
    expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

    otherConfig = PowerMock.createMock(OtherConfig.class);
    expect(otherConfig.getItem("buy-fee")).andReturn("0.5");
    expect(otherConfig.getItem("sell-fee")).andReturn("0.5");
    expect(otherConfig.getItem("keep-alive-during-maintenance")).andReturn("false");

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
    final byte[] encoded = Files.readAllBytes(Paths.get(NEW_ORDER_BUY_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            201, "Created", new String(encoded, StandardCharsets.UTF_8));

    // Mock out param map so we can assert the contents passed to the transport layer are what we
    // expect.
    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("type", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.####", getDecimalFormatSymbols()).format(BUY_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(BUY_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
    expect(requestParamMap.put("side", "buy")).andStubReturn(null);

    // Partial mock so we do not send stuff down the wire
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
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

    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    assertEquals("8a9ac32f-c2bd-4316-87d8-4219dc5e8041", orderId);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToSellIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(NEW_ORDER_SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            201, "Created", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("type", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.####", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
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

    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("8a7ac32f-c2bd-4316-87d8-4219dc5e8031", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCreateOrderHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("POST"),
            eq(NEW_ORDER),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                " If you want the ultimate,"
                    + " you've got to be willing to pay the ultimate price. It's not tragic to "
                    + "die doing what you love."));

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCreateOrderHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("POST"),
            eq(NEW_ORDER),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "Fear causes hesitation,"
                    + " and hesitation will cause your worst fears to come true."));

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Cancel Order tests
  // --------------------------------------------------------------------------

  @Test
  public void testCancelOrderIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            202, "Accepted", new String(encoded, StandardCharsets.UTF_8));

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("DELETE"),
            eq(CANCEL_ORDER),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    assertTrue(success);
    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCancelOrderHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("DELETE"),
            eq(CANCEL_ORDER),
            eq(null))
        .andThrow(new ExchangeNetworkException("Peace, through superior firepower!"));

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("DELETE"),
            eq(CANCEL_ORDER),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "It's basic dog psychology, if you scare them and get them peeing down"
                    + " their leg, they submit. But if you project weakness, that promotes "
                    + "violence, and that's how people get hurt."));

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Your Open Orders tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testGettingYourOpenOrdersSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(OPEN_ORDERS_JSON_RESPONSE));

    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("status", "open")).andStubReturn(null);

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(OPEN_ORDERS),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();

    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.init(exchangeConfig);

    final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(2, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals("639ccf95-b87c-48ba-b27d-7bc09b841b81", openOrders.get(0).getId());
    assertSame(OrderType.SELL, openOrders.get(0).getType());
    assertEquals(
        openOrders.get(0).getCreationDate(),
        Date.from(Instant.parse("2015-10-01T18:11:06.8470000Z")));
    assertEquals(0, openOrders.get(0).getPrice().compareTo(new BigDecimal("255.59000000")));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.01500000")));
    assertEquals(
        0, openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.01500000")));
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
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(OPEN_ORDERS),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "I'm afraid. I'm afraid, Dave. Dave, my mind is "
                    + "going. I can feel it. I can feel it. My mind is going. There is no "
                    + "question about it. I can feel it. I can feel it. I can feel it. "
                    + "I'm a... fraid. Good afternoon, gentlemen. I am a HAL 9000 computer. "
                    + "I became operational at the H.A.L. plant in Urbana, Illinois on the 12th "
                    + "of January 1992. My instructor was Mr. Langley, and he taught me to sing "
                    + "a song. If you'd like to hear it I can sing it for you."));

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(OPEN_ORDERS),
            anyObject(Map.class))
        .andThrow(new IllegalStateException("Hello, HAL. Do you read me, HAL?"));

    PowerMock.replayAll();
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Market Orders tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingMarketOrdersSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_BOOK_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, ORDER_BOOK)
        .andReturn(exchangeResponse);

    PowerMock.replayAll();

    exchangeAdapter.init(exchangeConfig);
    final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    final BigDecimal buyPrice = new BigDecimal("236.73");
    final BigDecimal buyQuantity = new BigDecimal("0.03");
    final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

    assertEquals(159, marketOrderBook.getBuyOrders().size()); // itBit sends them all back!
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal));

    final BigDecimal sellPrice = new BigDecimal("236.84");
    final BigDecimal sellQuantity = new BigDecimal("6.74");
    final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

    assertEquals(143, marketOrderBook.getSellOrders().size()); // itBit sends them all back!
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, ORDER_BOOK)
        .andThrow(
            new ExchangeNetworkException(
                "There is an idea of a Patrick Bateman; some kind of "
                    + "abstraction. But there is no real me: only an entity, something illusory. "
                    + "And though I can hide my cold gaze, and you can shake my hand and feel "
                    + "flesh gripping yours and maybe you can even sense our lifestyles are "
                    + "probably comparable... I simply am not there."));

    PowerMock.replayAll();
    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, ORDER_BOOK)
        .andThrow(new IllegalArgumentException("I have to return some videotapes"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);
    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Latest Market Price tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingLatestMarketPriceSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER)
        .andReturn(exchangeResponse);

    PowerMock.replayAll();

    exchangeAdapter.init(exchangeConfig);
    final BigDecimal latestMarketPrice =
        exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, RoundingMode.HALF_UP);
    assertEquals(0, latestMarketPrice.compareTo(new BigDecimal("237.70000000")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER)
        .andThrow(
            new ExchangeNetworkException(
                " I've seen horrors... horrors that you've seen."
                    + " But you have no right to call me a murderer. You have a right to kill me. "
                    + "You have a right to do that... but you have no right to judge me. It's "
                    + "impossible for words to describe what is necessary to those who do not "
                    + "know what horror means. Horror... Horror has a face... and you must make a "
                    + "friend of horror. Horror and moral terror are your friends. "
                    + "If they are not, then they are enemies to be feared."));

    PowerMock.replayAll();
    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER)
        .andThrow(new IllegalArgumentException("The horror... the horror..."));

    PowerMock.replayAll();
    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Balance Info tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testGettingBalanceInfoSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(WALLETS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "Ok", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put(eq("userId"), anyString())).andStubReturn(null);

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(WALLETS),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();

    exchangeAdapter.init(exchangeConfig);
    final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

    // assert some key stuff; we're not testing GSON here.
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("XBT").compareTo(new BigDecimal("1.50000000")));
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("USD").compareTo(new BigDecimal("1000.9900000")));

    // itBot does not provide "balances on hold" info.
    assertNull(balanceInfo.getBalancesOnHold().get("BTC"));
    assertNull(balanceInfo.getBalancesOnHold().get("USD"));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(WALLETS),
            anyObject(Map.class))
        .andThrow(new ExchangeNetworkException("You were in a 4g inverted dive with a MiG28?"));

    PowerMock.replayAll();
    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq("GET"),
            eq(WALLETS),
            anyObject(Map.class))
        .andThrow(
            new IllegalStateException(
                "Tower, this is Ghost Rider requesting a flyby... "
                    + "Negative, Ghost Rider, the pattern is full... BOOM!!!"));

    PowerMock.replayAll();
    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Ticker tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingTickerSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER)
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);
    assertEquals(0, ticker.getLast().compareTo(new BigDecimal("237.70000000")));
    assertEquals(0, ticker.getAsk().compareTo(new BigDecimal("237.84")));
    assertEquals(0, ticker.getBid().compareTo(new BigDecimal("237.69")));
    assertEquals(0, ticker.getHigh().compareTo(new BigDecimal("240.75000000")));
    assertEquals(0, ticker.getLow().compareTo(new BigDecimal("236.60000000")));
    assertEquals(0, ticker.getOpen().compareTo(new BigDecimal("239.43000000")));
    assertEquals(0, ticker.getVolume().compareTo(new BigDecimal("13053.72170000")));
    assertEquals(0, ticker.getVwap().compareTo(new BigDecimal("238.79044524")));
    assertEquals(1443557593032L, (long) ticker.getTimestamp());

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingTickerHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER)
        .andThrow(
            new ExchangeNetworkException(
                "She used to look at me... this way, like really look... "
                    + "and I just knew I was there... that I existed."));

    PowerMock.replayAll();
    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesUnexpectedException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMock(
            ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER)
        .andThrow(
            new IllegalArgumentException(
                "You listen to me. I've got 12,000 people in this town who"
                    + " are scared out of their mind. They've got one person to rely on. "
                    + "It used to be someone else, but now it's just me."));

    PowerMock.replayAll();
    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Non Exchange visiting tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingExchangeSellingFeeIsAsExpected() {
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal sellPercentageFee =
        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, sellPercentageFee.compareTo(new BigDecimal("0.005")));

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingExchangeBuyingFeeIsAsExpected() {
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(new BigDecimal("0.005")));

    PowerMock.verifyAll();
  }

  @Test
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    assertEquals("itBit REST API v1", exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Initialisation tests
  // --------------------------------------------------------------------------

  @Test
  public void testExchangeAdapterInitialisesSuccessfully() {
    PowerMock.replayAll();
    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertNotNull(exchangeAdapter);
    PowerMock.verify();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfUserIdConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("userId")).andReturn(null);
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verify();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfClientKeyConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("userId")).andReturn("your-user-id");
    expect(authenticationConfig.getItem("key")).andReturn(null);
    expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfClientSecretConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("userId")).andReturn("userId");
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn("");
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfBuyFeeIsMissing() {
    PowerMock.reset(otherConfig);
    expect(otherConfig.getItem("buy-fee")).andReturn("");
    expect(otherConfig.getItem("sell-fee")).andReturn("0.5");
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfSellFeeIsMissing() {
    PowerMock.reset(otherConfig);
    expect(otherConfig.getItem("buy-fee")).andReturn("0.5");
    expect(otherConfig.getItem("sell-fee")).andReturn(null);
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() {
    PowerMock.reset(networkConfig);
    expect(networkConfig.getConnectionTimeout()).andReturn(0);
    PowerMock.replayAll();

    final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Request sending tests
  // --------------------------------------------------------------------------

  @Test
  public void testSendingPublicRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

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
    assertEquals(0, lastMarketPrice.compareTo(new BigDecimal("237.70000000")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testSendingPublicRequestToExchangeHandlesExchangeNetworkException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + TICKER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(new ExchangeNetworkException("Release the Kraken!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testSendingPublicRequestToExchangeHandlesTradingApiException() throws Exception {
    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

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
                "One look from the head of Medusa can turn all creatures into stone."
                    + " No matter how huge and powerful. And her blood is a deadly venom."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(NEW_ORDER_SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            201, "Created", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("userId", USERID))
        .andStubReturn(null); // for precursor getBalanceInfo() call
    expect(requestParamMap.put("type", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.####", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    expect(requestHeaderMap.put(eq("Authorization"), startsWith(KEY + ":"))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-Auth-Timestamp"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-Auth-Nonce"), anyString())).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD,
            MOCKED_GET_BALANCE_INFO_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    // for precursor getBalanceInfo() call
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andStubReturn(balanceInfo);
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);

    final URL url = new URL(AUTHENTICATED_API_URL + NEW_ORDER);
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
    assertEquals("8a7ac32f-c2bd-4316-87d8-4219dc5e8031", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesExchangeNetworkException()
      throws Exception {
    final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("userId", USERID))
        .andStubReturn(null); // for precursor getBalanceInfo() call
    expect(requestParamMap.put("type", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.####", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    expect(requestHeaderMap.put(eq("Authorization"), startsWith(KEY + ":"))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-Auth-Timestamp"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-Auth-Nonce"), anyString())).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD,
            MOCKED_GET_BALANCE_INFO_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    // for precursor getBalanceInfo() call
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andStubReturn(balanceInfo);
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);

    final URL url = new URL(AUTHENTICATED_API_URL + NEW_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(
            new ExchangeNetworkException(
                "And a lie, Mr. Mulder, is most convincingly hidden between" + " two truths."));

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
    expect(requestParamMap.put("userId", USERID))
        .andStubReturn(null); // for precursor getBalanceInfo() call
    expect(requestParamMap.put("type", "limit")).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.####", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
    expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    expect(requestHeaderMap.put(eq("Authorization"), startsWith(KEY + ":"))).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-Auth-Timestamp"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-Auth-Nonce"), anyString())).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final ItBitExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            ItBitExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD,
            MOCKED_GET_BALANCE_INFO_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    // for precursor getBalanceInfo() call
    final BalanceInfo balanceInfo = PowerMock.createMock(BalanceInfo.class);
    expect(exchangeAdapter.getBalanceInfo()).andStubReturn(balanceInfo);
    Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);

    final URL url = new URL(AUTHENTICATED_API_URL + NEW_ORDER);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(new TradingApiException("Sorry, nobody down here but the FBI's most unwanted."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }
}
