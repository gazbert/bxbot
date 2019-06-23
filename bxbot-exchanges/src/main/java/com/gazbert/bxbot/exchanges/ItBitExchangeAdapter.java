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
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.TickerImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.Ticker;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Exchange Adapter for integrating with the itBit exchange. The itBit API is documented <a
 * href="https://www.itbit.com/h/api">here</a>.
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
 * could lose money. Despite running live on itBit, it has only been unit tested up until the point
 * of calling the {@link #sendPublicRequestToExchange(String)} and {@link
 * #sendAuthenticatedRequestToExchange(String, String, Map)} methods. Use it at our own risk!
 * </strong>
 *
 * <p>The adapter only supports the REST implementation of the <a
 * href="https://api.itbit.com/docs">Trading API</a>.
 *
 * <p>The itBit exchange uses XBT for the Bitcoin currency code instead of the usual BTC. So, if you
 * were to call {@link #getBalanceInfo()}, you would need to use XBT (instead of BTC) as the key
 * when fetching your Bitcoin balance info from the returned maps.
 *
 * <p>The adapter also assumes that only 1 exchange account wallet has been created on the exchange.
 * If there is more than 1, it will use the first one it finds when performing the {@link
 * #getBalanceInfo()} call.
 *
 * <p>Exchange fees are loaded from the exchange.yaml file on startup; they are not fetched from the
 * exchange at runtime as the itBit REST API v1 does not support this. The fees are used across all
 * markets. Make sure you keep an eye on the <a href="https://www.itbit.com/h/fees">exchange
 * fees</a> and update the config accordingly. There are different exchange fees for <a
 * href="https://www.itbit.com/h/fees-maker-taker-model">Takers and Makers</a> - this adapter will
 * use the <em>Taker</em> fees to keep things simple for now.
 *
 * <p>NOTE: ItBit requires all price values to be limited to 2 decimal places and amount values to
 * be limited to 4 decimal places when creating orders. This adapter truncates any prices with more
 * than 2 decimal places and rounds using {@link java.math.RoundingMode#HALF_EVEN}, E.g. 250.176
 * would be sent to the exchange as 250.18. The same is done for the order amount, but to 4 decimal
 * places.
 *
 * <p>The exchange regularly goes down for maintenance. If the keep-alive-during-maintenance
 * config-item is set to true in the exchange.yaml config file, the bot will stay alive and wait
 * until the next trade cycle.
 *
 * <p>The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single
 * thread in order to preserve trade execution order. The {@link URLConnection} achieves this by
 * blocking/waiting on the input stream (response) for each API call.
 *
 * <p>The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error
 * occurs trying to connect to the exchange. A {@link TradingApiException} is thrown for
 * <em>all</em> other failures.
 *
 * @author gazbert
 * @since 1.0
 */
public final class ItBitExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String ITBIT_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://api.itbit.com/" + ITBIT_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in itBit Exchange Adapter. ";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error.";
  private static final String UNDER_MAINTENANCE_WARNING_MESSAGE =
      "Exchange is undergoing maintenance - keep alive " + "is true.";

  private static final String USER_ID_PROPERTY_NAME = "userId";
  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
  private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";

  private static final String KEEP_ALIVE_DURING_MAINTENANCE_PROPERTY_NAME =
      "keep-alive-during-maintenance";
  private static final String EXCHANGE_UNDERGOING_MAINTENANCE_RESPONSE =
      "The itBit API is currently undergoing " + "maintenance";
  private static final String NULL_RESPONSE = "NULL RESPONSE";

  private static final String WALLETS_RESOURCE = "wallets";
  private static final String MARKETS_RESOURCE = "markets";

  private BigDecimal buyFeePercentage;
  private BigDecimal sellFeePercentage;

  private String walletId;
  private boolean keepAliveDuringMaintenance;

  private String userId = "";
  private String key = "";
  private String secret = "";

  private Mac mac;
  private boolean initializedMacAuthentication = false;
  private long nonce = 0;

  private Gson gson;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise itBit ExchangeConfig: " + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);
    setOtherConfig(config);

    nonce = System.currentTimeMillis() / 1000;
    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // itBit REST Trade API Calls adapted to the Trading API.
  // See https://api.itbit.com/docs
  // --------------------------------------------------------------------------

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {
    ExchangeHttpResponse response = null;

    try {
      if (walletId == null) {
        // need to fetch walletId if first API call
        getBalanceInfo();
      }

      final Map<String, String> params = createRequestParamMap();
      params.put("type", "limit");

      // note we need to limit amount to 4 decimal places else exchange will barf
      params.put("amount", new DecimalFormat("#.####", getDecimalFormatSymbols()).format(quantity));

      // Display param seems to be optional as per the itBit sample code:
      // https://github.com/itbit/itbit-restapi-python/blob/master/itbit_api.py - def create_order
      // params.put("display", new DecimalFormat("#.####",
      // getDecimalFormatSymbols()).format(quantity)); // use the same as amount

      // note we need to limit price to 2 decimal places else exchange will barf
      params.put("price", new DecimalFormat("#.##", getDecimalFormatSymbols()).format(price));

      params.put("instrument", marketId);

      // This param is unique for itBit - no other Exchange Adapter I've coded requires this :-/
      // A bit hacky below, but I'm not tweaking the Trading API createOrder() call just for itBit.
      params.put("currency", marketId.substring(0, 3));

      if (orderType == OrderType.BUY) {
        params.put("side", "buy");
      } else if (orderType == OrderType.SELL) {
        params.put("side", "sell");
      } else {
        final String errorMsg =
            "Invalid order type: "
                + orderType
                + " - Can only be "
                + OrderType.BUY.getStringValue()
                + " or "
                + OrderType.SELL.getStringValue();
        LOG.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      response =
          sendAuthenticatedRequestToExchange(
              "POST", WALLETS_RESOURCE + "/" + walletId + "/orders", params);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Create Order response: " + response);
      }

      if (response.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
        final ItBitNewOrderResponse itBitNewOrderResponse =
            gson.fromJson(response.getPayload(), ItBitNewOrderResponse.class);
        return itBitNewOrderResponse.id;
      } else {
        final String errorMsg = "Failed to create order on exchange. Details: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
        throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? "NULL_RESPONSE" : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  /*
   * marketId is not needed for cancelling orders on this exchange.
   */
  @Override
  public boolean cancelOrder(String orderId, String marketIdNotNeeded)
      throws TradingApiException, ExchangeNetworkException {
    ExchangeHttpResponse response = null;

    try {
      if (walletId == null) {
        // need to fetch walletId if first API call
        getBalanceInfo();
      }

      response =
          sendAuthenticatedRequestToExchange(
              "DELETE", WALLETS_RESOURCE + "/" + walletId + "/orders/" + orderId, null);
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
      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        final String underMaintenanceMsg =
            "Exchange is undergoing maintenance - keep alive is true.";
        LOG.warn(() -> underMaintenanceMsg);
        throw new ExchangeNetworkException(underMaintenanceMsg);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? NULL_RESPONSE : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response = null;

    try {
      if (walletId == null) {
        // need to fetch walletId if first API call
        getBalanceInfo();
      }

      final Map<String, String> params = createRequestParamMap();
      params.put("status", "open"); // we only want open orders

      response =
          sendAuthenticatedRequestToExchange(
              "GET", WALLETS_RESOURCE + "/" + walletId + "/orders", params);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Open Orders response: " + response);
      }

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final ItBitYourOrder[] itBitOpenOrders =
            gson.fromJson(response.getPayload(), ItBitYourOrder[].class);

        return adaptItBitOpenOrders(itBitOpenOrders, marketId);

      } else {
        final String errorMsg =
            "Failed to get your open orders from exchange. Details: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
        throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? NULL_RESPONSE : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response = null;

    try {
      response = sendPublicRequestToExchange(MARKETS_RESOURCE + "/" + marketId + "/order_book");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Market Orders response: " + response);
      }

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

        final ItBitOrderBookWrapper orderBook =
            gson.fromJson(response.getPayload(), ItBitOrderBookWrapper.class);

        final List<MarketOrder> buyOrders = new ArrayList<>();
        for (ItBitMarketOrder itBitBuyOrder : orderBook.bids) {
          final MarketOrder buyOrder =
              new MarketOrderImpl(
                  OrderType.BUY,
                  itBitBuyOrder.get(0),
                  itBitBuyOrder.get(1),
                  itBitBuyOrder.get(0).multiply(itBitBuyOrder.get(1)));
          buyOrders.add(buyOrder);
        }

        final List<MarketOrder> sellOrders = new ArrayList<>();
        for (ItBitMarketOrder itBitSellOrder : orderBook.asks) {
          final MarketOrder sellOrder =
              new MarketOrderImpl(
                  OrderType.SELL,
                  itBitSellOrder.get(0),
                  itBitSellOrder.get(1),
                  itBitSellOrder.get(0).multiply(itBitSellOrder.get(1)));
          sellOrders.add(sellOrder);
        }

        return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);
      } else {
        final String errorMsg =
            "Failed to get market order book from exchange. Details: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {

      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
        throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? NULL_RESPONSE : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response = null;

    try {
      response = sendPublicRequestToExchange(MARKETS_RESOURCE + "/" + marketId + "/ticker");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Latest Market Price response: " + response);
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
      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
        throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? NULL_RESPONSE : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  @Override
  public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response = null;

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(USER_ID_PROPERTY_NAME, userId);

      response = sendAuthenticatedRequestToExchange("GET", WALLETS_RESOURCE, params);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Balance Info response: " + response);
      }

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final ItBitWallet[] itBitWallets =
            gson.fromJson(response.getPayload(), ItBitWallet[].class);

        return adaptItBitBalanceInfo(itBitWallets);

      } else {
        final String errorMsg =
            "Failed to get your wallet balance info from exchange. Details: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
        throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? NULL_RESPONSE : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  /*
   * itBit does not provide API call for fetching % buy fee; it only provides the fee monetary
   * value for a given order via /wallets/{walletId}/trades API call. We load the % fee statically
   * from exchange.yaml file.
   */
  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return buyFeePercentage;
  }

  /*
   * itBit does not provide API call for fetching % sell fee; it only provides the fee monetary
   * value for a given order via/wallets/{walletId}/trades API call. We load the % fee statically
   * from exchange.yaml file.
   */
  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return sellFeePercentage;
  }

  @Override
  public String getImplName() {
    return "itBit REST API v1";
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response = null;

    try {
      response = sendPublicRequestToExchange(MARKETS_RESOURCE + "/" + marketId + "/ticker");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Ticker response: " + response);
      }

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final ItBitTicker itBitTicker = gson.fromJson(response.getPayload(), ItBitTicker.class);
        return new TickerImpl(
            itBitTicker.lastPrice,
            itBitTicker.bid,
            itBitTicker.ask,
            itBitTicker.low24h,
            itBitTicker.high24h,
            itBitTicker.openToday,
            itBitTicker.volume24h,
            itBitTicker.vwap24h,
            Date.from(Instant.parse(itBitTicker.serverTimeUtc)).getTime());
      } else {
        final String errorMsg = "Failed to get market ticker from exchange. Details: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
        LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
        throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
      }

      final String unexpectedErrorMsg =
          UNEXPECTED_ERROR_MSG + (response == null ? NULL_RESPONSE : response);
      LOG.error(unexpectedErrorMsg, e);
      throw new TradingApiException(unexpectedErrorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  //  See https://api.itbit.com/docs
  // --------------------------------------------------------------------------

  /**
   * GSON class for holding itBit order returned from: "Cancel Order"
   * /wallets/{walletId}/orders/{orderId} API call.
   *
   * <p>No payload returned by exchange on success, fields populated only on error.
   */
  private static class ItBitCancelOrderResponse {

    String code;
    String description;
    String requestId;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("code", code)
          .add("description", description)
          .add("requestId", requestId)
          .toString();
    }
  }

  /**
   * GSON class for holding itBit new order response from: "Create New Order" POST
   * /wallets/{walletId}/orders API call.
   *
   * <p>It is exactly the same as order returned in Get Orders response.
   */
  private static class ItBitNewOrderResponse extends ItBitYourOrder {}

  /**
   * GSON class for holding itBit order returned from: "Get Orders"
   * /wallets/{walletId}/orders{?instrument,page,perPage,status} API call.
   */
  private static class ItBitYourOrder {

    String id;
    String walletId;
    String side; // 'buy' or 'sell'
    String instrument; // the marketId e.g. 'XBTUSD'
    String type; // order type e.g. "limit"
    BigDecimal amount; // the original amount
    BigDecimal displayAmount; // ??? not documented in the REST API
    BigDecimal price;
    BigDecimal volumeWeightedAveragePrice;
    BigDecimal amountFilled;
    String createdTime; // e.g. "2015-10-01T18:10:39.3930000Z"
    String status; // e.g. "open"
    String clientOrderIdentifier; // cool - broker support :-)

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
          .add("clientOrderIdentifier", clientOrderIdentifier)
          .toString();
    }
  }

  /**
   * GSON class for holding itBit ticker returned from: "Get Order Book"
   * /markets/{tickerSymbol}/order_book API call.
   */
  private static class ItBitOrderBookWrapper {

    List<ItBitMarketOrder> bids;
    List<ItBitMarketOrder> asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("bids", bids).add("asks", asks).toString();
    }
  }

  /**
   * GSON class for holding Market Orders. First element in array is price, second element is
   * amount.
   */
  private static class ItBitMarketOrder extends ArrayList<BigDecimal> {

    private static final long serialVersionUID = -4959711260747077759L;
  }

  /**
   * GSON class for holding itBit ticker returned from: "Get Ticker" /markets/{tickerSymbol}/ticker
   * API call.
   */
  private static class ItBitTicker {

    String pair; // e.g. XBTUSD
    BigDecimal bid;
    BigDecimal bidAmt;
    BigDecimal ask;
    BigDecimal askAmt;
    BigDecimal lastPrice;
    BigDecimal lastAmt;
    BigDecimal volume24h;
    BigDecimal volumeToday;
    BigDecimal high24h;
    BigDecimal low24h;
    BigDecimal highToday;
    BigDecimal lowToday;
    BigDecimal openToday;
    BigDecimal vwapToday;
    BigDecimal vwap24h;

    @SerializedName("serverTimeUTC")
    String serverTimeUtc;

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
          .add("volume24h", volume24h)
          .add("volumeToday", volumeToday)
          .add("high24h", high24h)
          .add("low24h", low24h)
          .add("highToday", highToday)
          .add("lowToday", lowToday)
          .add("openToday", openToday)
          .add("vwapToday", vwapToday)
          .add("vwap24h", vwap24h)
          .add("serverTimeUtc", serverTimeUtc)
          .toString();
    }
  }

  /**
   * GSON class for holding itBit wallets returned from: "Get All Wallets"
   * /wallets{?userId,page,perPage} API call.
   */
  private static class ItBitWallet {

    String id;
    String userId;
    String name;
    List<ItBitBalance> balances;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add(USER_ID_PROPERTY_NAME, userId)
          .add("name", name)
          .add("balances", balances)
          .toString();
    }
  }

  /** GSON class for holding itBit wallet balances. */
  private static class ItBitBalance {

    BigDecimal availableBalance;
    BigDecimal totalBalance;
    String currency; // e.g. USD

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("availableBalance", availableBalance)
          .add("totalBalance", totalBalance)
          .add("currency", currency)
          .toString();
    }
  }

  // --------------------------------------------------------------------------
  //  Transport layer
  // --------------------------------------------------------------------------

  private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod)
      throws ExchangeNetworkException, TradingApiException {
    try {
      final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod);
      return makeNetworkRequest(url, "GET", null, createHeaderParamMap());

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  /*
   * Makes an authenticated API call to the itBit exchange.
   *
   * Quite complex, but well documented: https://api.itbit.com/docs#faq-2.-how-do-i-sign-a-request
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String httpMethod, String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
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
        params = createRequestParamMap();
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

          signatureParamList.add(
              requestBodyForSignature); // request body is empty JSON string for a GET
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
          signatureParamList.add(
              requestBodyForSignature); // request body is empty JSON string for a DELETE
          break;

        default:
          throw new IllegalArgumentException(
              "Don't know how to build secure [" + httpMethod + "] request!");
      }

      // Add the nonce
      signatureParamList.add(Long.toString(nonce));

      // Add the UNIX time
      signatureParamList.add(unixTime);

      /*
       * Convert that array to JSON, encoded as UTF-8. The resulting JSON should contain no
       * spaces or other whitespace characters. For example, a valid JSON-encoded array might look
       * like:
       * '["GET","https://api.itbit.com/v1/wallets/7e037345-1288-4c39-12fe-d0f99a475a98","","5",
       * "1405385860202"]'
       */
      final String signatureParamsInJson = gson.toJson(signatureParamList);
      LOG.debug(() -> "Signature params in JSON: " + signatureParamsInJson);

      // Prepend the string version of the nonce to the JSON-encoded array string
      final String noncePrependedToJson = nonce + signatureParamsInJson;

      // Construct the SHA-256 hash of the noncePrependedToJson. Call this the message hash.
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(noncePrependedToJson.getBytes(StandardCharsets.UTF_8));
      final byte[] messageHash = md.digest();

      // Prepend the UTF-8 encoded request URL to the message hash.
      // Generate the SHA-512 HMAC of the prependRequestUrlToMsgHash using your API secret as the
      // key.
      mac.reset(); // force reset
      mac.update(invocationUrl.getBytes(StandardCharsets.UTF_8));
      mac.update(messageHash);

      final String signature = DatatypeConverter.printBase64Binary(mac.doFinal());

      // Request headers required by Exchange
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("Content-Type", "application/json");

      // Add Authorization header
      // Generate the authorization header by concatenating the client key with a colon separator
      // (‘:’)
      // and the signature. The resulting string should look like "clientkey:signature".
      requestHeaders.put("Authorization", key + ":" + signature);

      requestHeaders.put("X-Auth-Timestamp", unixTime);
      requestHeaders.put("X-Auth-Nonce", Long.toString(nonce));

      final URL url = new URL(invocationUrl);
      return makeNetworkRequest(url, httpMethod, requestBody, requestHeaders);

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);

    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to create SHA-256 digest when building message signature.";
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  /*
   * Initialises the secure messaging layer.
   * Sets up the MAC to safeguard the data we send to the exchange.
   * Used to encrypt the hash of the entire message with the private key to ensure message
   * integrity. We fail hard n fast if any of this stuff blows.
   */
  private void initSecureMessageLayer() {
    try {
      final SecretKeySpec keyspec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
      mac = Mac.getInstance("HmacSHA512");
      mac.init(keyspec);
      initializedMacAuthentication = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA512 installed?";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = "Failed to setup MAC security. Secret key seems invalid!";
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods
  // --------------------------------------------------------------------------

  private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
    userId = getAuthenticationConfigItem(authenticationConfig, USER_ID_PROPERTY_NAME);
    key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
    secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
  }

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    final String buyFeeInConfig = getOtherConfigItem(otherConfig, BUY_FEE_PROPERTY_NAME);
    buyFeePercentage =
        new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    LOG.info(() -> "Buy fee % in BigDecimal format: " + buyFeePercentage);

    final String sellFeeInConfig = getOtherConfigItem(otherConfig, SELL_FEE_PROPERTY_NAME);
    sellFeePercentage =
        new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    LOG.info(() -> "Sell fee % in BigDecimal format: " + sellFeePercentage);

    final String keepAliveDuringMaintenanceConfig =
        getOtherConfigItem(otherConfig, KEEP_ALIVE_DURING_MAINTENANCE_PROPERTY_NAME);
    if (keepAliveDuringMaintenanceConfig != null && !keepAliveDuringMaintenanceConfig.isEmpty()) {
      keepAliveDuringMaintenance = Boolean.valueOf(keepAliveDuringMaintenanceConfig);
      LOG.info(() -> "Keep Alive During Maintenance: " + keepAliveDuringMaintenance);
    } else {
      LOG.info(() -> KEEP_ALIVE_DURING_MAINTENANCE_PROPERTY_NAME + " is not set in exchange.yaml");
    }
  }

  // --------------------------------------------------------------------------
  //  Util methods
  // --------------------------------------------------------------------------

  private List<OpenOrder> adaptItBitOpenOrders(ItBitYourOrder[] itBitOpenOrders, String marketId)
      throws TradingApiException {

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
              "Unrecognised order type received in getYourOpenOrders(). Value: "
                  + itBitOpenOrder.side);
      }

      final OpenOrder order =
          new OpenOrderImpl(
              itBitOpenOrder.id,
              Date.from(
                  Instant.parse(
                      itBitOpenOrder.createdTime)), // format: 2015-10-01T18:10:39.3930000Z
              marketId,
              orderType,
              itBitOpenOrder.price,
              itBitOpenOrder.amount.subtract(
                  itBitOpenOrder.amountFilled), // remaining - not provided by itBit
              itBitOpenOrder.amount,
              itBitOpenOrder.price.multiply(
                  itBitOpenOrder.amount)); // total - not provided by itBit

      ordersToReturn.add(order);
    }
    return ordersToReturn;
  }

  private BalanceInfoImpl adaptItBitBalanceInfo(ItBitWallet[] itBitWallets) {
    // assume only 1 trading account wallet being used on exchange
    final ItBitWallet exchangeWallet = itBitWallets[0];

    /*
     * If this is the first time to fetch the balance/wallet info, store the wallet UUID for
     * future calls. The Trading Engine will always call this method first, before any user
     * Trading Strategies are invoked, so any of the other Trading API methods that rely on the
     * wallet UUID will be satisfied.
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

    // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by
    // exchange.
    return new BalanceInfoImpl(balancesAvailable, new HashMap<>());
  }

  private void initGson() {
    // We need to disable HTML escaping for this adapter else GSON will change = to unicode for
    // query strings, e.g.
    // https://api.itbit.com/v1/wallets?userId=56DA621F -->
    // https://api.itbit.com/v1/wallets?userId\u003d56DA621F
    final GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
    gson = gsonBuilder.create();
  }

  private static boolean isExchangeUndergoingMaintenance(ExchangeHttpResponse response) {
    if (response != null) {
      final String payload = response.getPayload();
      return payload != null && payload.contains(EXCHANGE_UNDERGOING_MAINTENANCE_RESPONSE);
    }
    return false;
  }

  /*
   * Hack for unit-testing map params passed to transport layer.
   */
  private Map<String, String> createRequestParamMap() {
    return new HashMap<>();
  }

  /*
   * Hack for unit-testing header params passed to transport layer.
   */
  private Map<String, String> createHeaderParamMap() {
    return new HashMap<>();
  }

  /*
   * Hack for unit-testing transport layer.
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
