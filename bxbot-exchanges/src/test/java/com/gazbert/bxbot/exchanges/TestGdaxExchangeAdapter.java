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

import com.gazbert.bxbot.exchange.api.*;
import com.gazbert.bxbot.trading.api.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Tests the behaviour of the GDAX Exchange Adapter.
 * </p>
 * <p>
 * <p>
 * Coverage could be better: it does not include calling the
 * {@link GdaxExchangeAdapter#sendPublicRequestToExchange(String, Map)} and
 * {@link GdaxExchangeAdapter#sendAuthenticatedRequestToExchange(String, String, Map)} methods;
 * the code in these methods is a bloody nightmare to test!
 * </p>
 * <p>
 * TODO Unit test {@link GdaxExchangeAdapter#sendPublicRequestToExchange(String, Map)} method.
 * TODO Unit test {@link GdaxExchangeAdapter#sendAuthenticatedRequestToExchange(String, String, Map)} method.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest(GdaxExchangeAdapter.class)
public class TestGdaxExchangeAdapter {

    // Canned JSON responses from exchange - expected to reside on filesystem relative to project root
    private static final String BOOK_JSON_RESPONSE = "./src/test/exchange-data/gdax/book.json";
    private static final String ORDERS_JSON_RESPONSE = "./src/test/exchange-data/gdax/orders.json";
    private static final String ACCOUNTS_JSON_RESPONSE = "./src/test/exchange-data/gdax/accounts.json";
    private static final String TICKER_JSON_RESPONSE = "./src/test/exchange-data/gdax/ticker.json";
    private static final String NEW_BUY_ORDER_JSON_RESPONSE = "./src/test/exchange-data/gdax/new_buy_order.json";
    private static final String NEW_SELL_ORDER_JSON_RESPONSE = "./src/test/exchange-data/gdax/new_sell_order.json";
    private static final String CANCEL_ORDER_JSON_RESPONSE = "./src/test/exchange-data/gdax/cancel.json";

    // Canned test data
    private static final String MARKET_ID = "BTC-GBP";
    private static final String ORDER_BOOK_DEPTH_LEVEL = "2"; //  "2" = Top 50 bids and asks (aggregated)
    private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
    private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.01");
    private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
    private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.01");
    private static final String ORDER_ID_TO_CANCEL = "3ecf7a12-fc89-4d3d-baef-f158f80b3bd3";

    // Exchange API calls
    private static final String BOOK = "products/" + MARKET_ID + "/book";
    private static final String ORDERS = "orders";
    private static final String ACCOUNTS = "accounts";
    private static final String TICKER = "products/" + MARKET_ID + "/ticker";
    private static final String NEW_ORDER = "orders";
    private static final String CANCEL_ORDER = "orders/" + ORDER_ID_TO_CANCEL;

    // Mocked out methods
    private static final String MOCKED_GET_REQUEST_PARAM_MAP_METHOD = "getRequestParamMap";
    private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD = "sendAuthenticatedRequestToExchange";
    private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD = "sendPublicRequestToExchange";

    // Exchange Adapter config for the tests
    private static final String PASSPHRASE = "lePassPhrase";
    private static final String KEY = "key123";
    private static final String SECRET = "notGonnaTellYa";
    private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
    private static final List<String> nonFatalNetworkErrorMessages = Arrays.asList(
            "Connection refused", "Connection reset", "Remote host closed connection during handshake");

    private ExchangeConfig exchangeConfig;
    private AuthenticationConfig authenticationConfig;
    private NetworkConfig networkConfig;
    private OptionalConfig optionalConfig;


    /*
     * Create some exchange config - the TradingEngine would normally do this.
     */
    @Before
    public void setupForEachTest() throws Exception {

        authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
        expect(authenticationConfig.getItem("passphrase")).andReturn(PASSPHRASE);
        expect(authenticationConfig.getItem("key")).andReturn(KEY);
        expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

        networkConfig = PowerMock.createMock(NetworkConfig.class);
        expect(networkConfig.getConnectionTimeout()).andReturn(30);
        expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
        expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

        optionalConfig = PowerMock.createMock(OptionalConfig.class);
        expect(optionalConfig.getItem("buy-fee")).andReturn("0.25");
        expect(optionalConfig.getItem("sell-fee")).andReturn("0.25");

        exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
        expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
        expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
        expect(exchangeConfig.getOptionalConfig()).andReturn(optionalConfig);
    }

    // ------------------------------------------------------------------------------------------------
    //  Create Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCreateOrderToBuyIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(NEW_BUY_ORDER_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("size", new DecimalFormat("#.########").format(BUY_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("price", new DecimalFormat("#.##").format(BUY_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("side", "buy")).andStubReturn(null);
        expect(requestParamMap.put("product_id", MARKET_ID)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        assertTrue(orderId.equals("193d2ad9-e671-4d66-9211-7f75f6380231"));

        PowerMock.verifyAll();
    }

    @Test
    public void testCreateOrderToSellIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(NEW_SELL_ORDER_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("size", new DecimalFormat("#.########").format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("price", new DecimalFormat("#.##").format(SELL_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("side", "sell")).andStubReturn(null);
        expect(requestParamMap.put("product_id", MARKET_ID)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        assertTrue(orderId.equals("693d7ad9-e671-4d66-9911-7f75f6380134"));

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testCreateOrderHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), anyObject(Map.class)).
                andThrow(new ExchangeNetworkException(" When it comes to the safety of these people, there's me and " +
                        "then there's God, understand?"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testCreateOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), anyObject(Map.class)).
                andThrow(new IllegalArgumentException(" We all see what we want to see. Coffey looks and he" +
                        " sees Russians. He sees hate and fear. You have to look with better eyes than that"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Cancel Order tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCancelOrderIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_ORDER_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("DELETE"),
                eq(CANCEL_ORDER), eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
        assertTrue(success);
        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testCancelOrderHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("DELETE"),
                eq(CANCEL_ORDER), eq(null)).andThrow(
                new ExchangeNetworkException("We don't need them. We can't trust them. We may have to take steps." +
                        " We're gonna have to take steps."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testCancelOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                eq("DELETE"), eq(CANCEL_ORDER), eq(null)).andThrow(
                new IllegalStateException("Fluid breathing system, we just got it. You use it when you go really deep."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Your Open Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingYourOpenOrdersSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(ORDERS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(ORDERS), eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(openOrders.size() == 2);
        assertTrue(openOrders.get(0).getMarketId().equals(MARKET_ID));
        assertTrue(openOrders.get(0).getId().equals("cdad7602-f290-41e5-a64d-42a1a20fd02"));
        assertTrue(openOrders.get(0).getType() == OrderType.SELL);
        assertTrue(openOrders.get(0).getCreationDate().equals(Date.from(Instant.parse("2015-10-15T21:10:38.193Z"))));
        assertTrue(openOrders.get(0).getPrice().compareTo(new BigDecimal("275.00000000")) == 0);
        assertTrue(openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.01000000")) == 0);
        assertTrue(openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.00500000")) == 0);
        assertTrue(openOrders.get(0).getTotal().compareTo(openOrders.get(0).getPrice().multiply(openOrders.get(0).getOriginalQuantity())) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingYourOpenOrdersHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(ORDERS), eq(null)).andThrow(new ExchangeNetworkException("Bond. James Bond."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(ORDERS), eq(null)).andThrow(new IllegalStateException("All those moments will be lost in time..." +
                " like tears in rain."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Market Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingMarketOrders() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(BOOK_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("level", ORDER_BOOK_DEPTH_LEVEL)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(BOOK),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));

        final BigDecimal buyPrice = new BigDecimal("165.87");
        final BigDecimal buyQuantity = new BigDecimal("16.2373");
        final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

        assertTrue(marketOrderBook.getBuyOrders().size() == 50);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getType() == OrderType.BUY);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal) == 0);

        final BigDecimal sellPrice = new BigDecimal("165.96");
        final BigDecimal sellQuantity = new BigDecimal("24.31");
        final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

        assertTrue(marketOrderBook.getSellOrders().size() == 50);
        assertTrue(marketOrderBook.getSellOrders().get(0).getType() == OrderType.SELL);
        assertTrue(marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(BOOK),
                anyObject(Map.class)).
                andThrow(new ExchangeNetworkException("Re-verify our range to target... one ping only."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(BOOK),
                anyObject(Map.class)).
                andThrow(new IllegalArgumentException("Mr. Ambassador, you have nearly a hundred naval vessels" +
                        " operating in the North Atlantic right now. Your aircraft has dropped enough sonar buoys" +
                        " so that a man could walk from Greenland to Iceland to Scotland without getting his feet " +
                        "wet. Now, shall we dispense with the bull?"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Latest Market Price tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingLatestMarketPriceSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER),
                eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal latestMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, BigDecimal.ROUND_HALF_UP);
        assertTrue(latestMarketPrice.compareTo(new BigDecimal("165.36")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER),
                eq(null)).andThrow(new ExchangeNetworkException(
                "I need your clothes, your boots and your motorcycle."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER),
                eq(null)).andThrow(new IllegalArgumentException("Come with me if you want to live."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Balance Info tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingBalanceInfoSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(ACCOUNTS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(ACCOUNTS), eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

        // assert some key stuff; we're not testing GSON here.
        assertTrue(balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("100.0000000000000004")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("GBP").compareTo(new BigDecimal("501.0100000000000001")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("EUR").compareTo(new BigDecimal("0")) == 0);

        assertTrue(balanceInfo.getBalancesOnHold().get("BTC").compareTo(new BigDecimal("100.0000000000000005")) == 0);
        assertTrue(balanceInfo.getBalancesOnHold().get("GBP").compareTo(new BigDecimal("499.9900000000000002")) == 0);
        assertTrue(balanceInfo.getBalancesOnHold().get("EUR").compareTo(new BigDecimal("0")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(ACCOUNTS), eq(null)).andThrow(new ExchangeNetworkException(
                "Three o'clock is always too late or too early for anything you want to do."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final GdaxExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                GdaxExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(ACCOUNTS), eq(null)).andThrow(new IllegalStateException(
                "There is a time for many words, and there is also a time for sleep."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeSellingFeeIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final GdaxExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal sellPercentageFee = exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(sellPercentageFee.compareTo(new BigDecimal("0.0025")) == 0);
        PowerMock.verifyAll();
    }

    @Test
    public void testGettingExchangeBuyingFeeIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final GdaxExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.0025")) == 0);
        PowerMock.verifyAll();
    }

    @Test
    public void testGettingImplNameIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final GdaxExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        assertTrue(exchangeAdapter.getImplName().equals("GDAX REST API v1"));
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Initialisation tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testExchangeAdapterInitialisesSuccessfully() throws Exception {

        PowerMock.replayAll();

        final GdaxExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        assertNotNull(exchangeAdapter);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPassphraseConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("passphrase")).andReturn(null);
        expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
        expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("passphrase")).andReturn("your_passphrase");
        expect(authenticationConfig.getItem("key")).andReturn(null);
        expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("passphrase")).andReturn("your_passphrase");
        expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
        expect(authenticationConfig.getItem("secret")).andReturn(null);
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfBuyFeeIsMissing() throws Exception {

        PowerMock.reset(optionalConfig);
        expect(optionalConfig.getItem("buy-fee")).andReturn("");
        expect(optionalConfig.getItem("sell-fee")).andReturn("0.25");
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSellFeeIsMissing() throws Exception {

        PowerMock.reset(optionalConfig);
        expect(optionalConfig.getItem("buy-fee")).andReturn("0.25");
        expect(optionalConfig.getItem("sell-fee")).andReturn("");

        PowerMock.replayAll();
        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() throws Exception {

        PowerMock.reset(networkConfig);
        expect(networkConfig.getConnectionTimeout()).andReturn(0);
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }
}