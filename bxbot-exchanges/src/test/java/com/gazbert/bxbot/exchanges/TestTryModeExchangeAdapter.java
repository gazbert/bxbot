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

import static org.easymock.EasyMock.anyObject;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Tests the behaviour of the Try-Mode Exchange Adapter.
 *
 * <p>It has been configured to use Bitstamp for the public API calls.
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

  private static final String BUY_JSON_RESPONSE = "./src/test/exchange-data/bitstamp/buy.json";
  private static final String SELL_JSON_RESPONSE = "./src/test/exchange-data/bitstamp/sell.json";
  private static final String CANCEL_ORDER_JSON_RESPONSE =
      "./src/test/exchange-data/bitstamp/cancel_order.json";

  private static final String BUY = "buy/";
  private static final String SELL = "sell/";
  private static final String CANCEL_ORDER = "cancel_order";

  // --------------------------------------------------------------------------
  // Canned test data
  // --------------------------------------------------------------------------

  private static final String MARKET_ID = "btcusd";
  private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
  private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
  private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.03");
  private static final String ORDER_ID_TO_CANCEL = "80894263";

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

  private static final String MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD = "createRequestParamMap";
  private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD =
      "sendAuthenticatedRequestToExchange";

  // --------------------------------------------------------------------------
  // Delegate Exchange Adapter config
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

  /** Create some exchange config - the TradingEngine would normally do this. */
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
  // --------------------------------------------------------------------------

  @Ignore("TODO: Enable test")
  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToBuyIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(BUY_JSON_RESPONSE));
    final ExchangeHttpResponse exchangeResponse =
        new ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

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

  @Ignore("TODO: Enable test")
  @Test
  @SuppressWarnings("unchecked")
  public void testCreateOrderToSellIsSuccessful() throws Exception {
    final byte[] encoded = Files.readAllBytes(Paths.get(SELL_JSON_RESPONSE));
    final ExchangeHttpResponse exchangeResponse =
        new ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

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

  @Ignore("TODO: Enable test")
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

  @Ignore("TODO: Enable test")
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
                "And now we're gonna scrap all that to chase a rogue transmission? "
                    + "Think about it. A human being out there, where there can't be "
                    + "any humans. A hidden planet that turns up out of nowhere and just "
                    + "happens to be perfect of us. It's too good to be true."));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Cancel Order tests
  // --------------------------------------------------------------------------

  @Ignore("TODO: Enable test")
  @Test
  @SuppressWarnings("unchecked")
  public void testCancelOrderIsSuccessful() throws Exception {
    // Load the canned response from the exchange
    final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_JSON_RESPONSE));
    final ExchangeHttpResponse exchangeResponse =
        new ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

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

  @Ignore("TODO: Enable test")
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
                " Final report of the vessel Prometheus. The ship and her entire crew are gone."
                    + " If you're receiving this transmission, make no attempt to come to its"
                    + " point of origin. There is only death here now, and I'm leaving it "
                    + "behind. It is New Year's Day, the year of our Lord, 2094. My name is "
                    + "Elisabeth Shaw, last survivor of the Prometheus. And I am still searching"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

    PowerMock.verifyAll();
  }

  @Ignore("TODO: Enable test")
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
        .andThrow(
            new IllegalStateException("The trick, William Potter, is not minding that it hurts"));

    PowerMock.replayAll();
    exchangeAdapter.init(exchangeConfig);

    // marketId arg not needed for cancelling orders on this exchange.
    exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  //  Get Your Open Orders tests
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
            CLOSED_SELL_ORDER_PRICE, // this will result in an empty open order list
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

    // Ouch!
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

    // Ouch!
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
            CLOSED_BUY_ORDER_PRICE, // this will result in an empty open order list
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
  //  Initialisation tests assume config property files are located under
  //  src/test/resources
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
