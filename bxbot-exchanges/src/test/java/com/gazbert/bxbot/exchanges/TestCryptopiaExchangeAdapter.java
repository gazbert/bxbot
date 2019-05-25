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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gazbert.bxbot.exchange.api.OtherConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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

/**
 * Tests the behaviour of the Cryptopia Exchange Adapter.
 *
 * @author nodueck
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*", "javax.management.*",
        "com.sun.org.apache.xerces.*", "javax.xml.parsers.*", "org.xml.sax.*", "org.w3c.dom.*"})
@PrepareForTest(CryptopiaExchangeAdapter.class)
public class TestCryptopiaExchangeAdapter extends AbstractExchangeAdapterTest {

    // Canned JSON responses from exchange - expected to reside on filesystem relative to project root
    private static final String GET_MARKET_ORDERS_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/get_market_orders.json";
    private static final String GET_OPEN_ORDERS_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/get_open_orders.json";
    private static final String GET_BALANCE_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/get_balance.json";
    private static final String GET_MARKET_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/get_market.json";
    private static final String GET_TRADE_PAIRS_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/get_trade_pairs.json";
    private static final String SUBMIT_TRADE_BUY_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/submit_trade_buy.json";
    private static final String SUBMIT_TRADE_SELL_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/submit_trade_sell.json";
    private static final String CANCEL_TRADE_JSON_RESPONSE = "./src/test/exchange-data/cryptopia/cancel_trade.json";

    // Exchange API calls
    private static final String GET_MARKET_ORDERS = "GetMarketOrders";
    private static final String GET_OPEN_ORDERS = "GetOpenOrders";
    private static final String GET_BALANCE = "GetBalance";
    private static final String GET_MARKET = "GetMarket";
    private static final String GET_TRADE_PAIRS = "GetTradePairs";
    private static final String SUBMIT_TRADE = "SubmitTrade";
    private static final String CANCEL_TRADE = "CancelTrade";

    // Canned test data
    private static final String MARKET_ID = "dot_btc";
    private static final String LABEL = "DOT/BTC";
    private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.18");
    private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.03");
    private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("300.176");
    private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.03");
    private static final String ORDER_ID_TO_CANCEL = "426152651";

    // Mocked out methods
    private static final String MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD = "createRequestParamMap";
    private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD = "sendAuthenticatedRequestToExchange";
    private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD = "sendPublicRequestToExchange";
    private static final String MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD = "createHeaderParamMap";
    private static final String MOCKED_MAKE_NETWORK_REQUEST_METHOD = "makeNetworkRequest";

    // Exchange Adapter config for the tests
    private static final String PUBLIC_KEY = "key123";
    private static final String PRIVATE_KEY = "notGonnaTellYa";
    private static final List<Integer> nonFatalNetworkErrorCodes = Arrays.asList(502, 503, 504);
    private static final List<String> nonFatalNetworkErrorMessages = Arrays.asList(
            "Connection refused", "Connection reset", "Remote host closed connection during handshake");

    private static final String PUBLIC_API_BASE_URL = "https://www.cryptopia.co.nz/api/";
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    private ExchangeConfig exchangeConfig;
    private AuthenticationConfig authenticationConfig;
    private NetworkConfig networkConfig;
    private OtherConfig otherConfig;


    /*
     * Create some exchange config - the TradingEngine would normally do this.
     */
    @Before
    public void setupForEachTest() throws Exception {

        authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
        expect(authenticationConfig.getItem("public_key")).andReturn(PUBLIC_KEY);
        expect(authenticationConfig.getItem("private_key")).andReturn(PRIVATE_KEY);

        networkConfig = PowerMock.createMock(NetworkConfig.class);
        expect(networkConfig.getConnectionTimeout()).andReturn(30);
        expect(networkConfig.getNonFatalErrorCodes()).andReturn(nonFatalNetworkErrorCodes);
        expect(networkConfig.getNonFatalErrorMessages()).andReturn(nonFatalNetworkErrorMessages);
        
        otherConfig = PowerMock.createMock(OtherConfig.class);
        expect(otherConfig.getItem("use_global_trading_fee")).andReturn("false");

        exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
        expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
        expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
        expect(exchangeConfig.getOtherConfig()).andReturn(otherConfig);
        // optional config not needed for this adapter
    }

    // ------------------------------------------------------------------------------------------------
    //  Create Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateOrderToBuyIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(SUBMIT_TRADE_BUY_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("market", LABEL)).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(BUY_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("rate", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(BUY_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("type", "Buy")).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(SUBMIT_TRADE),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        assertTrue(orderId.equals("23467"));

        PowerMock.verifyAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateOrderToSellIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(SUBMIT_TRADE_SELL_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("market", LABEL)).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("rate", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("type", "Sell")).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(SUBMIT_TRADE),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY,
                SELL_ORDER_PRICE);
        assertTrue(orderId.equals("23467"));

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testCreateOrderHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(SUBMIT_TRADE),
                anyObject(Map.class)).
                andThrow(new ExchangeNetworkException("Marion, don't look at it. Shut your eyes, Marion. Don't look at" +
                        " it, no matter what happens!"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testCreateOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(SUBMIT_TRADE),
                anyObject(Map.class)).
                andThrow(new IllegalArgumentException("What a fitting end to your life's pursuits. You're about to " +
                        "become a permanent addition to this archaeological find. Who knows? In a thousand years," +
                        " even you may be worth something."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Cancel Order tests
    // ------------------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void testCancelOrderIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(CANCEL_TRADE_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("Type", "Trade")).andStubReturn(null);
        expect(requestParamMap.put("OrderId", Long.parseLong(ORDER_ID_TO_CANCEL))).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_TRADE),
                eq(requestParamMap)).andReturn(exchangeResponse);

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
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_TRADE),
                anyObject(Map.class)).
                andThrow(new ExchangeNetworkException("Good morning. I am Meredith Vickers, and it is my job to" +
                        " make sure you do yours"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testCancelOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_TRADE),
                anyObject(Map.class)).
                andThrow(new IllegalStateException("The ring, it chose you. Take it... place the ring on the lantern..." +
                        " place the ring, speak the oath... great honor... responsibility"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Market Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingMarketOrdersSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_MARKET_ORDERS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET_ORDERS + "/" + MARKET_ID.toUpperCase()).
                andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));

        final BigDecimal buyPrice = new BigDecimal("0.00006000");
        final BigDecimal buyQuantity = new BigDecimal("455055.00360000");
        final BigDecimal buyTotal = buyPrice.multiply(buyQuantity, MathContext.DECIMAL32);

        assertTrue(marketOrderBook.getBuyOrders().size() == 1); 
        assertTrue(marketOrderBook.getBuyOrders().get(0).getType() == OrderType.BUY);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal) == 0);

        final BigDecimal sellPrice = new BigDecimal("0.02000000");
        final BigDecimal sellQuantity = new BigDecimal("499.99640000");
        final BigDecimal sellTotal = sellPrice.multiply(sellQuantity, MathContext.DECIMAL32);

        assertTrue(marketOrderBook.getSellOrders().size() == 1); 
        assertTrue(marketOrderBook.getSellOrders().get(0).getType() == OrderType.SELL);
        assertTrue(marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET_ORDERS + "/" + MARKET_ID.toUpperCase()).
                andThrow(new ExchangeNetworkException("There are three basic types, Mr. Pizer: the Wills, the Won'ts," +
                        " and the Can'ts. The Wills accomplish everything, the Won'ts oppose everything, and the " +
                        "Can'ts won't try anything."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET_ORDERS + "/" + MARKET_ID.toUpperCase()).
                andThrow(new IllegalArgumentException("Deckard. B26354"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Your Open Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingYourOpenOrdersSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_OPEN_ORDERS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, Object> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("Market", MARKET_ID)).andStubReturn(null);
        expect(requestParamMap.put("Count", Long.parseLong("100"))).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_OPEN_ORDERS),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(openOrders.size() == 1);
        assertTrue(openOrders.get(0).getMarketId().equals(MARKET_ID));
        assertTrue(openOrders.get(0).getId().equals("23467"));
        
        assertTrue(openOrders.get(0).getType() == OrderType.BUY);
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2014-12-2014T20:04:05");
        
        Date compareDate = new Calendar.Builder()
        			.setDate(2014, 11, 7)
        			.setTimeOfDay(20, 4, 5)
        			.build()
        			.getTime();
		assertTrue(openOrders.get(0).getCreationDate().compareTo(compareDate) == 0);
        assertTrue(openOrders.get(0).getPrice().compareTo(new BigDecimal("0.00000034")) == 0);
        assertTrue(openOrders.get(0).getQuantity().compareTo(new BigDecimal("145.98000000")) == 0);
        assertTrue(openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("169.9676")) == 0);
        BigDecimal compareTotal = openOrders.get(0).getPrice().multiply(openOrders.get(0).getQuantity()).setScale(8, RoundingMode.HALF_EVEN);
		assertTrue(openOrders.get(0).getTotal().compareTo(compareTotal) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingYourOpenOrdersHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_OPEN_ORDERS), anyObject()).
                andThrow(new ExchangeNetworkException("There's an entirely different universe beyond that black hole. " +
                        "A point where time and space as we know it no longer exists. We will be the first to see it, " +
                        "to explore it, to experience it!"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_OPEN_ORDERS), anyObject()).
                andThrow(new IllegalStateException("Nope, I can't make it! My main circuits are gone, my " +
                        "anti-grav-systems blown, and both backup systems are failing"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Latest Market Price tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingLatestMarketPriceSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_MARKET_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET + "/" + MARKET_ID.toUpperCase()).
                andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal latestMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, BigDecimal.ROUND_HALF_UP);
        assertTrue(latestMarketPrice.compareTo(new BigDecimal("0.00006000")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingLatestMarketPriceHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET + "/" + MARKET_ID.toUpperCase()).
                andThrow(new ExchangeNetworkException("They say most of your brain shuts down in cryo-sleep. " +
                        "All but the primitive side, the animal side. No wonder I'm still awake. Transporting me with " +
                        "civilians. Sounded like 40, 40-plus. Heard an Arab voice. Some hoodoo holy man, probably on " +
                        "his way to New Mecca. But what route? What route? I smelt a woman. Sweat, boots, tool belt," +
                        " leather. Prospector type. Free settlers. And they only take the back roads. And here's my " +
                        "real problem. Mr. Johns... the blue-eyed devil. Planning on taking me back to slam... " +
                        "only this time he picked a ghost lane. A long time between stops. A long time for something" +
                        " to go wrong..."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET + "/" + MARKET_ID.toUpperCase()).
                andThrow(new IllegalArgumentException(" All you people are so scared of me. Most days I'd take that as" +
                        " a compliment. But it ain't me you gotta worry about now"));

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
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_BALANCE_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_BALANCE),
                eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

        assertTrue(balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("6700")) == 0);
        assertFalse(balanceInfo.getBalancesAvailable().containsKey("USD"));
        
        assertTrue(balanceInfo.getBalancesOnHold().get("BTC").compareTo(new BigDecimal("2")) == 0);
        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingBalanceInfoHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_BALANCE), eq(null)).
                andThrow(new ExchangeNetworkException(" Don't know, I don't know such stuff. I just do eyes, ju-, ju-," +
                        " just eyes... just genetic design, just eyes. You Nexus, huh? I design your eyes"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_BALANCE), eq(null)).
                andThrow(new IllegalStateException(" I've seen things you people wouldn't believe. Attack ships on fire" +
                        " off the shoulder of Orion. I watched C-beams glitter in the dark near the Tannhauser gate. " +
                        "All those moments will be lost in time... like tears in rain... Time to die"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Exchange Fees for Buy orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeBuyingFeeSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_TRADE_PAIRS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(GET_TRADE_PAIRS))
        		.andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.20")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingExchangeBuyingFeeHandlesTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(GET_TRADE_PAIRS))
        		.andThrow(new ExchangeNetworkException("Right. Well, um, using layman's terms... Use a retaining magnetic" +
                        " field to focus a narrow beam of gravitons - these, in turn, fold space-time consistent with" +
                        " Weyl tensor dynamics until the space-time curvature becomes infinitely large, and you produce" +
                        " a singularity. Now, the singularity..."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingExchangeBuyingFeeHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
                eq(GET_TRADE_PAIRS)).
                andThrow(new IllegalStateException("I created the Event Horizon to reach the stars, but she's gone much," +
                        " much farther than that. She tore a hole in our universe, a gateway to another dimension." +
                        " A dimension of pure chaos. Pure... evil. When she crossed over, she was just a ship." +
                        " But when she came back... she was alive! Look at her, Miller. Isn't she beautiful?"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Exchange Fees for Sell orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeSellingFeeSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_TRADE_PAIRS_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(GET_TRADE_PAIRS))
        		.andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.20")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingExchangeSellingFeeHandlesTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(GET_TRADE_PAIRS))
        		.andThrow(new ExchangeNetworkException("Day 11, Test 37, Configuration 2.0. For lack of a " +
        				"better option, Dummy is still on fire safety."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingExchangeSellingFeeHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(GET_TRADE_PAIRS))
        		.andThrow(new IllegalStateException("What was made public about the Event Horizon - that " +
                "she was a deep space research vessel, that her reactor went critical, and that the ship blew up - " +
                "none of that is true. The Event Horizon is the culmination of a secret government project to create a" +
                " spacecraft capable of faster-than-light flight."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Ticker tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingTickerSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_MARKET_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET + "/" + MARKET_ID.toUpperCase()).
                andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final Ticker ticker = exchangeAdapter.getTicker(MARKET_ID);
        assertTrue(ticker.getLast().compareTo(new BigDecimal("0.00006000")) == 0);
        assertTrue(ticker.getAsk().compareTo(new BigDecimal("0.00006001")) == 0);
        assertTrue(ticker.getBid().compareTo(new BigDecimal("0.02000000")) == 0);
        assertTrue(ticker.getHigh().compareTo(new BigDecimal("0.00006003")) == 0);
        assertTrue(ticker.getLow().compareTo(new BigDecimal("0.00006002")) == 0);
        assertTrue(ticker.getOpen().compareTo(new BigDecimal("0.00000500")) == 0);
        assertTrue(ticker.getVolume().compareTo(new BigDecimal("1000.05639978")) == 0);
        assertNull(ticker.getVwap()); // vwap not supplied by finex
        assertTrue(ticker.getTimestamp() - Long.valueOf(new Date().getTime()) < 1000);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testGettingTickerHandlesExchangeNetworkException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET + "/" + MARKET_ID.toUpperCase()).
                andThrow(new ExchangeNetworkException(" You're born, you live and you die. There are no due overs," +
                        " no second chances to make things right if you frak them up the first time, " +
                        "not in this life anyway."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getTicker(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testGettingTickerHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, GET_MARKET + "/" + MARKET_ID.toUpperCase()).
                andThrow(new IllegalArgumentException("Like I said, you make your choices and you live with them " +
                        "and in the end you are those choices."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingImplNameIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final CryptopiaExchangeAdapter exchangeAdapter = new CryptopiaExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        assertTrue(exchangeAdapter.getImplName().equals("Cryptopia API"));
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Initialisation tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testExchangeAdapterInitialisesSuccessfully() throws Exception {

        PowerMock.replayAll();
        final CryptopiaExchangeAdapter exchangeAdapter = new CryptopiaExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);
        assertNotNull(exchangeAdapter);
        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("public_key")).andReturn(null);
        expect(authenticationConfig.getItem("private_key")).andReturn("your_client_secret");
        PowerMock.replayAll();

        new CryptopiaExchangeAdapter().init(exchangeConfig);
        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("public_key")).andReturn("your_client_key");
        expect(authenticationConfig.getItem("private_key")).andReturn(null);
        PowerMock.replayAll();

        new CryptopiaExchangeAdapter().init(exchangeConfig);
        PowerMock.verifyAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() throws Exception {

        PowerMock.reset(networkConfig);
        expect(networkConfig.getConnectionTimeout()).andReturn(0);
        PowerMock.replayAll();

        new CryptopiaExchangeAdapter().init(exchangeConfig);
        PowerMock.verifyAll();
    }
    
    
    // ------------------------------------------------------------------------------------------------
    //  Request sending tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSendingPublicRequestToExchangeSuccessfully() throws Exception {

        final byte[] encoded = Files.readAllBytes(Paths.get(GET_MARKET_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        final URL url = new URL(PUBLIC_API_BASE_URL + GET_MARKET + "/" + MARKET_ID.toUpperCase());
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_MAKE_NETWORK_REQUEST_METHOD,
                eq(url),
                eq("GET"),
                eq(null),
                eq(new HashMap<>()))
                .andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal lastMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        assertTrue(lastMarketPrice.compareTo(new BigDecimal("0.00006000")) == 0);

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    public void testSendingPublicRequestToExchangeHandlesExchangeNetworkException() throws Exception {

        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        final URL url = new URL(PUBLIC_API_BASE_URL + GET_MARKET + "/" + MARKET_ID.toUpperCase());
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_MAKE_NETWORK_REQUEST_METHOD,
                eq(url),
                eq("GET"),
                eq(null),
                eq(new HashMap<>()))
                .andThrow(new ExchangeNetworkException("There are three types of people in this world: " +
                        "sheep, wolves, and sheepdogs. Some people prefer to believe that evil doesn't exist in the " +
                        "world, and if it ever darkened their doorstep, they wouldn't know how to protect themselves." +
                        " Those are the sheep. Then you've got predators who use violence to prey on the weak. " +
                        "They're the wolves. And then there are those blessed with the gift of aggression, " +
                        "an overpowering need to protect the flock. These men are the rare breed who live to confront " +
                        "the wolf. They are the sheepdog."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test(expected = TradingApiException.class)
    public void testSendingPublicRequestToExchangeHandlesTradingApiException() throws Exception {

        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);

        final URL url = new URL(PUBLIC_API_BASE_URL + GET_MARKET + "/" + MARKET_ID.toUpperCase());
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_MAKE_NETWORK_REQUEST_METHOD,
                eq(url),
                eq("GET"),
                eq(null),
                eq(new HashMap<>()))
                .andThrow(new TradingApiException("If you think that this war isn't changing you you're wrong. " +
                        "You can only circle the flames so long."));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendingAuthenticatedRequestToExchangeSuccessfully() throws Exception {

        final byte[] encoded = Files.readAllBytes(Paths.get(SUBMIT_TRADE_SELL_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));

        final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
        expect(requestParamMap.put("market", MARKET_ID.replace("_", "/").toUpperCase())).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("rate", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("type", "Sell")).andStubReturn(null);

        final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
        expect(requestHeaderMap.put(eq("Authorization"), anyString())).andStubReturn(null);
        expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
        PowerMock.replay(requestHeaderMap); // map needs to be in play early

        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD).andReturn(requestHeaderMap);

        final URL url = new URL(AUTHENTICATED_API_URL + SUBMIT_TRADE);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_MAKE_NETWORK_REQUEST_METHOD,
                eq(url),
                eq("POST"),
                eq(new GsonBuilder().create().toJson(requestParamMap)),
                eq(requestHeaderMap))
                .andReturn(exchangeResponse);

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        assertTrue(orderId.equals("23467"));

        PowerMock.verifyAll();
    }

    @Test(expected = ExchangeNetworkException.class)
    @SuppressWarnings("unchecked")
    public void testSendingAuthenticatedRequestToExchangeHandlesExchangeNetworkException() throws Exception {

        final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
        expect(requestParamMap.put("market", MARKET_ID.replace("_", "/").toUpperCase())).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("rate", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("type", "Sell")).andStubReturn(null);

        final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
        expect(requestHeaderMap.put(eq("Authorization"), anyString())).andStubReturn(null);
        expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
        PowerMock.replay(requestHeaderMap); // map needs to be in play early

        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD).andReturn(requestHeaderMap);

        final URL url = new URL(AUTHENTICATED_API_URL + SUBMIT_TRADE);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_MAKE_NETWORK_REQUEST_METHOD,
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
    public void testSendingAuthenticatedRequestToExchangeHandlesTradingApiException() throws Exception {

        final Map<String, Object> requestParamMap = PowerMock.createPartialMock(HashMap.class, "put");
        expect(requestParamMap.put("market", MARKET_ID.replace("_", "/").toUpperCase())).andStubReturn(null);
        expect(requestParamMap.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("rate", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(SELL_ORDER_PRICE))).andStubReturn(null);
        expect(requestParamMap.put("type", "Sell")).andStubReturn(null);

        final Map<String, String> requestHeaderMap = PowerMock.createPartialMock(HashMap.class, "put");
        expect(requestHeaderMap.put(eq("Authorization"), anyString())).andStubReturn(null);
        expect(requestHeaderMap.put("Content-Type", "application/json")).andStubReturn(null);
        PowerMock.replay(requestHeaderMap); // map needs to be in play early

        final CryptopiaExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptopiaExchangeAdapter.class, MOCKED_MAKE_NETWORK_REQUEST_METHOD, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD,
                MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_CREATE_REQUEST_HEADER_MAP_METHOD).andReturn(requestHeaderMap);

        final URL url = new URL(AUTHENTICATED_API_URL + SUBMIT_TRADE);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_MAKE_NETWORK_REQUEST_METHOD,
                eq(url),
                eq("POST"),
                anyString(),
                eq(requestHeaderMap))
                .andThrow(new TradingApiException("Do you wish me a good morning, or mean that it is a good morning" +
                        " whether I want it or not; or that you feel good this morning; or that it is a morning to" +
                        " be good on?"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

        PowerMock.verifyAll();
    }
}