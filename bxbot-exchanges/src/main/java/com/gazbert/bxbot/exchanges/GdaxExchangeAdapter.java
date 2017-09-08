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
import com.gazbert.bxbot.exchange.api.OptionalConfig;
import com.gazbert.bxbot.trading.api.*;
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
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;

/**
 * <p>
 * Exchange Adapter for integrating with the GDAX (formerly Coinbase) exchange.
 * The GDAX API is documented <a href="https://www.gdax.com/">here</a>.
 * </p>
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on GDAX, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String, Map)} and
 * {@link #sendAuthenticatedRequestToExchange(String, String, Map)} methods. Use it at our own risk!
 * </strong>
 * </p>
 * <p>
 * This adapter only supports the GDAX <a href="https://docs.gdax.com/#api">REST API</a>. The design
 * of the API and documentation is excellent.
 * </p>
 * <p>
 * The adapter currently only supports <a href="https://docs.gdax.com/#place-a-new-order">Limit Orders</a>.
 * It was originally developed and tested for BTC-GBP market, but it should work for BTC-USD.
 * </p>
 * <p>
 * Exchange fees are loaded from the exchange.xml file on startup; they are not fetched from the exchange
 * at runtime as the GDAX REST API does not support this. The fees are used across all markets. Make sure you keep
 * an eye on the <a href="https://docs.gdax.com/#fees">exchange fees</a> and update the config accordingly.
 * </p>
 * <p>
 * NOTE: GDAX requires all price values to be limited to 2 decimal places when creating orders.
 * This adapter truncates any prices with more than 2 decimal places and rounds using
 * {@link java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be sent to the exchange as 250.18.
 * </p>
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
 * @since 1.0
 */
public final class GdaxExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.gdax.com/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 12 Jul 2016.
     */
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in GDAX Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Name of passphrase prop in config file.
     */
    private static final String PASSPHRASE_PROPERTY_NAME = "passphrase";

    /**
     * Name of PUBLIC key prop in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret prop in config file.
     */
    private static final String SECRET_PROPERTY_NAME = "secret";

    /**
     * Name of buy fee property in config file.
     */
    private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";

    /**
     * Name of sell fee property in config file.
     */
    private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";

    /**
     * Exchange buy fees in % in {@link BigDecimal} format.
     */
    private BigDecimal buyFeePercentage;

    /**
     * Exchange sell fees in % in {@link BigDecimal} format.
     */
    private BigDecimal sellFeePercentage;

    /**
     * Used to indicate if we have initialised the MAC authentication protocol.
     */
    private boolean initializedMACAuthentication = false;

    /**
     * The passphrase used in the MAC signature.
     */
    private String passphrase = "";

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
     * GSON engine used for parsing JSON in GDAX API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LOG.info(() -> "About to initialise GDAX ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);
        setOptionalConfig(config);

        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // GDAX API Calls adapted to the Trading API.
    // See https://docs.gdax.com/#api
    // ------------------------------------------------------------------------------------------------

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeNetworkException {

        try {

            /*
             * Build Limit Order: https://docs.gdax.com/#place-a-new-order
             *
             * stp param optional           - (Self-trade prevention flag) defaults to 'dc' Decrease & Cancel
             * post_only param optional     - defaults to 'false'
             * time_in_force param optional - defaults to 'GTC' Good til Cancel
             * client_oid param is optional - thia adapter does not use it.
             */
            final Map<String, String> params = getRequestParamMap();

            if (orderType == OrderType.BUY) {
                params.put("side", "buy");
            } else if (orderType == OrderType.SELL) {
                params.put("side", "sell");
            } else {
                final String errorMsg = "Invalid order type: " + orderType
                        + " - Can only be "
                        + OrderType.BUY.getStringValue() + " or "
                        + OrderType.SELL.getStringValue();
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            params.put("product_id", marketId);

            // note we need to limit price to 2 decimal places else exchange will barf
            params.put("price", new DecimalFormat("#.##").format(price));

            // note we need to limit size to 8 decimal places else exchange will barf
            params.put("size", new DecimalFormat("#.########").format(quantity));

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("POST", "orders", params);
            LOG.debug(() -> "Create Order response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                final GdaxOrder createOrderResponse = gson.fromJson(response.getPayload(), GdaxOrder.class);
                if (createOrderResponse != null && (createOrderResponse.id != null && !createOrderResponse.id.isEmpty())) {
                    return createOrderResponse.id;
                } else {
                    final String errorMsg = "Failed to place order on exchange. Error response: " + response;
                    LOG.error(errorMsg);
                    throw new TradingApiException(errorMsg);
                }

            } else {
                final String errorMsg = "Failed to create order on exchange. Details: " + response;
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

    /*
     * marketId is not needed for cancelling orders on this exchange.
     */
    @Override
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeNetworkException {

        try {

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("DELETE", "orders/" + orderId, null);
            LOG.debug(() -> "Cancel Order response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                // response payload is now a JSON array with 1 String entry: the orderId :-)
                final String[] cancelledOrderId = gson.fromJson(response.getPayload(), String[].class);
                if (cancelledOrderId[0].equals(orderId)) {
                    return true;
                } else {
                    final String errorMsg = "Failed to cancel order on exchange due to Order Id mismatch. " +
                            "OrderId sent: " + orderId + " ResponseOrderId: " + cancelledOrderId[0] + " Response: " + response;
                    LOG.error(errorMsg);
                    return false;
                }
            } else {
                final String errorMsg = "Failed to cancel order on exchange. Details: " + response;
                LOG.error(errorMsg);
                return false;
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

        try {

            // we use default request no-param call - only open or un-settled orders are returned.
            // As soon as an order is no longer open and settled, it will no longer appear in the default request.
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("GET", "orders", null);
            LOG.debug(() -> "Open Orders response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final GdaxOrder[] gdaxOpenOrders = gson.fromJson(response.getPayload(), GdaxOrder[].class);

                final List<OpenOrder> ordersToReturn = new ArrayList<>();
                for (final GdaxOrder openOrder : gdaxOpenOrders) {

                    if (!marketId.equalsIgnoreCase(openOrder.product_id)) {
                        continue;
                    }

                    OrderType orderType;
                    switch (openOrder.side) {
                        case "buy":
                            orderType = OrderType.BUY;
                            break;
                        case "sell":
                            orderType = OrderType.SELL;
                            break;
                        default:
                            throw new TradingApiException(
                                    "Unrecognised order type received in getYourOpenOrders(). Value: " + openOrder.side);
                    }

                    final OpenOrder order = new OpenOrder(
                            openOrder.id,
                            Date.from(Instant.parse(openOrder.created_at)),
                            marketId,
                            orderType,
                            openOrder.price,
                            openOrder.size.subtract(openOrder.filled_size), // quantity remaining - not provided by GDAX
                            openOrder.size,                                 // orig quantity
                            openOrder.price.multiply(openOrder.size)        // total - not provided by GDAX
                    );

                    ordersToReturn.add(order);
                }
                return ordersToReturn;
            } else {
                final String errorMsg = "Failed to get your open orders from exchange. Details: " + response;
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
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        try {

            final Map<String, String> params = getRequestParamMap();
            params.put("level", "2"); //  "2" = Top 50 bids and asks (aggregated)

            final ExchangeHttpResponse response = sendPublicRequestToExchange("products/" + marketId + "/book", params);
            LOG.debug(() -> "Market Orders response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final GdaxBookWrapper orderBook = gson.fromJson(response.getPayload(), GdaxBookWrapper.class);

                final List<MarketOrder> buyOrders = new ArrayList<>();
                for (GdaxMarketOrder gdaxBuyOrder : orderBook.bids) {
                    final MarketOrder buyOrder = new MarketOrder(
                            OrderType.BUY,
                            gdaxBuyOrder.get(0),
                            gdaxBuyOrder.get(1),
                            gdaxBuyOrder.get(0).multiply(gdaxBuyOrder.get(1)));
                    buyOrders.add(buyOrder);
                }

                final List<MarketOrder> sellOrders = new ArrayList<>();
                for (GdaxMarketOrder gdaxSellOrder : orderBook.asks) {
                    final MarketOrder sellOrder = new MarketOrder(
                            OrderType.SELL,
                            gdaxSellOrder.get(0),
                            gdaxSellOrder.get(1),
                            gdaxSellOrder.get(0).multiply(gdaxSellOrder.get(1)));
                    sellOrders.add(sellOrder);
                }

                return new MarketOrderBook(marketId, sellOrders, buyOrders);

            } else {
                final String errorMsg = "Failed to get market order book from exchange. Details: " + response;
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
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {

        try {
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("GET", "accounts", null);
            LOG.debug(() -> "Balance Info response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final GdaxAccount[] gdaxAccounts = gson.fromJson(response.getPayload(), GdaxAccount[].class);

                final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();
                final HashMap<String, BigDecimal> balancesOnHold = new HashMap<>();

                for (final GdaxAccount gdaxAccount : gdaxAccounts) {
                    balancesAvailable.put(gdaxAccount.currency, gdaxAccount.available);
                    balancesOnHold.put(gdaxAccount.currency, gdaxAccount.hold);
                }
                return new BalanceInfo(balancesAvailable, balancesOnHold);

            } else {
                final String errorMsg = "Failed to get your wallet balance info from exchange. Details: " + response;
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
    public BigDecimal getLatestMarketPrice(String marketId) throws ExchangeNetworkException, TradingApiException {

        try {

            final ExchangeHttpResponse response = sendPublicRequestToExchange("products/" + marketId + "/ticker", null);
            LOG.debug(() -> "Latest Market Price response: " + response);

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                final GdaxTicker gdaxTicker = gson.fromJson(response.getPayload(), GdaxTicker.class);
                return gdaxTicker.price;
            } else {
                final String errorMsg = "Failed to get market ticker from exchange. Details: " + response;
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
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        // GDAX does not provide API call for fetching % buy fee; it only provides the fee monetary value for a
        // given order via e.g. /orders/<order-id> API call. We load the % fee statically from exchange.xml
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        // GDAX does not provide API call for fetching % sell fee; it only provides the fee monetary value for a
        // given order via e.g. /orders/<order-id> API call. We load the % fee statically from exchange.xml
        return sellFeePercentage;
    }

    @Override
    public String getImplName() {
        return "GDAX REST API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://docs.gdax.com/#api
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for GDAX '/orders' API call response.
     * <p>
     * There are other critters in here different to what is spec'd: https://docs.gdax.com/#list-orders
     */
    private static class GdaxOrder {

        public String id;
        public BigDecimal price;
        public BigDecimal size;
        public String product_id;     // e.g. "BTC-GBP", "BTC-USD"
        public String side;           // "buy" or "sell"
        public String stp;            // Self-Trade Prevention flag, e.g. "dc"
        public String type;           // order type, e.g. "limit"
        public String time_in_force;  // e.g. "GTC" (Good Til Cancelled)
        public boolean post_only;     // shows in book and provides exchange liquidity, but will no be executed
        public String created_at;     // e.g. "2014-11-14 06:39:55.189376+00"
        public BigDecimal fill_fees;
        public BigDecimal filled_size;
        public String status;          // e.g. "open"
        public boolean settled;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("price", price)
                    .add("size", size)
                    .add("product_id", product_id)
                    .add("side", side)
                    .add("stp", stp)
                    .add("type", type)
                    .add("time_in_force", time_in_force)
                    .add("post_only", post_only)
                    .add("created_at", created_at)
                    .add("fill_fees", fill_fees)
                    .add("filled_size", filled_size)
                    .add("status", status)
                    .add("settled", settled)
                    .toString();
        }
    }

    /**
     * GSON class for GDAX '/products/{marketId}/book' API call response.
     */
    private static class GdaxBookWrapper {

        public long sequence;
        public List<GdaxMarketOrder> bids;
        public List<GdaxMarketOrder> asks;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("sequence", sequence)
                    .add("bids", bids)
                    .add("asks", asks)
                    .toString();
        }
    }

    /**
     * GSON class for holding Market Orders. First element in array is price, second element is amount, third is number
     * of orders.
     */
    private static class GdaxMarketOrder extends ArrayList<BigDecimal> {
        private static final long serialVersionUID = -4919711220797077759L;
    }

    /**
     * GSON class for GDAX '/products/{marketId}/ticker' API call response.
     */
    private static class GdaxTicker {

        public long trade_id;
        public BigDecimal price;
        public BigDecimal size;
        public String time; // e.g. "2015-10-14T19:19:36.604735Z"

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("trade_id", trade_id)
                    .add("price", price)
                    .add("size", size)
                    .add("time", time)
                    .toString();
        }
    }

    /**
     * GSON class for GDAX '/accounts' API call response.
     */
    private static class GdaxAccount {

        public String id;
        public String currency;
        public BigDecimal balance; // e.g. "0.0000000000000000"
        public BigDecimal hold;
        public BigDecimal available;
        public String profile_id; // no idea what this is?

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("currency", currency)
                    .add("balance", balance)
                    .add("hold", hold)
                    .add("available", available)
                    .add("profile_id", profile_id)
                    .toString();
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the GDAX exchange.
     *
     * @param apiMethod the API method to call.
     * @param params    any (optional) query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod, Map<String, String> params)
            throws ExchangeNetworkException, TradingApiException {

        if (params == null) {
            params = new HashMap<>(); // no params, so empty query string
        }

        try {

            // Build the query string with any given params
            final StringBuilder queryString = new StringBuilder("?");
            for (final Map.Entry<String, String> param : params.entrySet()) {
                if (queryString.length() > 1) {
                    queryString.append("&");
                }
                //noinspection deprecation
                queryString.append(param.getKey());
                queryString.append("=");
                queryString.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }

            // Request headers required by Exchange
            final Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + queryString);
            return sendNetworkRequest(url, "GET", null, requestHeaders);

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * <p>
     * Makes an authenticated API call to the GDAX exchange.
     * </p>
     * <p>
     * <p>
     * The GDAX authentication process is complex, but well documented
     * <a href="https://docs.gdax.com/#creating-a-request">here</a>.
     * </p>
     * <p>
     * <pre>
     * All REST requests must contain the following headers:
     *
     * CB-ACCESS-KEY          The api key as a string.
     * CB-ACCESS-SIGN         The base64-encoded signature (see Signing a Message).
     * CB-ACCESS-TIMESTAMP    A timestamp for your request.
     * CB-ACCESS-PASSPHRASE   The passphrase you specified when creating the API key.
     *
     * The CB-ACCESS-TIMESTAMP header MUST be number of seconds since Unix Epoch in UTC. Decimal values are allowed.
     *
     * Your timestamp must be within 30 seconds of the api service time or your request will be considered expired and
     * rejected. We recommend using the time endpoint to query for the API server time if you believe there many be
     * time skew between your server and the API servers.
     *
     * All request bodies should have content type application/json and be valid JSON.
     *
     * The CB-ACCESS-SIGN header is generated by creating a sha256 HMAC using the base64-decoded secret key on the
     * prehash string timestamp + method + requestPath + body (where + represents string concatenation) and
     * base64-encode the output. The timestamp value is the same as the CB-ACCESS-TIMESTAMP header.
     *
     * The body is the request body string or omitted if there is no request body (typically for GET requests).
     *
     * The method should be UPPER CASE.
     *
     * Remember to first base64-decode the alphanumeric secret string (resulting in 64 bytes) before using it as the
     * key for HMAC. Also, base64-encode the digest output before sending in the header.
     * </pre>
     *
     * @param httpMethod the HTTP method to use, e.g. GET, POST, DELETE
     * @param apiMethod  the API method to call.
     * @param params     the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
            String httpMethod, String apiMethod, Map<String, String> params) throws
            ExchangeNetworkException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {

            if (params == null) {
                // create empty map for non-param API calls
                params = new HashMap<>();
            }

            // Get UNIX time in secs
            final String timestamp = Long.toString(System.currentTimeMillis() / 1000);

            // Build the request
            final String invocationUrl;
            String requestBody = "";

            switch (httpMethod) {

                case "GET":
                    LOG.debug(() -> "Building secure GET request...");
                    // Build (optional) query param string
                    final StringBuilder queryParamBuilder = new StringBuilder();
                    for (final Map.Entry<String, String> param : params.entrySet()) {
                        if (queryParamBuilder.length() > 0) {
                            queryParamBuilder.append("&");
                        }
                        queryParamBuilder.append(param.getKey());
                        queryParamBuilder.append("=");
                        queryParamBuilder.append(param.getValue());
                    }

                    final String queryParams = queryParamBuilder.toString();
                    LOG.debug(() -> "Query param string: " + queryParams);

                    if (params.isEmpty()) {
                        invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    } else {
                        invocationUrl = AUTHENTICATED_API_URL + apiMethod + "?" + queryParams;
                    }
                    break;

                case "POST":
                    LOG.debug(() -> "Building secure POST request...");
                    invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    requestBody = gson.toJson(params);
                    break;

                case "DELETE":
                    LOG.debug(() -> "Building secure DELETE request...");
                    invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    break;

                default:
                    throw new IllegalArgumentException("Don't know how to build secure [" + httpMethod + "] request!");
            }

            // Build the signature string
            final String signatureBuilder = timestamp + httpMethod.toUpperCase() +
                    "/" +
                    apiMethod +
                    requestBody;

            // Sign the signature string and Base64 encode it
            mac.reset();
            mac.update(signatureBuilder.getBytes("UTF-8"));
            final String signature = DatatypeConverter.printBase64Binary(mac.doFinal());

            // Request headers required by Exchange
            final Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/json");
            requestHeaders.put("CB-ACCESS-KEY", key);
            requestHeaders.put("CB-ACCESS-SIGN", signature);
            requestHeaders.put("CB-ACCESS-TIMESTAMP", timestamp);
            requestHeaders.put("CB-ACCESS-PASSPHRASE", passphrase);

            final URL url = new URL(invocationUrl);
            return sendNetworkRequest(url, httpMethod, requestBody, requestHeaders);

        } catch (MalformedURLException | UnsupportedEncodingException e) {
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

            // GDAX secret is in Base64 so we must decode it first.
            final byte[] decodedBase64Secret = DatatypeConverter.parseBase64Binary(secret);

            final SecretKeySpec keyspec = new SecretKeySpec(decodedBase64Secret, "HmacSHA256");
            mac = Mac.getInstance("HmacSHA256");
            mac.init(keyspec);
            initializedMACAuthentication = true;
        } catch (NoSuchAlgorithmException e) {
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
        passphrase = getAuthenticationConfigItem(authenticationConfig, PASSPHRASE_PROPERTY_NAME);
        key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
        secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
    }

    private void setOptionalConfig(ExchangeConfig exchangeConfig) {

        final OptionalConfig optionalConfig = getOptionalConfig(exchangeConfig);

        final String buyFeeInConfig = getOptionalConfigItem(optionalConfig, BUY_FEE_PROPERTY_NAME);
        buyFeePercentage = new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
        LOG.info(() -> "Buy fee % in BigDecimal format: " + buyFeePercentage);

        final String sellFeeInConfig = getOptionalConfigItem(optionalConfig, SELL_FEE_PROPERTY_NAME);
        sellFeePercentage = new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
        LOG.info(() -> "Sell fee % in BigDecimal format: " + sellFeePercentage);
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