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

package com.gazbert.bxbot.core.exchanges;

import com.gazbert.bxbot.core.api.exchange.*;
import com.gazbert.bxbot.core.api.trading.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO Work in progress...
 *
 * <p>
 * Tests the behaviour of the GDAX FIX Exchange Adapter.
 * </p>
 *
 * <p>
 * Coverage could be better: it does not include calling the
 * {@link GdaxFixExchangeAdapter#sendPublicRequestToExchange(String, Map)} and
 * {@link GdaxFixExchangeAdapter#sendAuthenticatedRequestToExchange(String, String, Map)} methods;
 * the code in these methods is a bloody nightmare to test!
 * </p>
 *
 * TODO Unit test {@link GdaxFixExchangeAdapter#sendPublicRequestToExchange(String, Map)} method.
 * TODO Unit test {@link GdaxFixExchangeAdapter#sendAuthenticatedRequestToExchange(String, String, Map)} method.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
@PrepareForTest(GdaxFixExchangeAdapter.class)
public class TestGdaxFixExchangeAdapter {

    // Canned JSON responses from exchange - expected to reside on filesystem relative to project root
    private static final String BOOK_JSON_RESPONSE = "./src/test/exchange-data/gdax/book.json";

    // Canned test data
    private static final String MARKET_ID = "BTC-GBP";
    private static final String ORDER_BOOK_DEPTH_LEVEL = "2"; //  "2" = Top 50 bids and asks (aggregated)
    private static final String ORDER_ID_TO_CANCEL = "3ecf7a12-fc89-4d3d-baef-f158f80b3bd3";

    // Exchange API calls
    private static final String BOOK =  "products/" + MARKET_ID + "/book";

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
    private OtherConfig otherConfig;


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

        otherConfig = PowerMock.createMock(OtherConfig.class);
        expect(otherConfig.getItem("buy-fee")).andReturn("0.25");
        expect(otherConfig.getItem("sell-fee")).andReturn("0.25");

        exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
        expect(exchangeConfig.getAuthenticationConfig()).andReturn(authenticationConfig);
        expect(exchangeConfig.getNetworkConfig()).andReturn(networkConfig);
        expect(exchangeConfig.getOtherConfig()).andReturn(otherConfig);
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Market Orders tests
    // ------------------------------------------------------------------------------------------------

//    @Test
//    public void testGettingMarketOrders() throws Exception {
//
//        // Load the canned response from the exchange
//        final byte[] encoded = Files.readAllBytes(Paths.get(BOOK_JSON_RESPONSE));
//        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
//                new AbstractExchangeAdapter.ExchangeHttpResponse(200, "OK", new String(encoded, StandardCharsets.UTF_8));
//
//        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
//        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
//        expect(requestParamMap.put("level", ORDER_BOOK_DEPTH_LEVEL)).andStubReturn(null);
//
//        // Partial mock so we do not send stuff down the wire
//        final GdaxFixExchangeAdapter exchangeAdapter = PowerMock.createPartialMockAndInvokeDefaultConstructor(
//                GdaxFixExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD,
//                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);
//
//        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
//        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(BOOK),
//                eq(requestParamMap)).andReturn(exchangeResponse);
//
//        PowerMock.replayAll();
//        exchangeAdapter.init(exchangeConfig);
//
//        final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);
//
//        // assert some key stuff; we're not testing GSON here.
//        assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));
//
//        final BigDecimal buyPrice = new BigDecimal("165.87");
//        final BigDecimal buyQuantity = new BigDecimal("16.2373");
//        final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);
//
//        assertTrue(marketOrderBook.getBuyOrders().size() == 50);
//        assertTrue(marketOrderBook.getBuyOrders().get(0).getType() == OrderType.BUY);
//        assertTrue(marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice) == 0);
//        assertTrue(marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity) == 0);
//        assertTrue(marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal) == 0);
//
//        final BigDecimal sellPrice = new BigDecimal("165.96");
//        final BigDecimal sellQuantity = new BigDecimal("24.31");
//        final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);
//
//        assertTrue(marketOrderBook.getSellOrders().size() == 50);
//        assertTrue(marketOrderBook.getSellOrders().get(0).getType() == OrderType.SELL);
//        assertTrue(marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice) == 0);
//        assertTrue(marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity) == 0);
//        assertTrue(marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal) == 0);
//
//        PowerMock.verifyAll();
//    }

//    @Test (expected = ExchangeNetworkException.class )
//    public void testGettingMarketOrdersHandlesExchangeNetworkException() throws Exception {
//
//        // Partial mock so we do not send stuff down the wire
//        final GdaxFixExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
//                GdaxFixExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
//
//        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(BOOK),
//                anyObject(Map.class)).
//                andThrow(new ExchangeNetworkException("Re-verify our range to target... one ping only."));
//
//        PowerMock.replayAll();
//        exchangeAdapter.init(exchangeConfig);
//
//        exchangeAdapter.getMarketOrders(MARKET_ID);
//        PowerMock.verifyAll();
//    }

//    @Test (expected = TradingApiException.class)
//    public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {
//
//        // Partial mock so we do not send stuff down the wire
//        final GdaxFixExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
//                GdaxFixExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
//
//        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(BOOK),
//                anyObject(Map.class)).
//                andThrow(new IllegalArgumentException("Mr. Ambassador, you have nearly a hundred naval vessels" +
//                        " operating in the North Atlantic right now. Your aircraft has dropped enough sonar buoys" +
//                        " so that a man could walk from Greenland to Iceland to Scotland without getting his feet " +
//                        "wet. Now, shall we dispense with the bull?"));
//
//        PowerMock.replayAll();
//        exchangeAdapter.init(exchangeConfig);
//
//        exchangeAdapter.getMarketOrders(MARKET_ID);
//        PowerMock.verifyAll();
//    }

    // ------------------------------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeSellingFeeIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal sellPercentageFee = exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(sellPercentageFee.compareTo(new BigDecimal("0.0025")) == 0);
        PowerMock.verifyAll();
    }

    @Test
    public void testGettingExchangeBuyingFeeIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.0025")) == 0);
        PowerMock.verifyAll();
    }

    @Test
    public void testGettingImplNameIsAsExpected() throws Exception {

        PowerMock.replayAll();
        final ExchangeAdapter exchangeAdapter = new GdaxExchangeAdapter();
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
        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        assertNotNull(exchangeAdapter);
        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPassphraseConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("passphrase")).andReturn(null);
        expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
        expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("passphrase")).andReturn("your_passphrase");
        expect(authenticationConfig.getItem("key")).andReturn(null);
        expect(authenticationConfig.getItem("secret")).andReturn("your_client_secret");
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() throws Exception {

        PowerMock.reset(authenticationConfig);
        expect(authenticationConfig.getItem("passphrase")).andReturn("your_passphrase");
        expect(authenticationConfig.getItem("key")).andReturn("your_client_key");
        expect(authenticationConfig.getItem("secret")).andReturn(null);
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfBuyFeeIsMissing() throws Exception {

        PowerMock.reset(otherConfig);
        expect(otherConfig.getItem("buy-fee")).andReturn("");
        expect(otherConfig.getItem("sell-fee")).andReturn("0.25");
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSellFeeIsMissing() throws Exception {

        PowerMock.reset(otherConfig);
        expect(otherConfig.getItem("buy-fee")).andReturn("0.25");
        expect(otherConfig.getItem("sell-fee")).andReturn("");

        PowerMock.replayAll();
        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() throws Exception {

        PowerMock.reset(networkConfig);
        expect(networkConfig.getConnectionTimeout()).andReturn(0);
        PowerMock.replayAll();

        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
        exchangeAdapter.init(exchangeConfig);

        PowerMock.verifyAll();
    }

    /*
     * Used for making real API calls to the exchange in order to grab JSON responses.
     * Have left this in; it might come in useful.
     * You'll need to change the PASSPHRASE, KEY, SECRET, constants to real-world values.
     */
//    @Test
    public void runIntegrationTest() throws Exception {

//        PowerMock.replayAll();
//        final ExchangeAdapter exchangeAdapter = new GdaxFixExchangeAdapter();
//        exchangeAdapter.init(exchangeConfig);
//        exchangeAdapter.getMarketOrders(MARKET_ID);
//        exchangeAdapter.getImplName();
//        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
//        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
//        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
//        exchangeAdapter.getYourOpenOrders(MARKET_ID);
//        exchangeAdapter.getBalanceInfo();

//        // Careful here - make sure the SELL_ORDER_PRICE is sensible!
//        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
//        exchangeAdapter.cancelOrder(orderId, MARKET_ID);

//        PowerMock.verifyAll();
    }
}