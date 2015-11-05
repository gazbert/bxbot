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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * <em>TODO - Remove tmp PATCH in {@link #sendAuthenticatedRequestToExchange(String, Map)} for occasional 400 orders responses sent by exchange</em>
 * </p>
 *
 * <p>
 * Exchange Adapter for integrating with the Bitfinex exchange.
 * The Bitfinex API is documented <a href="https://www.bitfinex.com/pages/api">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Bitfinex, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String)} and {@link #sendAuthenticatedRequestToExchange(String, Map)}
 * methods. Use it at our own risk!
 * </strong>
 * </p>
 *
 * <p>
 * The adapter uses v1 of the Bitfinex API - it is limited to 60 API calls per minute. It only supports 'exchange'
 * accounts; it does <em>not</em> support 'trading' (margin trading) accounts or 'deposit' (liquidity SWAPs) accounts.
 * Furthermore, the adapter does not support sending 'hidden' orders.
 * </p>
 *
 * <p>
 * There are different exchange fees for Takers and Makers - see <a href="https://www.bitfinex.com/pages/fees">here.</a>
 * This adapter will use the <em>Taker</em> fees to keep things simple for now.
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
public final class BitfinexExchangeAdapter implements TradingApi {

    private static final Logger LOG = Logger.getLogger(BitfinexExchangeAdapter.class);

    /**
     * The version of the Bitfinex API being used.
     */
    private static final String BITFINEX_API_VERSION = "v1";

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.bitfinex.com/" + BITFINEX_API_VERSION + "/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 8 Sep 2015.
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
     * Your Bitfinex API keys and connection timeout config.
     * This file must be on BX-bot's runtime classpath located at: ./resources/bitfinex/bitfinex-config.properties
     */
    private static final String CONFIG_FILE = "bitfinex/bitfinex-config.properties";

    /**
     * Name of PUBLIC key prop in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret prop in config file.
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
     * GSON engine used for parsing JSON in Bitfinex API call responses.
     */
    private Gson gson;


    /**
     * Constructor initialises the Exchange Adapter for using the Bitfinex API.
     */
    public BitfinexExchangeAdapter() {

        // set the initial nonce used in the secure messaging.
        nonce = System.currentTimeMillis() / 1000;

        loadConfig();
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Bitfinex API Calls adapted to the Trading API.
    // See https://www.bitfinex.com/pages/api
    // ------------------------------------------------------------------------------------------------

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final String results = sendPublicRequestToExchange("book/" + marketId);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getMarketOrders() response: " + results);
            }

            final BitfinexOrderBook orderBook = gson.fromJson(results, BitfinexOrderBook.class);

            // adapt BUYs
            final List<MarketOrder> buyOrders = new ArrayList<>();
            for (BitfinexMarketOrder bitfinexBuyOrder : orderBook.bids) {
                final MarketOrder buyOrder = new MarketOrder(
                        OrderType.BUY,
                        bitfinexBuyOrder.price,
                        bitfinexBuyOrder.amount,
                        bitfinexBuyOrder.price.multiply(bitfinexBuyOrder.amount));
                buyOrders.add(buyOrder);
            }

            // adapt SELLs
            final List<MarketOrder> sellOrders = new ArrayList<>();
            for (BitfinexMarketOrder bitfinexSellOrder : orderBook.asks) {
                final MarketOrder sellOrder = new MarketOrder(
                        OrderType.SELL,
                        bitfinexSellOrder.price,
                        bitfinexSellOrder.amount,
                        bitfinexSellOrder.price.multiply(bitfinexSellOrder.amount));
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
            final String results = sendAuthenticatedRequestToExchange("orders", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getYourOpenOrders() response: " + results);
            }

            final BitfinexOpenOrders bitfinexOpenOrders = gson.fromJson(results, BitfinexOpenOrders.class);

            // adapt
            final List<OpenOrder> ordersToReturn = new ArrayList<>();
            for (final BitfinexOpenOrder bitfinexOpenOrder : bitfinexOpenOrders) {
                OrderType orderType;
                switch (bitfinexOpenOrder.side) {
                    case "buy":
                        orderType = OrderType.BUY;
                        break;
                    case "sell":
                        orderType = OrderType.SELL;
                        break;
                    default:
                        throw new TradingApiException(
                                "Unrecognised order type received in getYourOpenOrders(). Value: " + bitfinexOpenOrder.type);
                }

                final OpenOrder order = new OpenOrder(
                        Long.toString(bitfinexOpenOrder.id),
                        // for some reason 'finex adds decimal point to long date value, e.g. "1442073766.0"  - grrrr!
                        Date.from(Instant.ofEpochMilli(Integer.parseInt(bitfinexOpenOrder.timestamp.split("\\.")[0]))),
                        marketId,
                        orderType,
                        bitfinexOpenOrder.price,
                        bitfinexOpenOrder.remaining_amount,
                        bitfinexOpenOrder.original_amount,
                        bitfinexOpenOrder.price.multiply(bitfinexOpenOrder.original_amount) // total - not provided by finex :-(
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
            final Map<String, Object> params = getRequestParamMap();

            params.put("symbol", marketId);

            // note we need to limit amount and price to 8 decimal places else exchange will barf
            params.put("amount", new DecimalFormat("#.########").format(quantity));
            params.put("price", new DecimalFormat("#.########").format(price));

            params.put("exchange", "bitfinex");

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

            // 'type' is either "market" / "limit" / "stop" / "trailing-stop" / "fill-or-kill" / "exchange market" /
            // "exchange limit" / "exchange stop" / "exchange trailing-stop" / "exchange fill-or-kill".
            // (type starting by "exchange " are exchange orders, others are margin trading orders)

            // this adapter only supports 'exchange limit orders'
            params.put("type", "exchange limit");

            // This adapter does not currently support hidden orders.
            // Exchange API notes: "true if the order should be hidden. Default is false."
            // If you try and set "is_hidden" to false, the exchange barfs and sends a 401 back. Nice.
            //params.put("is_hidden", "false");

            final String results = sendAuthenticatedRequestToExchange("order/new", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("createOrder() response: " + results);
            }

            final BitfinexNewOrderResponse createOrderResponse = gson.fromJson(results, BitfinexNewOrderResponse.class);
            final long id = createOrderResponse.order_id;
            if (id == 0) {
                final String errorMsg = "Failed to place order on exchange. Error response: " + results;
                LOG.error(errorMsg);
                throw new TradingApiException(errorMsg);
            } else {
                return Long.toString(createOrderResponse.order_id);
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
            final Map<String, Object> params = getRequestParamMap();
            params.put("order_id", Long.parseLong(orderId));
            final String results = sendAuthenticatedRequestToExchange("order/cancel", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("cancelOrder() response: " + results);
            }

            // Exchange returns order id and other details if successful, a 400 HTTP Status if the order id was not recognised.
            gson.fromJson(results, BitfinexCancelOrderResponse.class);
            return true;

        } catch (ExchangeTimeoutException | TradingApiException e) {
            if (e.getCause() != null && e.getCause().getMessage().contains("400")) {
                final String errorMsg = "Failed to cancel order on exchange. Did not recognise Order Id: " + orderId;
                LOG.error(errorMsg, e);
                return false;
            } else {
                throw e;
            }
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final String results = sendPublicRequestToExchange("pubticker/" + marketId);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getLatestMarketPrice() response: " + results);
            }

            final BitfinexTicker ticker = gson.fromJson(results, BitfinexTicker.class);
            return ticker.last_price;

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
            final String results = sendAuthenticatedRequestToExchange("balances", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getBalanceInfo() response: " + results);
            }

            final BitfinexBalances allAccountBalances = gson.fromJson(results, BitfinexBalances.class);

            // adapt
            final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();

            /*
             * The adapter only fetches the 'exchange' account balance details - this is the Bitfinex 'exchange' account,
             * i.e. the limit order trading account balance.
             */
            allAccountBalances.stream().filter(
                    accountBalance -> accountBalance.type.equalsIgnoreCase("exchange")).forEach(accountBalance -> {

                if (accountBalance.currency.equalsIgnoreCase("usd")) {
                    balancesAvailable.put("USD", accountBalance.available);
                } else if (accountBalance.currency.equalsIgnoreCase("btc")) {
                    balancesAvailable.put("BTC", accountBalance.available);
                }
            });

            // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by exchange.
            return new BalanceInfo(balancesAvailable, new HashMap<>());

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

        try {
            final String results = sendAuthenticatedRequestToExchange("account_infos", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getPercentageOfBuyOrderTakenForExchangeFee() response: " + results);
            }

            // Nightmare to adapt! Just take the top-level taker fees.
            final BitfinexAccountInfos bitfinexAccountInfos = gson.fromJson(results, BitfinexAccountInfos.class);
            final BigDecimal fee = bitfinexAccountInfos.get(0).taker_fees;

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
            ExchangeTimeoutException {

        try {
            final String results = sendAuthenticatedRequestToExchange("account_infos", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getPercentageOfSellOrderTakenForExchangeFee() response: " + results);
            }

            // Nightmare to adapt! Just take the top-level taker fees.
            final BitfinexAccountInfos bitfinexAccountInfos = gson.fromJson(results, BitfinexAccountInfos.class);
            final BigDecimal fee = bitfinexAccountInfos.get(0).taker_fees;

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
        return "Bitfinex API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.bitfinex.com/pages/api
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for a market Order Book.
     *
     * @author gazbert
     */
    private static class BitfinexOrderBook {

        // field names map to the JSON arg names
        public BitfinexMarketOrder[] bids;
        public BitfinexMarketOrder[] asks;

        @Override
        public String toString() {
            return BitfinexOrderBook.class.getSimpleName()
                    + " ["
                    + "bids=" + Arrays.toString(bids)
                    + ", asks=" + Arrays.toString(asks)
                    + "]";
        }
    }

    /**
     * GSON class for a Market Order.
     *
     * @author gazbert
     */
    private static class BitfinexMarketOrder {

        // field names map to the JSON arg names
        public BigDecimal price;
        public BigDecimal amount;
        public String timestamp;

        @Override
        public String toString() {
            return BitfinexMarketOrder.class.getSimpleName()
                    + " ["
                    + "price=" + price
                    + ", amount=" + amount
                    + ", timestamp=" + timestamp
                    + "]";
        }
    }

    /**
     * GSON class for receiving your open orders in 'orders' API call response.
     *
     * @author gazbert
     */
    private static class BitfinexOpenOrders extends ArrayList<BitfinexOpenOrder> {
        private static final long serialVersionUID = 5516523641153401953L;
    }

    /**
     * GSON class for mapping returned order from 'orders' API call response.
     *
     * @author gazbert
     */
    private static class BitfinexOpenOrder {

        public long id;
        public String symbol;
        public String exchange;
        public BigDecimal price;
        public BigDecimal avg_execution_price;
        public String side; // e.g. "sell"
        public String type; // e.g. "exchange limit"
        public String timestamp;
        public boolean is_live;
        public boolean is_cancelled;
        public boolean is_hidden;
        public boolean was_forced;
        public BigDecimal original_amount;
        public BigDecimal remaining_amount;
        public BigDecimal executed_amount;

        @Override
        public String toString() {
            return BitfinexOpenOrder.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", symbol=" + symbol
                    + ", exchange=" + exchange
                    + ", price=" + price
                    + ", avg_execution_price=" + avg_execution_price
                    + ", side=" + side
                    + ", type=" + type
                    + ", timestamp=" + timestamp
                    + ", is_live=" + is_live
                    + ", is_cancelled=" + is_cancelled
                    + ", is_hidden=" + is_hidden
                    + ", was_forced=" + was_forced
                    + ", original_amount=" + original_amount
                    + ", remaining_amount=" + remaining_amount
                    + ", executed_amount=" + executed_amount
                    + "]";
        }
    }

    /**
     * GSON class for a Bitfinex 'pubticker' API call response.
     *
     * @author gazbert
     */
    private static class BitfinexTicker {

        public BigDecimal mid;
        public BigDecimal bid;
        public BigDecimal ask;
        public BigDecimal last_price;
        public BigDecimal low;
        public BigDecimal high;
        public BigDecimal volume;
        public String timestamp;

        @Override
        public String toString() {
            return BitfinexTicker.class.getSimpleName()
                    + " [" +
                    "mid=" + mid
                    + ", bid=" + bid
                    + ", ask=" + ask
                    + ", last_price=" + last_price
                    + ", low=" + low
                    + ", high=" + high
                    + ", volume=" + volume
                    + ", timestamp=" + timestamp
                    + "]";
        }
    }

    /**
     * GSON class for holding Bitfinex response from 'account_infos' API call.
     *
     * This is a lot of work to just get the exchange fees!
     *
     * We want the taker fees.
     *
     * <pre>
     *  [
     *      {
     *          "maker_fees": "0.1",
     *          "taker_fees": "0.2",
     *          "fees": [
     *              {
     *                  "pairs": "BTC",
     *                  "maker_fees": "0.1",
     *                  "taker_fees": "0.2"
     *              },
     *              {
     *                  "pairs": "LTC",
     *                  "maker_fees": "0.1",
     *                  "taker_fees": "0.2"
     *              },
     *              {
     *                  "pairs": "DRK",
     *                  "maker_fees": "0.1",
     *                  "taker_fees": "0.2"
     *              }
     *          ]
     *      }
     *  ]
     * </pre>
     *
     * @author gazbert
     */
    private static class BitfinexAccountInfos extends ArrayList<BitfinexAccountInfo> {
        private static final long serialVersionUID = 5516521641453401953L;
    }

    /**
     * GSON class for holding Bitfinex Account Info.
     *
     * @author gazbert
     */
    private static class BitfinexAccountInfo {

        public BigDecimal maker_fees;
        public BigDecimal taker_fees;
        public BitfinexPairFees fees;

        @Override
        public String toString() {
            return BitfinexAccountInfo.class.getSimpleName()
                    + " [maker_fees=" + maker_fees
                    + ", taker_fees=" + taker_fees
                    + ", fees=" + fees + "]";
        }
    }


    /**
     * GSON class for holding Bitfinex Pair Fees.
     *
     * @author gazbert
     */
    private static class BitfinexPairFees extends ArrayList<BitfinexPairFee> {
        private static final long serialVersionUID = 1516526641473401953L;
    }

    /**
     * GSON class for holding Bitfinex Pair Fee.
     *
     * @author gazbert
     */
    private static class BitfinexPairFee {

        public String pairs;
        public BigDecimal maker_fees;
        public BigDecimal taker_fees;

        @Override
        public String toString() {
            return BitfinexAccountBalance.class.getSimpleName()
                    + " [pairs=" + pairs
                    + ", maker_fees=" + maker_fees
                    + ", taker_fees=" + taker_fees + "]";
        }
    }

    /**
     * GSON class for holding Bitfinex response from 'balances' API call.
     * <p>
     * Basically an array of BitfinexAccountBalance types.
     * <p>
     * <pre>
     * [
     * {"type":"deposit","currency":"btc","amount":"0.12347175","available":"0.001"},
     * {"type":"deposit","currency":"usd","amount":"0.0","available":"0.0"},
     * {"type":"exchange","currency":"btc","amount":"0.0","available":"0.0"},
     * {"type":"exchange","currency":"usd","amount":"0.0","available":"0.0"},
     * {"type":"trading","currency":"btc","amount":"0.0","available":"0.0"},
     * {"type":"trading","currency":"usd","amount":"0.0","available":"0.0"}
     * ]
     * </pre>
     *
     * @author gazbert
     */
    private static class BitfinexBalances extends ArrayList<BitfinexAccountBalance> {
        private static final long serialVersionUID = 5516523641953401953L;
    }

    /**
     * GSON class for holding a Bitfinex account type balance info.
     * <p>
     * There are 3 types of account: 'deposit' (swaps), 'exchange' (limit orders), 'trading' (margin).
     * <pre>
     * [
     * {"type":"deposit","currency":"btc","amount":"0.12347175","available":"0.001"},
     * {"type":"deposit","currency":"usd","amount":"0.0","available":"0.0"},
     * {"type":"exchange","currency":"btc","amount":"0.0","available":"0.0"},
     * {"type":"exchange","currency":"usd","amount":"0.0","available":"0.0"},
     * {"type":"trading","currency":"btc","amount":"0.0","available":"0.0"},
     * {"type":"trading","currency":"usd","amount":"0.0","available":"0.0"}
     * ]
     * </pre>
     *
     * @author gazbert
     */
    private static class BitfinexAccountBalance {

        // field names map to the JSON arg names
        public String type;
        public String currency;
        public BigDecimal amount;
        public BigDecimal available;

        @Override
        public String toString() {
            return BitfinexAccountBalance.class.getSimpleName()
                    + " [type=" + type
                    + ", currency=" + currency
                    + ", amount=" + amount
                    + ", available=" + available + "]";
        }
    }

    /**
     * GSON class for Bitfinex 'order/new' response.
     *
     * @author gazbert
     */
    private static class BitfinexNewOrderResponse {

        public long id; // same as order_id
        public String symbol;
        public String exchange;
        public BigDecimal price;
        public BigDecimal avg_execution_price;
        public String side; // e.g. "sell"
        public String type; // e.g. "exchange limit"
        public String timestamp;
        public boolean is_live;
        public boolean is_cancelled;
        public boolean is_hidden;
        public boolean was_forced;
        public BigDecimal original_amount;
        public BigDecimal remaining_amount;
        public BigDecimal executed_amount;
        public long order_id; // same as id

        @Override
        public String toString() {
            return BitfinexNewOrderResponse.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", symbol=" + symbol
                    + ", exchange=" + exchange
                    + ", price=" + price
                    + ", avg_execution_price=" + avg_execution_price
                    + ", side=" + side
                    + ", type=" + type
                    + ", timestamp=" + timestamp
                    + ", is_live=" + is_live
                    + ", is_cancelled=" + is_cancelled
                    + ", is_hidden=" + is_hidden
                    + ", was_forced=" + was_forced
                    + ", original_amount=" + original_amount
                    + ", remaining_amount=" + remaining_amount
                    + ", executed_amount=" + executed_amount
                    + ", order_id=" + order_id
                    + "]";
        }
    }

    /**
     * GSON class for Bitfinex 'order/cancel' response.
     *
     * @author gazbert
     */
    private static class BitfinexCancelOrderResponse {

        public long id; // only get this param; there is no order_id
        public String symbol;
        public String exchange;
        public BigDecimal price;
        public BigDecimal avg_execution_price;
        public String side; // e.g. "sell"
        public String type; // e.g. "exchange limit"
        public String timestamp;
        public boolean is_live;
        public boolean is_cancelled;
        public boolean is_hidden;
        public boolean was_forced;
        public BigDecimal original_amount;
        public BigDecimal remaining_amount;
        public BigDecimal executed_amount;

        @Override
        public String toString() {
            return BitfinexCancelOrderResponse.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", symbol=" + symbol
                    + ", exchange=" + exchange
                    + ", price=" + price
                    + ", avg_execution_price=" + avg_execution_price
                    + ", side=" + side
                    + ", type=" + type
                    + ", timestamp=" + timestamp
                    + ", is_live=" + is_live
                    + ", is_cancelled=" + is_cancelled
                    + ", is_hidden=" + is_hidden
                    + ", was_forced=" + was_forced
                    + ", original_amount=" + original_amount
                    + ", remaining_amount=" + remaining_amount
                    + ", executed_amount=" + executed_amount
                    + "]";
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to Bitfinex exchange. Uses HTTP GET.
     *
     * @param apiMethod the API method to call..
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private String sendPublicRequestToExchange(String apiMethod) throws ExchangeTimeoutException, TradingApiException {

        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

        try {

            final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod);
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
             * connectionTimeout is in SECONDS and comes from bitfinex-config.properties config.
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
                 * Occasionally get this on finex.
                 */
                if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {

                    final String errorMsg = "Failed to connect to Bitfinex. SSL Connection was reset by the server.";
                    LOG.error(errorMsg, e);
                    throw new ExchangeTimeoutException(errorMsg, e);

                /*
                 * Exchange sometimes fails with these codes, but recovers by next request...
                 */
                } else if (exchangeConnection != null && (exchangeConnection.getResponseCode() == 502
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
     * Makes Authenticated API call to Bitfinex exchange. Uses HTTP POST.
     * </p>
     *
     * <pre>
     * Bitfinex Example:
     *
     * POST https://api.bitfinex.com/v1/order/new
     *
     * With JSON payload of:
     *
     * {
     *    "request": "/v1/<request-type>
     *    "nonce": "1234",
     *    "other-params : "for the request if any..."
     * }
     *
     * To authenticate a request, we must calculate the following:
     *
     * payload = request-parameters-dictionary -> JSON encode -> base64
     * signature = HMAC-SHA384(payload, api-secret) as hexadecimal in lowercase (MUST be lowercase)
     * send (api-key, payload, signature)
     *
     * These are sent as HTTP headers named:
     *
     * X-BFX-APIKEY
     * X-BFX-PAYLOAD
     * X-BFX-SIGNATURE
     * </pre>
     *
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private String sendAuthenticatedRequestToExchange(String apiMethod, Map<String, Object> params)
            throws ExchangeTimeoutException, TradingApiException {

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
                // create empty map for non param API calls, e.g. "balances"
                params = new HashMap<>();
            }

            // nonce is required by Bitfinex in every request
            params.put("nonce", Long.toString(nonce));
            nonce++; // increment ready for next call.

            // must include the method in request param too
            params.put("request", "/" + BITFINEX_API_VERSION + "/" + apiMethod);

            // JSON-ify the param dictionary
            final String paramsInJson = gson.toJson(params);

            // Need to base64 encode payload as per API
            final String base64payload = DatatypeConverter.printBase64Binary(paramsInJson.getBytes());

            final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using following URL for API call: " + url);
            }

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);

            // Add the public key
            exchangeConnection.setRequestProperty("X-BFX-APIKEY", key);

            // Add Base64 encoded JSON payload
            exchangeConnection.setRequestProperty("X-BFX-PAYLOAD", base64payload);

            // Add the signature
            mac.reset(); // force reset
            mac.update(base64payload.getBytes());

            /*
             * signature = HMAC-SHA384(payload, api-secret) as hexadecimal - MUST be in LOWERCASE else signature fails.
             * See: http://bitcoin.stackexchange.com/questions/25835/bitfinex-api-call-returns-400-bad-request
             */
            final String signature = toHex(mac.doFinal()).toLowerCase();
            exchangeConnection.setRequestProperty("X-BFX-SIGNATURE", signature);

            // payload is JSON for this exchange
            exchangeConnection.setRequestProperty("Content-Type", "application/json");

            // Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            /*
             * Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
             * connectionTimeout is in SECONDS and comes from bitfinex-config.properties config.
             */
            final int timeoutInMillis = connectionTimeout * 1000;
            exchangeConnection.setConnectTimeout(timeoutInMillis);
            exchangeConnection.setReadTimeout(timeoutInMillis);

            // POST the request
            final OutputStreamWriter outputPostStream = new OutputStreamWriter(exchangeConnection.getOutputStream());
            outputPostStream.write(paramsInJson);
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
                 * Occasionally get this on finex.
                 */
                if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {

                    final String errorMsg = "Failed to connect to Bitfinex. SSL Connection was reset by the server.";
                    LOG.error(errorMsg, e);
                    throw new ExchangeTimeoutException(errorMsg, e);

                /*
                 * Exchange sometimes fails with these codes, but recovers by next request...
                 */
                } else if (exchangeConnection != null && (exchangeConnection.getResponseCode() == 502
                        || exchangeConnection.getResponseCode() == 503
                        || exchangeConnection.getResponseCode() == 504

                        // TODO - remove this tmp PATCH when Bitfinex fix their side or I find the bug in my code... ;-)
                        // Patch for exchange returning occasional 400 responses.
                        // java.io.IOException: Server returned HTTP response code: 400 for URL: https://api.bitfinex.com/v1/orders
                        || (exchangeConnection.getResponseCode() == 400 && apiMethod.equals("orders")))) {


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

    /**
     * Loads Exchange Adapter config.
     */
    private void loadConfig() {

        final String configFile = getConfigFileLocation();
        final Properties configEntries = new Properties();
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFile);

        if (inputStream == null) {
            final String errorMsg = "Cannot find Bitfinex config at: " + configFile + " HINT: is it on BX-bot's classpath?";
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
    private Map<String, Object> getRequestParamMap() {
        return new HashMap<>();
    }
}