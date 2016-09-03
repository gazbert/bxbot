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

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.trading.api.*;
import com.google.common.base.MoreObjects;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Dummy Exchange adapter used to keep the bot up and running for engine and strategy testing.
 * <p>
 * Makes public calls to the BTC-e exchange. It does not trade. All private (authenticated) requests are stubbed.
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
    private static final String AUTHENTICATED_API_URL = "https://btc-e.com/tapi";

    /**
     * The Public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://btc-e.com/api/3/";

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in BTC-e Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Name of public key property in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret property in config file.
     */
    private static final String SECRET_PROPERTY_NAME = "secret";

    /**
     * Nonce used for sending authenticated messages to the exchange.
     */
    private static long nonce = 0;

    /**
     * Used to indicate if we have initialised the MAC authentication protocol
     */
    private boolean initializedMACAuthentication = false;

    /**
     * The public key used in MAC authentication.
     */
    private String key = "";

    /**
     * The secret used in MAC authentication.
     */
    private String secret = "";

    /**
     * Provides the "Message Authentication Code" (MAC) algorithm used for the secure messaging layer.
     * Used to encrypt the hash of the entire message with the private key to ensure message integrity.
     */
    private Mac mac;

    /**
     * GSON engine used for parsing JSON in BTC-e API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LOG.info(() -> "About to initialise BTC-e ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);

        nonce = System.currentTimeMillis() / 1000; // set the initial nonce used in the secure messaging.
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // BTC-e API Calls adapted to the Trading API.
    // See https://btc-e.com/api/documentation and https://btc-e.com/page/2
    // ------------------------------------------------------------------------------------------------

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendPublicRequestToExchange("depth", marketId);
            LOG.debug(() -> "Market Orders response: " + response);

            final BtceMarketOrderBookWrapper marketOrderWrapper = gson.fromJson(response.getPayload(),
                    BtceMarketOrderBookWrapper.class);

            final List<MarketOrder> buyOrders = new ArrayList<>();
            final List<List<BigDecimal>> btceBuyOrders = marketOrderWrapper.orderBook.bids;
            for (final List<BigDecimal> order : btceBuyOrders) {
                final MarketOrder buyOrder = new MarketOrder(
                        OrderType.BUY,
                        order.get(0), // price
                        order.get(1), // quantity
                        order.get(0).multiply(order.get(1)));
                buyOrders.add(buyOrder);
            }

            final List<MarketOrder> sellOrders = new ArrayList<>();
            final List<List<BigDecimal>> btceSellOrders = marketOrderWrapper.orderBook.asks;
            for (final List<BigDecimal> order : btceSellOrders) {
                final MarketOrder sellOrder = new MarketOrder(
                        OrderType.SELL,
                        order.get(0), // price
                        order.get(1), // quantity
                        order.get(0).multiply(order.get(1)));
                sellOrders.add(sellOrder);
            }

            return new MarketOrderBook(marketId, sellOrders, buyOrders);

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
            final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker", marketId);
            LOG.debug(() -> "Latest Market Price response: " + response);

            final BtceTickerWrapper btceTicker = gson.fromJson(response.getPayload(), BtceTickerWrapper.class);
            return btceTicker.ticker.last;

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {

        final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();
        balancesAvailable.put("BTC", new BigDecimal("1.0"));
        final BalanceInfo balanceInfo = new BalanceInfo(balancesAvailable, null);
        return balanceInfo;
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendPublicRequestToExchange("fee", marketId);
            LOG.debug(() -> "Buy Fee response: " + response);

            final BtceFees fees = gson.fromJson(response.getPayload(), BtceFees.class);

            // adapt the % into BigDecimal format
            return fees.get(marketId).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendPublicRequestToExchange("fee", marketId);
            LOG.debug(() -> "Sell Fee response: " + response);

            final BtceFees fees = gson.fromJson(response.getPayload(), BtceFees.class);

            // adapt the % into BigDecimal format
            return fees.get(marketId).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public String getImplName() {
        return "Dummy Test Adapter - based on BTC-e API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://btc-e.com/api/documentation and  https://btc-e.com/page/2
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON base class for API call requests and responses.
     */
    private static class BtceMessageBase {
        // field names map to the JSON arg names
        public int success;
        public String error;

        BtceMessageBase() {
            error = "";
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("success", success)
                    .add("error", error)
                    .toString();
        }
    }

    /**
     * GSON class for wrapping BTC-e Info response from getInfo() API call.
     */
    private static class BtceInfoWrapper extends BtceMessageBase {
        @SerializedName("return")
        public BtceInfo info;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("info", info)
                    .toString();
        }
    }

    /**
     * GSON class for holding BTC-e info response from getInfo() API call.
     */
    private static class BtceInfo {

        // field names map to the JSON arg names
        public BtceFunds funds;
        public BtceRights rights;
        public int transaction_count;
        public int open_orders;
        public String server_time;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("funds", funds)
                    .add("rights", rights)
                    .add("transaction_count", transaction_count)
                    .add("open_orders", open_orders)
                    .add("server_time", server_time)
                    .toString();
        }
    }

    /**
     * GSON class for holding fund balances - basically a GSON enabled map.
     * Keys into map are 'usd' and 'btc' for the bits we're interested in.
     * Values are BigDec's.
     */
    private static class BtceFunds extends HashMap<String, BigDecimal> {
        private static final long serialVersionUID = 5516523641453401953L;
    }

    /**
     * GSON class for holding access rights - basically a GSON enabled map.
     * Keys are 'info' and 'trade'. We expect both to be set to 1 (i.e. true).
     */
    private static class BtceRights extends HashMap<String, BigDecimal> {
        private static final long serialVersionUID = 5353335214767688903L;
    }

    /**
     * GSON class for the Order Book wrapper.
     */
    private static class BtceMarketOrderBookWrapper extends BtceMessageBase {
        // field names map to the JSON arg names)
        @SerializedName("btc_usd")
        BtceOrderBook orderBook;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("orderBook", orderBook)
                    .toString();
        }
    }

    /**
     * GSON class for holding BTC-e Order Book response from depth API call.
     * <p>
     * JSON looks like:
     * <pre>
     *   {
     *     "asks":[[567.711,1.97874609],[568.226,0.018],[569,1.192759],[569.234,0.04312865],[576.418,28.68]],
     *     "bids":[[567.37,0.03607374],[566.678,0.04321508],[566.615,0.5],[566.502,0.02489897],[557.8,0.363]]
     *   }
     * </pre>
     * Each is a list of open orders and each order is represented as a list of price and amount.
     */
    private static class BtceOrderBook {
        // field names map to the JSON arg names
        public List<List<BigDecimal>> bids;
        public List<List<BigDecimal>> asks;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("bids", bids)
                    .add("asks", asks)
                    .toString();
        }
    }

    /**
     * GSON class for holding BTC-e create order response wrapper from Trade API call.
     */
    private static class BtceCreateOrderResponseWrapper extends BtceMessageBase {
        @SerializedName("return")
        BtceCreateOrderResponse orderResponse;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("orderResponse", orderResponse)
                    .toString();
        }
    }

    /**
     * GSON class for holding BTC-e create order response details from Trade API call.
     */
    private static class BtceCreateOrderResponse extends BtceCreateOrderResponseWrapper {

        public BigDecimal received;
        public BigDecimal remains;
        public long order_id;
        public BtceFunds funds;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("received", received)
                    .add("remains", remains)
                    .add("order_id", order_id)
                    .add("funds", funds)
                    .toString();
        }
    }

    /**
     * <p>
     * GSON class for holding BTC-e open order response wrapper from ActiveOrders API call.
     * </p>
     * <p>
     * <p>
     * JSON response when we don't have open orders is:
     * <pre>
     * {"success":0,"error":"no orders"}
     * </pre>
     * Error? WTF?!
     * </p>
     * <p>
     * <p>
     * JSON response when we have open orders is:
     * <pre>
     * {
     *   "success":1,
     *   "return":{
     *     "253356918":{
     *       "pair":"btc_usd","type":"sell","amount":0.01000000,"rate":641.00000000,
     *       "timestamp_created":1401653697,"status":0
     *     }
     *   }
     * }
     * </pre>
     * </p>
     */
    private static class BtceOpenOrderResponseWrapper extends BtceMessageBase {

        @SerializedName("return")
        public BtceOpenOrders openOrders;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("openOrders", openOrders)
                    .toString();
        }
    }

    /**
     * GSON class for holding open orders - basically a GSON enabled map.
     * Keys into map is the orderId.
     */
    private static class BtceOpenOrders extends HashMap<Long, BtceOpenOrder> {
        private static final long serialVersionUID = 2298531396505507808L;
    }

    /**
     * GSON class for holding BTC-e open order response details from ActiveOrders API call.
     */
    private static class BtceOpenOrder {

        public String pair; // market id
        public String type; // buy|sell
        public BigDecimal amount;
        public BigDecimal rate; // price
        public Long timestamp_created;
        public int status; // ??

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pair", pair)
                    .add("type", type)
                    .add("amount", amount)
                    .add("rate", rate)
                    .add("timestamp_created", timestamp_created)
                    .add("status", status)
                    .toString();
        }
    }

    /**
     * <p>
     * GSON class for holding BTC-e cancel order response.
     * <pre>
     * {
     *   "success":1,
     *   "return":{
     *     "order_id":343154,
     *     "funds":{
     *       "usd":325,
     *       "btc":24.998,
     *       "sc":121.998,
     *       "ltc":0,
     *       "ruc":0,
     *       "nmc":0
     *     }
     *   }
     * }
     * </pre>
     * </p>
     * Unusual API - why the frak do I want to know about funds when I cancel an order?
     */
    private static class BtceCancelledOrderWrapper extends BtceMessageBase {

        @SerializedName("return")
        BtceCancelledOrder cancelledOrder;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("cancelledOrder", cancelledOrder)
                    .toString();
        }
    }

    /**
     * GSON class for cancelled order response.
     */
    private static class BtceCancelledOrder {

        public long order_id;
        public BtceFunds funds;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("order_id", order_id)
                    .add("funds", funds)
                    .toString();
        }
    }

    /**
     * GSON class for a BTC-e ticker response wrapper.
     */
    private static class BtceTickerWrapper {
        // TODO fix this - can GSON take wildcard types or will I have to code my own deserializer?
        @SerializedName("btc_usd")
        public BtceTicker ticker;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("ticker", ticker)
                    .toString();
        }
    }

    /**
     * GSON class for a BTC-e ticker response.
     */
    private static class BtceTicker {

        public BigDecimal high;
        public BigDecimal low;
        public BigDecimal avg;
        public BigDecimal vol;
        public BigDecimal vol_cur;
        public BigDecimal last;
        public BigDecimal buy;
        public BigDecimal sell;
        public long updated;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("high", high)
                    .add("low", low)
                    .add("avg", avg)
                    .add("vol", vol)
                    .add("vol_cur", vol_cur)
                    .add("last", last)
                    .add("buy", buy)
                    .add("sell", sell)
                    .add("updated", updated)
                    .toString();
        }
    }

    /**
     * GSON class for holding fee response.
     * <p>
     * Basically a GSON enabled map.
     * Keys into map is the market id.
     * Values are BigDec's.
     */
    private static class BtceFees extends HashMap<String, BigDecimal> {
        private static final long serialVersionUID = 5516523641423401953L;
    }

    /**
     * Deserializer needed for BTC-e open orders API call response:
     */
    private class OpenOrdersDeserializer implements JsonDeserializer<BtceOpenOrders> {
        public BtceOpenOrders deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            final BtceOpenOrders openOrders = new BtceOpenOrders();

            if (json.isJsonObject()) {
                final JsonObject jsonObject = json.getAsJsonObject();
                for (Entry<String, JsonElement> jsonOrder : jsonObject.entrySet()) {
                    final Long orderId = new Long(jsonOrder.getKey()); // will barf hard n fast if NaN
                    final BtceOpenOrder openOrder = context.deserialize(jsonOrder.getValue(), BtceOpenOrder.class);
                    openOrders.put(orderId, openOrder);
                }
            }
            return openOrders;
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the BTC-e exchange.
     *
     * @param apiMethod the API method to call.
     * @param resource  to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod, String resource) throws TradingApiException,
            ExchangeNetworkException {

        // Request headers required by Exchange
        final Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        try {

            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + "/" + resource);
            return sendNetworkRequest(url, "GET", null, requestHeaders);

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * Initialises the secure messaging layer
     * Sets up the MAC to safeguard the data we send to the exchange.
     * We fail hard n fast if any of this stuff blows.
     */
    private void initSecureMessageLayer() {

        // Setup the MAC
        try {
            final SecretKeySpec keyspec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
            mac = Mac.getInstance("HmacSHA512");
            mac.init(keyspec);
            initializedMACAuthentication = true;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA512 installed?";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        } catch (InvalidKeyException e) {
            final String errorMsg = "Failed to setup MAC security. Secret key seems invalid!";
            LOG.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Config methods
    // ------------------------------------------------------------------------------------------------

    private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {

        final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
        key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
        secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
    }

    // ------------------------------------------------------------------------------------------------
    //  Util methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Initialises the GSON layer.
     */
    private void initGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(BtceOpenOrders.class, new OpenOrdersDeserializer());
        gson = gsonBuilder.create();
    }

    /*
     * Hack for unit-testing map params passed to transport layer.
     */
    private Map<String, String> getRequestParamMap() {
        return new HashMap<>();
    }
}