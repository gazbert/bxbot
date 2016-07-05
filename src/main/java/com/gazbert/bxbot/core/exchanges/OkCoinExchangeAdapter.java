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
import com.gazbert.bxbot.core.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Exchange Adapter for integrating with the OKCoin exchange.
 * The OKCoin API is documented <a href="https://www.okcoin.com/about/rest_getStarted.do">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on OKCoin, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String, Map)} and {@link #sendAuthenticatedRequestToExchange(String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 *
 * <p>
 * It only supports the REST implementation of the <a href="https://www.okcoin.com/about/rest_api.do#stapi">Spot Trading API</a>.
 * </p>
 *
 * <p>
 * The exchange % buy and sell fees are currently loaded statically from the okcoin-config.properties file on startup;
 * they are not fetched from the exchange at runtime as the OKCoin API does not support this - it only provides the fee
 * monetary value for a given order id via the order_fee.do API call. The fees are used across all markets.
 * Make sure you keep an eye on the <a href="https://www.okcoin.com/about/fees.do">exchange fees</a> and update the
 * config accordingly.
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
public final class OkCoinExchangeAdapter extends AbstractExchangeAdapter implements TradingApi {

    private static final Logger LOG = Logger.getLogger(OkCoinExchangeAdapter.class);

    /**
     * The version of the OKCoin API being used.
     */
    private static final String OKCOIN_API_VERSION = "v1";

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://www.okcoin.com/api/" + OKCOIN_API_VERSION + "/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 17 Sep 2015.
     */
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in OKCoin Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Used for building error messages for missing config.
     */
    private static final String CONFIG_IS_NULL_OR_ZERO_LENGTH = " cannot be null or zero length! HINT: is the value set in the ";

    /**
     * Your OKCoin API keys and connection timeout config.
     * This file must be on BX-bot's runtime classpath located at: ./resources/okcoin/okcoin-config.properties
     */
    private static final String CONFIG_FILE = "okcoin/okcoin-config.properties";

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
     * The connection timeout in SECONDS for terminating hung connections to the exchange.
     */
    private int connectionTimeout;

    /**
     * Exchange buy fees in % in {@link BigDecimal} format.
     */
    private BigDecimal buyFeePercentage;

    /**
     * Exchange sell fees in % in {@link BigDecimal} format.
     */
    private BigDecimal sellFeePercentage;

    /**
     * Used to indicate if we have initialised the authentication and secure messaging layer.
     */
    private boolean initializedSecureMessagingLayer = false;

    /**
     * The key used in the secure message.
     */
    private String key = "";

    /**
     * The secret used for signing secure message.
     */
    private String secret = "";

    /**
     * The Message Digest generator used by the secure messaging layer.
     * Used to create the hash of the entire message with the private key to ensure message integrity.
     */
    private MessageDigest messageDigest;

    /**
     * GSON engine used for parsing JSON in OKCoin API call responses.
     */
    private Gson gson;


    /**
     * Constructor initialises the Exchange Adapter for using the OKCoin API.
     */
    public OkCoinExchangeAdapter() {
        loadConfig();
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // OKCoin REST Spot Trading API Calls adapted to the Trading API.
    // See https://www.okcoin.com/about/rest_getStarted.do
    // ------------------------------------------------------------------------------------------------

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeTimeoutException {

        try {

            final Map<String, String> params = getRequestParamMap();
            params.put("symbol", marketId);

            if (orderType == OrderType.BUY) {
                params.put("type", "buy");
            } else if (orderType == OrderType.SELL) {
                params.put("type", "sell");
            } else {
                final String errorMsg = "Invalid order type: " + orderType
                        + " - Can only be "
                        + OrderType.BUY.getStringValue() + " or "
                        + OrderType.SELL.getStringValue();
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            params.put("price", new DecimalFormat("#.########").format(price));

            // note we need to limit amount to 8 decimal places else exchange will barf
            params.put("amount", new DecimalFormat("#.########").format(quantity));

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("trade.do", params);
            LogUtils.log(LOG, Level.DEBUG, () -> "createOrder() response: " + response);

            final OKCoinTradeResponse createOrderResponse = gson.fromJson(response.getPayload(), OKCoinTradeResponse.class);
            if (createOrderResponse.result) {
                return Long.toString(createOrderResponse.order_id);
            } else {
                final String errorMsg = "Failed to place order on exchange. Error response: " + response;
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
    public boolean cancelOrder(String orderId, String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final Map<String, String> params = getRequestParamMap();
            params.put("order_id", orderId);
            params.put("symbol", marketId);

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("cancel_order.do", params);
            LogUtils.log(LOG, Level.DEBUG, () -> "cancelOrder() response: " + response);

            final OKCoinCancelOrderResponse cancelOrderResponse = gson.fromJson(response.getPayload(), OKCoinCancelOrderResponse.class);
            if (cancelOrderResponse.result) {
                return true;
            } else {
                final String errorMsg = "Failed to cancel order on exchange. Error response: " + response;
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

            final Map<String, String> params = getRequestParamMap();
            params.put("symbol", marketId);
            params.put("order_id", "-1"); // -1 means bring back all the orders

            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("order_info.do", params);
            LogUtils.log(LOG, Level.DEBUG, () -> "getYourOpenOrders() response: " + response);

            final OKCoinOrderInfoWrapper orderInfoWrapper = gson.fromJson(response.getPayload(), OKCoinOrderInfoWrapper.class);
            if (orderInfoWrapper.result) {

                // adapt
                final List<OpenOrder> ordersToReturn = new ArrayList<>();
                for (final OKCoinOpenOrder openOrder : orderInfoWrapper.orders) {
                    OrderType orderType;
                    switch (openOrder.type) {
                        case "buy":
                            orderType = OrderType.BUY;
                            break;
                        case "sell":
                            orderType = OrderType.SELL;
                            break;
                        default:
                            throw new TradingApiException(
                                    "Unrecognised order type received in getYourOpenOrders(). Value: " + openOrder.type);
                    }

                    final OpenOrder order = new OpenOrder(
                            Long.toString(openOrder.order_id),
                            new Date(openOrder.create_date),
                            marketId,
                            orderType,
                            openOrder.price,
                            openOrder.amount,
                            null, // orig_quantity - not provided by OKCoin :-(
                            openOrder.price.multiply(openOrder.amount) // total - not provided by OKCoin :-(
                    );

                    ordersToReturn.add(order);
                }
                return ordersToReturn;

            } else {
                final String errorMsg = "Failed to get Open Order Info from exchange. Error response: " + response;
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
            params.put("symbol", marketId);

            final ExchangeHttpResponse response = sendPublicRequestToExchange("depth.do", params);
            LogUtils.log(LOG, Level.DEBUG, () -> "getMarketOrders() response: " + response);

            final OKCoinDepthWrapper orderBook = gson.fromJson(response.getPayload(), OKCoinDepthWrapper.class);

            // adapt BUYs
            final List<MarketOrder> buyOrders = new ArrayList<>();
            for (OKCoinMarketOrder okCoinBuyOrder : orderBook.bids) {
                final MarketOrder buyOrder = new MarketOrder(
                        OrderType.BUY,
                        okCoinBuyOrder.get(0),
                        okCoinBuyOrder.get(1),
                        okCoinBuyOrder.get(0).multiply(okCoinBuyOrder.get(1)));
                buyOrders.add(buyOrder);
            }

            // adapt SELLs
            final List<MarketOrder> sellOrders = new ArrayList<>();
            for (OKCoinMarketOrder okCoinSellOrder : orderBook.asks) {
                final MarketOrder sellOrder = new MarketOrder(
                        OrderType.SELL,
                        okCoinSellOrder.get(0),
                        okCoinSellOrder.get(1),
                        okCoinSellOrder.get(0).multiply(okCoinSellOrder.get(1)));
                sellOrders.add(sellOrder);
            }

            // For some reason, OKCoin sorts ask orders in descending order instead of ascending.
            // We need to re-order price ascending - lowest ASK price will be first in list.
            Collections.sort(sellOrders, (thisOrder, thatOrder) -> {
                if (thisOrder.getPrice().compareTo(thatOrder.getPrice()) < 0) {
                    return -1;
                } else if(thisOrder.getPrice().compareTo(thatOrder.getPrice()) > 0) {
                    return 1;
                } else {
                    return 0; // same price
                }
            });

            return new MarketOrderBook(marketId, sellOrders, buyOrders);

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
            final Map<String, String> params = getRequestParamMap();
            params.put("symbol", marketId);

            final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker.do", params);
            LogUtils.log(LOG, Level.DEBUG, () -> "getLatestMarketPrice() response: " + response);

            final OKCoinTickerWrapper tickerWrapper = gson.fromJson(response.getPayload(), OKCoinTickerWrapper.class);
            return tickerWrapper.ticker.last;

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
            final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("userinfo.do", null);
            LogUtils.log(LOG, Level.DEBUG, () -> "getBalanceInfo() response: " + response);

            final OKCoinUserInfoWrapper userInfoWrapper = gson.fromJson(response.getPayload(), OKCoinUserInfoWrapper.class);
            if (userInfoWrapper.result) {

                // adapt
                final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
                for (final Map.Entry<String, BigDecimal> balance : userInfoWrapper.info.funds.free.entrySet()) {
                    balancesAvailable.put(balance.getKey().toUpperCase(), balance.getValue());
                }

                final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
                for (final Map.Entry<String, BigDecimal> balance : userInfoWrapper.info.funds.freezed.entrySet()) {
                    balancesOnOrder.put(balance.getKey().toUpperCase(), balance.getValue());
                }

                return new BalanceInfo(balancesAvailable, balancesOnOrder);

            } else {
                final String errorMsg = "Failed to get Balance Info from exchange. Error response: " + response;
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

        // OKCoin does not provide API call for fetching % buy fee; it only provides the fee monetary value for a
        // given order via order_fee.do API call. We load the % fee statically from okcoin-config.properties
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeTimeoutException {

        // OKCoin does not provide API call for fetching % sell fee; it only provides the fee monetary value for a
        // given order via order_fee.do API call. We load the % fee statically from okcoin-config.properties
        return sellFeePercentage;
    }

    @Override
    public String getImplName() {
        return "OKCoin REST Spot Trading API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.okcoin.com/about/rest_getStarted.do
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for wrapping cancel_order.do response.
     */
    public static class OKCoinCancelOrderResponse extends OKCoinMessageBase {

        public long order_id;

        @Override
        public String toString() {
            return OKCoinCancelOrderResponse.class.getSimpleName()
                    + " ["
                    + "order_id=" + order_id
                    + "]";
        }
    }

    /**
     * GSON class for wrapping trade.do response.
     */
    public static class OKCoinTradeResponse extends OKCoinMessageBase {

        public long order_id;

        @Override
        public String toString() {
            return OKCoinTradeResponse.class.getSimpleName()
                    + " ["
                    + "order_id=" + order_id
                    + "]";
        }
    }

    /**
     * GSON class for wrapping order_info.do response.
     *
     * @author gazbert
     */
    private static class OKCoinOrderInfoWrapper extends OKCoinMessageBase {

        // field names map to the JSON arg names
        public List<OKCoinOpenOrder> orders;

        @Override
        public String toString() {
            return OKCoinUserInfoWrapper.class.getSimpleName()
                    + " [orders=" + orders + "]";
        }
    }

    /**
     * GSON class for holding your open orders info from order_info.do API call.
     */
    private static class OKCoinOpenOrder {

        // field names map to the JSON arg names
        public BigDecimal amount;
        public BigDecimal avg_price;
        public long create_date;
        public BigDecimal deal_amount;
        public long order_id;
        public long orders_id; // deprecated
        public BigDecimal price;
        public int status; // -1 = cancelled, 0 = unfilled, 1 = partially filled, 2 = fully filled, 4 = cancel request in process
        public String symbol; // e.g. 'btc_usd'
        public String type; // 'sell' or 'buy'


        @Override
        public String toString() {
            return OKCoinOpenOrder.class.getSimpleName()
                    + " ["
                    + "amount=" + amount
                    + ", avg_price=" + avg_price
                    + ", create_date=" + create_date
                    + ", deal_amount=" + deal_amount
                    + ", order_id=" + order_id
                    + ", orders_id=" + orders_id
                    + ", price=" + price
                    + ", status=" + status
                    + ", symbol=" + symbol
                    + ", type=" + type
                    + "]";
        }
    }

    /**
     * GSON class for wrapping depth.do response.
     */
    private static class OKCoinDepthWrapper {

        public List<OKCoinMarketOrder> asks;
        public List<OKCoinMarketOrder> bids;

        @Override
        public String toString() {
            return OKCoinFundsInfo.class.getSimpleName()
                    + " ["
                    + "asks=" + asks
                    + ", bids=" + bids
                    + "]";
        }

    }

    /**
     * GSON class for holding Market Orders. First element in array is price, second element is amount.
     *
     * @author gazbert
     */
    private static class OKCoinMarketOrder extends ArrayList<BigDecimal> {
        private static final long serialVersionUID = -4919711260747077759L;
    }

    /**
     * GSON class for wrapping userinfo.do response.
     */
    private static class OKCoinUserInfoWrapper extends OKCoinMessageBase {

        public OKCoinUserInfo info;

        @Override
        public String toString() {
            return OKCoinUserInfoWrapper.class.getSimpleName()
                    + " [info=" + info + "]";
        }
    }


    /**
     * GSON class for holding funds in userinfo.do response.
     */
    private static class OKCoinUserInfo {

        public OKCoinFundsInfo funds;

        @Override
        public String toString() {
            return OKCoinUserInfo.class.getSimpleName()
                    + " [funds=" + funds + "]";
        }
    }

    /**
     * GSON class for holding funds info from userinfo.do response.
     */
    private static class OKCoinFundsInfo {

        public OKCoinAssetInfo asset;
        public OKCoinBalances free;
        public OKCoinBalances freezed;

        @Override
        public String toString() {
            return OKCoinFundsInfo.class.getSimpleName()
                    + " ["
                    + "asset=" + asset
                    + ", free=" + free
                    + ", freezed=" + freezed
                    + "]";
        }
    }

    /**
     * GSON class for holding asset info from userinfo.do response.
     */
    private static class OKCoinAssetInfo {

        public BigDecimal net;
        public BigDecimal total;

        @Override
        public String toString() {
            return OKCoinAssetInfo.class.getSimpleName()
                    + " ["
                    + "net=" + net
                    + ", total=" + total
                    + "]";
        }
    }

    /**
     * GSON class for holding wallet balances - basically a GSON enabled map.
     */
    private static class OKCoinBalances extends HashMap<String, BigDecimal> {
        private static final long serialVersionUID = -4919711060747077759L;
    }

    /**
     * GSON class for wrapping OKCoin ticker.do response.
     */
    private static class OKCoinTickerWrapper {

        public String date;
        public OKCoinTicker ticker;

        @Override
        public String toString() {
            return OKCoinTickerWrapper.class.getSimpleName() + " ["
                    + ", date=" + date
                    + ", ticker=" + ticker
                    + "]";
        }
    }

    /**
     * GSON class for a OKCoin ticker response.
     */
    private static class OKCoinTicker {

        public BigDecimal buy;
        public BigDecimal high;
        public BigDecimal last;
        public BigDecimal low;
        public BigDecimal sell;
        public BigDecimal vol;

        @Override
        public String toString() {
            return OKCoinTicker.class.getSimpleName() + " ["
                    + "buy=" + buy
                    + ", high=" + high
                    + ", last=" + last
                    + ", low=" + low
                    + ", sell=" + sell
                    + ", vol=" + vol
                    + "]";
        }
    }

    /**
     * GSON base class for API call requests and responses.
     */
    private static class OKCoinMessageBase {

        public int error_code; // will be 0 if not an error response
        public boolean result; // will be JSON boolean value in response: true or false


        @Override
        public String toString() {
            return OKCoinMessageBase.class.getSimpleName()
                    + " ["
                    + "result=" + result
                    + ", error_code=" + error_code
                    + "]";
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the OKCoin exchange.
     *
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod, Map<String, String> params) throws
            ExchangeTimeoutException, TradingApiException {

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

        try {
            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + queryString);
            return sendPublicNetworkRequest(url, connectionTimeout);

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * <p>
     * Makes an authenticated API call to the OKCoin exchange.
     * </p>
     *
     * <p>
     * A tricky one to build!
     * </p>
     *
     * <h2>POST payload generation</h2>
     *
     * <pre>
     * All parameters except for "sign" must be signed. The parameters must be re-ordered according to the
     * initials of the parameter name, alphabetically. For example, if the request parameters are
     * string[] parameters=
     *
     * {"api_key=c821db84-6fbd-11e4-a9e3-c86000d26d7c","symbol=btc_usd","type=buy","price=680","amount=1.0"};
     *
     * The result string is:
     * amount=1.0&api_key=c821db84-6fbd-11e4-a9e3-c86000d26d7c&price=680&symbol=btc_usd&type=buy
     * </pre>
     *
     * <h2>Signature creation</h2>
     *
     * <pre>
     * 'secretKey' is required to generate MD5 signature. Add the 'secret_Key' to the above string to generate the
     * final string to be signed, such as:
     *
     * amount=1.0&api_key=c821db84-6fbd-11e4-a9e3-c86000d26d7c&price=680&symbol=btc_usd&type=buy&secret_key=secretKey
     *
     * Note: '&secret_key=secretKey' is a must.
     * Use 32 bit MD5 encryption function to sign the string. Pass the encrypted string to 'sign' parameter.
     * Letters of the encrypted string must be in upper case.
     * </pre>
     *
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(String apiMethod, Map<String, String> params)
            throws ExchangeTimeoutException, TradingApiException {

        if (!initializedSecureMessagingLayer) {
            final String errorMsg = "Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {

            if (params == null) {
                params = new HashMap<>();
            }

            // we always need the API key
            params.put("api_key", key);

            String sortedQueryString = createAlphabeticallySortedQueryString(params);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sorted Query String without secret: " + sortedQueryString);
            }

            // Add secret key to Query String
            sortedQueryString += "&secret_key=" + secret;

            final String signature = createMd5HashAndReturnAsUpperCaseString(sortedQueryString);
            params.put("sign", signature);

            // Build the payload with all the param args in it
            final StringBuilder payload = new StringBuilder();
            for (final String param : params.keySet()) {
                if (payload.length() > 0) {
                    payload.append("&");
                }
                //noinspection deprecation
                payload.append(param).append("=").append(URLEncoder.encode(params.get(param)));
            }
            LogUtils.log(LOG, Level.DEBUG, () -> "Using following URL encoded POST payload for API call: " + payload);

            // Request headers required by Exchange
            final Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

            final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
            return sendAuthenticatedNetworkRequest(url, payload.toString(), requestHeaders, connectionTimeout);

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);
        }
    }

    /**
     * Sorts the request params alphabetically (uses natural ordering) and returns them as a query string.
     *
     * @param params the request params to sort.
     * @return the query string containing the sorted request params.
     */
    private String createAlphabeticallySortedQueryString(Map<String, String> params) {

        final List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); // OKCoin required natural/alphabetical ordering of params

        final StringBuilder sortedQueryString = new StringBuilder();
        for (final String param : keys) {
            if (sortedQueryString.length() > 0) {
                sortedQueryString.append("&");
            }
            //noinspection deprecation
            sortedQueryString.append(param).append("=").append(params.get(param));
        }
        return sortedQueryString.toString();
    }

    /**
     * Creates an MD5 hash for a given string and returns the hash as an uppercase string.
     *
     * @param stringToHash the string to create the MD5 hash for.
     * @return the MD5 hash as an uppercase string.
     */
    private String createMd5HashAndReturnAsUpperCaseString(String stringToHash) {

        final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        if (stringToHash == null || stringToHash.isEmpty()) {
            return "";
        }

        messageDigest.update(stringToHash.getBytes());
        final byte[] md5HashInBytes = messageDigest.digest();

        final StringBuilder md5HashAsUpperCaseString = new StringBuilder();
        for (final byte md5HashByte : md5HashInBytes) {
            md5HashAsUpperCaseString.append(HEX_DIGITS[(md5HashByte & 0xf0) >> 4]).append("").append(HEX_DIGITS[md5HashByte & 0xf]);
        }
        return md5HashAsUpperCaseString.toString();
    }

    /**
     * Initialises the secure messaging layer
     * Sets up the Message Digest to safeguard the data we send to the exchange.
     * We fail hard n fast if any of this stuff blows.
     */
    private void initSecureMessageLayer() {

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            initializedSecureMessagingLayer = true;
        } catch (NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to setup MessageDigest for secure message layer. Details: " + e.getMessage();
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
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
            final String errorMsg = "Cannot find OKCoin config at: " + configFile + " HINT: is it on BX-bot's classpath?";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            configEntries.load(inputStream);

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
            LogUtils.log(LOG, Level.INFO, () -> BUY_FEE_PROPERTY_NAME + ": " + buyFeeInConfig + "%");

            buyFeePercentage = new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
            LogUtils.log(LOG, Level.INFO, () -> "Buy fee % in BigDecimal format: " + buyFeePercentage);

            // Grab the sell fee
            final String sellFeeInConfig = configEntries.getProperty(SELL_FEE_PROPERTY_NAME);
            if (sellFeeInConfig == null || sellFeeInConfig.length() == 0) {
                final String errorMsg = SELL_FEE_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            LogUtils.log(LOG, Level.INFO, () -> SELL_FEE_PROPERTY_NAME + ": " + sellFeeInConfig + "%");

            sellFeePercentage = new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
            LogUtils.log(LOG, Level.INFO, () -> "Sell fee % in BigDecimal format: " + sellFeePercentage);

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
            LogUtils.log(LOG, Level.INFO, () -> CONNECTION_TIMEOUT_PROPERTY_NAME + ": " + connectionTimeout);

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