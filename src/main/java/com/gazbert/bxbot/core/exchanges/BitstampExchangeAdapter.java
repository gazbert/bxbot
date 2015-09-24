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

import com.gazbert.bxbot.core.api.trading.BalanceInfo;
import com.gazbert.bxbot.core.api.trading.ExchangeTimeoutException;
import com.gazbert.bxbot.core.api.trading.MarketOrder;
import com.gazbert.bxbot.core.api.trading.MarketOrderBook;
import com.gazbert.bxbot.core.api.trading.OpenOrder;
import com.gazbert.bxbot.core.api.trading.OrderType;
import com.gazbert.bxbot.core.api.trading.TradingApi;
import com.gazbert.bxbot.core.api.trading.TradingApiException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Exchange Adapter for integrating with the Bitstamp exchange.
 * The Bitstamp API is documented <a href="https://www.bitstamp.net/api/">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Bitstamp, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String)} and {@link #sendAuthenticatedRequestToExchange(String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 *
 * <p>
 * This Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single thread in order to
 * preserve trade execution order. The {@link URLConnection} achieves this by blocking/waiting on the input stream
 * (response) for each API call.
 * </p>
 *
 * <p>
 * The {@link TradingApi} calls will throw a {@link ExchangeTimeoutException} if a network error occurs trying to
 * connect to the exchange. A {@link TradingApiException} is thrown for <em>all</em> other failures.
 * </p>
 *
 * <p>
 * NOTE: Bitstamp requires all price values to be limited to 2 decimal places when creating orders. This adapter
 * truncates any prices with more than 2 decimal places and rounds using {@link java.math.RoundingMode#HALF_EVEN},
 * E.g. 250.176 would be sent to the exchange as 250.18.
 * </p>
 *
 * @author gazbert
 */
public final class BitstampExchangeAdapter implements TradingApi {

    private static final Logger LOG = Logger.getLogger(BitstampExchangeAdapter.class);

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Bitstamp Exchange Adapter. ";

    /**
     * The Authenticated API URI.
     */
    private static final String API_BASE_URL = "https://www.bitstamp.net/api/";

    /**
     * Your Bitstamp API keys and connection timeout config.
     * This file must be on BX-bot's runtime classpath located at: ./resources/bitstamp/bitstamp-config.properties
     */
    private static final String CONFIG_FILE = "bitstamp/bitstamp-config.properties";

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
     * Name of connection timeout property in config file.
     */
    private static final String CONNECTION_TIMEOUT_PROPERTY_NAME = "connection-timeout";

    /**
     * Nonce used for sending authenticated messages to the exchange.
     */
    private static long nonce = 0;

    /**
     * The connection timeout in SECONDS for terminating hung connections to the exchange.
     */
    private int connectionTimeout;

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


    /**
     * Constructor initialises the Exchange Adapter for using the Bitstamp API.
     */
    public BitstampExchangeAdapter() {

        // set the initial nonce used in the secure messaging.
        nonce = System.currentTimeMillis() / 1000;

        loadConfig();
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Bitstamp API Calls adapted to the Trading API.
    // See https://www.bitstamp.net/api/
    // ------------------------------------------------------------------------------------------------

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final String results = sendPublicRequestToExchange("order_book");

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("getMarketOrders() response: " + results);
            }

            final BitstampOrderBook bitstampOrderBook = gson.fromJson(results, BitstampOrderBook.class);

            // adapt BUYs
            final List<MarketOrder> buyOrders = new ArrayList<>();
            final List<List<BigDecimal>> bitstampBuyOrders = bitstampOrderBook.bids;
            for (final List<BigDecimal> order : bitstampBuyOrders) {
                final MarketOrder buyOrder = new MarketOrder(
                        OrderType.BUY,
                        order.get(0), // price
                        order.get(1), // quantity
                        order.get(0).multiply(order.get(1)));
                buyOrders.add(buyOrder);
            }

            // adapt SELLs
            final List<MarketOrder> sellOrders = new ArrayList<>();
            final List<List<BigDecimal>> bitstampSellOrders = bitstampOrderBook.asks;
            for (final List<BigDecimal> order : bitstampSellOrders) {
                final MarketOrder sellOrder = new MarketOrder(
                        OrderType.SELL,
                        order.get(0), // price
                        order.get(1), // quantity
                        order.get(0).multiply(order.get(1)));
                sellOrders.add(sellOrder);
            }

            return new MarketOrderBook(marketId, sellOrders, buyOrders);

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final String results = sendAuthenticatedRequestToExchange("open_orders", null);

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("getYourOpenOrders() response: " + results);
            }

            final BitstampOrderResponse[] myOpenOrders = gson.fromJson(results, BitstampOrderResponse[].class);

            // adapt
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

                final OpenOrder order = new OpenOrder(
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

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeTimeoutException {

        try {
            final Map<String, String> params = new HashMap<>();

            // note we need to limit price to 2 decimal places else exchange will barf
            params.put("price", new DecimalFormat("#.##").format(price));

            // note we need to limit amount to 8 decimal places else exchange will barf
            params.put("amount", new DecimalFormat("#.########").format(quantity));

            final String results;
            if (orderType == OrderType.BUY) {
                // buying BTC
                results = sendAuthenticatedRequestToExchange("buy", params);
            } else if (orderType == OrderType.SELL) {
                // selling BTC
                results = sendAuthenticatedRequestToExchange("sell", params);
            } else {
                final String errorMsg = "Invalid order type: " + orderType
                        + " - Can only be "
                        + OrderType.BUY.getStringValue() + " or "
                        + OrderType.SELL.getStringValue();
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("createOrder() response: " + results);
            }

            final BitstampOrderResponse createOrderResponse = gson.fromJson(results, BitstampOrderResponse.class);
            final long id = createOrderResponse.id;
            if (id == 0) {
                final String errorMsg = "Failed to place order on exchange. Error response: " + results;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            } else {
                return Long.toString(createOrderResponse.id);
            }

        } catch (ExchangeTimeoutException | TradingApiException e) {
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
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeTimeoutException {

        try {
            final Map<String, String> params = new HashMap<>();
            params.put("id", orderId);
            final String results = sendAuthenticatedRequestToExchange("cancel_order", params);

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("cancelOrder() response: " + results);
            }

            // Returns 'true' if order has been found and canceled.
            if (results.equalsIgnoreCase("true")) {
                return true;
            } else {
                final String errorMsg = "Failed to cancel order on exchange. Error response: " + results;
                LOG.error(errorMsg);
                return false;
            }

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final String results = sendPublicRequestToExchange("ticker");

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("getLatestMarketPrice() response: " + results);
            }

            final BitstampTicker bitstampTicker = gson.fromJson(results, BitstampTicker.class);
            return bitstampTicker.last;

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeTimeoutException {

        try {
            final String results = sendAuthenticatedRequestToExchange("balance", null);

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("getBalanceInfo() response: " + results);
            }

            final BitstampBalance balances = gson.fromJson(results, BitstampBalance.class);

            // adapt
            final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
            balancesAvailable.put("BTC", balances.btc_available);
            balancesAvailable.put("USD", balances.usd_available);

            final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
            balancesOnOrder.put("BTC", balances.btc_reserved);
            balancesOnOrder.put("USD", balances.usd_reserved);

            return new BalanceInfo(balancesAvailable, balancesOnOrder);

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeTimeoutException  {

        try {
            final String results = sendAuthenticatedRequestToExchange("balance", null);

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("getPercentageOfBuyOrderTakenForExchangeFee() response: " + results);
            }

            final BitstampBalance balances = gson.fromJson(results, BitstampBalance.class);
            final BigDecimal fee = balances.fee;

            // adapt the % into BigDecimal format
            return fee.divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeTimeoutException  {

        try {
            final String results = sendAuthenticatedRequestToExchange("balance", null);

            // useful to log diff types of error response in JSON response
            if (LOG.isDebugEnabled()) {
                LOG.debug("getPercentageOfSellOrderTakenForExchangeFee() response: " + results);
            }

            final BitstampBalance balances = gson.fromJson(results, BitstampBalance.class);
            final BigDecimal fee = balances.fee;

            // adapt the % into BigDecimal format
            return fee.divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public String getImplName() {
        return "Bitstamp HTTP API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.bitstamp.net/api/
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for holding Bitstamp Balance response from balance API call.
     *
     * @author gazbert
     */
    private static class BitstampBalance {

        // field names map to the JSON arg names
        public BigDecimal btc_reserved;
        public BigDecimal fee;
        public BigDecimal btc_available;
        public BigDecimal usd_reserved;
        public BigDecimal usd_balance;
        public BigDecimal usd_available;

        @Override
        public String toString() {
            return BitstampBalance.class.getSimpleName() + " [btc_reserved=" + btc_reserved
                    + ", fee=" + fee
                    + ", btc_available=" + btc_available
                    + ", usd_reserved=" + usd_reserved
                    + ", usd_balance=" + usd_balance
                    + ", usd_available=" + usd_available + "]";
        }
    }

    /**
     * <p>
     * GSON class for holding Bitstamp Order Book response from order_book API call.
     * </p>
     *
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
     *
     * @author gazbert
     */
    private static class BitstampOrderBook {

        public long timestamp; //unix timestamp date and time
        public List<List<BigDecimal>> bids;
        public List<List<BigDecimal>> asks;

        @Override
        public String toString() {
            return BitstampOrderBook.class.getSimpleName() + " [timestamp=" + timestamp
                    + ", bids=" + bids
                    + ", asks=" + asks
                    + "]";
        }
    }

    /**
     * GSON class for a Bitstamp ticker response.
     *
     * @author gazbert
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
            return BitstampTicker.class.getSimpleName() + " [high=" + high
                    + ", last=" + last
                    + ", timestamp=" + timestamp
                    + ", bid=" + bid
                    + ", vwap=" + vwap
                    + ", volume=" + volume
                    + ", low=" + low
                    + ", ask=" + ask
                    + "]";
        }
    }

    /**
     * GSON class for Bitstamp create order response.
     *
     * @author gazbert
     */
    private static class BitstampOrderResponse {

        public long id;
        public Date datetime;
        public int type; // 0 = buy; 1 = sell
        public BigDecimal price;
        public BigDecimal amount;

        @Override
        public String toString() {
            return BitstampOrderResponse.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", datetime=" + datetime
                    + ", type=" + type
                    + ", price=" + price
                    + ", amount=" + amount
                    + "]";
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
     *
     * @author gazbert
     */
    private class BitstampDateDeserializer implements JsonDeserializer<Date> {
        private SimpleDateFormat bitstampDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
     * Makes a public API call to Bitstamp exchange. Uses HTTP GET.
     * 
     * @param apiMethod the API method to call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private String sendPublicRequestToExchange(String apiMethod) throws ExchangeTimeoutException, TradingApiException {

        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

        try {

            // MUST have the trailing slash even if no params...
            final URL url = new URL(API_BASE_URL + apiMethod + "/");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using following URL for API call: " + url);
            }

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);

            exchangeConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            /*
             * Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
             * Exchange sometimes gets stuck here for ~1 min once every half hour or so. Especially read timeouts.
             * connectionTimeout is in SECONDS and comes from bitstamp-config.properties config.
             */
            final int timeoutInMillis = connectionTimeout * 1000;
            exchangeConnection.setConnectTimeout(timeoutInMillis);
            exchangeConnection.setReadTimeout(timeoutInMillis);

            // Grab the response - we just block here as per Connection API
            final BufferedReader responseInputStream = new BufferedReader(new InputStreamReader(
                    exchangeConnection.getInputStream()));

            // Read the JSON response lines into our response buffer
            String responseLine;
            while ((responseLine = responseInputStream.readLine()) != null) {
                exchangeResponse.append(responseLine);
            }
            responseInputStream.close();

            // return the JSON response string
            return exchangeResponse.toString();

        } catch (MalformedURLException e) {
            final String errorMsg = "Failed to send request to Exchange.";
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);

        } catch (SocketTimeoutException e) {
            final String errorMsg = "Failed to connect to Exchange due to socket timeout.";
            LOG.error(errorMsg, e);
            throw new ExchangeTimeoutException(errorMsg, e);

        } catch (IOException e) {

            /*
             * Exchange sometimes fails with these codes, but recovers by next request...
             */
            if (e.getMessage().contains("502") || e.getMessage().contains("503") || e.getMessage().contains("504")) {
                final String errorMsg = "Failed to connect to Exchange due to 5XX timeout.";
                LOG.error(errorMsg, e);
                throw new ExchangeTimeoutException(errorMsg, e);
            } else {
                final String errorMsg = "Failed to connect to Exchange due to unexpected IO error.";
                LOG.error(errorMsg, e);
                throw new TradingApiException(errorMsg, e);
            }
        } finally {
            if (exchangeConnection != null) {
                exchangeConnection.disconnect();
            }
        }
    }

    /**
     * Makes Authenticated API call to Bitstamp exchange. Uses HTTP POST.
     * 
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private String sendAuthenticatedRequestToExchange(String apiMethod, Map<String, String> params) throws
            ExchangeTimeoutException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Connect to the exchange
        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

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
            mac.update(String.valueOf(nonce).getBytes());
            mac.update(clientId.getBytes());
            mac.update(key.getBytes());

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
            String postData = "";
            for (final String param : params.keySet()) {
                if (postData.length() > 0) {
                    postData += "&";
                }
                //noinspection deprecation
                postData += param + "=" + URLEncoder.encode(params.get(param));
            }

            final URL url = new URL(API_BASE_URL + apiMethod + "/"); // MUST have the trailing slash...
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using following URL for API call: " + url);
            }

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);

            exchangeConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Scare them with her name!!! Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            /*
             * Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
             * Exchange sometimes gets stuck here for ~1 min once every half hour or so. Especially read timeouts.
             * connectionTimeout is in SECONDS and comes from cryptsy-config.properties config.
             */
            final int timeoutInMillis = connectionTimeout * 1000;
            exchangeConnection.setConnectTimeout(timeoutInMillis);
            exchangeConnection.setReadTimeout(timeoutInMillis);

            // POST the request
            final OutputStreamWriter outputPostStream = new OutputStreamWriter(exchangeConnection.getOutputStream());
            outputPostStream.write(postData);
            outputPostStream.close();

            // Grab the response - we just block here as per Connection API
            final BufferedReader responseInputStream = new BufferedReader(new InputStreamReader(
                    exchangeConnection.getInputStream()));

            // Read the JSON response lines into our response buffer
            String responseLine;
            while ((responseLine = responseInputStream.readLine()) != null) {
                exchangeResponse.append(responseLine);
            }
            responseInputStream.close();

            // return the JSON response string
            return exchangeResponse.toString();

        } catch (MalformedURLException e) {
            final String errorMsg = "Failed to send request to Exchange.";
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);

        } catch (SocketTimeoutException e) {
            final String errorMsg = "Failed to connect to Exchange due to socket timeout.";
            LOG.error(errorMsg, e);
            throw new ExchangeTimeoutException(errorMsg, e);

        } catch (IOException e) {

            /*
             * Exchange sometimes fails with these codes, but recovers by next request...
             */
            if (e.getMessage().contains("502") || e.getMessage().contains("503") || e.getMessage().contains("504")) {
                final String errorMsg = "Failed to connect to Exchange due to 5XX timeout.";
                LOG.error(errorMsg, e);
                throw new ExchangeTimeoutException(errorMsg, e);
            } else {
                final String errorMsg = "Failed to connect to Exchange due to unexpected IO error.";
                LOG.error(errorMsg, e);
                throw new TradingApiException(errorMsg, e);
            }
        } finally {
            if (exchangeConnection != null) {
                exchangeConnection.disconnect();
            }
        }
    }

    /**
     * Converts a given byte array to a hex String.
     *
     * @param byteArrayToConvert byte array to convert.
     * @return the string representation of the given byte array.
     * @throws UnsupportedEncodingException if the byte array encoding is not recognised.
     */
    private String toHex(byte[] byteArrayToConvert) throws UnsupportedEncodingException {
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

    /**
     * Loads Exchange Adapter config.
     */
    private void loadConfig() {

        final String configFile = getConfigFileLocation();
        final Properties configEntries = new Properties();
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFile);

        if (inputStream == null) {
            final String errorMsg = "Cannot find Bitstamp config at: " + configFile + " HINT: is it on BX-bot's classpath?";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            configEntries.load(inputStream);

            /*
             * Grab the client id
             */
            clientId = configEntries.getProperty(CLIENT_ID_PROPERTY_NAME);

            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(CLIENT_ID_PROPERTY_NAME + ": " + clientId);
//            }

            if (clientId == null || clientId.length() == 0) {
                final String errorMsg = CLIENT_ID_PROPERTY_NAME + " cannot be null or zero length!"
                        + " HINT: is the value set in the " + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }     
            
            /*
             * Grab the key
             */
            key = configEntries.getProperty(KEY_PROPERTY_NAME);

            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(KEY_PROPERTY_NAME + ": " + key);
//            }

            if (key == null || key.length() == 0) {
                final String errorMsg = KEY_PROPERTY_NAME + " cannot be null or zero length!"
                        + " HINT: is the value set in the " + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            
            /*
             * Grab the secret
             */
            secret = configEntries.getProperty(SECRET_PROPERTY_NAME);

            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(SECRET_PROPERTY_NAME + ": " + secret);
//            }

            if (secret == null || secret.length() == 0) {
                final String errorMsg = SECRET_PROPERTY_NAME + " cannot be null or zero length!"
                        + " HINT: is the value set in the " + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }           
                       
            /*
             * Grab the connection timeout
             */
            connectionTimeout = Integer.parseInt( // will barf if not a number; we want this to fail fast.
                    configEntries.getProperty(CONNECTION_TIMEOUT_PROPERTY_NAME));
            if (connectionTimeout == 0) {
                final String errorMsg = CONNECTION_TIMEOUT_PROPERTY_NAME + " cannot be 0 value!"
                        + " HINT: is the value set in the " + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            if (LOG.isInfoEnabled()) {
                LOG.info(CONNECTION_TIMEOUT_PROPERTY_NAME + ": " + connectionTimeout);
            }

        } catch (IOException e) {
            final String errorMsg = "Failed to load Exchange config: " + configFile;
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                final String errorMsg = "Failed to close input stream for: " + configFile;
                LOG.error(errorMsg, e);
            }
        }
    }

    /**
     * Initialises the GSON layer.
     */
    private void initGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new BitstampDateDeserializer());
        gson = gsonBuilder.create();
    }

    /*
     * Hack for unit-testing config loading ;-o
     */
    private static String getConfigFileLocation() {
        return CONFIG_FILE;
    }
}