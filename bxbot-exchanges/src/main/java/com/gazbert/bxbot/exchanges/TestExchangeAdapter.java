/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.trading.api.*;
import com.google.common.base.MoreObjects;
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Dummy Exchange adapter used to keep the bot up and running for engine and strategy testing.
 * <p>
 * Makes public calls to the Bitstamp exchange. It does not trade. All private (authenticated) requests are stubbed.
 * <p>
 * Might be handy for 'dry' testing your algos.
 *
 * @author gazbert
 * @since 1.0
 */
public final class TestExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * The Authenticated API URI.
     */
    private static final String API_BASE_URL = "https://www.bitstamp.net/api/v2/";

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Bitstamp Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * GSON engine used for parsing JSON in Bitstamp API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LOG.info(() -> "About to initialise Bitstamp ExchangeConfig: " + config);
        setNetworkConfig(config);
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Bitstamp API Calls adapted to the Trading API.
    // See https://www.bitstamp.net/api/
    // ------------------------------------------------------------------------------------------------

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendPublicRequestToExchange("order_book/" + marketId);
            LOG.debug(() -> "Market Orders response: " + response);

            final BitstampOrderBook bitstampOrderBook = gson.fromJson(response.getPayload(), BitstampOrderBook.class);

            final List<MarketOrder> buyOrders = new ArrayList<>();
            final List<List<BigDecimal>> bitstampBuyOrders = bitstampOrderBook.bids;
            for (final List<BigDecimal> order : bitstampBuyOrders) {
                final MarketOrder buyOrder = new MarketOrderImpl(
                        OrderType.BUY,
                        order.get(0), // price
                        order.get(1), // quantity
                        order.get(0).multiply(order.get(1)));
                buyOrders.add(buyOrder);
            }

            final List<MarketOrder> sellOrders = new ArrayList<>();
            final List<List<BigDecimal>> bitstampSellOrders = bitstampOrderBook.asks;
            for (final List<BigDecimal> order : bitstampSellOrders) {
                final MarketOrder sellOrder = new MarketOrderImpl(
                        OrderType.SELL,
                        order.get(0), // price
                        order.get(1), // quantity
                        order.get(0).multiply(order.get(1)));
                sellOrders.add(sellOrder);
            }

            return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeNetworkException {
        return new ArrayList<>();
    }

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeNetworkException {
        return "DUMMY_ORDER_ID: " + UUID.randomUUID().toString();
    }

    /*
     * marketId is not needed for cancelling orders on this exchange.
     */
    @Override
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeNetworkException {
        return true;
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws TradingApiException, ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker/" + marketId);
            LOG.debug(() -> "Latest Market Price response: " + response);

            final BitstampTicker bitstampTicker = gson.fromJson(response.getPayload(), BitstampTicker.class);
            return bitstampTicker.last;

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {

        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        balancesAvailable.put("BTC", new BigDecimal("2.0"));
        balancesAvailable.put("USD", new BigDecimal("100.0"));
        balancesAvailable.put("EUR", new BigDecimal("100.0"));
        balancesAvailable.put("LTC", new BigDecimal("100.0"));
        balancesAvailable.put("XRP", new BigDecimal("100.0"));

        final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
        balancesOnOrder.put("BTC", new BigDecimal("2.0"));
        balancesOnOrder.put("USD", new BigDecimal("100.0"));
        balancesOnOrder.put("EUR", new BigDecimal("100.0"));
        balancesOnOrder.put("LTC", new BigDecimal("100.0"));
        balancesOnOrder.put("XRP", new BigDecimal("100.0"));

        return new BalanceInfoImpl(balancesAvailable, balancesOnOrder);
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {
        return new BigDecimal("0.025"); // 0.25%
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {
        return new BigDecimal("0.025"); // 0.25%
    }

    @Override
    public String getImplName() {
        return "Dummy Test Adapter - based on Bitstamp HTTP API v2";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.bitstamp.net/api/
    // ------------------------------------------------------------------------------------------------

    /**
     * <p>
     * GSON class for holding Bitstamp Order Book response from order_book API call.
     * </p>
     * <p>
     * <p>
     * JSON looks like:
     * <pre>
     * {
     *   "timestamp": "1400943488",
     *   "bids": [["521.86", "0.00017398"], ["519.58", "0.25100000"], ["0.01", "38820.00000000"]],
     *   "asks": [["521.88", "10.00000000"], ["522.00", "310.24504478"], ["522.13", "0.02852084"]]
     * }
     * </pre>
     * </p>
     * Each is a list of open orders and each order is represented as a list of price and amount.
     */
    private static class BitstampOrderBook {

        public long timestamp; //unix timestamp date and time
        public List<List<BigDecimal>> bids;
        public List<List<BigDecimal>> asks;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("timestamp", timestamp)
                    .add("bids", bids)
                    .add("asks", asks)
                    .toString();
        }
    }

    /**
     * GSON class for a Bitstamp ticker response.
     */
    private static class BitstampTicker {

        public BigDecimal high;
        public BigDecimal last;
        public long timestamp;
        public BigDecimal bid;
        public BigDecimal vwap;
        public BigDecimal volume;
        public BigDecimal low;
        public BigDecimal ask;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("high", high)
                    .add("last", last)
                    .add("timestamp", timestamp)
                    .add("bid", bid)
                    .add("vwap", vwap)
                    .add("volume", volume)
                    .add("low", low)
                    .add("ask", ask)
                    .toString();
        }
    }

    /**
     * Deserializer needed because stamp Date format is different in open_order response and causes default GSON parsing to barf:
     * <pre>
     * [main] 2014-05-25 20:51:31,074 ERROR BitstampExchangeAdapter  - Failed to parse a Bitstamp date
     * java.text.ParseException: Unparseable date: "2014-05-25 19:50:32"
     * at java.text.DateFormat.parse(DateFormat.java:357)
     * at com.gazbert.bxbot.adapter.BitstampExchangeAdapter$DateDeserializer.deserialize(BitstampExchangeAdapter.java:596)
     * at com.gazbert.bxbot.adapter.BitstampExchangeAdapter$DateDeserializer.deserialize(BitstampExchangeAdapter.java:1)
     * at com.google.gson.TreeTypeAdapter.read(TreeTypeAdapter.java:58)
     * </pre>
     */
    private static class BitstampDateDeserializer implements JsonDeserializer<Date> {
        private final SimpleDateFormat bitstampDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            Date dateFromBitstamp = null;
            if (json.isJsonPrimitive()) {
                try {
                    dateFromBitstamp = bitstampDateFormat.parse(json.getAsString());
                } catch (ParseException e) {
                    final String errorMsg = "DateDeserializer failed to parse a Bitstamp date!";
                    LOG.error(errorMsg, e);
                    throw new JsonParseException(errorMsg, e);
                }
            }
            return dateFromBitstamp;
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to Bitstamp exchange.
     *
     * @param apiMethod the API method to call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod) throws ExchangeNetworkException, TradingApiException {

        // Request headers required by Exchange
        final Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        try {

            // MUST have the trailing slash even if no params... else exchange barfs!
            final URL url = new URL(API_BASE_URL + apiMethod + "/");
            return sendNetworkRequest(url, "GET", null, requestHeaders);

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Util methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Initialises the GSON layer.
     */
    private void initGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new BitstampDateDeserializer());
        gson = gsonBuilder.create();
    }
}