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
import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Exchange Adapter for integrating with the Gemini exchange.
 * The Gemini API is documented <a href="https://docs.gemini.com/rest-api/">here</a>.
 * </p>
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Gemini, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String)} and {@link #sendAuthenticatedRequestToExchange(String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 * <p>
 * The adapter only supports the REST implementation of the <a href="https://docs.gemini.com/rest-api/">Trading API</a>.
 * </p>
 * <p>
 * TODO Exchange fees
 * <p>
 * TODO API limits/throttling
 * <p>
 * TODO any rounding?
 * <p>
 * <p>
 * The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single thread in order to
 * preserve trade execution order. The {@link URLConnection} achieves this by blocking/waiting on the input stream
 * (response) for each API call.
 * </p>
 * <p>
 * The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error occurs trying to
 * connect to the exchange. A {@link TradingApiException} is thrown for <em>all</em> other failures.
 * </p>
 *
 * @author gazbert
 * @since 31/07/2016
 */
public final class GeminiExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * The version of the Gemini API being used.
     */
    private static final String GEMINI_API_VERSION = "v1";

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.gemini.com/" + GEMINI_API_VERSION + "/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 31 July 2016.
     */
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Bitfinex Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Name of api key property in config file.
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
     * GSON engine used for parsing JSON in Gemini API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LOG.info(() -> "About to initialise Gemini ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);

        nonce = System.currentTimeMillis() / 1000; // set the initial nonce used in the secure messaging.
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Gemini REST Trade API Calls adapted to the Trading API.
    // See https://docs.gemini.com/rest-api/
    // ------------------------------------------------------------------------------------------------

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
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        try {

            final ExchangeHttpResponse response = sendPublicRequestToExchange("book/" + marketId);
            LOG.debug(() -> "Market Orders response: " + response);

            final GeminiOrderBook orderBook = gson.fromJson(response.getPayload(), GeminiOrderBook.class);

            final List<MarketOrder> buyOrders = new ArrayList<>();
            for (GeminiMarketOrder bitfinexBuyOrder : orderBook.bids) {
                final MarketOrder buyOrder = new MarketOrder(
                        OrderType.BUY,
                        bitfinexBuyOrder.price,
                        bitfinexBuyOrder.amount,
                        bitfinexBuyOrder.price.multiply(bitfinexBuyOrder.amount));
                buyOrders.add(buyOrder);
            }

            final List<MarketOrder> sellOrders = new ArrayList<>();
            for (GeminiMarketOrder bitfinexSellOrder : orderBook.asks) {
                final MarketOrder sellOrder = new MarketOrder(
                        OrderType.SELL,
                        bitfinexSellOrder.price,
                        bitfinexSellOrder.amount,
                        bitfinexSellOrder.price.multiply(bitfinexSellOrder.amount));
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
    public BigDecimal getLatestMarketPrice(String marketId) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {

        try {

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("balances", null);
            LOG.debug(() -> "Balance Info response: " + response);

            final GeminiBalances allAccountBalances = gson.fromJson(response.getPayload(), GeminiBalances.class);
            final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();

            // This adapter only supports 'exchange' account type.
            allAccountBalances
                    .stream()
                    .filter(accountBalance -> accountBalance.type.equalsIgnoreCase("exchange"))
                    .forEach(accountBalance -> balancesAvailable.put(accountBalance.currency, accountBalance.available));

            // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by exchange.
            return new BalanceInfo(balancesAvailable, new HashMap<>());

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
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public String getImplName() {
        return "Gemini REST API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://docs.gemini.com/rest-api/
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for a market Order Book.
     */
    private static class GeminiOrderBook {

        // field names map to the JSON arg names
        public List<GeminiMarketOrder> bids;
        public List<GeminiMarketOrder> asks;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("bids", bids)
                    .add("asks", asks)
                    .toString();
        }
    }

    /**
     * GSON class for a Market Order.
     */
    private static class GeminiMarketOrder {

        public BigDecimal price;
        public BigDecimal amount;
        // ignore the timestamp attribute as per the API spec

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("price", price)
                    .add("amount", amount)
                    .toString();
        }
    }

    /**
     * GSON class for a 'balances' API call response.
     */
    private static class GeminiBalances extends ArrayList<GeminiAccountBalance> {
        private static final long serialVersionUID = 5516523141993401253L;
    }

    /**
     * GSON class for holding account type balance info.
     * This adapter only supports type 'exchange'.
     */
    private static class GeminiAccountBalance {

        // field names map to the JSON arg names
        public String type;
        public String currency;
        public BigDecimal amount;
        public BigDecimal available;
        public BigDecimal availableForWithdrawal;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("type", type)
                    .add("currency", currency)
                    .add("amount", amount)
                    .add("available", available)
                    .add("availableForWithdrawal", availableForWithdrawal)
                    .toString();
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the Gemini exchange.
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

            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod);
            return sendNetworkRequest(url, "GET", null, requestHeaders);

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * <p>
     * Makes an authenticated API call to the Gemini exchange.
     * </p>
     * <p>
     * <pre>
     *
     * E.g.
     *
     * POST https://api.gemini.com/v0/order/new
     *
     * With JSON payload of:
     *
     * {
     *    // Standard headers
     *    "request": "/v1/order/new",
     *    "nonce": <nonce>,
     *
     *    // Request-specific data
     *    "order_id": 18834
     * }
     *
     * To authenticate a request, we must calculate the following:
     *
     * b64 = base64.b64encode("""{
     * "request": "/v1/order/status",
     * "nonce": 123456,
     * "order_id": 18834
     * }
     * """)
     *
     * Whitespace is ignored by the server, and may be included if desired.
     * The hashes are always taken on the base64 string directly, with no normalization,
     * so whatever is sent in the payload is what should be hashed, and what the server will verify.
     *
     * Finally, the HMac:
     *
     * hmac.new("privateKey", b64, hashlib.sha384).hexdigest()
     * '337cc8b4ea692cfe65b4a85fcc9f042b2e3f702ac956fd098d600ab15705775017beae402be773ceee10719ff70d710f'
     *
     * These are sent as HTTP headers:
     *
     * X-GEMINI-APIKEY: apiKey
     * X-GEMINI-PAYLOAD:ewogICAgInJlcXVlc3QiOiAiL3YxL29yZGVyL3N
     * X-GEMINI-SIGNATURE: 337cc8b4ea692cfe65b4a85fcc9f042b2e3f
     *
     * </pre>
     *
     * @param apiMethod the API method to call.
     * @param params    the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(String apiMethod, Map<String, Object> params)
            throws ExchangeNetworkException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {

            if (params == null) {
                // create empty map for non param API calls, e.g. "balances"
                params = new HashMap<>();
            }

            // Add the API call method
            params.put("request", "/" + GEMINI_API_VERSION + "/" + apiMethod);

            // nonce is required by Gemini in every request
            params.put("nonce", Long.toString(nonce));
            nonce++;

            // JSON-ify the param dictionary
            final String paramsInJson = gson.toJson(params);

            // Need to base64 encode payload as per API
            final String base64payload = DatatypeConverter.printBase64Binary(paramsInJson.getBytes());

            // Request headers required by Exchange
            final Map<String, String> requestHeaders = new HashMap<>();

            // Create the signature
            mac.reset(); // force reset
            mac.update(base64payload.getBytes());
            final String signature = toHex(mac.doFinal()).toLowerCase();

            // Add the headers
            requestHeaders.put("X-GEMINI-APIKEY", key);
            requestHeaders.put("X-GEMINI-PAYLOAD", base64payload);
            requestHeaders.put("X-GEMINI-SIGNATURE", signature);

            // payload is JSON for this exchange
            requestHeaders.put("Content-Type", "application/json");

            final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
            return sendNetworkRequest(url, "POST", paramsInJson, requestHeaders);

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

        try {
            final SecretKeySpec keyspec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA384");
            mac = Mac.getInstance("HmacSHA384");
            mac.init(keyspec);
            initializedMACAuthentication = true;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA384 installed?";
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
