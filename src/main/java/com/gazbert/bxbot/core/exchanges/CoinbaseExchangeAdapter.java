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
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Exchange Adapter for integrating with the Coinbase exchange.
 * The Coinbase API is documented <a href="https://docs.exchange.coinbase.com/">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Coinbase, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String, Map)} and
 * {@link #sendAuthenticatedRequestToExchange(String, String, Map)} methods. Use it at our own risk!
 * </strong>
 * </p>
 *
 * <p>
 * This adapter only supports the Coinbase <a href="https://docs.exchange.coinbase.com/#api">REST API</a>. The design
 * of the API and documentation is excellent.
 * </p>
 *
 * <p>
 * The adapter currently only supports <a href="https://docs.exchange.coinbase.com/#place-a-new-order">Limit Orders</a>.
 * It was originally developed and tested for BTC-GBP market, but it should work for BTC-USD.
 * </p>
 *
 * <p>
 * Exchange fees are loaded from the coinbase-config.properties file on startup; they are not fetched from the exchange
 * at runtime as the Coinbase REST API does not support this. The fees are used across all markets. Make sure you keep
 * an eye on the <a href="https://docs.exchange.coinbase.com/#fees">exchange fees</a> and update the config accordingly.
 * This adapter will use the <em>Taker</em> fees to keep things simple for now.
 * </p>
 *
 * <p>
 * NOTE: Coinbase requires all price values to be limited to 2 decimal places when creating orders.
 * This adapter truncates any prices with more than 2 decimal places and rounds using
 * {@link java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be sent to the exchange as 250.18.
 * </p>
 *
 * <p>
 * The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single thread in order to
 * preserve trade execution order. The {@link URLConnection} achieves this by blocking/waiting on the input stream
 * (response) for each API call.
 * </p>
 *
 * <p>
 * The {@link TradingApi} calls will throw a {@link ExchangeTimeoutException} if a network error occurs trying to
 * connect to the exchange. A {@link TradingApiException} is thrown for <em>all</em> other failures.
 * </p>
 *
 * @author gazbert
 */
public final class CoinbaseExchangeAdapter implements TradingApi {

    private static final Logger LOG = Logger.getLogger(CoinbaseExchangeAdapter.class);

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.exchange.coinbase.com/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 12 Oct 2015.
     */
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Coinbase Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * IO 50x Timeout error message for logging.
     */
    private static final String IO_50X_TIMEOUT_ERROR_MSG = "Failed to connect to Exchange due to 50x timeout.";

    /**
     * IO Socket Timeout error message for logging.
     */
    private static final String IO_SOCKET_TIMEOUT_ERROR_MSG = "Failed to connect to Exchange due to socket timeout.";

    /**
     * Used for building error messages for missing config.
     */
    private static final String CONFIG_IS_NULL_OR_ZERO_LENGTH = " cannot be null or zero length! HINT: is the value set in the ";

    /**
     * Your Coinbase API keys and connection timeout config.
     * This file must be on BX-bot's runtime classpath located at: ./resources/coinbase/coinbase-config.properties
     */
    private static final String CONFIG_FILE = "coinbase/coinbase-config.properties";

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
     * Name of connection timeout property in config file.
     */
    private static final String CONNECTION_TIMEOUT_PROPERTY_NAME = "connection-timeout";

    /**
     * Exchange buy fees in % in {@link BigDecimal} format.
     */
    private BigDecimal buyFeePercentage;

    /**
     * Exchange sell fees in % in {@link BigDecimal} format.
     */
    private BigDecimal sellFeePercentage;

    /**
     * The connection timeout in SECONDS for terminating hung connections to the exchange.
     */
    private int connectionTimeout;

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
     * GSON engine used for parsing JSON in Coinbase API call responses.
     */
    private Gson gson;


    /**
     * Constructor initialises the Exchange Adapter for using the Coinbase API.
     */
    public CoinbaseExchangeAdapter() {

        loadConfig();
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Coinbase API Calls adapted to the Trading API.
    // See https://docs.exchange.coinbase.com/#api
    // ------------------------------------------------------------------------------------------------

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeTimeoutException {

        try {

            /*
             * Build Limit Order: https://docs.exchange.coinbase.com/#place-a-new-order
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

            final CoinbaseHttpResponse response = sendAuthenticatedRequestToExchange("POST", "orders", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("createOrder() response: " + response);
            }

            if (response.statusCode == HttpURLConnection.HTTP_OK) {

                final CoinbaseOrder createOrderResponse = gson.fromJson(response.getPayload(), CoinbaseOrder.class);
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

            final CoinbaseHttpResponse response = sendAuthenticatedRequestToExchange("DELETE", "orders/" + orderId, null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("cancelOrder() response: " + response);
            }

            if (response.statusCode == HttpURLConnection.HTTP_OK) {

                // eek! Wish they stuck with a proper JSON response with maybe cancelTime, orderId etc in it...
                if (response.getPayload().equalsIgnoreCase("OK")) {
                    return true;
                } else {
                    final String errorMsg = "Failed to cancel order on exchange. Details: " + response;
                    LOG.error(errorMsg);
                    return false;
                }
            } else {
                final String errorMsg = "Failed to cancel order on exchange. Details: " + response;
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
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {

            // we use default request no-param call - only open or un-settled orders are returned.
            // As soon as an order is no longer open and settled, it will no longer appear in the default request.
            final CoinbaseHttpResponse response = sendAuthenticatedRequestToExchange("GET", "orders", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getYourOpenOrders() response: " + response);
            }

            if (response.statusCode == HttpURLConnection.HTTP_OK) {

                final CoinbaseOrder[] coinbaseOpenOrders = gson.fromJson(response.getPayload(), CoinbaseOrder[].class);

                // adapt
                final List<OpenOrder> ordersToReturn = new ArrayList<>();
                for (final CoinbaseOrder openOrder : coinbaseOpenOrders) {
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
                            openOrder.size.subtract(openOrder.filled_size), // quantity remaining - not provided by Coinbase
                            openOrder.size,                                 // orig quantity
                            openOrder.price.multiply(openOrder.size)        // total - not provided by Coinbase
                    );

                    ordersToReturn.add(order);
                }
                return ordersToReturn;
            } else {
                final String errorMsg = "Failed to get your open orders from exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {

            final Map<String, String> params = getRequestParamMap();
            params.put("level", "2"); //  "2" = Top 50 bids and asks (aggregated)

            final CoinbaseHttpResponse response = sendPublicRequestToExchange("products/" + marketId + "/book", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getMarketOrders() response: " + response);
            }

            if (response.statusCode == HttpURLConnection.HTTP_OK) {

                final CoinbaseBookWrapper orderBook = gson.fromJson(response.getPayload(), CoinbaseBookWrapper.class);

                // adapt BUYs
                final List<MarketOrder> buyOrders = new ArrayList<>();
                for (CoinbaseMarketOrder coinbaseBuyOrder : orderBook.bids) {
                    final MarketOrder buyOrder = new MarketOrder(
                            OrderType.BUY,
                            coinbaseBuyOrder.get(0),
                            coinbaseBuyOrder.get(1),
                            coinbaseBuyOrder.get(0).multiply(coinbaseBuyOrder.get(1)));
                    buyOrders.add(buyOrder);
                }

                // adapt SELLs
                final List<MarketOrder> sellOrders = new ArrayList<>();
                for (CoinbaseMarketOrder coinbaseSellOrder : orderBook.asks) {
                    final MarketOrder sellOrder = new MarketOrder(
                            OrderType.SELL,
                            coinbaseSellOrder.get(0),
                            coinbaseSellOrder.get(1),
                            coinbaseSellOrder.get(0).multiply(coinbaseSellOrder.get(1)));
                    sellOrders.add(sellOrder);
                }

                return new MarketOrderBook(marketId, sellOrders, buyOrders);

            } else {
                final String errorMsg = "Failed to get market order book from exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

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
            final CoinbaseHttpResponse response = sendAuthenticatedRequestToExchange("GET", "accounts", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getBalanceInfo() response: " + response);
            }

            if (response.statusCode == HttpURLConnection.HTTP_OK) {

                final CoinbaseAccount[] coinbaseAccounts = gson.fromJson(response.getPayload(), CoinbaseAccount[].class);

                // adapt
                final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();
                final HashMap<String, BigDecimal> balancesOnHold = new HashMap<>();

                for (final CoinbaseAccount coinbaseAccount : coinbaseAccounts) {
                    balancesAvailable.put(coinbaseAccount.currency, coinbaseAccount.available);
                    balancesOnHold.put(coinbaseAccount.currency, coinbaseAccount.hold);
                }
                return new BalanceInfo(balancesAvailable, balancesOnHold);

            } else {
                final String errorMsg = "Failed to get your wallet balance info from exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws ExchangeTimeoutException, TradingApiException {

        try {

            final CoinbaseHttpResponse response = sendPublicRequestToExchange("products/" + marketId + "/ticker", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getLatestMarketPrice() response: " + response);
            }

            if (response.statusCode == HttpURLConnection.HTTP_OK) {
                final CoinbaseTicker coinbaseTicker = gson.fromJson(response.getPayload(), CoinbaseTicker.class);
                return coinbaseTicker.price;
            } else {
                final String errorMsg = "Failed to get market ticker from exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeTimeoutException {

        // Coinbase does not provide API call for fetching % buy fee; it only provides the fee monetary value for a
        // given order via e.g. /orders/<order-id> API call. We load the % fee statically from coinbase-config.properties
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeTimeoutException {

        // Coinbase does not provide API call for fetching % sell fee; it only provides the fee monetary value for a
        // given order via e.g. /orders/<order-id> API call. We load the % fee statically from coinbase-config.properties
        return sellFeePercentage;
    }

    @Override
    public String getImplName() {
        return "Coinbase REST API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://docs.exchange.coinbase.com/#api
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for Coinbase '/orders' API call response.
     *
     * There are other critters in here different to what is spec'd: https://docs.exchange.coinbase.com/#list-orders
     *
     * @author gazbert
     */
    private static class CoinbaseOrder {

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
            return CoinbaseOrder.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", price=" + price
                    + ", size=" + size
                    + ", product_id=" + product_id
                    + ", side=" + side
                    + ", stp=" + stp
                    + ", type=" + type
                    + ", time_in_force=" + time_in_force
                    + ", post_only=" + post_only
                    + ", created_at=" + created_at
                    + ", fill_fees=" + fill_fees
                    + ", filled_size=" + filled_size
                    + ", status=" + status
                    + ", settled=" + settled
                    + "]";
        }
    }

    /**
     * GSON class for Coinbase '/products/{marketId}/book' API call response.
     *
     * @author gazbert
     */
    private static class CoinbaseBookWrapper {

        public long sequence;
        public List<CoinbaseMarketOrder> bids;
        public List<CoinbaseMarketOrder> asks;

        @Override
        public String toString() {
            return CoinbaseBookWrapper.class.getSimpleName()
                    + " ["
                    + "bids=" + bids
                    + ", asks=" + asks
                    + "]";
        }
    }

    /**
     * GSON class for holding Market Orders. First element in array is price, second element is amount, third is number
     * of orders.
     *
     * @author gazbert
     */
    private static class CoinbaseMarketOrder extends ArrayList<BigDecimal> {
        private static final long serialVersionUID = -4919711220797077759L;
    }

    /**
     * GSON class for Coinbase '/products/{marketId}/ticker' API call response.
     *
     * @author gazbert
     */
    private static class CoinbaseTicker {

        public long trade_id;
        public BigDecimal price;
        public BigDecimal size;
        public String time; // e.g. "2015-10-14T19:19:36.604735Z"

        @Override
        public String toString() {
            return CoinbaseTicker.class.getSimpleName()
                    + " ["
                    + "trade_id=" + trade_id
                    + ", price=" + price
                    + ", size=" + size
                    + ", time=" + time
                    + "]";
        }
    }

    /**
     * GSON class for Coinbase '/accounts' API call response.
     *
     * @author gazbert
     */
    private static class CoinbaseAccount {

        public String id;
        public String currency;
        public BigDecimal balance; // e.g. "0.0000000000000000"
        public BigDecimal hold;
        public BigDecimal available;
        public String profile_id; // no idea what this is?

        @Override
        public String toString() {
            return CoinbaseAccount.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", currency=" + currency
                    + ", balance=" + balance
                    + ", hold=" + hold
                    + ", available=" + available
                    + ", profile_id=" + profile_id
                    + "]";
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Wrapper class for holding Coinbase HTTP responses.
     *
     * Package private for unit testing ;-o
     *
     * @author gazbert
     */
    static class CoinbaseHttpResponse {

        private int statusCode;
        private String reasonPhrase;
        private String payload;

        public CoinbaseHttpResponse(int statusCode, String reasonPhrase, String payload) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return CoinbaseHttpResponse.class.getSimpleName()
                    + " ["
                    + "statusCode=" + statusCode
                    + ", reasonPhrase=" + reasonPhrase
                    + ", payload=" + payload
                    + "]";
        }
    }

    /**
     * Makes a public API call to Coinbase exchange. Uses HTTP GET.
     *
     * @param apiMethod the API method to call.
     * @param params any (optional) query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private CoinbaseHttpResponse sendPublicRequestToExchange(String apiMethod, Map<String, String> params)
            throws ExchangeTimeoutException, TradingApiException {

        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

        try {

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

            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + queryString);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using following URL for API call: " + url);
            }

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);

            // no JSON this time
            exchangeConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            /*
             * Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
             * Exchange sometimes gets stuck here, especially read timeouts.
             * connectionTimeout is in SECONDS and comes from coinbase-config.properties config.
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

            return new CoinbaseHttpResponse(exchangeConnection.getResponseCode(), exchangeConnection.getResponseMessage(),
                    exchangeResponse.toString());

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);

        } catch (SocketTimeoutException e) {
            final String errorMsg = IO_SOCKET_TIMEOUT_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new ExchangeTimeoutException(errorMsg, e);

        } catch (IOException e) {

            try {

                /*
                 * Exchange sometimes fails with these codes, but recovers by next request...
                 */
                if (exchangeConnection != null && (exchangeConnection.getResponseCode() == 502
                        || exchangeConnection.getResponseCode() == 503
                        || exchangeConnection.getResponseCode() == 504)) {

                    final String errorMsg = IO_50X_TIMEOUT_ERROR_MSG;
                    LOG.error(errorMsg, e);
                    throw new ExchangeTimeoutException(errorMsg, e);

                } else {
                    final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
                    LOG.error(errorMsg, e);
                    e.printStackTrace();
                    throw new TradingApiException(errorMsg, e);
                }
            } catch (IOException e1) {

                final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
                LOG.error(errorMsg, e1);
                throw new TradingApiException(errorMsg, e1);
            }
        } finally {
            if (exchangeConnection != null) {
                exchangeConnection.disconnect();
            }
        }
    }

    /**
     * <p>
     * Makes Authenticated API call to Coinbase exchange.
     * </p>
     *
     * <p>
     * The Coinbase authentication process is well documented
     * <a href="https://docs.exchange.coinbase.com/#creating-a-request">here</a>.
     * </p>
     *
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
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private CoinbaseHttpResponse sendAuthenticatedRequestToExchange(
            String httpMethod, String apiMethod, Map<String, String> params) throws
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

            if (params == null) {
                // create empty map for non-param API calls
                params = new HashMap<>();
            }

            // Get UNIX time in secs
            final String timestamp = Long.toString(System.currentTimeMillis()/1000);

            // Build the request
            final String invocationUrl;
            String requestBody = "";

            switch (httpMethod) {
                case "GET" :
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Building secure GET request...");
                    }

                    // Build (optional) query param string
                    final StringBuilder queryParamBuilder = new StringBuilder();
                    for (final String param : params.keySet()) {
                        if (queryParamBuilder.length() > 0) {
                            queryParamBuilder.append("&");
                        }
                        queryParamBuilder.append(param).append("=").append(params.get(param));
                    }

                    final String queryParams = queryParamBuilder.toString();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Query param string: " + queryParams);
                    }

                    if (params.isEmpty()) {
                        invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    } else {
                        invocationUrl = AUTHENTICATED_API_URL + apiMethod + "?" + queryParams;
                    }
                    break;

                case "POST" :
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Building secure POST request...");
                    }

                    invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    requestBody = gson.toJson(params);
                    break;

                case "DELETE" :
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Building secure DELETE request...");
                    }

                    invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    break;

                default:
                    throw new IllegalArgumentException("Don't know how to build secure [" + httpMethod + "] request!");
            }

            // Build the signature string
            final StringBuilder signatureBuilder = new StringBuilder(timestamp);
            signatureBuilder.append(httpMethod.toUpperCase()).append("/").append(apiMethod).append(requestBody);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Signature String: " + signatureBuilder);
            }

            // Sign the signature string and Base64 encode it
            mac.reset();
            mac.update(signatureBuilder.toString().getBytes());
            final String signature = DatatypeConverter.printBase64Binary(mac.doFinal());

            final URL url = new URL(invocationUrl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using following URL for API call: " + url);
            }

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);
            exchangeConnection.setRequestMethod(httpMethod); // GET|POST|DELETE

            // Add the public API key
            exchangeConnection.setRequestProperty("CB-ACCESS-KEY", key);

            // Add signature
            exchangeConnection.setRequestProperty("CB-ACCESS-SIGN", signature);

            // Add timestamp
            exchangeConnection.setRequestProperty("CB-ACCESS-TIMESTAMP", timestamp);

            // Add passphrase
            exchangeConnection.setRequestProperty("CB-ACCESS-PASSPHRASE", passphrase);

            // payload is JSON for this exchange
            exchangeConnection.setRequestProperty("Content-Type", "application/json");

            // Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            /*
             * Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
             * Exchange sometimes gets stuck here, especially read timeouts.
             * connectionTimeout is in SECONDS and comes from coinbase-config.properties config.
             */
            final int timeoutInMillis = connectionTimeout * 1000;
            exchangeConnection.setConnectTimeout(timeoutInMillis);
            exchangeConnection.setReadTimeout(timeoutInMillis);

            if (httpMethod.equalsIgnoreCase("POST")) {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Doing POST with request body: " + requestBody);
                }

                final OutputStreamWriter outputPostStream = new OutputStreamWriter(exchangeConnection.getOutputStream());
                outputPostStream.write(requestBody);
                outputPostStream.close();
            }

            // Grab the response - we just block here as per Connection API
            final BufferedReader responseInputStream = new BufferedReader(new InputStreamReader(
                    exchangeConnection.getInputStream()));

            // Read the JSON response lines into our response buffer
            String responseLine;
            while ((responseLine = responseInputStream.readLine()) != null) {
                exchangeResponse.append(responseLine);
            }
            responseInputStream.close();

            return new CoinbaseHttpResponse(exchangeConnection.getResponseCode(), exchangeConnection.getResponseMessage(),
                    exchangeResponse.toString());

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);

        } catch (SocketTimeoutException e) {
            final String errorMsg = IO_SOCKET_TIMEOUT_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new ExchangeTimeoutException(errorMsg, e);

        } catch (IOException e) {

            try {

                /*
                 * Exchange sometimes fails with these codes, but recovers by next request...
                 */
                if (exchangeConnection != null && (exchangeConnection.getResponseCode() == 502
                        || exchangeConnection.getResponseCode() == 503
                        || exchangeConnection.getResponseCode() == 504)) {

                    final String errorMsg = IO_50X_TIMEOUT_ERROR_MSG;
                    LOG.error(errorMsg, e);
                    throw new ExchangeTimeoutException(errorMsg, e);

                } else {
                    final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
                    LOG.error(errorMsg, e);
                    throw new TradingApiException(errorMsg, e);
                }
            } catch (IOException e1) {

                final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
                LOG.error(errorMsg, e1);
                throw new TradingApiException(errorMsg, e1);
            }
        } finally {
            if (exchangeConnection != null) {
                exchangeConnection.disconnect();
            }
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

            // Coinbase secret is in Base64 so we must decode it first.
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

    /**
     * Loads Exchange Adapter config.
     */
    private void loadConfig() {

        final String configFile = getConfigFileLocation();
        final Properties configEntries = new Properties();
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFile);

        if (inputStream == null) {
            final String errorMsg = "Cannot find Coinbase config at: " + configFile + " HINT: is it on BX-bot's classpath?";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            configEntries.load(inputStream);

            /*
             * Grab the passphrase
             */
            passphrase = configEntries.getProperty(PASSPHRASE_PROPERTY_NAME);

            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(PASSPHRASE_PROPERTY_NAME + ": " + passphrase);
//            }

            if (passphrase == null || passphrase.length() == 0) {
                final String errorMsg = PASSPHRASE_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            /*
             * Grab the public key
             */
            key = configEntries.getProperty(KEY_PROPERTY_NAME);

            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(KEY_PROPERTY_NAME + ": " + key);
//            }

            if (key == null || key.length() == 0) {
                final String errorMsg = KEY_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            /*
             * Grab the private key
             */
            secret = configEntries.getProperty(SECRET_PROPERTY_NAME);

            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(SECRET_PROPERTY_NAME + ": " + secret);
//            }

            if (secret == null || secret.length() == 0) {
                final String errorMsg = SECRET_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Grab the buy fee
            final String buyFeeInConfig = configEntries.getProperty(BUY_FEE_PROPERTY_NAME);
            if (buyFeeInConfig == null || buyFeeInConfig.length() == 0) {
                final String errorMsg = BUY_FEE_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            if (LOG.isInfoEnabled()) {
                LOG.info(BUY_FEE_PROPERTY_NAME + ": " + buyFeeInConfig + "%");
            }

            buyFeePercentage = new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
            if (LOG.isInfoEnabled()) {
                LOG.info("Buy fee % in BigDecimal format: " + buyFeePercentage);
            }

            // Grab the sell fee
            final String sellFeeInConfig = configEntries.getProperty(SELL_FEE_PROPERTY_NAME);
            if (sellFeeInConfig == null || sellFeeInConfig.length() == 0) {
                final String errorMsg = SELL_FEE_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            if (LOG.isInfoEnabled()) {
                LOG.info(SELL_FEE_PROPERTY_NAME + ": " + sellFeeInConfig + "%");
            }

            sellFeePercentage = new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
            if (LOG.isInfoEnabled()) {
                LOG.info("Sell fee % in BigDecimal format: " + sellFeePercentage);
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
     * Hack for unit-testing config loading.
     */
    private static String getConfigFileLocation() {
        return CONFIG_FILE;
    }

    /*
     * Hack for unit-testing map params passed to transport layer.
     */
    private Map<String, String> getRequestParamMap() {
        return new HashMap<>();
    }
}