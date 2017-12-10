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
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.trading.api.*;
import com.google.common.base.MoreObjects;
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>
 * Exchange Adapter for integrating with the Bitstamp exchange. It uses v2 of the Bitstamp API;
 * it is documented <a href="https://www.bitstamp.net/api/">here</a>.
 * </p>
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Bitstamp, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String)} and {@link #sendAuthenticatedRequestToExchange(String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 * <p>
 * Note: the Bitstamp API returns 200 OK response even for errors. The response payload will be different though, e.g.
 * <pre>
 *     {"error": "Order not found"}
 * </pre>
 * </p>
 * <p>
 * This Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single thread in order to
 * preserve trade execution order. The {@link URLConnection} achieves this by blocking/waiting on the input stream
 * (response) for each API call.
 * </p>
 * <p>
 * The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error occurs trying to
 * connect to the exchange. A {@link TradingApiException} is thrown for <em>all</em> other failures.
 * </p>
 * <p>
 * NOTE: Bitstamp requires all price values to be limited to 2 decimal places when creating orders. This adapter
 * truncates any prices with more than 2 decimal places and rounds using {@link java.math.RoundingMode#HALF_EVEN},
 * E.g. 250.176 would be sent to the exchange as 250.18.
 * </p>
 *
 * @author gazbert
 * @since 1.0
 */
public final class BitstampExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

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
     * Name of client id property in config file.
     */
    private static final String CLIENT_ID_PROPERTY_NAME = "client-id";

    /**
     * Name of key property in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret property in config file.
     */
    private static final String SECRET_PROPERTY_NAME = "secret";

    /**
     * Nonce used for sending authenticated messages to the exchange.
     */
    private long nonce = 0;

    /**
     * Used to indicate if we have initialised the MAC authentication protocol.
     */
    private boolean initializedMACAuthentication = false;

    /**
     * The client id.
     */
    private String clientId = "";

    /**
     * The key used in the MAC message.
     */
    private String key = "";

    /**
     * The secret used for signing MAC message.
     */
    private String secret = "";

    /**
     * Provides the "Message Authentication Code" (MAC) algorithm used for the secure messaging layer.
     * Used to encrypt the hash of the entire message with the private key to ensure message integrity.
     */
    private Mac mac;

    /**
     * GSON engine used for parsing JSON in Bitstamp API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LOG.info(() -> "About to initialise Bitstamp ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);

        nonce = System.currentTimeMillis() / 1000; // set the initial nonce used in the secure messaging.
        initSecureMessageLayer();
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

        try {
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("open_orders/" + marketId, null);
            LOG.debug(() -> "Open Orders response: " + response);

            final BitstampOrderResponse[] myOpenOrders = gson.fromJson(response.getPayload(), BitstampOrderResponse[].class);

            // No need to filter on marketId; exchange does this for us.
            final List<OpenOrder> ordersToReturn = new ArrayList<>();
            for (final BitstampOrderResponse openOrder : myOpenOrders) {
                OrderType orderType;
                if (openOrder.type == 0) {
                    orderType = OrderType.BUY;
                } else if (openOrder.type == 1) {
                    orderType = OrderType.SELL;
                } else {
                    throw new TradingApiException(
                            "Unrecognised order type received in getYourOpenOrders(). Value: " + openOrder.type);
                }

                final OpenOrder order = new OpenOrderImpl(
                        Long.toString(openOrder.id),
                        openOrder.datetime,
                        marketId,
                        orderType,
                        openOrder.price,
                        openOrder.amount,
                        null, // orig_quantity - not provided by stamp :-(
                        openOrder.price.multiply(openOrder.amount) // total - not provided by stamp :-(
                );

                ordersToReturn.add(order);
            }
            return ordersToReturn;

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeNetworkException {

        try {
            final Map<String, String> params = getRequestParamMap();

            // note we need to limit price to 2 decimal places else exchange will barf
            params.put("price", new DecimalFormat("#.##", getDecimalFormatSymbols()).format(price));

            // note we need to limit amount to 8 decimal places else exchange will barf
            params.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(quantity));

            final ExchangeHttpResponse response;
            if (orderType == OrderType.BUY) {
                // buying BTC
                response = sendAuthenticatedRequestToExchange("buy/" + marketId, params);
            } else if (orderType == OrderType.SELL) {
                // selling BTC
                response = sendAuthenticatedRequestToExchange("sell/" + marketId, params);
            } else {
                final String errorMsg = "Invalid order type: " + orderType
                        + " - Can only be "
                        + OrderType.BUY.getStringValue() + " or "
                        + OrderType.SELL.getStringValue();
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            LOG.debug(() -> "Create Order response: " + response);

            final BitstampOrderResponse createOrderResponse = gson.fromJson(response.getPayload(), BitstampOrderResponse.class);
            final long id = createOrderResponse.id;
            if (id == 0) {
                final String errorMsg = "Failed to place order on exchange. Error response: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            } else {
                return Long.toString(createOrderResponse.id);
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    /*
     * marketId is not needed for cancelling orders on this exchange.
     */
    @Override
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeNetworkException {

        try {
            final Map<String, String> params = getRequestParamMap();
            params.put("id", orderId);

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("cancel_order", params);
            LOG.debug(() -> "Cancel Order response: " + response);

            final BitstampCancelOrderResponse cancelOrderResponse = gson.fromJson(response.getPayload(), BitstampCancelOrderResponse.class);
            if (!orderId.equals(String.valueOf(cancelOrderResponse.id))) {
                final String errorMsg = "Failed to cancel order on exchange. Error response: " + response;
                LOG.error(errorMsg);
                return false;
            } else {
                return true;
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
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

        try {
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("balance", null);
            LOG.debug(() -> "Balance Info response: " + response);

            final BitstampBalance balances = gson.fromJson(response.getPayload(), BitstampBalance.class);

            final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
            balancesAvailable.put("BTC", balances.btc_available);
            balancesAvailable.put("USD", balances.usd_available);
            balancesAvailable.put("EUR", balances.eur_available);
            balancesAvailable.put("LTC", balances.ltc_available);
            balancesAvailable.put("XRP", balances.xrp_available);

            final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
            balancesOnOrder.put("BTC", balances.btc_reserved);
            balancesOnOrder.put("USD", balances.usd_reserved);
            balancesOnOrder.put("EUR", balances.eur_reserved);
            balancesOnOrder.put("LTC", balances.ltc_reserved);
            balancesOnOrder.put("XRP", balances.xrp_reserved);

            return new BalanceInfoImpl(balancesAvailable, balancesOnOrder);

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("balance", null);
            LOG.debug(() -> "Buy Fee response: " + response);

            final BitstampBalance balances = gson.fromJson(response.getPayload(), BitstampBalance.class);

            // Ouch!
            final Class<?> clazz = balances.getClass();
            final Field[] fields = clazz.getDeclaredFields();
            for (final Field field : fields) {
                if (field.getName().startsWith(marketId)) {
                    final BigDecimal fee = (BigDecimal) field.get(balances);
                    // adapt the % into BigDecimal format
                    return fee.divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
                }
            }

            final String errorMsg = "Unable to map marketId to currency balances returned from the Exchange. "
                    + "MarketId: " + marketId + " BitstampBalances: " + balances;
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);

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
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("balance", null);
            LOG.debug(() -> "Sell Fee response: " + response);

            final BitstampBalance balances = gson.fromJson(response.getPayload(), BitstampBalance.class);

            // Ouch!
            final Class<?> clazz = balances.getClass();
            final Field[] fields = clazz.getDeclaredFields();
            for (final Field field : fields) {
                if (field.getName().startsWith(marketId)) {
                    final BigDecimal fee = (BigDecimal) field.get(balances);
                    // adapt the % into BigDecimal format
                    return fee.divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
                }
            }

            final String errorMsg = "Unable to map marketId to currency balances returned from the Exchange. "
                    + "MarketId: " + marketId + " BitstampBalances: " + balances;
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public String getImplName() {
        return "Bitstamp HTTP API v2";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.bitstamp.net/api/
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for holding Bitstamp Balance response from balance API call.
     * Updated for v2 API - markets correct as of 25 June 2017.
     * Well this is fun - why not return a map of reserved, map of available, etc... ;-(
     */
    private static class BitstampBalance {

        // field names map to the JSON arg names
        public BigDecimal btc_available;
        public BigDecimal btc_balance;
        public BigDecimal btc_reserved;
        public BigDecimal btceur_fee;
        public BigDecimal btcusd_fee;

        public BigDecimal eur_available;
        public BigDecimal eur_balance;
        public BigDecimal eur_reserved;
        public BigDecimal eurusd_fee;

        public BigDecimal ltc_available;
        public BigDecimal ltc_balance;
        public BigDecimal ltc_reserved;
        public BigDecimal ltcbtc_fee;
        public BigDecimal ltceur_fee;
        public BigDecimal ltcusd_fee;

        public BigDecimal usd_available;
        public BigDecimal usd_balance;
        public BigDecimal usd_reserved;

        public BigDecimal xrp_available;
        public BigDecimal xrp_balance;
        public BigDecimal xrp_reserved;
        public BigDecimal xrpbtc_fee;
        public BigDecimal xrpeur_fee;
        public BigDecimal xrpusd_fee;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("btc_available", btc_available)
                    .add("btc_balance", btc_balance)
                    .add("btc_reserved", btc_reserved)
                    .add("btceur_fee", btceur_fee)
                    .add("btcusd_fee", btcusd_fee)
                    .add("eur_available", eur_available)
                    .add("eur_balance", eur_balance)
                    .add("eur_reserved", eur_reserved)
                    .add("eurusd_fee", eurusd_fee)
                    .add("ltc_available", ltc_available)
                    .add("ltc_balance", ltc_balance)
                    .add("ltc_reserved", ltc_reserved)
                    .add("ltcbtc_fee", ltcbtc_fee)
                    .add("ltceur_fee", ltceur_fee)
                    .add("ltcusd_fee", ltcusd_fee)
                    .add("usd_available", usd_available)
                    .add("usd_balance", usd_balance)
                    .add("usd_reserved", usd_reserved)
                    .add("xrp_available", xrp_available)
                    .add("xrp_balance", xrp_balance)
                    .add("xrp_reserved", xrp_reserved)
                    .add("xrpbtc_fee", xrpbtc_fee)
                    .add("xrpeur_fee", xrpeur_fee)
                    .add("xrpusd_fee", xrpusd_fee)
                    .toString();
        }
    }

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
     * GSON class for Bitstamp create order response.
     */
    private static class BitstampOrderResponse {

        public long id;
        public Date datetime;
        public int type; // 0 = buy; 1 = sell
        public BigDecimal price;
        public BigDecimal amount;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("datetime", datetime)
                    .add("type", type)
                    .add("price", price)
                    .add("amount", amount)
                    .toString();
        }
    }

    /**
     * GSON class for Bitstamp cancel order response.
     */
    private static class BitstampCancelOrderResponse {

        public long id;
        public BigDecimal price;
        public BigDecimal amount;
        public int type; // 0 = buy; 1 = sell

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("price", price)
                    .add("amount", amount)
                    .add("type", type)
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

        try {
            final URL url = new URL(API_BASE_URL + apiMethod);
            return makeNetworkRequest(url, "GET", null, new HashMap<>());

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * Makes authenticated API call to Bitstamp exchange.
     *
     * @param apiMethod the API method to call.
     * @param params    the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(String apiMethod, Map<String, String> params) throws
            ExchangeNetworkException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {

            // Setup common params for the API call
            if (params == null) {
                params = new HashMap<>();
            }

            params.put("key", key);
            params.put("nonce", Long.toString(nonce));

            // Create MAC message for signature
            // message = nonce + client_id + api_key
            mac.reset(); // force reset
            mac.update(String.valueOf(nonce).getBytes("UTF-8"));
            mac.update(clientId.getBytes("UTF-8"));
            mac.update(key.getBytes("UTF-8"));

            /*
             * Signature is a HMAC-SHA256 encoded message containing: nonce, client ID and API key.
             * The HMAC-SHA256 code must be generated using a secret key that was generated with your API key.
             * This code must be converted to it's hexadecimal representation (64 uppercase characters).
             *
             * signature = hmac.new(API_SECRET, msg=message, digestmod=hashlib.sha256).hexdigest().upper()
             */
            final String signature = toHex(mac.doFinal()).toUpperCase();
            params.put("signature", signature);

            // increment ready for next call...
            nonce++;

            // Build the URL with query param args in it
            final StringBuilder postData = new StringBuilder("");
            for (final Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() > 0) {
                    postData.append("&");
                }
                postData.append(param.getKey());
                postData.append("=");
                postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }

            // Request headers required by Exchange
            final Map<String, String> requestHeaders = getHeaderParamMap();
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

            final URL url = new URL(API_BASE_URL + apiMethod + "/"); // MUST have the trailing slash else exchange barfs...
            return makeNetworkRequest(url, "POST", postData.toString(), requestHeaders);

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * Converts a given byte array to a hex String.
     *
     * @param byteArrayToConvert byte array to convert.
     * @return the string representation of the given byte array.
     */
    private String toHex(byte[] byteArrayToConvert) {
        final StringBuilder hexString = new StringBuilder();

        for (final byte aByte : byteArrayToConvert) {
            hexString.append(String.format("%02x", aByte & 0xff));
        }
        return hexString.toString();
    }

    /**
     * Initialises the secure messaging layer
     * Sets up the MAC to safeguard the data we send to the exchange.
     * We fail hard n fast if any of this stuff blows.
     */
    private void initSecureMessageLayer() {

        // Setup the MAC
        try {
            final SecretKeySpec keyspec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            mac = Mac.getInstance("HmacSHA256");
            mac.init(keyspec);
            initializedMACAuthentication = true;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA256 installed?";
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
        clientId = getAuthenticationConfigItem(authenticationConfig, CLIENT_ID_PROPERTY_NAME);
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
        gsonBuilder.registerTypeAdapter(Date.class, new BitstampDateDeserializer());
        gson = gsonBuilder.create();
    }

    /*
     * Hack for unit-testing map params passed to transport layer.
     */
    private Map<String, String> getRequestParamMap() {
        return new HashMap<>();
    }

    /*
     * Hack for unit-testing header params passed to transport layer.
     */
    private Map<String, String> getHeaderParamMap() {
        return new HashMap<>();
    }

    /*
     * Hack for unit-testing transport layer.
     */
    private ExchangeHttpResponse makeNetworkRequest(URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
            throws TradingApiException, ExchangeNetworkException {
        return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
    }
}