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

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OptionalConfig;
import com.gazbert.bxbot.trading.api.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

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
import static org.junit.Assert.*;

/**
 * <p>
 * Tests the behaviour of the itBit Exchange Adapter.
 * </p>
 * <p>
 * <p>
 * Coverage could be better: it does not include calling the
 * {@link ItBitExchangeAdapter#sendPublicRequestToExchange(String)}  and
 * {@link ItBitExchangeAdapter#sendAuthenticatedRequestToExchange(String, String, Map)}  methods; the code in these
 * methods is a bloody nightmare to test!
 * </p>
 * <p>
 * TODO Unit test {@link ItBitExchangeAdapter#sendPublicRequestToExchange(String)} method.
 * TODO Unit test {@link ItBitExchangeAdapter#sendAuthenticatedRequestToExchange(String, String, Map)} method.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest(ItBitExchangeAdapter.class)
public class TestItBitExchangeAdapter {

    // Canned test data
    private static final String MARKET_ID = "XBTUSD";
    private static final String WALLET_ID = "62827e93-f19b-67bf-8d2f-663fa4f0f1ad";
    private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
    private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.01");
    private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
    private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.0005");
    private static final String ORDER_ID_TO_CANCEL = "0be8d3d7-f710-4e1e-b0e7-91ca276b7e1a";

    // Canned JSON responses from exchange - expected to reside on filesystem relative to project root
    private static final String WALLETS_JSON_RESPONSE = "./src/test/exchange-data/itbit/wallets.json";
    private static final String ORDER_BOOK_JSON_RESPONSE = "./src/test/exchange-data/itbit/order_book.json";
    private static final String OPEN_ORDERS_JSON_RESPONSE = "./src/test/exchange-data/itbit/orders.json";
    private static final String TICKER_JSON_RESPONSE = "./src/test/exchange-data/itbit/ticker.json";
    private static final String NEW_ORDER_BUY_JSON_RESPONSE = "./src/test/exchange-data/itbit/new_order_buy.json";
    private static final String NEW_ORDER_SELL_JSON_RESPONSE = "./src/test/exchange-data/itbit/new_order_sell.json";
    private static final String CANCEL_ORDER_JSON_RESPONSE = "./src/test/exchange-data/itbit/cancel_order.json";

    // Exchange API calls
    private static final String WALLETS = "wallets";
    private static final String ORDER_BOOK = "markets/" + MARKET_ID + "/order_book";
    private static final String OPEN_ORDERS = "wallets/" + WALLET_ID + "/orders";
    private static final String TICKER = "markets/" + MARKET_ID + "/ticker";
    private static final String NEW_ORDER = "wallets/" + WALLET_ID + "/orders"; // same as ORDERS but uses POST
    private static final String CANCEL_ORDER = "wallets/" + WALLET_ID + "/orders/" + ORDER_ID_TO_CANCEL;

    // Mocked out methods
    private static final String MOCKED_GET_REQUEST_PARAM_MAP_METHOD = "getRequestParamMap";
    private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD = "sendAuthenticatedRequestToExchange";
    private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD = "sendPublicRequestToExchange";

    // Mocked out state
    private static final String MOCKED_WALLET_ID_FIELD_NAME = "walletId";

    // Exchange Adapter config for the tests
    private static final String USERID = "userId123";
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
        expect(authenticationConfig.getItem("userId")).andReturn(USERID);
        expect(authenticationConfig.getItem("key")).andReturn(KEY);
        expect(authenticationConfig.getItem("secret")).andReturn(SECRET);

        networkConfig = PowerMock.createMock(NetworkConfig.class);
        expect(networkConfig.getConnectionTimeout()).andReturn(30);
        expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
        expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);

        optionalConfig = PowerMock.createMock(OptionalConfig.class);
        expect(optionalConfig.getItem("buy-fee")).andReturn("0.5");
        expect(optionalConfig.getItem("sell-fee")).andReturn("0.5");

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
        final byte[] encoded = Files.readAllBytes(Paths.get(NEW_ORDER_BUY_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse = new AbstractExchangeAdapter.ExchangeHttpResponse(
                201, "Created", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("type", "limit")).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.####").format(BUY_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("price", new DecimalFormat("#.##").format(BUY_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
        expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
        expect(requestParamMap.put("side", "buy")).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        assertTrue(orderId.equals("8a9ac32f-c2bd-4316-87d8-4219dc5e8041"));

        PowerMock.verifyAll();
    }

    @Test
    public void testCreateOrderToSellIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(NEW_ORDER_SELL_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse = new AbstractExchangeAdapter.ExchangeHttpResponse(
                201, "Created", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("type", "limit")).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.####").format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("price", new DecimalFormat("#.##").format(SELL_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("instrument", MARKET_ID)).andStubReturn(null);
        expect(requestParamMap.put("currency", MARKET_ID.substring(0, 3))).andStubReturn(null);
        expect(requestParamMap.put("side", "sell")).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        assertTrue(orderId.equals("8a7ac32f-c2bd-4316-87d8-4219dc5e8031"));

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testCreateOrderHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), anyObject(Map.class)).andThrow(new ExchangeNetworkException(" If you want the ultimate," +
                " you've got to be willing to pay the ultimate price. It's not tragic to die doing what you love."));

        PowerMock.replayAll();
        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testCreateOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("POST"),
                eq(NEW_ORDER), anyObject(Map.class)).andThrow(new IllegalArgumentException("Fear causes hesitation," +
                " and hesitation will cause your worst fears to come true."));

        PowerMock.replayAll();
        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
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
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse = new AbstractExchangeAdapter.ExchangeHttpResponse(
                202, "Accepted", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("DELETE"),
                eq(CANCEL_ORDER), eq(null)).andReturn(exchangeResponse);

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

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("DELETE"),
                eq(CANCEL_ORDER), eq(null)).andThrow(
                new ExchangeNetworkException("Peace, through superior firepower!"));

        PowerMock.replayAll();
        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testCancelOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                eq("DELETE"), eq(CANCEL_ORDER), eq(null)).andThrow(
                new IllegalStateException("It's basic dog psychology, if you scare them and get them peeing down" +
                        " their leg, they submit. But if you project weakness, that promotes violence, and that's" +
                        " how people get hurt."));

        PowerMock.replayAll();
        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
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
        final byte[] encoded = Files.readAllBytes(Paths.get(OPEN_ORDERS_JSON_RESPONSE));

        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("status", "open")).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(OPEN_ORDERS), eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.init(exchangeConfig);

        final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(openOrders.size() == 2);
        assertTrue(openOrders.get(0).getMarketId().equals(MARKET_ID));
        assertTrue(openOrders.get(0).getId().equals("639ccf95-b87c-48ba-b27d-7bc09b841b81"));
        assertTrue(openOrders.get(0).getType() == OrderType.SELL);
        assertTrue(openOrders.get(0).getCreationDate().equals(Date.from(Instant.parse("2015-10-01T18:11:06.8470000Z"))));
        assertTrue(openOrders.get(0).getPrice().compareTo(new BigDecimal("255.59000000")) == 0);
        assertTrue(openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.01500000")) == 0);
        assertTrue(openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.01500000")) == 0);
        assertTrue(openOrders.get(0).getTotal().compareTo(openOrders.get(0).getPrice().multiply(openOrders.get(0).getOriginalQuantity())) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingYourOpenOrdersHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                eq("GET"), eq(OPEN_ORDERS), anyObject(Map.class)).andThrow(
                new ExchangeNetworkException("I'm afraid. I'm afraid, Dave. Dave, my mind is " +
                        "going. I can feel it. I can feel it. My mind is going. There is no question about it. " +
                        "I can feel it. I can feel it. I can feel it. I'm a... fraid. Good afternoon, gentlemen." +
                        " I am a HAL 9000 computer. I became operational at the H.A.L. plant in Urbana, Illinois" +
                        " on the 12th of January 1992. My instructor was Mr. Langley, and he taught me to sing a" +
                        " song. If you'd like to hear it I can sing it for you."));

        PowerMock.replayAll();
        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(OPEN_ORDERS), anyObject(Map.class)).andThrow(new IllegalStateException("Hello, HAL. Do you read me, HAL?"));

        PowerMock.replayAll();
        Whitebox.setInternalState(exchangeAdapter, MOCKED_WALLET_ID_FIELD_NAME, WALLET_ID);
        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Market Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingMarketOrdersSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_BOOK_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, ORDER_BOOK).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        exchangeAdapter.init(exchangeConfig);
        final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));

        final BigDecimal buyPrice = new BigDecimal("236.73");
        final BigDecimal buyQuantity = new BigDecimal("0.03");
        final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

        assertTrue(marketOrderBook.getBuyOrders().size() == 159); // itBit sends them all back!
        assertTrue(marketOrderBook.getBuyOrders().get(0).getType() == OrderType.BUY);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal) == 0);

        final BigDecimal sellPrice = new BigDecimal("236.84");
        final BigDecimal sellQuantity = new BigDecimal("6.74");
        final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

        assertTrue(marketOrderBook.getSellOrders().size() == 143); // itBit sends them all back!
        assertTrue(marketOrderBook.getSellOrders().get(0).getType() == OrderType.SELL);
        assertTrue(marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, ORDER_BOOK).
                andThrow(new ExchangeNetworkException("There is an idea of a Patrick Bateman; some kind of " +
                        "abstraction. But there is no real me: only an entity, something illusory. And though I" +
                        " can hide my cold gaze, and you can shake my hand and feel flesh gripping yours and maybe" +
                        " you can even sense our lifestyles are probably comparable... I simply am not there."));

        PowerMock.replayAll();
        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, ORDER_BOOK).
                andThrow(new IllegalArgumentException("I have to return some videotapes"));

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
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        exchangeAdapter.init(exchangeConfig);
        final BigDecimal latestMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(
                8, BigDecimal.ROUND_HALF_UP);
        assertTrue(latestMarketPrice.compareTo(new BigDecimal("237.70000000")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER).
                andThrow(new ExchangeNetworkException(" I've seen horrors... horrors that you've seen." +
                        " But you have no right to call me a murderer. You have a right to kill me. You have a right" +
                        " to do that... but you have no right to judge me. It's impossible for words to describe what" +
                        " is necessary to those who do not know what horror means. Horror... Horror has a face... and" +
                        " you must make a friend of horror. Horror and moral terror are your friends. " +
                        "If they are not, then they are enemies to be feared."));

        PowerMock.replayAll();
        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, TICKER).
                andThrow(new IllegalArgumentException("The horror... the horror..."));

        PowerMock.replayAll();
        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Balance Info tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingBalanceInfoSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(WALLETS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse = new AbstractExchangeAdapter.ExchangeHttpResponse(
                200, "Ok", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put(eq("userId"), anyString())).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                ItBitExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(WALLETS), eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        exchangeAdapter.init(exchangeConfig);
        final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

        // assert some key stuff; we're not testing GSON here.
        assertTrue(balanceInfo.getBalancesAvailable().get("XBT").compareTo(new BigDecimal("1.50000000")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("USD").compareTo(new BigDecimal("1000.9900000")) == 0);

        // itBot does not provide "balances on hold" info.
        assertNull(balanceInfo.getBalancesOnHold().get("BTC"));
        assertNull(balanceInfo.getBalancesOnHold().get("USD"));

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(WALLETS), anyObject(Map.class)).
                andThrow(new ExchangeNetworkException("You were in a 4g inverted dive with a MiG28?"));

        PowerMock.replayAll();
        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final ItBitExchangeAdapter exchangeAdapter = PowerMock.createPartialMock(ItBitExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq("GET"),
                eq(WALLETS), anyObject(Map.class)).
                andThrow(new IllegalStateException("Tower, this is Ghost Rider requesting a flyby... " +
                        "Negative, Ghost Rider, the pattern is full... BOOM!!!"));

        PowerMock.replayAll();
        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeSellingFeeIsAsExpected() throws Exception {

        PowerMock.replayAll();

        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal sellPercentageFee = exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(sellPercentageFee.compareTo(new BigDecimal("0.005")) == 0);

        PowerMock.verifyAll();
    }

    @Test
    public void testGettingExchangeBuyingFeeIsAsExpected() throws Exception {

        PowerMock.replayAll();

        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.005")) == 0);

        PowerMock.verifyAll();
    }

    @Test
    public void testGettingImplNameIsAsExpected() throws Exception {

        PowerMock.replayAll();

        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        assertTrue(exchangeAdapter.getImplName().equals("itBit REST API v1"));
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Initialisation tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testExchangeAdapterInitialisesSuccessfully() throws Exception {

        PowerMock.replayAll();
        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        assertNotNull(exchangeAdapter);
        PowerMock.verify();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfUserIdConfigIsMissing() throws Exception {

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
    public void testExchangeAdapterThrowsExceptionIfClientKeyConfigIsMissing() throws Exception {

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
    public void testExchangeAdapterThrowsExceptionIfClientSecretConfigIsMissing() throws Exception {

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
    public void testExchangeAdapterThrowsExceptionIfBuyFeeIsMissing() throws Exception {

        PowerMock.reset(optionalConfig);
        expect(optionalConfig.getItem("buy-fee")).andReturn("");
        expect(optionalConfig.getItem("sell-fee")).andReturn("0.5");
        PowerMock.replayAll();

        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSellFeeIsMissing() throws Exception {

        PowerMock.reset(optionalConfig);
        expect(optionalConfig.getItem("buy-fee")).andReturn("0.5");
        expect(optionalConfig.getItem("sell-fee")).andReturn(null);
        PowerMock.replayAll();

        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() throws Exception {

        PowerMock.reset(networkConfig);
        expect(networkConfig.getConnectionTimeout()).andReturn(0);
        PowerMock.replayAll();

        final ItBitExchangeAdapter exchangeAdapter = new ItBitExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        PowerMock.verifyAll();
    }
}