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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * <p>
 * Exchange Adapter for integrating with the Cryptsy exchange.
 * The Cryptsy API is documented <a href="https://www.cryptsy.com/pages/api">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on Cryptsy, it has only been unit tested up until the point of calling the
 * {@link #sendRequestToExchange(String, Map)} method. Use it at our own risk!
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
 * Exchange fees are currently loaded from the cryptsy-config.properties file on startup; they are not fetched from the
 * exchange at runtime as the Cryptsy v1 API does not support this. The fees are used across all markets. Make sure you
 * keep an eye on the <a href="https://www.cryptsy.com/pages/fees">exchange fees</a> and update the config accordingly.
 * </p>
 *
 * @author gazbert
 *
 */
public final class CryptsyExchangeAdapter implements TradingApi {

    private static final Logger LOG = Logger.getLogger(CryptsyExchangeAdapter.class);

    /**
     * The authenticated API URI.
     */
    private static final String AUTH_API_URL = "https://api.cryptsy.com/api";

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Cryptsy Exchange Adapter. ";

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
     * Your Cryptsy API keys, exchange fees, and connection timeout config.
     * This file must be on BX-bot's runtime classpath located at: ./resources/cryptsy/cryptsy-config.properties
     */
    private static final String CONFIG_FILE = "cryptsy/cryptsy-config.properties";

    /**
     * Name of public key property in config file.
     */
    private static final String PUBLIC_KEY_PROPERTY_NAME = "public-key";

    /**
     * Name of private key property in config file.
     */
    private static final String PRIVATE_KEY_PROPERTY_NAME = "private-key";

    /**
     * Name of connection timeout property in config file.
     */
    private static final String CONNECTION_TIMEOUT_PROPERTY_NAME = "connection-timeout";

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
    private static long nonce = 0;

    /**
     * The public key used in MAC authentication.
     */
    private String publicKey = "";

    /**
     * Our private key used in MAC authentication.
     */
    private String privateKey = "";

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
     * Flag to indicate if we have initialised the MAC authentication protocol.
     */
    private boolean initializedMACAuthentication = false;

    /**
     * Provides the "Message Authentication Code" (MAC) algorithm used for the secure messaging layer.
     * Used to encrypt the hash of the entire message with the private key to ensure message integrity.
     */
    private Mac mac;

    /**
     * GSON engine used for parsing JSON in the Cryptsy API call responses.
     */
    private Gson gson;


    /**
     * Constructor initialises the Exchange Adapter for using the Cryptsy API.
     */
    public CryptsyExchangeAdapter() {

        // set the initial nonce used in the secure messaging.
        nonce = System.currentTimeMillis() / 1000;

        loadConfig();
        initSecureMessageLayer();
        initGson();
    }

    // ------------------------------------------------------------------------------------------------
    // Cryptsy API Calls adapted to the Trading API.
    // See https://www.cryptsy.com/pages/privateapi
    // ------------------------------------------------------------------------------------------------

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeTimeoutException {

        try {
            final Map<String, String> params = getRequestParamMap();
            params.put("marketid", marketId);

            final String results = sendRequestToExchange("marketorders", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getMarketOrders() response: " + results);
            }

            final CryptsyMarketOrderBookWrapper marketOrderWrapper = gson.fromJson(results, CryptsyMarketOrderBookWrapper.class);

            // adapt BUYs
            final List<MarketOrder> buyOrders = new ArrayList<>();
            final CryptsyBuyOrder[] cryptsyBuyOrders = marketOrderWrapper.info.buyorders;
            for (CryptsyBuyOrder cryptsyBuyOrder : cryptsyBuyOrders) {
                final MarketOrder buyOrder = new MarketOrder(
                        OrderType.BUY,
                        cryptsyBuyOrder.buyprice,
                        cryptsyBuyOrder.quantity,
                        cryptsyBuyOrder.total);
                buyOrders.add(buyOrder);
            }

            // adapt SELLs
            final List<MarketOrder> sellOrders = new ArrayList<>();
            final CryptsySellOrder[] cryptsySellOrders = marketOrderWrapper.info.sellorders;
            for (CryptsySellOrder cryptsySellOrder : cryptsySellOrders) {
                final MarketOrder sellOrder = new MarketOrder(
                        OrderType.SELL,
                        cryptsySellOrder.sellprice,
                        cryptsySellOrder.quantity,
                        cryptsySellOrder.total);
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
            final Map<String, String> params = getRequestParamMap();
            params.put("marketid", marketId);

            final String results = sendRequestToExchange("myorders", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getYourOpenOrders() response: " + results);
            }

            final CryptsyMyOpenOrders myOpenOrders = gson.fromJson(results, CryptsyMyOpenOrders.class);

            // adapt
            final List<OpenOrder> ordersToReturn = new ArrayList<>();
            final CryptsyOrder[] cryptsyOrders = myOpenOrders.orders;
            for (CryptsyOrder cryptsyOrder : cryptsyOrders) {
                OrderType orderType;
                final String cryptsyOrderType = cryptsyOrder.ordertype;
                if (cryptsyOrderType.equalsIgnoreCase(OrderType.BUY.getStringValue())) {
                    orderType = OrderType.BUY;
                } else if (cryptsyOrderType.equalsIgnoreCase(OrderType.SELL.getStringValue())) {
                    orderType = OrderType.SELL;
                } else {
                    throw new TradingApiException(
                            "Unrecognised order type received in getYourOpenOrders(). Value: " + cryptsyOrderType);
                }

                final OpenOrder order = new OpenOrder(
                        Long.toString(cryptsyOrder.orderid),
                        cryptsyOrder.created,
                        marketId,
                        orderType,
                        cryptsyOrder.price,
                        cryptsyOrder.quantity,
                        cryptsyOrder.orig_quantity,
                        cryptsyOrder.total);

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
            final Map<String, String> params = getRequestParamMap();
            params.put("marketid", marketId);
            params.put("ordertype", orderType.getStringValue());

            // we need to limit to 8 decimal places else exchange will barf
            params.put("quantity", new DecimalFormat("#.########").format(quantity));
            params.put("price", new DecimalFormat("#.########").format(price));

            final String results = sendRequestToExchange("createorder", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("createOrder() response: " + results);
            }

            final CryptsyCreateOrderResponse createOrderResponse = gson.fromJson(results, CryptsyCreateOrderResponse.class);

            if (createOrderResponse.success == 1) {
                return Long.toString(createOrderResponse.orderid);
            } else {
                final String errorMsg = "Failed to place order on exchange. Error response: " + createOrderResponse.error
                        + " MoreInfo: " + createOrderResponse.moreinfo;
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
            final Map<String, String> params = getRequestParamMap();
            params.put("orderid", orderId);

            final String results = sendRequestToExchange("cancelorder", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("cancelOrder() response: " + results);
            }

            final CryptsyStringResults cancelOrderResponse = gson.fromJson(results, CryptsyStringResults.class);
            if (cancelOrderResponse.success == 1) {
                return true;
            } else {
                final String errorMsg = "Failed to cancel order on exchange. Error response: " + cancelOrderResponse.error
                        + " MoreInfo: " + cancelOrderResponse.info;
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
            final Map<String, String> params = getRequestParamMap();
            params.put("marketid", marketId);

            final String results = sendRequestToExchange("markettrades", params);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getLatestMarketPrice() response: " + results);
            }

            final CryptsyLatestTrades trades = gson.fromJson(results, CryptsyLatestTrades.class);

            // adapt
            final CryptsyTrade[] cryptsyTrades = trades.trades;
            if (cryptsyTrades.length == 0) {
                final String errorMsg = "Cryptsy 'markettrades' API call returned empty array of trades: " + Arrays.toString(cryptsyTrades);
                throw new TradingApiException(errorMsg);
            }

            // just take the latest one
            return new BigDecimal(cryptsyTrades[0].tradeprice);

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
            final String results = sendRequestToExchange("getinfo", null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getBalanceInfo() response: " + results);
            }

            final CryptsyInfoWrapper info = gson.fromJson(results, CryptsyInfoWrapper.class);

            // adapt
            return new BalanceInfo(info.info.balances_available, info.info.balances_hold);

        } catch (ExchangeTimeoutException | TradingApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(UNEXPECTED_ERROR_MSG, e);
            throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
        }
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
        // TODO get from Cryptsy API?
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
        // TODO get from Cryptsy API?
        return sellFeePercentage;
    }

    @Override
    public String getImplName() {
        return "Cryptsy Authenticated API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  GSON classes for JSON responses.
    //  See https://www.cryptsy.com/pages/privateapi
    // ------------------------------------------------------------------------------------------------

    /**
     * GSON class for the Order Book wrapper.
     *
     * @author gazbert
     */
    private static class CryptsyMarketOrderBookWrapper extends CryptsyMessageBase {

        // field names map to the JSON arg names
        @SerializedName("return")
        public CryptsyMarketOrderBook info;

        @Override
        public String toString() {
            return CryptsyMarketOrderBookWrapper.class.getSimpleName()
                    + " ["
                    + "info=" + info
                    + "]";
        }
    }

    /**
     * GSON class for a market Order Book.
     *
     * @author gazbert
     */
    private static class CryptsyMarketOrderBook {

        // field names map to the JSON arg names
        public CryptsySellOrder[] sellorders;
        public CryptsyBuyOrder[] buyorders;

        @Override
        public String toString() {
            return CryptsyMarketOrderBook.class.getSimpleName()
                    + " ["
                    + "sellorders=" + Arrays.toString(sellorders)
                    + ", buyorders=" + Arrays.toString(buyorders)
                    + "]";
        }
    }

    /**
     * GSON class for a BUY order.
     *
     * @author gazbert
     */
    private static class CryptsyBuyOrder {

        // field names map to the JSON arg names
        public BigDecimal buyprice;
        public BigDecimal quantity;
        public BigDecimal total;

        @Override
        public String toString() {
            return CryptsyBuyOrder.class.getSimpleName()
                    + " ["
                    + "buyprice=" + buyprice
                    + ", quantity=" + quantity
                    + ", total=" + total
                    + "]";
        }
    }

    /**
     * GSON class for a SELL order.
     *
     * @author gazbert
     */
    private static class CryptsySellOrder {

        public BigDecimal sellprice;
        public BigDecimal quantity;
        public BigDecimal total;

        @Override
        public String toString() {
            return CryptsySellOrder.class.getSimpleName()
                    + " ["
                    + "sellprice=" + sellprice
                    + ", quantity=" + quantity
                    + ", total=" + total
                    + "]";
        }
    }

    /**
     * GSON class used to receive response after creating an order.
     *
     * @author gazbert
     */
    private static class CryptsyCreateOrderResponse extends CryptsyMessageBase {

        public long orderid;
        public String moreinfo;

        @Override
        public String toString() {
            return CryptsyCreateOrderResponse.class.getSimpleName()
                    + " ["
                    + "orderid=" + orderid
                    + ", moreinfo=" + moreinfo
                    + "]";
        }
    }

    /**
     * GSON class for receiving my open orders in API call response.
     *
     * @author gazbert
     */
    private static class CryptsyMyOpenOrders extends CryptsyMessageBase {

        @SerializedName("return")
        public CryptsyOrder[] orders;

        @Override
        public String toString() {
            return CryptsyMyOpenOrders.class.getSimpleName()
                    + " ["
                    + "orders=" + Arrays.toString(orders)
                    + "]";
        }
    }

    /**
     * GSON class for mapping returned orders from API call response.
     *
     * @author gazbert
     */
    private static class CryptsyOrder {

        public long orderid;
        public Date created;
        public int marketid;
        public String ordertype;
        public BigDecimal price;
        public BigDecimal quantity;
        public BigDecimal orig_quantity;
        public BigDecimal total;

        @Override
        public String toString() {
            return CryptsyOrder.class.getSimpleName()
                    + " ["
                    + "orderid=" + orderid
                    + ", created=" + created
                    + ", marketid=" + marketid
                    + ", ordertype=" + ordertype
                    + ", price=" + price
                    + ", quantity=" + quantity
                    + ", orig_quantity=" + orig_quantity
                    + ", total=" + total
                    + "]";
        }
    }

    /**
     * GSON class for mapping last completed Trades from API call response.
     *
     * @author gazbert
     */
    private static class CryptsyLatestTrades extends CryptsyMessageBase {
        @SerializedName("return")
        public CryptsyTrade[] trades;
    }

    /**
     * GSON class for mapping a completed Trade from API call response.
     *
     * @author gazbert
     */
    private static class CryptsyTrade {

        public long tradeid;
        public String tradetype;
        public Date datetime;
        public int marketid;
        public double tradeprice;
        public double quantity;
        public double fee;
        public double total;
        public String initiate_ordertype;
        public long order_id;

        @Override
        public String toString() {
            return CryptsyTrade.class.getSimpleName()
                    + " ["
                    + "tradeid=" + tradeid
                    + ", tradetype=" + tradetype
                    + ", datetime=" + datetime
                    + ", marketid=" + marketid
                    + ", tradeprice=" + tradeprice
                    + ", quantity=" + quantity
                    + ", fee=" + fee
                    + ", total=" + total
                    + ", initiate_ordertype=" + initiate_ordertype
                    + ", order_id=" + order_id
                    + "]";
        }
    }

    /**
     * GSON base class for API call requests and responses.
     *
     * @author gazbert
     */
    private static class CryptsyMessageBase {

        public int success;
        public String error;

        public CryptsyMessageBase() {
            error = "";
        }

        @Override
        public String toString() {
            return CryptsyMessageBase.class.getSimpleName()
                    + " ["
                    + "success=" + success
                    + ", error=" + error
                    + "]";
        }
    }

    /**
     * GSON class for holding String results from API call requests/responses.
     *
     * @author gazbert
     */
    private static class CryptsyStringResults extends CryptsyMessageBase {
        @SerializedName("return")
        public String info;
    }

    /**
     * GSON class for wrapping Cryptsy Info response from getInfo() API call.
     *
     * @author gazbert
     */
    private static class CryptsyInfoWrapper extends CryptsyMessageBase {
        @SerializedName("return")
        public CryptsyInfo info;
    }

    /**
     * GSON class for holding Cryptsy info response from getInfo() API call.
     *
     * @author gazbert
     */
    private static class CryptsyInfo {

        public CryptsyBalances balances_available;
        public CryptsyBalances balances_hold;
        public long servertimestamp;
        public String servertimezone;
        public Date serverdatetime;
        public int openordercount;

        @Override
        public String toString() {
            return CryptsyInfo.class.getSimpleName()
                    + " [servertimestamp=" + servertimestamp
                    + ", servertimezone=" + servertimezone
                    + ", serverdatetime=" + serverdatetime
                    + ", openordercount=" + openordercount + "]";
        }
    }

    /**
     * GSON class for holding altcoin wallet balances - basically a GSON enabled map.
     *
     * @author gazbert
     */
    private static class CryptsyBalances extends HashMap<String, BigDecimal> {
        private static final long serialVersionUID = -4919716060747077759L;
    }

    /**
     * Deserializer is needed because Cryptsy causes GSON to throw IllegalStateException under certain circumstances:
     *
     * <pre>
     * TODO include the exception stack trace here...
     * </pre>
     *
     * @author gazbert
     */
    private class DateDeserializer implements JsonDeserializer<Date> {

        private SimpleDateFormat cryptsDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {

            Date dateFromCryptsy = null;
            if (json.isJsonPrimitive()) {
                try {
                    dateFromCryptsy = cryptsDateFormat.parse(json.getAsString());
                } catch (ParseException e) {
                    final String errorMsg = "DateDeserializer failed to parse a Cryptsy date!";
                    LOG.error(errorMsg, e);
                    throw new JsonParseException(errorMsg, e);
                }
            }
            return dateFromCryptsy;
        }
    }

    /**
     * Deserializer is needed because Cryptsy causes GSON to throw IllegalStateException under certain circumstances:
     *
     * <pre>
     * Exception in thread "main" com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1
     * at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:176)
     * at com.google.gson.Gson.fromJson(Gson.java:803)
     * at com.google.gson.Gson.fromJson(Gson.java:768)
     * at com.google.gson.Gson.fromJson(Gson.java:717)
     * at com.google.gson.Gson.fromJson(Gson.java:689)
     * at com.gazbert.bxbot.adapter.CryptsyExchangeAdapter.getBalanceInfo(CryptsyExchangeAdapter.java:313)
     * at com.gazbert.bxbot.core.engine.TradingEngine.checkAndBailIfBtcBalanceOnExchangeIsBelowEmergencyStopLimit(TradingEngine.java:280)
     * at com.gazbert.bxbot.core.engine.TradingEngine.startTradeCycle(TradingEngine.java:170)
     * at com.gazbert.bxbot.core.engine.TradingEngine.start(TradingEngine.java:150)
     * at com.gazbert.bxbot.core.engine.BXBot.main(BXBot.java:60)
     * Caused by: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1
     * at com.google.gson.stream.JsonReader.beginObject(JsonReader.java:374)
     * at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:165)
     * ... 9 more
     * </pre>
     */
    private class BalancesDeserializer implements JsonDeserializer<CryptsyBalances> {

        public CryptsyBalances deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {

            final CryptsyBalances balances = new CryptsyBalances();

            if (json.isJsonObject()) {
                final JsonObject jsonObject = json.getAsJsonObject();
                for (Entry<String, JsonElement> jsonOrder : jsonObject.entrySet()) {
                    final String currency = jsonOrder.getKey();
                    final BigDecimal balance = context.deserialize(jsonOrder.getValue(), BigDecimal.class);
                    balances.put(currency, balance);
                }
            }
            return balances;
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes an authenticated API call to the Cryptsy exchange.
     *
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeTimeoutException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private String sendRequestToExchange(String apiMethod, Map<String, String> params) throws
            ExchangeTimeoutException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

        try {

            // Setup common params for the API call
            if (params == null) {
                params = new HashMap<>();
            }
            params.put("method", apiMethod);
            params.put("nonce", Long.toString(nonce)); // nonce is required by Cryptsy in every request

            // Must be 1 higher for next call
            nonce++;

            // Build the URL with query param args in it - yuck!
            String postData = "";
            for (final String param : params.keySet()) {
                if (postData.length() > 0) {
                    postData += "&";
                }
                //noinspection deprecation
                postData += param + "=" + URLEncoder.encode(params.get(param));
            }

            final URL url = new URL(AUTH_API_URL);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using following URL for API call: " + url);
            }

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);

            // Add the public key
            exchangeConnection.setRequestProperty("Key", publicKey);

            // Sign the payload with the private key
            exchangeConnection.setRequestProperty("Sign", toHex(mac.doFinal(postData.getBytes("UTF-8"))));

            exchangeConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            /*
             * Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
             * Cryptsy sometimes gets stuck here for ~1 min once every half hour or so. Especially read timeouts.
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
                 * Exchange sometimes fails with these codes, but often recovers by next request...
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

        // Setup the MAC.
        try {
            final SecretKeySpec keyspec = new SecretKeySpec(privateKey.getBytes("UTF-8"), "HmacSHA512");
            mac = Mac.getInstance("HmacSHA512");
            mac.init(keyspec);
            initializedMACAuthentication = true;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA512 installed?";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        } catch (InvalidKeyException e) {
            final String errorMsg = "Failed to setup MAC security. Private key seems invalid!";
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

        System.out.println("configFile: " + configFile);

        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFile);

        if (inputStream == null) {
            final String errorMsg = "Cannot find Cryptsy config at: " + configFile + " HINT: is it on BX-bot's classpath?";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            configEntries.load(inputStream);

            // Grab the private key
            publicKey = configEntries.getProperty(PUBLIC_KEY_PROPERTY_NAME);
            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(PUBLIC_KEY_PROPERTY_NAME + ": " + publicKey);
//            }

            if (publicKey == null || publicKey.length() == 0) {
                final String errorMsg = PUBLIC_KEY_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Grab the private key
            privateKey = configEntries.getProperty(PRIVATE_KEY_PROPERTY_NAME);
            // WARNING: careful when you log this
//            if (LOG.isInfoEnabled()) {
//                LOG.info(PRIVATE_KEY_PROPERTY_NAME + ": " + privateKey);
//            }

            if (privateKey == null || privateKey.length() == 0) {
                final String errorMsg = PRIVATE_KEY_PROPERTY_NAME + CONFIG_IS_NULL_OR_ZERO_LENGTH + configFile + "?";
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

            // Grab the connection timeout
            connectionTimeout = Integer.parseInt(configEntries.getProperty(CONNECTION_TIMEOUT_PROPERTY_NAME));
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
     * Initialises GSON layer for processing JSON in the Cryptsy API call responses.
     */
    private void initGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
        gsonBuilder.registerTypeAdapter(CryptsyBalances.class, new BalancesDeserializer());
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