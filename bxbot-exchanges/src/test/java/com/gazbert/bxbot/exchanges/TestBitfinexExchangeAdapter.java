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
 * 测试 Bitfinex 交换适配器的行为。
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
                    "Remote host closed connection during handshake 握手期间远程主机关闭连接");

    private static final String BITFINEX_API_VERSION = "v1";
    private static final String PUBLIC_API_BASE_URL =
            "https://api.bitfinex.com/" + BITFINEX_API_VERSION + "/";
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    private ExchangeConfig exchangeConfig;
    private AuthenticationConfig authenticationConfig;
    private NetworkConfig networkConfig;

    /**
     * Create some exchange config - the TradingEngine would normally do this.
     * 创建一些交换配置 - TradingEngine 通常会这样做。
     */
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
        // 此适配器不需要可选配置
    }

    // --------------------------------------------------------------------------
    //  Create Orders tests
    // 创建订单测试
    // --------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateOrderToBuyIsSuccessful() throws Exception {
        // Load the canned response from the exchange
        // 从交易所加载预设响应
        final byte[] encoded = Files.readAllBytes(Paths.get(ORDER_NEW_BUY_JSON_RESPONSE));
        final AbstractExchangeAdapter.ExchangeHttpResponse exchangeResponse =
                new AbstractExchangeAdapter.ExchangeHttpResponse(
                        200, "OK", new String(encoded, StandardCharsets.UTF_8));

        // Mock out param map so we can assert the contents passed to the transport layer are what we expect.
        // 模拟出参数映射，因此我们可以断言传递给传输层的内容是我们所期望的。
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
        // 部分模拟，因此我们不会通过网络发送东西
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
                                " Marion, don't look at it. Shut your eyes, Marion. Don't look at it, no matter what happens! " +
                                        "玛丽恩，别看它。闭上眼睛，玛丽恩。不要看它，不管发生什么????"));

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
                                        + "thousand years, even you may be worth something. " +
                                        "多么适合你的生活追求。你将成为这个考古发现的永久补充。谁知道？在一个\n" +
                                        " 千年，即使你也可能有价值。"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.BUY, BUY_ORDER_QUANTITY, BUY_ORDER_PRICE);
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Cancel Order tests  取消订单测试
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
        //取消此交易所的订单不需要 marketId arg。
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
                                        + " make sure you do yours" +
                                        "早上好。我是 Meredith Vickers，我的工作是确保你做你的"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        // 取消此交易所的订单不需要 marketId arg。
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
                                "The ring, it chose you. Take it... place the ring on the lantern..." +
                                        "戒指，它选择了你。拿去……把戒指放在灯笼上……"
                                        + " place the ring, speak the oath... great honor... responsibility " +
                                        "戴上戒指，宣誓……荣誉……责任"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        // marketId arg not needed for cancelling orders on this exchange.
        // 取消此交易所的订单不需要 marketId arg。
        exchangeAdapter.cancelOrder(ORDER_ID_TO_CANCEL, null);

        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Get Market Orders tests
    // 获取市价单测试
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
        // assert some key stuff; we're not testing GSON here.
        assertEquals(MARKET_ID, marketOrderBook.getMarketId());

        final BigDecimal buyPrice = new BigDecimal("239.43");
        final BigDecimal buyQuantity = new BigDecimal("5.0");
        final BigDecimal buyTotal = buyPrice.multiply(buyQuantity);

        assertEquals(906, marketOrderBook.getBuyOrders().size()); // 'finex sends them all back!  // 'finex 将它们全部返回！
        assertSame(OrderType.BUY, marketOrderBook.getBuyOrders().get(0).getType());
        assertEquals(0, marketOrderBook.getBuyOrders().get(0).getPrice().compareTo(buyPrice));
        assertEquals(0, marketOrderBook.getBuyOrders().get(0).getQuantity().compareTo(buyQuantity));
        assertEquals(0, marketOrderBook.getBuyOrders().get(0).getTotal().compareTo(buyTotal));

        final BigDecimal sellPrice = new BigDecimal("239.53");
        final BigDecimal sellQuantity = new BigDecimal("6.35595596");
        final BigDecimal sellTotal = sellPrice.multiply(sellQuantity);

        assertEquals(984, marketOrderBook.getSellOrders().size()); // 'finex sends them all back! 'finex 把他们都送回去了！
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
                                        + "everything, and the Can'ts won't try anything." +
                                        "有三种基本类型，皮泽先生:愿意，想要，不能。 愿意完成一切，不会反对一切，而不能尝试任何事情。"));

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
    // 获取您的未结订单测试
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
                                        + "first to see it, to explore it, to experience it!" +
                                        "在那个黑洞之外有一个完全不同的宇宙。我们所知道的时间和空间不再存在的点。" +
                                        "我们将是第一个看到它、探索它、体验它的人！"));

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
                                        + "anti-grav-systems blown, and both backup systems are failing" +
                                        "不行，我做不到！我的主电路消失了，我的反重力系统被炸毁了，两个备用系统都失败了"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getYourOpenOrders(MARKET_ID);
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Get Latest Market Price tests
    // 获取最新的市场价格测试
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
                                "They say most of your brain shuts down in cryo-sleep. " +
                                        "他们说你的大部分大脑都在低温睡眠中关闭。"
                                        + "All but the primitive side, the animal side. No wonder I'm still awake. " +
                                        "除了原始的一面，动物的一面。难怪我还醒着。"
                                        + "Transporting me with civilians. Sounded like 40, 40-plus. Heard an Arab voice." +
                                        "用平民运送我。听起来像 40、40 多。听到阿拉伯的声音。" +
                                        " Some hoodoo holy man, probably on his way to New Mecca. But what route? " +
                                        "某个不祥的圣人，可能正在前往新麦加的路上。但是什么路线？" +
                                        "What route? I smelt a woman. Sweat, boots, tool belt, leather. " +
                                        "什么路线？我闻到了一个女人的味道。汗水，靴子，工具带，皮革。"
                                        + "Prospector type. Free settlers. And they only take the back roads. " +
                                        "探矿者类型。自由定居者。他们只走小路。"
                                        + "And here's my real problem. Mr. Johns... the blue-eyed devil. " +
                                        "这是我真正的问题。约翰斯先生……蓝眼睛的恶魔。"
                                        + "Planning on taking me back to slam... only this time he picked a ghost lane. A long time between stops. A long time for something to go wrong..." +
                                        "打算带我回去打满贯……只是这一次他选择了鬼道。停站之间的时间很长。好久没出事了……"));

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
                                        + " a compliment. But it ain't me you gotta worry about now" +
                                        "你们所有的人都那么害怕我。大多数时候，我会把它当作一种恭维。但现在你要担心的不是我"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Get Balance Info tests
    // 获取余额信息测试
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
        //断言一些关键的东西；我们不是在这里测试 GSON。
        assertEquals(
                0, balanceInfo.getBalancesAvailable().get("BTC").compareTo(new BigDecimal("0.1267283")));
        assertEquals(0, balanceInfo.getBalancesAvailable().get("USD").compareTo(new BigDecimal("0")));

        // Bitfinex does not provide "balances on hold" info.
        // Bitfinex 不提供“暂停余额”信息。
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
                                        + " eyes" +
                                        "不知道，我不知道这样的东西。我只是做眼睛，ju-，ju-，只是眼睛……只是基因设计，只是眼睛。" +
                                        "你Nexus，是吧？" +
                                        "我设计你的眼睛"));

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
                                        + "rain... Time to die." +
                                        "我见过你们人们不会相信的事情。在猎户座的肩膀上攻击着火的船只。" +
                                        "我看到 C 型光束在 Tannhauser 大门附近的黑暗中闪闪发光。所有那些时刻都会随着时间流逝……" +
                                        "就像雨中的泪水……死亡的时间。"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getBalanceInfo();
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Get Exchange Fees for Buy orders tests
    // 获取买单测试的交易所费用
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
                                        + "Now, the singularity..." +
                                        "对。嗯，嗯，用外行的术语... 使用保持磁场来聚焦一束狭窄的引力子——这些反过来，" +
                                        "折叠时空与外尔张量动力学一致，直到时空曲率变得无限大，你产生一个奇点。现在，奇点..."));

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
                                        + "Look at her, Miller. Isn't she beautiful?" +
                                        "我创造了事件视界来到达星星，但她走得远比这要远得多。她在我们的宇宙中撕开了一个洞，一个通往另一个维度的门户。" +
                                        "一个纯粹混沌的维度。纯粹的邪恶。当她穿越时，她只是一艘船。但当她回来的时候……她还活着！" +
                                        "看看她，米勒。她不是很漂亮吗？"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(MARKET_ID);
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Get Exchange Fees for Sell orders tests
    // 获取卖单测试的交易所费用
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
                                        + " on fire safety." +
                                        "第 11 天，测试 37，配置 2.0。由于没有更好的选择，Dummy 仍然处于防火安全状态。"));

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
                                        + "flight." +
                                        "关于事件视界号的公开内容——她是一艘深空研究船，她的反应堆变得危急，飞船爆炸了——这些都不是真的。 Event Horizon是秘密政府项目的高潮，" +
                                        "该项目旨在创造一种能够超光速飞行的航天器。"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(MARKET_ID);
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Get Ticker tests
    // 获取 Ticker 测试
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
                                        + "life anyway." +
                                        "你出生，你活着，你死去。如果你第一次把事情搞砸了，就没有第二次机会把事情做好，反正这辈子也不会。"));

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
                                        + " those choices." +
                                        "就像我说的，你做出你的选择，你和他们一起生活，最终你就是那些选择。"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.getLatestMarketPrice(MARKET_ID);
        PowerMock.verifyAll();
    }

    // --------------------------------------------------------------------------
    //  Non Exchange visiting tests
    // 非交易所访问测试
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
    // 初始化测试
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
    // 请求发送测试
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
                                        + "They are the sheepdog." +
                                        "这个世界上只有三种人：羊、狼和牧羊犬。有些人宁愿相信世界上不存在邪恶，如果它曾经让他们的家门口变暗，他们将不知道如何保护自己。那些是羊。然后你就有了使用暴力捕食弱者的掠食者。" +
                                        "他们是狼。还有那些天生具有侵略性的人，一种保护羊群的强烈需求。这些人是与狼对抗的稀有品种。他们是牧羊犬。"));

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
                                        + "You can only circle the flames so long." +
                                        "如果你认为这场战争没有改变你，那你就错了。你只能绕着火焰转那么久。"));

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
                .andThrow(new ExchangeNetworkException("The road goes ever on and on...这条路一直在继续……"));

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
                                        + "be good on?" +
                                        "你是祝我早上好，还是说不管我愿不愿意，今天早上好；或者你今天早上感觉很好；或者说今天早上好？"));

        PowerMock.replayAll();
        exchangeAdapter.init(exchangeConfig);

        exchangeAdapter.createOrder(MARKET_ID, OrderType.SELL, SELL_ORDER_QUANTITY, SELL_ORDER_PRICE);

        PowerMock.verifyAll();
    }
}
