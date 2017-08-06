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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;

/**
 * <p>
 * Exchange Adapter for integrating with the itBit exchange.
 * The itBit API is documented <a href="https://www.itbit.com/h/api">here</a>.
 * </p>
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on itBit, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String)} and {@link #sendAuthenticatedRequestToExchange(String, String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 * <p>
 * The adapter only supports the REST implementation of the <a href="https://api.itbit.com/docs">Trading API</a>.
 * </p>
 * <p>The itBit exchange uses XBT for the Bitcoin currency code instead of the usual BTC. So, if you were to call
 * {@link #getBalanceInfo()}, you would need to use XBT (instead of BTC) as the key when fetching your Bitcoin balance
 * info from the returned maps.</p>
 * <p>
 * The adapter also assumes that only 1 exchange account wallet has been created on the exchange. If there is more
 * than 1, it will use the first one it finds when performing the {@link #getBalanceInfo()} call.
 * </p>
 * <p>
 * Exchange fees are loaded from the exchange.xml file on startup; they are not fetched from the exchange at
 * runtime as the itBit REST API v1 does not support this. The fees are used across all markets. Make sure you keep an
 * eye on the <a href="https://www.itbit.com/h/fees">exchange fees</a> and update the config accordingly.
 * There are different exchange fees for <a href="https://www.itbit.com/h/fees-maker-taker-model">Takers and Makers</a>
 * - this adapter will use the <em>Taker</em> fees to keep things simple for now.
 * </p>
 * <p>
 * NOTE: ItBit requires all price values to be limited to 2 decimal places and amount values to be limited to 4 decimal
 * places when creating orders. This adapter truncates any prices with more than 2 decimal places and rounds using
 * {@link java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be sent to the exchange as 250.18. The same is done for
 * the order amount, but to 4 decimal places.
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
public final class ItBitExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * The version of the itBit API being used.
     */
    private static final String ITBIT_API_VERSION = "v1";

    /**
     * The public API URI.
     * The itBit Production Host is: https://api.itbit.com/v1/
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.itbit.com/" + ITBIT_API_VERSION + "/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 25 Sep 2015.
     */
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in itBit Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Name of user id property in config file.
     */
    private static final String USER_ID_PROPERTY_NAME = "userId";

    /**
     * Name of PUBLIC key property in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret property in config file.
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
     * Nonce used for sending authenticated messages to the exchange.
     */
    private long nonce = 0;

    /**
     * The UUID of the wallet in use on the exchange.
     */
    private String walletId;

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
     * The user id.
     */
    private String userId = "";

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
     * GSON engine used for parsing JSON in itBit API call responses.
     */
    private Gson gson;


    @Override
    public void init(ExchangeConfig config) {

        LOG.info(() -> "About to initialise itBit ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);
        setOptionalConfig(config);

        nonce = System.currentTimeMillis() / 1000; // set the initial nonce used in the secure messaging.
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // itBit REST Trade API Calls adapted to the Trading API.
    // See https://api.itbit.com/docs
    // ------------------------------------------------------------------------------------------------

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeNetworkException {

        ExchangeHttpResponse response = null;

        try {

            if (walletId == null) {
                // need to fetch walletId if first API call
                getBalanceInfo();
            }

            final Map<String, String> params = getRequestParamMap();
            params.put("type", "limit");

            // note we need to limit amount to 4 decimal places else exchange will barf
            params.put("amount", new DecimalFormat("#.####").format(quantity));

            // Display param seems to be optional as per the itBit sample code:
            // https://github.com/itbit/itbit-restapi-python/blob/master/itbit_api.py - def create_order
            // params.put("display", new DecimalFormat("#.####").format(quantity)); // use the same as amount

            // note we need to limit price to 2 decimal places else exchange will barf
            params.put("price", new DecimalFormat("#.##").format(price));

            params.put("instrument", marketId);

            // This param is unique for itBit - no other Exchange Adapter I've coded requires this :-/
            // A bit hacky below, but I'm not tweaking the Trading API createOrder() call just for itBit.
            params.put("currency", marketId.substring(0, 3));

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

            // Adapter does not using optional clientOrderIdentifier and clientOrderIdentifier params.
            // params.put("metadata", "{}");
            // params.put("clientOrderIdentifier", "id_123");

            response = sendAuthenticatedRequestToExchange(
                    "POST", "wallets/" + walletId + "/orders", params);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Create Order response: " + response);
            }

            if (response.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                final ItBitNewOrderResponse itBitNewOrderResponse = gson.fromJson(response.getPayload(),
                        ItBitNewOrderResponse.class);
                return itBitNewOrderResponse.id;
            } else {
                final String errorMsg = "Failed to create order on exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            final String unexpectedErrorMsg = UNEXPECTED_ERROR_MSG + (response == null ? "NULL RESPONSE" : response);
            LOG.error(unexpectedErrorMsg, e);
            throw new TradingApiException(unexpectedErrorMsg, e);
        }
    }

    /*
     * marketId is not needed for cancelling orders on this exchange.
     */
    @Override
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeNetworkException {

        ExchangeHttpResponse response = null;

        try {

            if (walletId == null) {
                // need to fetch walletId if first API call
                getBalanceInfo();
            }

            response = sendAuthenticatedRequestToExchange(
                    "DELETE", "wallets/" + walletId + "/orders/" + orderId, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cancel Order response: " + response);
            }

            if (response.getStatusCode() == HttpURLConnection.HTTP_ACCEPTED) {
                gson.fromJson(response.getPayload(), ItBitCancelOrderResponse.class);
                return true;
            } else {
                final String errorMsg = "Failed to cancel order on exchange. Details: " + response;
                LOG.error(errorMsg);
                return false;
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            final String unexpectedErrorMsg = UNEXPECTED_ERROR_MSG + (response == null ? "NULL RESPONSE" : response);
            LOG.error(unexpectedErrorMsg, e);
            throw new TradingApiException(unexpectedErrorMsg, e);
        }
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        ExchangeHttpResponse response = null;

        try {

            if (walletId == null) {
                // need to fetch walletId if first API call
                getBalanceInfo();
            }

            final Map<String, String> params = getRequestParamMap();
            params.put("status", "open"); // we only want open orders

            response = sendAuthenticatedRequestToExchange(
                    "GET", "wallets/" + walletId + "/orders", params);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Open Orders response: " + response);
            }

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final ItBitYourOrder[] itBitOpenOrders = gson.fromJson(response.getPayload(), ItBitYourOrder[].class);

                // adapt
                final List<OpenOrder> ordersToReturn = new ArrayList<>();
                for (final ItBitYourOrder itBitOpenOrder : itBitOpenOrders) {

                    if (!marketId.equalsIgnoreCase(itBitOpenOrder.instrument)) {
                        continue;
                    }

                    OrderType orderType;
                    switch (itBitOpenOrder.side) {
                        case "buy":
                            orderType = OrderType.BUY;
                            break;
                        case "sell":
                            orderType = OrderType.SELL;
                            break;
                        default:
                            throw new TradingApiException(
                                    "Unrecognised order type received in getYourOpenOrders(). Value: " + itBitOpenOrder.side);
                    }

                    final OpenOrder order = new OpenOrder(
                            itBitOpenOrder.id,
                            Date.from(Instant.parse(itBitOpenOrder.createdTime)), // format: 2015-10-01T18:10:39.3930000Z
                            marketId,
                            orderType,
                            itBitOpenOrder.price,
                            itBitOpenOrder.amount.subtract(itBitOpenOrder.amountFilled), // remaining - not provided by itBit
                            itBitOpenOrder.amount,
                            itBitOpenOrder.price.multiply(itBitOpenOrder.amount)); // total - not provided by itBit

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
            final String unexpectedErrorMsg = UNEXPECTED_ERROR_MSG + (response == null ? "NULL RESPONSE" : response);
            LOG.error(unexpectedErrorMsg, e);
            throw new TradingApiException(unexpectedErrorMsg, e);
        }
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {

        ExchangeHttpResponse response = null;

        try {
            response = sendPublicRequestToExchange("markets/" + marketId + "/order_book");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Market Orders response: " + response);
            }

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final ItBitOrderBookWrapper orderBook = gson.fromJson(response.getPayload(), ItBitOrderBookWrapper.class);

                final List<MarketOrder> buyOrders = new ArrayList<>();
                for (ItBitMarketOrder itBitBuyOrder : orderBook.bids) {
                    final MarketOrder buyOrder = new MarketOrder(
                            OrderType.BUY,
                            itBitBuyOrder.get(0),
                            itBitBuyOrder.get(1),
                            itBitBuyOrder.get(0).multiply(itBitBuyOrder.get(1)));
                    buyOrders.add(buyOrder);
                }

                final List<MarketOrder> sellOrders = new ArrayList<>();
                for (ItBitMarketOrder itBitSellOrder : orderBook.asks) {
                    final MarketOrder sellOrder = new MarketOrder(
                            OrderType.SELL,
                            itBitSellOrder.get(0),
                            itBitSellOrder.get(1),
                            itBitSellOrder.get(0).multiply(itBitSellOrder.get(1)));
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
            final String unexpectedErrorMsg = UNEXPECTED_ERROR_MSG + (response == null ? "NULL RESPONSE" : response);
            LOG.error(unexpectedErrorMsg, e);
            throw new TradingApiException(unexpectedErrorMsg, e);
        }
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws TradingApiException, ExchangeNetworkException {

        ExchangeHttpResponse response = null;

        try {

            response = sendPublicRequestToExchange("markets/" + marketId + "/ticker");
            if (LOG.isDebugEnabled()) {
                LOG.debug( "Latest Market Price response: " + response);
            }

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final ItBitTicker itBitTicker = gson.fromJson(response.getPayload(), ItBitTicker.class);
                return itBitTicker.lastPrice;
            } else {
                final String errorMsg = "Failed to get market ticker from exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            final String unexpectedErrorMsg = UNEXPECTED_ERROR_MSG + (response == null ? "NULL RESPONSE" : response);
            LOG.error(unexpectedErrorMsg, e);
            throw new TradingApiException(unexpectedErrorMsg, e);
        }
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {

        ExchangeHttpResponse response = null;

        try {

            final Map<String, String> params = getRequestParamMap();
            params.put("userId", userId);

            response = sendAuthenticatedRequestToExchange("GET", "wallets", params);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Balance Info response: " + response);
            }

            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

                final ItBitWallet[] itBitWallets = gson.fromJson(response.getPayload(), ItBitWallet[].class);

                // assume only 1 trading account wallet being used on exchange
                final ItBitWallet exchangeWallet = itBitWallets[0];

                /*
                 * If this is the first time to fetch the balance/wallet info, store the wallet UUID for future calls.
                 * The Trading Engine will always call this method first, before any user Trading Strategies are invoked,
                 * so any of the other Trading API methods that rely on the wallet UUID will be satisfied.
                 */
                if (walletId == null) {
                    walletId = exchangeWallet.id;
                }

                // adapt
                final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
                final List<ItBitBalance> balances = exchangeWallet.balances;
                if (balances != null) {
                    for (final ItBitBalance balance : balances) {
                        balancesAvailable.put(balance.currency, balance.availableBalance);
                    }
                }

                // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by exchange.
                return new BalanceInfo(balancesAvailable, new HashMap<>());
            } else {
                final String errorMsg = "Failed to get your wallet balance info from exchange. Details: " + response;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            }

        } catch (ExchangeNetworkException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            final String unexpectedErrorMsg = UNEXPECTED_ERROR_MSG + (response == null ? "NULL RESPONSE" : response);
            LOG.error(unexpectedErrorMsg , e);
            throw new TradingApiException(unexpectedErrorMsg, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        // itBit does not provide API call for fetching % buy fee; it only provides the fee monetary value for a
        // given order via /wallets/{walletId}/trades API call. We load the % fee statically from exchange.xml file.
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        // itBit does not provide API call for fetching % sell fee; it only provides the fee monetary value for a
        // given order via/wallets/{walletId}/trades API call. We load the % fee statically from exchange.xml file.
        return sellFeePercentage;
    }

    @Override
    public String getImplName() {
        return "itBit REST API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://api.itbit.com/docs
    // ------------------------------------------------------------------------------------------------

    /**
     * <p>
     * GSON class for holding itBit order returned from:
     * "Cancel Order" /wallets/{walletId}/orders/{orderId} API call.
     * </p>
     * <p>
     * <p>
     * No payload returned by exchange on success.
     * </p>
     */
    private static class ItBitCancelOrderResponse {
    }

    /**
     * <p>
     * GSON class for holding itBit new order response from:
     * "Create New Order" POST /wallets/{walletId}/orders API call.
     * </p>
     * <p>
     * <p>
     * It is exactly the same as order returned in Get Orders response.
     * </p>
     */
    private static class ItBitNewOrderResponse extends ItBitYourOrder {
    }

    /**
     * GSON class for holding itBit order returned from:
     * "Get Orders" /wallets/{walletId}/orders{?instrument,page,perPage,status} API call.
     */
    private static class ItBitYourOrder {

        public String id;
        public String walletId;
        public String side; // 'buy' or 'sell'
        public String instrument; // the marketId e.g. 'XBTUSD'
        public String type; // order type e.g. "limit"
        public BigDecimal amount; // the original amount
        public BigDecimal displayAmount; // ??? not documented in the REST API
        public BigDecimal price;
        public BigDecimal volumeWeightedAveragePrice;
        public BigDecimal amountFilled;
        public String createdTime; // e.g. "2015-10-01T18:10:39.3930000Z"
        public String status; // e.g. "open"
        public ItBitOrderMetadata metadata; // {} value returned - no idea what this is
        public String clientOrderIdentifier; // cool - broker support :-)

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("walletId", walletId)
                    .add("side", side)
                    .add("instrument", instrument)
                    .add("type", type)
                    .add("amount", amount)
                    .add("displayAmount", displayAmount)
                    .add("price", price)
                    .add("volumeWeightedAveragePrice", volumeWeightedAveragePrice)
                    .add("amountFilled", amountFilled)
                    .add("createdTime", createdTime)
                    .add("status", status)
                    .add("metadata", metadata)
                    .add("clientOrderIdentifier", clientOrderIdentifier)
                    .toString();
        }
    }

    /**
     * GSON class for holding Your Order metadata. No idea what this is / or gonna be...
     */
    private static class ItBitOrderMetadata {
    }

    /**
     * GSON class for holding itBit ticker returned from:
     * "Get Order Book" /markets/{tickerSymbol}/order_book API call.
     */
    private static class ItBitOrderBookWrapper {

        public List<ItBitMarketOrder> bids;
        public List<ItBitMarketOrder> asks;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("bids", bids)
                    .add("asks", asks)
                    .toString();
        }
    }

    /**
     * GSON class for holding Market Orders. First element in array is price, second element is amount.
     */
    private static class ItBitMarketOrder extends ArrayList<BigDecimal> {
        private static final long serialVersionUID = -4959711260747077759L;
    }

    /**
     * GSON class for holding itBit ticker returned from:
     * "Get Ticker" /markets/{tickerSymbol}/ticker API call.
     */
    private static class ItBitTicker {

        // field names map to the JSON arg names
        public String pair; // e.g. XBTUSD
        public BigDecimal bid;
        public BigDecimal bidAmt;
        public BigDecimal ask;
        public BigDecimal askAmt;
        public BigDecimal lastPrice; // we only wants this precious
        public BigDecimal lastAmt;
        public BigDecimal lastvolume24hAmt;
        public BigDecimal volumeToday;
        public BigDecimal high24h;
        public BigDecimal low24h;
        public BigDecimal highToday;
        public BigDecimal lowToday;
        public BigDecimal openToday;
        public BigDecimal vwapToday;
        public BigDecimal vwap24h;
        public String serverTimeUTC;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pair", pair)
                    .add("bid", bid)
                    .add("bidAmt", bidAmt)
                    .add("ask", ask)
                    .add("askAmt", askAmt)
                    .add("lastPrice", lastPrice)
                    .add("lastAmt", lastAmt)
                    .add("lastvolume24hAmt", lastvolume24hAmt)
                    .add("volumeToday", volumeToday)
                    .add("high24h", high24h)
                    .add("low24h", low24h)
                    .add("highToday", highToday)
                    .add("lowToday", lowToday)
                    .add("openToday", openToday)
                    .add("vwapToday", vwapToday)
                    .add("vwap24h", vwap24h)
                    .add("serverTimeUTC", serverTimeUTC)
                    .toString();
        }
    }

    /**
     * GSON class for holding itBit wallets returned from:
     * "Get All Wallets" /wallets{?userId,page,perPage} API call.
     */
    private static class ItBitWallet {

        public String id;
        public String userId;
        public String name;
        public List<ItBitBalance> balances;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("userId", userId)
                    .add("name", name)
                    .add("balances", balances)
                    .toString();
        }
    }

    /**
     * GSON class for holding itBit wallet balances.
     */
    private static class ItBitBalance {

        public BigDecimal availableBalance;
        public BigDecimal totalBalance;
        public String currency; // e.g. USD

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("availableBalance", availableBalance)
                    .add("totalBalance", totalBalance)
                    .add("currency", currency)
                    .toString();
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the itBit exchange.
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
     * Makes an authenticated API call to the itBit exchange.
     * </p>
     * <p>
     * <p>
     * Quite complex, but well documented
     * <a href="https://api.itbit.com/docs#faq-2.-how-do-i-sign-a-request?">here.</a>
     * </p>
     *
     * @param httpMethod the HTTP method to use, e.g. GET, POST, DELETE
     * @param apiMethod  the API method to call.
     * @param params     the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException      if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(String httpMethod, String apiMethod, Map<String, String> params)
            throws ExchangeNetworkException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {

            // Generate new UNIX time in secs
            final String unixTime = Long.toString(System.currentTimeMillis());

            // increment nonce for use in this call
            nonce++;

            if (params == null) {
                // create empty map for non-param API calls
                params = new HashMap<>();
            }

            /*
             * Construct an array of UTF-8 encoded strings. That array should contain, in order,
             * the http verb of the request being signed (e.g. “GET”), the full url of the request,
             * the body of the message being sent, the nonce as a string, and the timestamp as a string.
             * If the request has no body, an empty string should be used.
             */
            final String invocationUrl;
            String requestBody = "";
            String requestBodyForSignature = "";
            final List<String> signatureParamList = new ArrayList<>();
            signatureParamList.add(httpMethod);

            switch (httpMethod) {

                case "GET":
                    LOG.debug(() -> "Building secure GET request...");

                    // Build (optional) query param string
                    final StringBuilder queryParamBuilder = new StringBuilder();
                    for (final Map.Entry<String, String> param : params.entrySet()) {
                        if (queryParamBuilder.length() > 0) {
                            queryParamBuilder.append("&");
                        }
                        // Don't URL encode as it messed up the UUID params, e.g. wallet id
                        //queryParams += param + "=" + URLEncoder.encode(params.get(param));
                        queryParamBuilder.append(param.getKey());
                        queryParamBuilder.append("=");
                        queryParamBuilder.append(param.getValue());
                    }

                    final String queryParams = queryParamBuilder.toString();
                    LOG.debug(() -> "Query param string: " + queryParams);

                    if (params.isEmpty()) {
                        invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                        signatureParamList.add(invocationUrl);
                    } else {
                        invocationUrl = AUTHENTICATED_API_URL + apiMethod + "?" + queryParams;
                        signatureParamList.add(invocationUrl);
                    }

                    signatureParamList.add(requestBodyForSignature); // request body is empty JSON string for a GET
                    break;

                case "POST":
                    LOG.debug(() -> "Building secure POST request...");

                    invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    signatureParamList.add(invocationUrl);

                    requestBody = gson.toJson(params);
                    signatureParamList.add(requestBody);
                    break;

                case "DELETE":
                    LOG.debug(() -> "Building secure DELETE request...");

                    invocationUrl = AUTHENTICATED_API_URL + apiMethod;
                    signatureParamList.add(invocationUrl);
                    signatureParamList.add(requestBodyForSignature); // request body is empty JSON string for a DELETE
                    break;

                default:
                    throw new IllegalArgumentException("Don't know how to build secure [" + httpMethod + "] request!");
            }

            // Add the nonce
            signatureParamList.add(Long.toString(nonce));

            // Add the UNIX time
            signatureParamList.add(unixTime);

            /*
             * Convert that array to JSON, encoded as UTF-8. The resulting JSON should contain no spaces or other
             * whitespace characters. For example, a valid JSON-encoded array might look like:
             * '["GET","https://api.itbit.com/v1/wallets/7e037345-1288-4c39-12fe-d0f99a475a98","","5","1405385860202"]'
             */
            final String signatureParamsInJson = gson.toJson(signatureParamList);
            LOG.debug(() -> "Signature params in JSON: " + signatureParamsInJson);

            // Prepend the string version of the nonce to the JSON-encoded array string
            final String noncePrependedToJson = Long.toString(nonce) + signatureParamsInJson;

            // Construct the SHA-256 hash of the noncePrependedToJson. Call this the message hash.
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(noncePrependedToJson.getBytes("UTF-8"));
            final byte[] messageHash = md.digest();

            // Prepend the UTF-8 encoded request URL to the message hash.
            // Generate the SHA-512 HMAC of the prependRequestUrlToMsgHash using your API secret as the key.
            mac.reset(); // force reset
            mac.update(invocationUrl.getBytes("UTF-8"));
            mac.update(messageHash);

            final String signature = DatatypeConverter.printBase64Binary(mac.doFinal());

            // Request headers required by Exchange
            final Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/json");

            // Add Authorization header
            // Generate the authorization header by concatenating the client key with a colon separator (‘:’)
            // and the signature. The resulting string should look like "clientkey:signature".
            requestHeaders.put("Authorization", key + ":" + signature);

            requestHeaders.put("X-Auth-Timestamp", unixTime);
            requestHeaders.put("X-Auth-Nonce", Long.toString(nonce));

            final URL url = new URL(invocationUrl);
            return sendNetworkRequest(url, httpMethod, requestBody, requestHeaders);

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);

        } catch (NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to create SHA-256 digest when building message signature.";
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
        userId = getAuthenticationConfigItem(authenticationConfig, USER_ID_PROPERTY_NAME);
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

        // We need to disable HTML escaping for this adapter else GSON will change = to unicode for query strings, e.g.
        // https://api.itbit.com/v1/wallets?userId=56DA621F --> https://api.itbit.com/v1/wallets?userId\u003d56DA621F
        final GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
        gson = gsonBuilder.create();
    }

    /*
     * Hack for unit-testing map params passed to transport layer.
     */
    private Map<String, String> getRequestParamMap() {
        return new HashMap<>();
    }
}
