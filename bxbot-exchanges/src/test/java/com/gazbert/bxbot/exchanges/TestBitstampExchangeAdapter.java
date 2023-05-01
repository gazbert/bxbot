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
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
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
import java.text.SimpleDateFormat;
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
 * Tests the behaviour of the Bitstamp Exchange Adapter.
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
@PrepareForTest(BitstampExchangeAdapter.class)
public class TestBitstampExchangeAdapter extends AbstractExchangeAdapter {

  private static final String ORDER_BOOK_JSON_RESPONSE =
      "./src/test/exchange-data/bitstamp/order_book.json";
  private static final String OPEN_ORDERS_JSON_RESPONSE =
      "./src/test/exchange-data/bitstamp/open_orders.json";
  private static final String BALANCE_JSON_RESPONSE =
      "./src/test/exchange-data/bitstamp/balance.json";
  private static final String TICKER_JSON_RESPONSE =
      "./src/test/exchange-data/bitstamp/ticker.json";
  private static final String BUY_JSON_RESPONSE = "./src/test/exchange-data/bitstamp/buy.json";
  private static final String SELL_JSON_RESPONSE = "./src/test/exchange-data/bitstamp/sell.json";
  private static final String CANCEL_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/bitstamp/cancel_order.json";

  private static final String ORDER_BOOK = "order_book/";
  private static final String OPEN_ORDERS = "open_orders/";
  private static final String BALANCE = "balance";
  private static final String TICKER = "ticker/";
  private static final String BUY = "buy/";
  private static final String SELL = "sell/";
  private static final String CANCEL_ORDER = "cancel_order";

  private static final String MARKET_ID = "btcusd";
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final String ORDER_ID_TO_CANCEL = "80894263";

  private static final String MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD = "createRequestParamMap";
  private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD =
      "sendAuthenticatedRequestToExchange";
  private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD =
      "sendPublicRequestToExchange";
  private static final String MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD = "createHeaderParamMap";
  private static final String MOCKED_MAKE_NETWORK_REQUEST_METHOD = "makeNetworkRequest";

  private static final String CLIENT_ID = "clientId123";
  private static final String KEY = "key123";
  private static final String SECRET = "notGonnaTellYa";
  private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
  private static final List<String> nonFatalNetworkErrorMessages =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake");

  private static final String API_BASE_URL = "https://www.bitstamp.net/api/v2/";

  // Bitstamp exchange Date format: 2015-01-09 21:14:50
  private final SimpleDateFormat bitstampExchangeDateFormat = new SimpleDateFormat("y-M-d H:m:s");

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;

  /** Create some exchange config - the TradingEngine would normally do this. */
  @Before
  public void setupForEachTest() {
    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    expect(authenticationConfig.getItem("client-id")).andReturn(CLIENT_ID);
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
  //  Cancel Order tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testCancelOrderIsSuccessful() throws Exception {
    // Load the canned response from the exchange
    final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    // Mock out param map so we can assert the contents passed to the transport layer are what we
    // expect.
    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(requestParamMap.put("id", ORDER_ID_TO_CANCEL)).andStubReturn(null);

    // Partial mock so we do not send stuff down the wire
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
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

    // marketId arg not needed for cancelling orders on this exchange.
    final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
    assertTrue(success);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCancelOrderHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(CANCEL_ORDER),
            anyObject(Map.class))
        .andThrow(
            new ExchangeNetworkException(
                "Traveling through hyperspace ain't like dusting crops, boy!"
                    + " Without precise calculations we could fly right through a star, or bounce "
                    + "too close to a supernova and that'd end your trip real quick, wouldn't "
                    + "it?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCancelOrderHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(CANCEL_ORDER),
            anyObject(Map.class))
        .andThrow(new IllegalStateException("The Force is strong with this one."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Create Orders tests
  // --------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToBuyIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BUY_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(BUY_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(BUY_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put("type", "buy")).andStubReturn(null);

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BUY + MARKET_ID),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    assertEquals("80890994", orderId);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToSellIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(SELL + MARKET_ID),
            eq(requestParamMap))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final String orderId =
        exchangeAdapter.createOrder(
            MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    assertEquals("80890993", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testCreateOrderHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(SELL + MARKET_ID),
            anyObject(Map.class))
        .andThrow(new ExchangeNetworkException("Aaaaaaaaaaaaaaaarrrgh!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testCreateOrderHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BUY + MARKET_ID),
            anyObject(Map.class))
        .andThrow(
            new IllegalArgumentException(
                "That's 'cause droids don't pull people's arms out of their "
                    + "sockets when they lose. Wookiees are known to do that."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
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

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_BOOK + MARKET_ID))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(MARKET_ID, marketOrderBook.getMarketId());

    final BigDecimal buyPrice = new BigDecimal("230.34");
    final BigDecimal buyQuantity = new BigDecimal("7.22860000");
    final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

    assertEquals(1268, marketOrderBook.getBuyOrders().size()); // stamp send them all back!
    assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity));
    assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal));

    final BigDecimal sellPrice = new BigDecimal("230.90");
    final BigDecimal sellQuantity = new BigDecimal("0.62263188");
    final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

    assertEquals(1957, marketOrderBook.getSellOrders().size()); // stamp send them all back!
    assertSame(OrderType.SELL, marketOrderBook.getSellOrders().get(0).getType());
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity));
    assertEquals(0, marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_BOOK + MARKET_ID))
        .andThrow(
            new ExchangeNetworkException(
                "Traveling through hyperspace ain’t like dusting crops, farm boy."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getMarketOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
            eq(ORDER_BOOK + MARKET_ID))
        .andThrow(
            new IllegalArgumentException(
                "Uh, we had a slight weapons malfunction, but uh... "
                    + "everything's perfectly all right now. We're fine. We're all fine here now, "
                    + "thank you. How are you?"));

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
    final byte[] encoded = Files.readAllBytes(Paths.get(OPEN_ORDERS_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS + MARKET_ID),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

    // assert some key stuff; we're not testing GSON here.
    assertEquals(2, openOrders.size());
    assertEquals(MARKET_ID, openOrders.get(0).getMarketId());
    assertEquals("52603560", openOrders.get(0).getId());
    assertSame(OrderType.SELL, openOrders.get(0).getType());
    assertEquals(
        openOrders.get(0).getCreationDate().getTime(),
        bitstampExchangeDateFormat.parse("2015-01-09 21:14:50").getTime());
    assertEquals(0, openOrders.get(0).getPrice().compareTo(new BigDecimal("350.00")));
    assertEquals(0, openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.20000000")));
    assertEquals(
        0,
        openOrders
            .get(0)
            .getTotal()
            .compareTo(openOrders.get(0).getPrice().multiply(openOrders.get(0).getQuantity())));

    // the values below are not provided by Bitstamp
    assertNull(openOrders.get(0).getOriginalQuantity());

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingYourOpenOrdersHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS + MARKET_ID),
            eq(null))
        .andThrow(new ExchangeNetworkException("The board is green!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getYourOpenOrders(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(OPEN_ORDERS + MARKET_ID),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "You may dispense with the pleasantries, Commander. "
                    + "I am here to put you back on schedule."));

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
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER + MARKET_ID))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal latestMarketPrice =
        exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, RoundingMode.HALF_UP);
    assertEquals(0, latestMarketPrice.compareTo(new BigDecimal("230.33")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER + MARKET_ID))
        .andThrow(new ExchangeNetworkException("Jumping in 5... 4... 3... 2... 1... Jump!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER + MARKET_ID))
        .andThrow(
            new IllegalArgumentException(
                "Sir, the possibility of successfully navigating an asteroid field"
                    + " is approximately 3,720 to 1."));

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

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("0.00760854")));
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("USD").compareTo(new BigDecimal("57.03")));
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("EUR").compareTo(new BigDecimal("16.01")));
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("LTC").compareTo(new BigDecimal("50.01")));
    assertEquals(
        0, balanceInfo.getBalancesAvailable().get("XRP").compareTo(new BigDecimal("10.01")));

    assertEquals(
        0, balanceInfo.getBalancesOnHold().get("BTC").compareTo(new BigDecimal("0.01918917")));
    assertEquals(0, balanceInfo.getBalancesOnHold().get("USD").compareTo(new BigDecimal("62.23")));
    assertEquals(0, balanceInfo.getBalancesOnHold().get("EUR").compareTo(new BigDecimal("12.01")));
    assertEquals(0, balanceInfo.getBalancesOnHold().get("LTC").compareTo(new BigDecimal("40.01")));
    assertEquals(0, balanceInfo.getBalancesOnHold().get("XRP").compareTo(new BigDecimal("5.01")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(
            new ExchangeNetworkException(
                "I’ve been waiting for you, Obi-Wan. We meet again, at last. "
                    + "The circle is now complete. When I left you, I was but the learner; "
                    + "now I am the master."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getBalanceInfo();
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(new IllegalStateException("Get me some more frakking birds in the air!"));

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
    final byte[] encoded = Files.readAllBytes(Paths.get(BALANCE_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal buyPercentageFee =
        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, buyPercentageFee.compareTo(new BigDecimal("0.0025")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingExchangeBuyingFeeHandlesTimeoutException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(new ExchangeNetworkException("Aren't you a little short for a stormtrooper?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingExchangeBuyingFeeHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "I felt a great disturbance in the Force, as if millions of voices"
                    + " suddenly cried out in terror and were suddenly silenced. "
                    + "I fear something terrible has happened."));

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
    final byte[] encoded = Files.readAllBytes(Paths.get(BALANCE_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final BigDecimal sellPercentageFee =
        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    assertEquals(0, sellPercentageFee.compareTo(new BigDecimal("0.0025")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingExchangeSellingFeeHandlesTimeoutException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(new ExchangeNetworkException("That's no moon. It's a space station."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingExchangeSellingFeeHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
            eq(BALANCE),
            eq(null))
        .andThrow(
            new IllegalStateException(
                "Don't be too proud of this technological terror you've constructed. "
                    + "The ability to destroy a planet is insignificant next to the power of the "
                    + "Force."));

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
    final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER + MARKET_ID))
        .andReturn(exchangeResponse);

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);
    assertEquals(0, ticker.getLast().compareTo(new BigDecimal("230.33")));
    assertEquals(0, ticker.getAsk().compareTo(new BigDecimal("230.69")));
    assertEquals(0, ticker.getBid().compareTo(new BigDecimal("230.34")));
    assertEquals(0, ticker.getHigh().compareTo(new BigDecimal("231.38")));
    assertEquals(0, ticker.getLow().compareTo(new BigDecimal("224.39")));
    assertEquals(0, ticker.getOpen().compareTo(new BigDecimal("220.69")));
    assertEquals(0, ticker.getVolume().compareTo(new BigDecimal("16666.06077534")));
    assertEquals(0, ticker.getVwap().compareTo(new BigDecimal("228.28")));
    assertEquals(1441040860L, (long) ticker.getTimestamp());

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testGettingTickerHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER + MARKET_ID))
        .andThrow(new ExchangeNetworkException("Indy, why does the floor move?"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);
    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testGettingTickerHandlesUnexpectedException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
    PowerMock.expectPrivate(
            exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER + MARKET_ID))
        .andThrow(
            new IllegalArgumentException(
                "Well, I mean that for nearly three thousand years man has "
                    + "been searching for the lost ark. It's not something to be taken lightly. "
                    + "No one knows its secrets. It's like nothing you've ever gone after before"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);
    exchangeAdapter.getTicker(MARKET_ID);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Non Exchange visiting tests
  // --------------------------------------------------------------------------

  @Test
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();
    final BitstampExchangeAdapter exchangeAdapter = new BitstampExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);
    assertEquals("Bitstamp HTTP API v2", exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Initialisation tests assume config property files are located under
  //  src/test/resources
  // --------------------------------------------------------------------------

  @Test
  public void testExchangeAdapterInitialisesSuccessfully() {
    PowerMock.replayAll();
    final BitstampExchangeAdapter exchangeAdapter = new BitstampExchangeAdapter();
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

    final ExchangeAdapter exchangeAdapter = new BitstampExchangeAdapter();
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

    final ExchangeAdapter exchangeAdapter = new BitstampExchangeAdapter();
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

    final ExchangeAdapter exchangeAdapter = new BitstampExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() {
    PowerMock.reset(networkConfig);
    expect(networkConfig.getConnectionTimeout()).andReturn(0);
    PowerMock.replayAll();

    final ExchangeAdapter exchangeAdapter = new BitstampExchangeAdapter();
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

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD);

    final URL url = new URL(API_BASE_URL + TICKER + MARKET_ID);
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
    assertEquals(0, lastMarketPrice.compareTo(new BigDecimal("230.33")));

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  public void testSendingPublicRequestToExchangeHandlesExchangeNetworkException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD);

    final URL url = new URL(API_BASE_URL + TICKER + MARKET_ID);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(
            new ExchangeNetworkException(
                "When 900 years old, you reach… Look as good, you will not."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  public void testSendingPublicRequestToExchangeHandlesTradingApiException() throws Exception {
    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD);

    final URL url = new URL(API_BASE_URL + TICKER + MARKET_ID);
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("GET"),
            eq(null),
            eq(new HashMap<>()))
        .andThrow(new TradingApiException("He’s holding a thermal detonator!"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.getLatestMarketPrice(MARKET_ID);

    PowerMock.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeSuccessfully() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(SELL_JSON_RESPONSE));
    final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
        new AbstractExchangeAdapter.ExchangeHttpResponse(
            200, "OK", new String(encoded, StandardCharsets.UTF_8));

    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("key", "key123")).andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);
    expect(requestParamMap.put(eq("signature"), anyString())).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/x-www-form-urlencoded"))
        .andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(API_BASE_URL + SELL + MARKET_ID + "/");
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
    assertEquals("80890993", orderId);

    PowerMock.verifyAll();
  }

  @Test(expected = ExchangeNetworkException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesExchangeNetworkException()
      throws Exception {
    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("key", "key123")).andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);
    expect(requestParamMap.put(eq("signature"), anyString())).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put("Content-Type", "application/x-www-form-urlencoded"))
        .andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(API_BASE_URL + SELL + MARKET_ID + "/");
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(new ExchangeNetworkException("These aren’t the droids you’re looking for..."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }

  @Test(expected = TradingApiException.class)
  @SuppressWarnings("unchecked")
  public void testSendingAuthenticatedRequestToExchangeHandlesTradingApiException()
      throws Exception {
    final Map<String, String> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(
            requestParamMap.put(
                "price",
                new DecimalFormat("#.##", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE)))
        .andStubReturn(null);
    expect(
            requestParamMap.put(
                "amount",
                new DecimalFormat("#.########", getDecimalFormatSymbols())
                    .format(SELL_ORDER_QUANTITY)))
        .andStubReturn(null);
    expect(requestParamMap.put("type", "sell")).andStubReturn(null);
    expect(requestParamMap.put("key", "key123")).andStubReturn(null);
    expect(requestParamMap.put(eq("nonce"), anyString())).andStubReturn(null);
    expect(requestParamMap.put(eq("signature"), anyString())).andStubReturn(null);

    final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
    expect(requestHeaderMap.put(eq("Content-Type"), eq("application/x-www-form-urlencoded")))
        .andStubReturn(null);
    PowerMock.replay(requestHeaderMap); // map needs to be in play early

    final BitstampExchangeAdapter exchangeAdapter =
        PowerMock.createPartialMockAndInvokeDefaultConstructor(
            BitstampExchangeAdapter.class,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
            MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD)
        .andReturn(requestHeaderMap);
    PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD)
        .andReturn(requestParamMap);

    final URL url = new URL(API_BASE_URL + SELL + MARKET_ID + "/");
    PowerMock.expectPrivate(
            exchangeAdapter,
            MOCKED_MAKE_NETWORK_REQUEST_METHOD,
            eq(url),
            eq("POST"),
            anyString(),
            eq(requestHeaderMap))
        .andThrow(
            new TradingApiException("Mmm. Lost a planet, Master Obi-Wan has. How embarrassing."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

    PowerMock.verifyAll();
  }
}
