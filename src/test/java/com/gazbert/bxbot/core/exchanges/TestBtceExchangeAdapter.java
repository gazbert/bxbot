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
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Tests the behaviour of the BTC-e Exchange Adapter.
 * </p>
 *
 * <p>
 * Coverage could be better: it does not include calling the {@link BtceExchangeAdapter#sendPublicRequestToExchange(String, String)}
 * and {@link BtceExchangeAdapter#sendAuthenticatedRequestToExchange(String, Map)} methods; the code in these methods
 * is a bloody nightmare to test!
 * </p>
 *
 * TODO Unit test {@link BtceExchangeAdapter#sendPublicRequestToExchange(String, String)} method.
 * TODO Unit test {@link BtceExchangeAdapter#sendAuthenticatedRequestToExchange(String, Map)} method.
 *
 * @author gazbert
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
@PrepareForTest(BtceExchangeAdapter.class)
public class TestBtceExchangeAdapter {

    // Valid config location - expected on runtime classpath in the ./src/test/resources folder.
    private static final String VALID_CONFIG_LOCATION = "btce/btce-config.properties";

    // Canned JSON responses from exchange - expected to reside on filesystem relative to project root
    private static final String DEPTH_JSON_RESPONSE = "./src/test/exchange-data/btce/depth.json";
    private static final String ACTIVE_ORDERS_JSON_RESPONSE = "./src/test/exchange-data/btce/ActiveOrders.json";
    private static final String GET_INFO_JSON_RESPONSE = "./src/test/exchange-data/btce/getInfo.json";
    private static final String TICKER_JSON_RESPONSE = "./src/test/exchange-data/btce/ticker.json";
    private static final String TRADE_BUY_JSON_RESPONSE = "./src/test/exchange-data/btce/tradeBuyResponse.json";
    private static final String TRADE_SELL_JSON_RESPONSE = "./src/test/exchange-data/btce/tradeSellResponse.json";
    private static final String CANCEL_ORDER_JSON_RESPONSE = "./src/test/exchange-data/btce/CancelOrder.json";
    private static final String FEE_JSON_RESPONSE = "./src/test/exchange-data/btce/fee.json";

    // Exchange API calls
    private static final String DEPTH = "depth";
    private static final String ACTIVE_ORDERS = "ActiveOrders";
    private static final String GET_INFO = "getInfo";
    private static final String TICKER = "ticker";
    private static final String TRADE = "Trade";
    private static final String CANCEL_ORDER = "CancelOrder";
    private static final String FEE = "fee";

    // Canned test data
    private static final String MARKET_ID = "btc_usd";
    private static final BigDecimal BUY_ORDER_PRICE = new BigDecimal("200.10");
    private static final BigDecimal BUY_ORDER_QUANTITY = new BigDecimal("0.05");
    private static final BigDecimal SELL_ORDER_PRICE = new BigDecimal("244.176");
    private static final BigDecimal SELL_ORDER_QUANTITY = new BigDecimal("0.05");
    private static final String ORDER_ID_TO_CANCEL = "804754278";

    // Mocked out methods
    private static final String MOCKED_GET_CONFIG_LOCATION_METHOD = "getConfigFileLocation";
    private static final String MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD = "sendAuthenticatedRequestToExchange";
    private static final String MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD = "sendPublicRequestToExchange";


    // ------------------------------------------------------------------------------------------------
    //  Create Orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCreateOrderToBuyIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(TRADE_BUY_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(TRADE), anyObject(Map.class)).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        assertTrue(orderId.equals("804754279"));

        PowerMock.verifyAll();
    }

    @Test
    public void testCreateOrderToSellIsSuccessful() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(TRADE_SELL_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(TRADE), anyObject(Map.class)).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        final String orderId = exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);
        assertTrue(orderId.equals("804754278"));

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testCreateOrderHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(TRADE), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Movement. Signal's clean. Range, 20 meters."));

        PowerMock.replayAll();

        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testCreateOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(TRADE), anyObject(Map.class)).
                andThrow(new IllegalArgumentException("I wanna introduce you to a personal friend of mine. " +
                        "This is an M41A pulse rifle. Ten millimeter with over-and-under thirty millimeter pump action grenade launcher."));

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

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_ORDER), anyObject(Map.class)).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        // marketId arg not needed for cancelling orders on this exchange.
        final boolean success = exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);
        assertTrue(success);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testCancelOrderHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_ORDER), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("When you eliminate the impossible, whatever remains, however improbable, must be the truth."));

        PowerMock.replayAll();

        // marketId arg not needed for cancelling orders on this exchange.
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testCancelOrderHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(CANCEL_ORDER), anyObject(Map.class)).
                andThrow(new IllegalStateException("When 900 years old, you reach… Look as good, you will not."));

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
        final byte[] encoded = Files.readAllBytes(Paths.get(DEPTH_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(DEPTH), anyObject(Map.class)).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        final MarketOrderBook marketOrderBook = exchangeAdapter.getMarketOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(marketOrderBook.getMarketId().equals(MARKET_ID));

        final BigDecimal buyPrice = new BigDecimal("224.89");
        final BigDecimal buyQuantity = new BigDecimal("7.42804721");
        final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

        assertTrue(marketOrderBook.getBuyOrders().size() == 150);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getType() == OrderType.BUY);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity) == 0);
        assertTrue(marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal) == 0);

        final BigDecimal sellPrice = new BigDecimal("224.933");
        final BigDecimal sellQuantity = new BigDecimal("0.011045");
        final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

        assertTrue(marketOrderBook.getSellOrders().size() == 150);
        assertTrue(marketOrderBook.getSellOrders().get(0).getType() == OrderType.SELL);
        assertTrue(marketOrderBook.getSellOrders().get(0).getPrice().compareTo(sellPrice) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getQuantity().compareTo(sellQuantity) == 0);
        assertTrue(marketOrderBook.getSellOrders().get(0).getTotal().compareTo(sellTotal) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingMarketOrdersHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(DEPTH), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("I say we take off and nuke the entire site from orbit. It's the only way to be sure."));

        PowerMock.replayAll();

        exchangeAdapter.getMarketOrders(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingMarketOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(DEPTH), anyObject(Map.class)).
                andThrow(new IllegalArgumentException("All right, sweethearts, what are you waiting for? Breakfast in bed?" +
                        " Another glorious day in the Corps! A day in the Marine Corps is like a day on the farm." +
                        " Every meal's a banquet! Every paycheck a fortune! Every formation a parade! I LOVE the Corps"));

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
        final byte[] encoded = Files.readAllBytes(Paths.get(ACTIVE_ORDERS_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(ACTIVE_ORDERS),
                anyObject(Map.class)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final List<OpenOrder> openOrders = exchangeAdapter.getYourOpenOrders(MARKET_ID);

        // assert some key stuff; we're not testing GSON here.
        assertTrue(openOrders.size() == 2);
        assertTrue(openOrders.get(0).getMarketId().equals(MARKET_ID));
        assertTrue(openOrders.get(0).getId().equals("804666275"));
        assertTrue(openOrders.get(0).getType() == OrderType.SELL);
        assertTrue(openOrders.get(0).getCreationDate().getTime() == new Date(1440956890).getTime());
        assertTrue(openOrders.get(0).getPrice().compareTo(new BigDecimal("234.88400000")) == 0);
        assertTrue(openOrders.get(0).getQuantity().compareTo(new BigDecimal("0.03000000")) == 0);
        assertTrue(openOrders.get(0).getTotal().compareTo(openOrders.get(0).getQuantity().multiply(openOrders.get(0).getPrice()))== 0);

        // the values below are not provided by BTC-e
        assertNull(openOrders.get(0).getOriginalQuantity());

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingYourOpenOrdersHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(ACTIVE_ORDERS), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Alien life form. Looks like it's been dead a long time. " +
                        "Fossilized. Looks like it's growing out of the chair."));

        PowerMock.replayAll();

        exchangeAdapter.getYourOpenOrders(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingYourOpenOrdersHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(ACTIVE_ORDERS), anyObject(Map.class)).
                andThrow(new IllegalStateException("Two protons expelled at each coupling site creates the mode of force, " +
                        "the embryo becomes a fish though we don't enter until a plate, we're here to experience, " +
                        "evolve the little toe, atrophy, don't ask me how, I'll be dead in a thousand light years, " +
                        "thank you, thank you, genesis turns to its source, reduction occurs step wise though the" +
                        " essence is all one, end of line. FTL system check. Diagnostic functions within parameters repeats the harlequin, the agony exquisite, the colors run the path of ashes..."));

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
        final byte[] encoded = Files.readAllBytes(Paths.get(TICKER_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), anyObject(Map.class)).
                andReturn(exchangeResponse);

        PowerMock.replayAll();

        final BigDecimal latestMarketPrice = exchangeAdapter.getLatestMarketPrice(MARKET_ID).setScale(8, BigDecimal.ROUND_HALF_UP);
        assertTrue(latestMarketPrice.compareTo(new BigDecimal("225.155")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingLatestMarketPriceHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("You're what? You're still collating? I find that hard to believe..."));

        PowerMock.replayAll();

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingLatestMarketPriceHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(TICKER), anyObject(Map.class)).
                andThrow(new IllegalArgumentException("Don't pander to me, kid. One tiny crack in the hull and our blood " +
                        "boils in thirteen seconds. Solar flare might crop up, cook us in our seats. And wait'll you're" +
                        " sitting pretty with a case of Andorian shingles, see if you're still so relaxed when your " +
                        "eyeballs are bleeding. Space is disease and danger wrapped in darkness and silence."));

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
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_INFO),
                eq(null)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final BalanceInfo balanceInfo = exchangeAdapter.getBalanceInfo();

        // assert some key stuff; we're not testing GSON here.
        assertTrue(balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("0.64614021")) == 0);
        assertTrue(balanceInfo.getBalancesAvailable().get("USD").compareTo(new BigDecimal("0.00013757")) == 0);

        // BTC-e does not provide "balances on hold" info.
        assertNull(balanceInfo.getBalancesOnHold().get("BTC"));
        assertNull(balanceInfo.getBalancesOnHold().get("LTC"));

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingBalanceInfoHandlesExchangeTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_INFO), eq(null)).
                andThrow(new ExchangeTimeoutException("And so say we all!"));

        PowerMock.replayAll();

        exchangeAdapter.getBalanceInfo();

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingBalanceInfoHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_AUTHENTICATED_REQUEST_TO_EXCHANGE_METHOD, eq(GET_INFO), eq(null)).
                andThrow(new IllegalStateException("There is a clause in the contract which specifically states any " +
                        "systematized transmission indicating a possible intelligent origin must be investigated."));

        PowerMock.replayAll();

        exchangeAdapter.getBalanceInfo();

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Exchange Fees for Buy orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeBuyingFeeSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(FEE_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(FEE),
                anyObject(Map.class)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.002")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingExchangeBuyingFeeHandlesTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(FEE), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("These aren’t the droids you’re looking for..."));

        PowerMock.replayAll();

        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingExchangeBuyingFeeHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(FEE), anyObject(Map.class)).
                andThrow(new IllegalStateException("Why you stuck-up, half-witted, scruffy-looking nerf-herder!"));

        PowerMock.replayAll();

        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Get Exchange Fees for Sell orders tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingExchangeSellingFeeSuccessfully() throws Exception {

        // Load the canned response from the exchange
        final byte[] encoded = Files.readAllBytes(Paths.get(FEE_JSON_RESPONSE));
        final String exchangeResponse = new String(encoded, StandardCharsets.UTF_8);

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMockAndInvokeDefaultConstructor(
                BtceExchangeAdapter.class, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(FEE),
                anyObject(Map.class)).andReturn(exchangeResponse);

        PowerMock.replayAll();

        final BigDecimal buyPercentageFee = exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        assertTrue(buyPercentageFee.compareTo(new BigDecimal("0.002")) == 0);

        PowerMock.verifyAll();
    }

    @Test (expected = ExchangeTimeoutException.class )
    public void testGettingExchangeSellingFeeHandlesTimeoutException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(FEE), anyObject(Map.class)).
                andThrow(new ExchangeTimeoutException("Aren't you a little short for a storm trooper?"));

        PowerMock.replayAll();

        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);

        PowerMock.verifyAll();
    }

    @Test (expected = TradingApiException.class)
    public void testGettingExchangeSellingFeeHandlesUnexpectedException() throws Exception {

        // Partial mock so we do not send stuff down the wire
        final BtceExchangeAdapter exchangeAdapter =  PowerMock.createPartialMock(BtceExchangeAdapter.class,
                MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD);
        PowerMock.expectPrivate(exchangeAdapter, MOCKED_SEND_PUBLIC_REQUEST_TO_EXCHANGE_METHOD, eq(FEE), anyObject(Map.class)).
                andThrow(new IllegalStateException("He’s holding a thermal detonator!"));

        PowerMock.replayAll();

        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGettingImplNameIsAsExpected() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

        final BtceExchangeAdapter exchangeAdapter = new BtceExchangeAdapter();
        assertTrue(exchangeAdapter.getImplName().equals("BTC-e API v1"));

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    //  Initialisation tests - assume config property files are located under src/test/resources
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testExchangeAdapterInitialisesSuccessfully() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

        final BtceExchangeAdapter exchangeAdapter = new BtceExchangeAdapter();
        assertNotNull(exchangeAdapter);

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfPublicKeyConfigIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "btce/missing-public-key-btce-config.properties");
        PowerMock.replayAll();

        new BtceExchangeAdapter();

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfSecretConfigIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "btce/missing-secret-btce-config.properties");
        PowerMock.replayAll();

        new BtceExchangeAdapter();

        PowerMock.verifyAll();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testExchangeAdapterThrowsExceptionIfTimeoutConfigIsMissing() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(
                "btce/missing-timeout-btce-config.properties");
        PowerMock.replayAll();

        new BtceExchangeAdapter();

        PowerMock.verifyAll();
    }

    /*
     * Used for making real API calls to the exchange in order to grab JSON responses.
     * Have left this in; it might come in useful.
     * It expects VALID_CONFIG_LOCATION to contain the correct credentials.
     */
//    @Test
    public void testCallingExchangeToGetJson() throws Exception {

        // Partial mock the adapter so we can manipulate config location
        PowerMock.mockStaticPartial(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD);
        PowerMock.expectPrivate(BtceExchangeAdapter.class, MOCKED_GET_CONFIG_LOCATION_METHOD).andReturn(VALID_CONFIG_LOCATION);
        PowerMock.replayAll();

//        final BtceExchangeAdapter exchangeAdapter = new BtceExchangeAdapter();
//        exchangeAdapter.getImplName();
//        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
//        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
//        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
//        exchangeAdapter.getYourOpenOrders(MARKET_ID);
//        exchangeAdapter.getBalanceInfo();

        PowerMock.verifyAll();
    }
}
