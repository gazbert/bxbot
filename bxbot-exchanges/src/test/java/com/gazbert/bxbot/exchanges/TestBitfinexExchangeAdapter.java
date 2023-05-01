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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
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
 * Tests the behaviour of the Bitfinex Exchange Adapter.
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
@PrepareForTest(BitfinexExchangeAdapter.class)
public class TestBitfinexExchangeAdapter extends AbstractExchangeAdapterTest {

  private static final String BOOK_JSON_RESPONSE = "./src/test/exchange-data/bitfinex/book.json";
  private static final String ORDERS_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/orders.json";
  private static final String BALANCE_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/balances.json";
  private static final String PUB_TICKER_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/pubticker.json";
  private static final String ACCOUNT_INFOS_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/account_infos.json";
  private static final String ORDER_NEW_BUY_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/order_new_buy.json";
  private static final String ORDER_NEW_SELL_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/order_new_sell.json";
  private static final String ORDER_CANCEL_JSON_RESPONSE =
      "./src/test/exchange-data/bitfinex/order_cancel.json";

  private static final String BOOK = "book";
  private static final String ORDERS = "orders";
  private static final String BALANCES = "balances";
  private static final String PUB_TICKER = "pubticker";
  private static final String ACCOUNT_INFOS = "account_infos";
  private static final String ORDER_NEW = "order/new";
  private static final String ORDER_CANCEL = "order/cancel";

  private static final String MARKET_ID = "btcusd";
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final String ORDER_ID_TO_CANCEL = "426152651";

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

  private static final String BITFINEX_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://api.bitfinex.com/" + BITFINEX_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;

  /** Create some exchange config - the TradingEngine would normally do this. */
  @Before
  public void setupForEachTest() {
    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("key")).andReturn(KEY);
    expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

    networkConfig = PowerMock.createMock(NetworkConfig.class);
    expect(networkConfig.getConnectionTimeout()).andReturn(30);
    expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
    expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

    exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
    expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
    expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
    // optional config not needed for this adapter
  }

  // --------------------------------------------------------------------------
  //  Create Orders tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToBuyIsSuccessful() throws Exception {
    // Load the canned response from the exchange
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_NEW_BUY_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    // Mock out param map so we can assert the contents passed to the transport layer are what we
    // expect.
    final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("symbol", MARKET_ID)).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(BUY_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.########", getDecimalFormatSymbols()).format(BUY_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("exchange", "bitfinex")).andStubReturn(null);
    expect(requestParamMap.put("side", "buy")).andStubReturn(null);
    expect(requestParamMap.put("type", "exchange limit")).andStubReturn(null);

    // Partial mock so we do not send stuff down the wire
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_NEW),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    assertEquals("425116925", orderId);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToSellIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_NEW_SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("symbol", MARKET_ID)).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("exchange", "bitfinex")).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);
    expect(requestParamMap.put("type", "exchange limit")).andStubReturn(null);

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_NEW),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("425116929", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCreateOrderHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_NEW),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "Marion, don't look at it. Shut your eyes, Marion. Don't look at"
                    + " it, no matter what happens!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCreateOrderHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_NEW),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "What a fitting end to your life's pursuits. You're about to "
                    + "become a permanent addition to this archaeological find. Who knows? In a "
                    + "thousand years, even you may be worth something."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Cancel Order tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testCancelOrderIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_CANCEL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("order_id", Long.parseLong(ORDER_ID_TO_CANCEL))).andStubReturn(null);

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_CANCEL),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    assertTrue(success);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCancelOrderHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_CANCEL),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "Good morning. I am Meredith Vickers, and it is my job to"
                    + " make sure you do yours"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_CANCEL),
            anyObject(Map.class))
        .andThrow(
            new IllegalStateException(
                "The ring, it chose you. Take it... place the ring on the lantern..."
                    + " place the ring, speak the oath... great honor... responsibility"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Market Orders tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingMarketOrdersSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BOOK_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, BOOK + "/" + MARKET_ID)
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    final BigDecimal buyPrice = new BigDecimal("239.43");
    final BigDecimal buyQuantity = new BigDecimal("5.0");
    final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

    assertEquals(906, marketOrderBook.getBuyOrders().size()); // 'finex sends them all back!
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal));

    final BigDecimal sellPrice = new BigDecimal("239.53");
    final BigDecimal sellQuantity = new BigDecimal("6.35595596");
    final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

    assertEquals(984, marketOrderBook.getSellOrders().size()); // 'finex sends them all back!
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, BOOK + "/" + MARKET_ID)
        .andThrow(
            new ExchangeNetworkException(
                "There are three basic types, Mr. Pizer: the Wills, the Won'ts,"
                    + " and the Can'ts. The Wills accomplish everything, the Won'ts oppose "
                    + "everything, and the Can'ts won't try anything."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, BOOK + "/" + MARKET_ID)
        .andThrow(new IllegalArgumentException("Deckard. B26354"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Your Open Orders tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingYourOpenOrdersSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDERS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDERS),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(2, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals("423760243", openOrders.get(0).getId());
    assertSame(OrderType.SELL, openOrders.get(0).getType());
    assertEquals(1442073766, openOrders.get(0).getCreationDate().getTime());
    assertEquals(0, openOrders.get(0).getPrice().compareTo(new BigDecimal("259.38")));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.03")));
    assertEquals(0, openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.03")));
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
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDERS),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "There's an entirely different universe beyond that black hole. "
                    + "A point where time and space as we know it no longer exists. We will be the "
                    + "first to see it, to explore it, to experience it!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDERS),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "Nope, I can't make it! My main circuits are gone, my "
                    + "anti-grav-systems blown, and both backup systems are failing"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Latest Market Price tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingLatestMarketPriceSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(PUB_TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            PUB_TICKER + "/" + MARKET_ID)
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal latestMarketPrice =
        exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, RoundingMode.HALF_UP);
    assertEquals(0, latestMarketPrice.compareTo(new BigDecimal("236.07")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            PUB_TICKER + "/" + MARKET_ID)
        .andThrow(
            new ExchangeNetworkException(
                "They say most of your brain shuts down in cryo-sleep. "
                    + "All but the primitive side, the animal side. No wonder I'm still awake. "
                    + "Transporting me with civilians. Sounded like 40, 40-plus. Heard an Arab "
                    + "voice. Some hoodoo holy man, probably on his way to New Mecca. But what "
                    + "route? What route? I smelt a woman. Sweat, boots, tool belt, leather. "
                    + "Prospector type. Free settlers. And they only take the back roads. "
                    + "And here's my real problem. Mr. Johns... the blue-eyed devil. "
                    + "Planning on taking me back to slam... only this time he picked a ghost "
                    + "lane. A long time between stops. A long time for something to go wrong..."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            PUB_TICKER + "/" + MARKET_ID)
        .andThrow(
            new IllegalArgumentException(
                " All you people are so scared of me. Most days I'd take that as"
                    + " a compliment. But it ain't me you gotta worry about now"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Balance Info tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingBalanceInfoSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BALANCE_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCES),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

    // assert some key stuff; we're not testing GSON here.
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("0.1267283")));
    assertEquals(0, balanceInfo.getBalancesAvailable().get("USD").compareTo(new BigDecimal("0")));

    // Bitfinex does not provide "balances on hold" info.
    assertNull(balanceInfo.getBalancesOnHold().get("BTC"));
    assertNull(balanceInfo.getBalancesOnHold().get("LTC"));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCES),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                " Don't know, I don't know such stuff. I just do eyes, ju-, ju-,"
                    + " just eyes... just genetic design, just eyes. You Nexus, huh? I design your"
                    + " eyes"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCES),
            eq(null))
        .andThrow(
            new IllegalStateException(
                " I've seen things you people wouldn't believe. Attack ships on fire "
                    + "off the shoulder of Orion. I watched C-beams glitter in the dark near the "
                    + "Tannhauser gate. All those moments will be lost in time... like tears in "
                    + "rain... Time to die."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Exchange Fees for Buy orders tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingExchangeBuyingFeeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ACCOUNT_INFOS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ACCOUNT_INFOS),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(new BigDecimal("0.0020")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingExchangeBuyingFeeHandlesTimeoutException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ACCOUNT_INFOS),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "Right. Well, um, using layman's terms... Use a retaining magnetic "
                    + "field to focus a narrow beam of gravitons - these, in turn, fold "
                    + "space-time consistent with Weyl tensor dynamics until the space-time "
                    + "curvature becomes infinitely large, and you produce a singularity. "
                    + "Now, the singularity..."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingExchangeBuyingFeeHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ACCOUNT_INFOS),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "I created the Event Horizon to reach the stars, but she's gone much, "
                    + "much farther than that. She tore a hole in our universe, a gateway to "
                    + "another dimension. A dimension of pure chaos. Pure... evil. When she "
                    + "crossed over, she was just a ship. But when she came back... she was alive! "
                    + "Look at her, Miller. Isn't she beautiful?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Exchange Fees for Sell orders tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingExchangeSellingFeeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ACCOUNT_INFOS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ACCOUNT_INFOS),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(new BigDecimal("0.0020")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingExchangeSellingFeeHandlesTimeoutException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ACCOUNT_INFOS),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "Day 11, Test 37, Configuration 2.0. For lack of a better option, Dummy is still"
                    + " on fire safety."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingExchangeSellingFeeHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(ACCOUNT_INFOS),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "What was made public about the Event Horizon - that she was a deep space research"
                    + " vessel, that her reactor went critical, and that the ship blew up - "
                    + "none of that is true. The Event Horizon is the culmination of a secret "
                    + "government project to create a spacecraft capable of faster-than-light "
                    + "flight."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Ticker tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingTickerSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(PUB_TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            PUB_TICKER + "/" + MARKET_ID)
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);
    assertEquals(0, ticker.getLast().compareTo(new BigDecimal("236.07")));
    assertEquals(0, ticker.getAsk().compareTo(new BigDecimal("236.3")));
    assertEquals(0, ticker.getBid().compareTo(new BigDecimal("236.1")));
    assertEquals(0, ticker.getHigh().compareTo(new BigDecimal("241.59")));
    assertEquals(0, ticker.getLow().compareTo(new BigDecimal("235.51")));
    assertNull(ticker.getOpen()); // vwap not supplied by finex
    assertEquals(0, ticker.getVolume().compareTo(new BigDecimal("8002.20183869")));
    assertNull(ticker.getVwap()); // vwap not supplied by finex
    assertEquals(1442080762L, (long) ticker.getTimestamp());

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingTickerHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            PUB_TICKER + "/" + MARKET_ID)
        .andThrow(
            new ExchangeNetworkException(
                " You're born, you live and you die. There are no due overs, no second chances "
                    + "to make things right if you frak them up the first time, not in this "
                    + "life anyway."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesUnexpectedException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            PUB_TICKER + "/" + MARKET_ID)
        .andThrow(
            new IllegalArgumentException(
                "Like I said, you make your choices and you live with them and in the end you are"
                    + " those choices."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Non Exchange visiting tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();
    final BitfinexExchangeAdapter exchangeAdapter = new BitfinexExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertEquals("Bitfinex API v1", exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Initialisation tests
  // --------------------------------------------------------------------------

  @Test
  public void testExchangeAdapterInitialisesSuccessfully() {
    PowerMock.replayAll();
    final BitfinexExchangeAdapter exchangeAdapter = new BitfinexExchangeAdapter();
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

    new BitfinexExchangeAdapter().init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() {
    PowerMock.reset(authenticationConfig);
    expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
    expect(authenticationConfig.getItem("secret")).andReturn(null);
    PowerMock.replayAll();

    new BitfinexExchangeAdapter().init(exchangeConfig);
    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() {
    PowerMock.reset(networkConfig);
    expect(networkConfig.getConnectionTimeout()).andReturn(0);
    PowerMock.replayAll();

    new BitfinexExchangeAdapter().init(exchangeConfig);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Request sending tests
  // --------------------------------------------------------------------------

  @Test
  public void testSendingPublicRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(PUB_TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + PUB_TICKER + "/" + MARKET_ID);
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
    assertEquals(0, lastMarketPrice.compareTo(new BigDecimal("236.07")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testSendingPublicRequestToExchangeHandlesExchangeNetworkException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + PUB_TICKER + "/" + MARKET_ID);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(
            new ExchangeNetworkException(
                "There are three types of people in this world: "
                    + "sheep, wolves, and sheepdogs. Some people prefer to believe that evil "
                    + "doesn't exist in the world, and if it ever darkened their doorstep, they "
                    + "wouldn't know how to protect themselves. Those are the sheep. Then you've "
                    + "got predators who use violence to prey on the weak. "
                    + "They're the wolves. And then there are those blessed with the gift of "
                    + "aggression, an overpowering need to protect the flock. "
                    + "These men are the rare breed who live to confront the wolf. "
                    + "They are the sheepdog."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testSendingPublicRequestToExchangeHandlesTradingApiException() throws Exception {
    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    final URL url = new URL(PUBLIC_API_BASE_URL + PUB_TICKER + "/" + MARKET_ID);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(
            new TradingApiException(
                "If you think that this war isn't changing you you're wrong. "
                    + "You can only circle the flames so long."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_NEW_SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("symbol", MARKET_ID)).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("exchange", "bitfinex")).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);
    expect(requestParamMap.put("type", "exchange limit")).andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);
    expect(requestParamMap.put("request", "/v1/order/new")).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("X-BFX-APIKEY", KEY)).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-BFX-PAYLOAD"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-BFX-SIGNATURE"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);

    final URL url = new URL(AUTHENTICATED_API_URL + ORDER_NEW);
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
    assertEquals("425116929", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesExchangeNetworkException()
      throws Exception {
    final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestParamMap.put("symbol", MARKET_ID)).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("exchange", "bitfinex")).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);
    expect(requestParamMap.put("type", "exchange limit")).andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);
    expect(requestParamMap.put("request", "/v1/order/new")).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("X-BFX-APIKEY", KEY)).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-BFX-PAYLOAD"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-BFX-SIGNATURE"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);

    final URL url = new URL(AUTHENTICATED_API_URL + ORDER_NEW);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(new ExchangeNetworkException("The road goes ever on and on..."));

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
    expect(requestParamMap.put("symbol", MARKET_ID)).andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(requestParamMap.put("exchange", "bitfinex")).andStubReturn(null);
    expect(requestParamMap.put("side", "sell")).andStubReturn(null);
    expect(requestParamMap.put("type", "exchange limit")).andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);
    expect(requestParamMap.put("request", "/v1/order/new")).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("X-BFX-APIKEY", KEY)).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-BFX-PAYLOAD"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put(eq("X-BFX-SIGNATURE"), anyString())).andStubReturn(null);
    expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final BitfinexExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitfinexExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);

    final URL url = new URL(AUTHENTICATED_API_URL + ORDER_NEW);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(
            new TradingApiException(
                "Do you wish me a good morning, or mean that it is a good morning whether I want "
                    + "it or not; or that you feel good this morning; or that it is a morning to "
                    + "be good on?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }
}
