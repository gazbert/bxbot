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

import com.gazbert.bxbot.core.api.trading.*;
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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * <p>
 * Tests the behaviour of the Cryptsy Exchange Adapter.
 * </p>
 *
 * <p>
 * Coverage could be better: it does not include calling the {@link CryptsyExchangeAdapter#sendRequestToExchange(String, Map)}
 * method; the code in this method is a bloody nightmare to test!
 * </p>
 *
 * TODO Unit test {@link CryptsyExchangeAdapter#sendRequestToExchange(String, Map)} method.
 *
 * @author gazbert
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
@PrepareForTest(CryptsyExchangeAdapter.class)
public class TestCryptsyExchangeAdapter {

    // Valid config location - expected on runtime classpath in the ./src/test/resources folder.
    private static final String VALID_CONFIG_LOCATION = "cryptsy/cryptsy-config.properties";

    // Canned JSON responses from exchange - expected to reside on filesystem relative to project root
    private static final String MARKET_ORDERS_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/marketorders.json";
    private static final String MY_ORDERS_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/myorders.json";
    private static final String GET_INFO_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/getinfo.json";
    private static final String MARKET_TRADES_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/markettrades.json";
    private static final String CREATE_BUY_ORDER_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/createbuyorder.json";
    private static final String CREATE_SELL_ORDER_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/createsellorder.json";
    private static final String CANCEL_ORDER_JSON_RESPONSE = "./src/test/exchange-data/cryptsy/cancelorder.json";

    // Exchange API calls
    private static final String MARKET_ORDERS = "marketorders";
    private static final String MY_ORDERS = "myorders";
    private static final String GET_INFO = "getinfo";
    private static final String MARKET_TRADES = "markettrades";
    private static final String CREATE_ORDER = "createorder";
    private static final String CANCEL_ORDER = "cancelorder";

    // Canned test data
    private static final String MARKET_ID = "3";
    private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("0.01055549");
    private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.1");
    private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("0.01454642");
    private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.03");
    private static final String ORDER_ID_TO_CANCEL = "352324517";


    // Mocked out methods
    private static final String MOCKED_GET_CONFIG_LOCATION_METHOD = "getConfigFileLocation";
    private static final String MOCKED_GET_REQUEST_PARAM_MAP_METHOD = "getRequestParamMap";
    private static final String MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD = "sendRequestToExchange";

    /**
     * Cryptsy exchange Date format: 2014-05-25 20:58:38
     */
    private static final SimpleDateFormat EXCHANGE_DATE_FORMAT = new SimpleDateFormat("y-M-d H:m:s");


    // ------------------------------------------------------------------------------------------------
    //  Create Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCreateOrderToBuyIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(CREATE_BUY_ORDER_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("marketid", MARKET_ID)).andStubReturn(null);
        expect(requestParamMap.put("ordertype", "Buy")).andStubReturn(null);
        expect(requestParamMap.put("quantity", new DecimalFormat("#.########").format(BUY_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("price", new DecimalFormat("#.########").format(BUY_ORDER_PRICE))).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CREATE_ORDER),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        assertTrue(orderId.equals("352324517"));

        PowerMock.verifyAll();
    }

    @Test
    public void testCreateOrderToSellIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(CREATE_SELL_ORDER_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("marketid", MARKET_ID)).andStubReturn(null);
        expect(requestParamMap.put("ordertype", "Sell")).andStubReturn(null);
        expect(requestParamMap.put("quantity", new DecimalFormat("#.########").format(SELL_ORDER_QUANTITY))).andStubReturn(null);
        expect(requestParamMap.put("price", new DecimalFormat("#.########").format(SELL_ORDER_PRICE))).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CREATE_ORDER),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        assertTrue(orderId.equals("352342434"));

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testCreateOrderHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CREATE_ORDER), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Say what??!"));

        PowerMock.replayAll();
        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testCreateOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CREATE_ORDER), anyObject(Map.class)).
                andThrow(new IllegalArgumentException("I admire its purity. A survivor... unclouded by conscience, " +
                        "remorse, or delusions of morality."));

        PowerMock.replayAll();
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
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("orderid", ORDER_ID_TO_CANCEL)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_ORDER),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        // marketId arg not needed for cancelling orders on this exchange.
        final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
        assertTrue(success);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testCancelOrderHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_ORDER), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("The ship will automatically destruct in \"T\" minus five minutes."));

        PowerMock.replayAll();

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testCancelOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_ORDER), anyObject(Map.class)).
                andThrow(new IllegalStateException("Final report of the commercial starship Nostromo, third officer reporting." +
                        " The other members of the crew, Kane, Lambert, Parker, Brett, Ash and Captain Dallas, are dead." +
                        " Cargo and ship destroyed. I should reach the frontier in about six weeks. With a little luck, " +
                        "the network will pick me up. This is Ripley, last survivor of the Nostromo, signing off."));

        PowerMock.replayAll();

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
        final byte[] encoded = Files.readAllBytes(Paths.get(MARKET_ORDERS_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("marketid", MARKET_ID)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MARKET_ORDERS),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));

        assertTrue(marketOrderBook.getBuyOrders().size() == 100);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getType() == OrderType.BUY);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(new BigDecimal("0.01252810")) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(new BigDecimal("5.56996728")) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(new BigDecimal("0.06978111")) == 0);

        assertTrue(marketOrderBook.getSellOrders().size() == 100);
        assertTrue(marketOrderBook.getSellOrders().get(0).getType() == OrderType.SELL);
        assertTrue(marketOrderBook.getSellOrders().get(0).getPrice().compareTo(new BigDecimal( "0.01254025")) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(new BigDecimal("0.07192072")) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getTotal().compareTo(new BigDecimal("0.00090190")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingMarketOrdersHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MARKET_ORDERS), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Nothing but the rain."));

        PowerMock.replayAll();
        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MARKET_ORDERS), anyObject(Map.class)).
                andThrow(new IllegalArgumentException("Error, error, does not compute. I don't have a soul, I have software. If I die, I'm gone."));

        PowerMock.replayAll();
        exchangeAdapter.getMarketOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Your Open Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingYourOpenOrdersSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(MY_ORDERS_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("marketid", MARKET_ID)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MY_ORDERS),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(openOrders.size() == 11);
        assertTrue(openOrders.get(0).getMarketId().equals(MARKET_ID));
        assertTrue(openOrders.get(0).getId().equals("100069536"));
        assertTrue(openOrders.get(0).getType() == OrderType.SELL);
        assertTrue(openOrders.get(0).getCreationDate().getTime() == EXCHANGE_DATE_FORMAT.parse("2014-06-07 15:16:38").getTime());
        assertTrue(openOrders.get(0).getPrice().compareTo(new BigDecimal("0.02500000")) == 0);
        assertTrue(openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.00112363")) == 0);
        assertTrue(openOrders.get(0).getOriginalQuantity().compareTo(new BigDecimal("0.00112363")) == 0);
        assertTrue(openOrders.get(0).getTotal().compareTo(new BigDecimal("0.00002809")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingYourOpenOrdersHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MY_ORDERS), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Have you lost your frakkin' mind?"));

        PowerMock.replayAll();
        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MY_ORDERS), anyObject(Map.class)).
                andThrow(new IllegalStateException("You're born, you live and you die. There are no do-overs, " +
                        "no second chances to make things right if you frak 'em up the first time. Not in this life anyway." +
                        " Like I said, you make your choices and you live with them. And in end you are those choices."));

        PowerMock.replayAll();
        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Latest Market Price tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingLatestMarketPriceSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(MARKET_TRADES_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        final Map<String, String> requestParamMap = PowerMock.createMock(Map.class);
        expect(requestParamMap.put("marketid", MARKET_ID)).andStubReturn(null);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD,
                MOCKED_GET_REQUEST_PARAM_MAP_METHOD);

        PowerMock.expectPrivate(exchangeAdapter, MOCKED_GET_REQUEST_PARAM_MAP_METHOD).andReturn(requestParamMap);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MARKET_TRADES),
                eq(requestParamMap)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final BigDecimal latestMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, BigDecimal.ROUND_HALF_UP);
        assertTrue(latestMarketPrice.compareTo(new BigDecimal("0.01265609")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingLatestMarketPriceHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MARKET_TRADES), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Sir, I'm running every diagnostic we've got. Checking each line of code could take days."));

        PowerMock.replayAll();

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(MARKET_TRADES), anyObject(Map.class)).
                andThrow(new IllegalArgumentException("First team was after the fire suppression system. " +
                        "It tried to override our safety lockouts and vent our air into space. Kill all of us before" +
                        " we could get our EVA suits on. Then the second team just walks in the Aux Fire Control and" +
                        " turns our own guns against the ships trying to rescue us. It was smart. Clever. " +
                        "Almost 2,000 guys bought the farm that day."));

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
        final byte[] encoded = Files.readAllBytes(Paths.get(GET_INFO_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                CryptsyExchangeAdapter.class, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(GET_INFO),
                eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

        // assert some key stuff; we're not testing GSON here.
        assertTrue(balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("0.06385655")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("LTC").compareTo(new BigDecimal("0.04262495")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("DGB").compareTo(new BigDecimal("0")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("DOGE").compareTo(new BigDecimal("1717.00982301")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("FTC").compareTo(new BigDecimal("3.53992665")) == 0);

        assertNull(balanceInfo.getBalancesOnHold().get("BTC"));
        assertNull(balanceInfo.getBalancesOnHold().get("LTC"));
        assertNull(balanceInfo.getBalancesOnHold().get("DGB"));
        assertTrue(balanceInfo.getBalancesOnHold().get("DOGE").compareTo(new BigDecimal("4153.12812459")) == 0);
        assertTrue(balanceInfo.getBalancesOnHold().get("FTC").compareTo(new BigDecimal("2.64070575")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingBalanceInfoHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(GET_INFO), eq(null)).
                andThrow(new ExchangeTimeoutException("Sometimes you gotta roll the hard six."));

        PowerMock.replayAll();
        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final CryptsyExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(CryptsyExchangeAdapter.class,
                MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_REQUEST_TO_EXCHANGE_METHOD, eq(GET_INFO), eq(null)).
                andThrow(new IllegalStateException("When you are in the cockpit, you are in control. " +
                        "It's hard to give it up... All you can do now is wait and hope you didn't make any mistakes." +
                        " Welcome to the big leagues."));

        PowerMock.replayAll();
        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeSellingFeeIsAsExpected() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

        final CryptsyExchangeAdapter exchangeAdapter = new CryptsyExchangeAdapter();
        final BigDecimal sellPercentageFee = exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(sellPercentageFee.compareTo(new BigDecimal("0.0033")) == 0);

        PowerMock.verifyAll();
    }

    @Test
    public void testGettingExchangeBuyingFeeIsAsExpected() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

        final CryptsyExchangeAdapter exchangeAdapter = new CryptsyExchangeAdapter();
        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.0033")) == 0);

        PowerMock.verifyAll();
    }

    @Test
    public void testGettingImplNameIsAsExpected() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

        final CryptsyExchangeAdapter exchangeAdapter = new CryptsyExchangeAdapter();
        assertTrue(exchangeAdapter.getImplName().equals("Cryptsy Authenticated API v1"));

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Initialisation tests - assume config property files are located under src/test/resources
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testExchangeAdapterInitialisesSuccessfully() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();
        final CryptsyExchangeAdapter exchangeAdapter = new CryptsyExchangeAdapter();
        assertNotNull(exchangeAdapter);
        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "cryptsy/missing-public-key-cryptsy-config.properties");
        PowerMock.replayAll();
        new CryptsyExchangeAdapter();
        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPrivateKeyConfigIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "cryptsy/missing-private-key-cryptsy-config.properties");
        PowerMock.replayAll();
        new CryptsyExchangeAdapter();
        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfBuyFeeIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "cryptsy/missing-buy-fee-cryptsy-config.properties");
        PowerMock.replayAll();
        new CryptsyExchangeAdapter();
        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSellFeeIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "cryptsy/missing-sell-fee-cryptsy-config.properties");
        PowerMock.replayAll();
        new CryptsyExchangeAdapter();
        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "cryptsy/missing-timeout-cryptsy-config.properties");
        PowerMock.replayAll();
        new CryptsyExchangeAdapter();
        PowerMock.verifyAll();
    }

    /*
     * Used for making real API calls to the exchange in order to grab JSON responses.
     * Have left this in; it might come in useful.
     * It expects VALID_CONFIG_LOCATION to contain the correct credentials.
     */
//    @Test
    public void runIntegrationTest() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(CryptsyExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

//        final CryptsyExchangeAdapter exchangeAdapter = new CryptsyExchangeAdapter();
//        exchangeAdapter.getImplName();
//        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
//        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
//        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
//        exchangeAdapter.getMarketOrders(MARKET_ID);
//        exchangeAdapter.getYourOpenOrders(MARKET_ID);
//        exchangeAdapter.getBalanceInfo();

//        // Careful here - make sure the SELL_ORDER_PRICE is sensible!
//        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
//        exchangeAdapter.cancelOrder(orderId, MARKET_ID);

        PowerMock.verifyAll();
    }
}
