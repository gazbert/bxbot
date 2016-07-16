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

package com.gazbert.bxbot.core.exchanges;

import com.gazbert.bxbot.core.api.exchange.AuthenticationConfig;
import com.gazbert.bxbot.core.api.exchange.ExchangeAdapter;
import com.gazbert.bxbot.core.api.exchange.ExchangeConfig;
import com.gazbert.bxbot.core.api.trading.*;
import com.gazbert.bxbot.core.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * TODO Work in progress...
 *
 * <p>
 * Exchange Adapter for integrating with the Kraken exchange.
 * The Kraken API is documented <a href="https://www.kraken.com/en-gb/help/api">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Kraken, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String, Map)} and {@link #sendAuthenticatedRequestToExchange(String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 *
 * <p>
 * TODO API version
 * </p>
 *
 * <p>
 * TODO Fees
 * </p>
 *
 * <p>
 * TODO Call rate limit
 * </p>
 *
 * <p>
 * TODO update this...
 * Kraken markets assets (currencies) can be referenced using their ISO4217-A3 names in the case of ISO registered names,
 * their 3 letter commonly used names in the case of unregistered names, or their X-ISO4217-A3 code (see http://www.ifex-project.org/).
 * E.g. you can access the XBT/USD market using either of the following ids: 'XBTUSD' or 'XXBTZUSD'. The exchange always
 * returns market id back in the latter format, i.e. 'XXBTZUSD'.
 * </p>
 *
 * <p>
 * The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single thread in order to
 * preserve trade execution order. The {@link URLConnection} achieves this by blocking/waiting on the input stream
 * (response) for each API call.
 * </p>
 *
 * <p>
 * The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error occurs trying to
 * connect to the exchange. A {@link TradingApiException} is thrown for <em>all</em> other failures.
 * </p>
 *
 * @author gazbert
 * @since 16/07/2016
 */
public final class KrakenExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

    private static final Logger LOG = Logger.getLogger(KrakenExchangeAdapter.class);

    /**
     * The version of the Kraken API being used.
     */
    private static final String KRAKEN_API_VERSION = "0";

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.kraken.com/" + KRAKEN_API_VERSION + "/public/";

    /**
     * The Authenticated API URI.
     */
    private static final String AUTHENTICATED_API_URL = "https://api.kraken.com/" + KRAKEN_API_VERSION + "/private/";

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Kraken Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Error message for when API call to get Market Orders fails.
     */
    private static final String FAILED_TO_GET_MARKET_ORDERS = "Failed to get market order book from exchange. Details: ";

    /**
     * Name of PUBLIC key prop in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret prop in config file.
     */
    private static final String SECRET_PROPERTY_NAME = "secret";

    /**
     * Nonce used for sending authenticated messages to the exchange.
     */
    private static long nonce = 0;

    /**
     * Used to indicate if we have initialised the MAC authentication protocol.
     */
    private boolean initializedMACAuthentication = false;

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
     * GSON engine used for parsing JSON in Kraken API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LogUtils.log(LOG, Level.INFO, () -> "About to initialise Kraken ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);

        nonce = System.currentTimeMillis() / 1000; // set the initial nonce used in the secure messaging.
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Kraken API Calls adapted to the Trading API.
    // See https://www.kraken.com/en-gb/help/api
    // ------------------------------------------------------------------------------------------------

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        try {

            final Map<String, String> params = getRequestParamMap();
            params.put("pair", marketId);

            final ExchangeHttpResponse response = sendPublicRequestToExchange("Depth", params);
            LogUtils.log(LOG, Level.DEBUG, () -> "getMarketOrders() response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final KrakenMarketOrderBookResponse orderBookResponse = gson.fromJson(response.getPayload(),
                        KrakenMarketOrderBookResponse.class);

                final List<String> errors = orderBookResponse.error;
                if (errors == null || errors.isEmpty()) {

                    // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
                    final KrakenMarketOrderBookResult krakenOrderBookResult = orderBookResponse.result;

                    // TODO Exchange returns the marketId as the key into the result map - being defensive here
                    // or just get grab first value we get regardless? What do the other API calls return?
                    final KrakenOrderBook krakenOrderBook = krakenOrderBookResult.get(marketId);
                    if (krakenOrderBook != null) {

                        // adapt BUYs
                        final List<MarketOrder> buyOrders = new ArrayList<>();
                        for (KrakenMarketOrder krakenBuyOrder : krakenOrderBook.bids) {
                            final MarketOrder buyOrder = new MarketOrder(
                                    OrderType.BUY,
                                    krakenBuyOrder.get(0),
                                    krakenBuyOrder.get(1),
                                    krakenBuyOrder.get(0).multiply(krakenBuyOrder.get(1)));
                            buyOrders.add(buyOrder);
                        }

                        // adapt SELLs
                        final List<MarketOrder> sellOrders = new ArrayList<>();
                        for (KrakenMarketOrder krakenSellOrder : krakenOrderBook.asks) {
                            final MarketOrder sellOrder = new MarketOrder(
                                    OrderType.SELL,
                                    krakenSellOrder.get(0),
                                    krakenSellOrder.get(1),
                                    krakenSellOrder.get(0).multiply(krakenSellOrder.get(1)));
                            sellOrders.add(sellOrder);
                        }

                        return new MarketOrderBook(marketId, sellOrders, buyOrders);

                    } else {
                        final String errorMsg =  FAILED_TO_GET_MARKET_ORDERS + response;
                        LOG.error(errorMsg);
                        throw new TradingApiException(errorMsg);
                    }

                } else {
                    final String errorMsg = FAILED_TO_GET_MARKET_ORDERS + response;
                    LOG.error(errorMsg);
                    throw new TradingApiException(errorMsg);
                }

            } else {
                final String errorMsg = FAILED_TO_GET_MARKET_ORDERS + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public String getImplName() {
        return "Kraken API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.kraken.com/en-gb/help/api
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for Market Order Book response.
     */
    private static class KrakenMarketOrderBookResponse {

        // field names map to the JSON arg names
        public List<String> error;
        public KrakenMarketOrderBookResult result;

        @Override
        public String toString() {
            return KrakenMarketOrderBookResponse.class.getSimpleName()
                    + " ["
                    + "error=" + error
                    + ", result=" + result
                    + "]";
        }
    }

    /**
     * GSON class that wraps Market Order Book.
     */
    private static class KrakenMarketOrderBookResult extends HashMap<String, KrakenOrderBook> {
    }

    /**
     * GSON class for a Market Order Book.
     */
    private static class KrakenOrderBook {

        // field names map to the JSON arg names
        public KrakenMarketOrder[] bids;
        public KrakenMarketOrder[] asks;

        @Override
        public String toString() {
            return KrakenOrderBook.class.getSimpleName()
                    + " ["
                    + "bids=" + Arrays.toString(bids)
                    + ", asks=" + Arrays.toString(asks)
                    + "]";
        }
    }

    /**
     * GSON class for holding Market Orders.
     * First element in array is price, second element is amount, 3rd is UNIX time.
     */
    private static class KrakenMarketOrder extends ArrayList<BigDecimal> {
        private static final long serialVersionUID = -4959711260742077759L;
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the Kraken exchange.
     *
     * @param apiMethod the API method to call.
     * @param params any (optional) query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod, Map<String, String> params)
            throws ExchangeNetworkException, TradingApiException {

        if (params == null) {
            params = new HashMap<>(); // no params, so empty query string
        }

        // Build the query string with any given params
        final StringBuilder queryString = new StringBuilder("?");
        for (final String param : params.keySet()) {
            if (queryString.length() > 1) {
                queryString.append("&");
            }
            //noinspection deprecation
            queryString.append(param).append("=").append(URLEncoder.encode(params.get(param)));
        }

        // Request headers required by Exchange
        final Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        try {

            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + queryString);
            return sendNetworkRequest(url, "GET", null, requestHeaders);

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * <p>
     * Makes an authenticated API call to the Kraken exchange.
     * </p>
     *
     * <pre>
     *
     * </pre>
     *
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(String apiMethod, Map<String, Object> params)
            throws ExchangeNetworkException, TradingApiException {

        throw new UnsupportedOperationException("Not developed yet!");
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
            final String errorMsg = "Failed to setup MAC security. HINT: Is HmacSHA512 installed?";
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
        gson = gsonBuilder.create();
    }

    /*
     * Hack for unit-testing map params passed to transport layer.
     */
    private Map<String, String> getRequestParamMap() {
        return new HashMap<>();
    }
}