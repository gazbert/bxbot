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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DO NOT USE: See https://github.com/gazbert/bxbot/issues/122
 *
 * <p>Exchange Adapter for integrating with the OKCoin exchange. The OKCoin API is documented <a
 * href="https://www.okcoin.com/about/rest_getStarted.do">here</a>.
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
 * could lose money. Despite running live on OKCoin, it has only been unit tested up until the point
 * of calling the {@link #sendPublicRequestToExchange(String, Map)} and {@link
 * #sendAuthenticatedRequestToExchange(String, Map)} methods. Use it at our own risk! </strong>
 *
 * <p>It only supports the REST implementation of the <a
 * href="https://www.okcoin.com/about/rest_api.do#stapi">Spot Trading API</a>.
 *
 * <p>The exchange % buy and sell fees are currently loaded statically from the exchange.yaml file
 * on startup; they are not fetched from the exchange at runtime as the OKCoin API does not support
 * this - it only provides the fee monetary value for a given order id via the order_fee.do API
 * call. The fees are used across all markets. Make sure you keep an eye on the <a
 * href="https://www.okcoin.com/about/fees.do">exchange fees</a> and update the config accordingly.
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
 * @deprecated #120 : The OKCoin V1 API is now deprecated and no longer works - adapter needs
 *     updating to use V3 API.
 */
@Deprecated(forRemoval = true)
public final class OkCoinExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String OKCOIN_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://www.okcoin.com/api/" + OKCOIN_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in OKCoin Exchange Adapter. ";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error.";

  private static final String SYMBOL = "symbol";
  private static final String ORDER_ID = "orderId";

  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
  private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";

  private BigDecimal buyFeePercentage;
  private BigDecimal sellFeePercentage;

  private String key = "";
  private String secret = "";

  private MessageDigest messageDigest;
  private boolean initializedSecureMessagingLayer = false;

  private Gson gson;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise OKCoin ExchangeConfig: " + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);
    setOtherConfig(config);

    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // OKCoin REST Spot Trading API Calls adapted to the Trading API.
  // See https://www.okcoin.com/about/rest_getStarted.do
  // --------------------------------------------------------------------------

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(SYMBOL, marketId);

      if (orderType == OrderType.BUY) {
        params.put("type", "buy");
      } else if (orderType == OrderType.SELL) {
        params.put("type", "sell");
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

      params.put("price", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(price));

      // note we need to limit amount to 8 decimal places else exchange will barf
      params.put(
          "amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(quantity));

      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("trade.do", params);
      LOG.debug(() -> "Create Order response: " + response);

      final OkCoinTradeResponse createOrderResponse =
          gson.fromJson(response.getPayload(), OkCoinTradeResponse.class);
      if (createOrderResponse.result) {
        return Long.toString(createOrderResponse.orderId);
      } else {
        final String errorMsg = "Failed to place order on exchange. Error response: " + response;
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
  public boolean cancelOrder(String orderId, String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("order_id", orderId);
      params.put(SYMBOL, marketId);

      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("cancel_order.do", params);
      LOG.debug(() -> "Cancel Order response: " + response);

      final OkCoinCancelOrderResponse cancelOrderResponse =
          gson.fromJson(response.getPayload(), OkCoinCancelOrderResponse.class);
      if (cancelOrderResponse.result) {
        return true;
      } else {
        final String errorMsg = "Failed to cancel order on exchange. Error response: " + response;
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
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(SYMBOL, marketId);
      params.put("order_id", "-1"); // -1 means bring back all the orders

      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("order_info.do", params);
      LOG.debug(() -> "Open Orders response: " + response);

      final OkCoinOrderInfoWrapper orderInfoWrapper =
          gson.fromJson(response.getPayload(), OkCoinOrderInfoWrapper.class);
      if (orderInfoWrapper.result) {

        final List<OpenOrder> ordersToReturn = new ArrayList<>();
        for (final OkCoinOpenOrder openOrder : orderInfoWrapper.orders) {
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
                  "Unrecognised order type received in getYourOpenOrders(). Value: "
                      + openOrder.type);
          }

          final OpenOrder order =
              new OpenOrderImpl(
                  Long.toString(openOrder.orderId),
                  new Date(openOrder.createDate),
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
        final String errorMsg =
            "Failed to get Open Order Info from exchange. Error response: " + response;
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
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(SYMBOL, marketId);

      final ExchangeHttpResponse response = sendPublicRequestToExchange("depth.do", params);
      LOG.debug(() -> "Market Orders response: " + response);

      final OkCoinDepthWrapper orderBook =
          gson.fromJson(response.getPayload(), OkCoinDepthWrapper.class);

      final List<MarketOrder> buyOrders = new ArrayList<>();
      for (OkCoinMarketOrder okCoinBuyOrder : orderBook.bids) {
        final MarketOrder buyOrder =
            new MarketOrderImpl(
                OrderType.BUY,
                okCoinBuyOrder.get(0),
                okCoinBuyOrder.get(1),
                okCoinBuyOrder.get(0).multiply(okCoinBuyOrder.get(1)));
        buyOrders.add(buyOrder);
      }

      final List<MarketOrder> sellOrders = new ArrayList<>();
      for (OkCoinMarketOrder okCoinSellOrder : orderBook.asks) {
        final MarketOrder sellOrder =
            new MarketOrderImpl(
                OrderType.SELL,
                okCoinSellOrder.get(0),
                okCoinSellOrder.get(1),
                okCoinSellOrder.get(0).multiply(okCoinSellOrder.get(1)));
        sellOrders.add(sellOrder);
      }

      // For some reason, OKCoin sorts ask orders in descending order instead of ascending.
      // We need to re-order price ascending - lowest ASK price will be first in list.
      sellOrders.sort(
          (thisOrder, thatOrder) ->
              Integer.compare(thisOrder.getPrice().compareTo(thatOrder.getPrice()), 0));
      return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(SYMBOL, marketId);

      final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker.do", params);
      LOG.debug(() -> "Latest Market Price response: " + response);

      final OkCoinTickerWrapper tickerWrapper =
          gson.fromJson(response.getPayload(), OkCoinTickerWrapper.class);
      return tickerWrapper.ticker.last;

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
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("userinfo.do", null);
      LOG.debug(() -> "Balance Info response: " + response);

      final OkCoinUserInfoWrapper userInfoWrapper =
          gson.fromJson(response.getPayload(), OkCoinUserInfoWrapper.class);
      if (userInfoWrapper.result) {
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        for (final Map.Entry<String, BigDecimal> balance :
            userInfoWrapper.info.funds.free.entrySet()) {
          balancesAvailable.put(balance.getKey().toUpperCase(), balance.getValue());
        }

        final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
        for (final Map.Entry<String, BigDecimal> balance :
            userInfoWrapper.info.funds.freezed.entrySet()) {
          balancesOnOrder.put(balance.getKey().toUpperCase(), balance.getValue());
        }

        return new BalanceInfoImpl(balancesAvailable, balancesOnOrder);

      } else {
        final String errorMsg =
            "Failed to get Balance Info from exchange. Error response: " + response;
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
   * OKCoin does not provide API call for fetching % buy fee; it only provides the fee monetary
   * value for a given order via order_fee.do API call. We load the % fee statically from
   * exchange.yaml file.
   */
  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return buyFeePercentage;
  }

  /*
   * OKCoin does not provide API call for fetching % sell fee; it only provides the fee monetary
   * value for a given order via order_fee.do API call. We load the % fee statically from
   * exchange.yaml file.
   */
  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return sellFeePercentage;
  }

  @Override
  public String getImplName() {
    return "OKCoin REST Spot Trading API v1";
  }

  @Override
  public Ticker getTicker(String marketId) throws ExchangeNetworkException, TradingApiException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(SYMBOL, marketId);

      final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker.do", params);
      LOG.debug(() -> "Latest Market Price response: " + response);

      final OkCoinTickerWrapper tickerWrapper =
          gson.fromJson(response.getPayload(), OkCoinTickerWrapper.class);
      return new TickerImpl(
          tickerWrapper.ticker.last,
          tickerWrapper.ticker.buy,
          tickerWrapper.ticker.sell,
          tickerWrapper.ticker.low,
          tickerWrapper.ticker.high,
          null, // open not supplied by OKCoin
          tickerWrapper.ticker.vol,
          null, // vwap not supplied by OKCoin
          Long.valueOf(tickerWrapper.date));

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  //  See https://www.okcoin.com/about/rest_getStarted.do
  // --------------------------------------------------------------------------

  /** GSON class for wrapping cancel_order.do response. */
  public static class OkCoinCancelOrderResponse extends OkCoinMessageBase {

    @SerializedName("order_id")
    long orderId;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add(ORDER_ID, orderId).toString();
    }
  }

  /** GSON class for wrapping trade.do response. */
  public static class OkCoinTradeResponse extends OkCoinMessageBase {

    @SerializedName("order_id")
    long orderId;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add(ORDER_ID, orderId).toString();
    }
  }

  /** GSON class for wrapping order_info.do response. */
  private static class OkCoinOrderInfoWrapper extends OkCoinMessageBase {

    List<OkCoinOpenOrder> orders;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("orders", orders).toString();
    }
  }

  /** GSON class for holding your open orders info from order_info.do API call. */
  private static class OkCoinOpenOrder {

    BigDecimal amount;

    @SerializedName("avg_price")
    BigDecimal avgPrice;

    @SerializedName("create_date")
    long createDate;

    BigDecimal dealAmount;

    @SerializedName("order_id")
    long orderId;

    @SerializedName("orders_id")
    long ordersId; // deprecated

    BigDecimal price;

    /* -1 = cancelled, 0 = unfilled, 1 = partially filled, 2 = fully filled,
     * 4 = cancel request in process
     */
    int status;

    String symbol; // e.g. 'btc_usd'
    String type; // 'sell' or 'buy'

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("amount", amount)
          .add("avgPrice", avgPrice)
          .add("createDate", createDate)
          .add("dealAmount", dealAmount)
          .add(ORDER_ID, orderId)
          .add("ordersId", ordersId)
          .add("price", price)
          .add("status", status)
          .add(SYMBOL, symbol)
          .add("type", type)
          .toString();
    }
  }

  /** GSON class for wrapping depth.do response. */
  private static class OkCoinDepthWrapper {

    List<OkCoinMarketOrder> asks;
    List<OkCoinMarketOrder> bids;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("asks", asks).add("bids", bids).toString();
    }
  }

  /**
   * GSON class for holding Market Orders. First element in array is price, second element is
   * amount.
   */
  private static class OkCoinMarketOrder extends ArrayList<BigDecimal> {

    private static final long serialVersionUID = -4919711260747077759L;
  }

  /** GSON class for wrapping userinfo.do response. */
  private static class OkCoinUserInfoWrapper extends OkCoinMessageBase {

    OkCoinUserInfo info;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("info", info).toString();
    }
  }

  /** GSON class for holding funds in userinfo.do response. */
  private static class OkCoinUserInfo {

    OkCoinFundsInfo funds;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("funds", funds).toString();
    }
  }

  /** GSON class for holding funds info from userinfo.do response. */
  private static class OkCoinFundsInfo {

    OkCoinAssetInfo asset;
    OkCoinBalances free;
    OkCoinBalances freezed;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("asset", asset)
          .add("free", free)
          .add("freezed", freezed)
          .toString();
    }
  }

  /** GSON class for holding asset info from userinfo.do response. */
  private static class OkCoinAssetInfo {

    BigDecimal net;
    BigDecimal total;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("net", net).add("total", total).toString();
    }
  }

  /** GSON class for holding wallet balances - basically a GSON enabled map. */
  private static class OkCoinBalances extends HashMap<String, BigDecimal> {

    private static final long serialVersionUID = -4919711060747077759L;
  }

  /** GSON class for wrapping OKCoin ticker.do response. */
  private static class OkCoinTickerWrapper {

    String date;
    OkCoinTicker ticker;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("date", date).add("ticker", ticker).toString();
    }
  }

  /** GSON class for a OkCoin ticker response. */
  private static class OkCoinTicker {

    BigDecimal buy;
    BigDecimal high;
    BigDecimal last;
    BigDecimal low;
    BigDecimal sell;
    BigDecimal vol;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("buy", buy)
          .add("high", high)
          .add("last", last)
          .add("low", low)
          .add("sell", sell)
          .add("vol", vol)
          .toString();
    }
  }

  /** GSON base class for API call requests and responses. */
  private static class OkCoinMessageBase {

    @SerializedName("error_code")
    int errorCode; // will be 0 if not an error response

    boolean result; // will be JSON boolean value in response: true or false

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("errorCode", errorCode)
          .add("result", result)
          .toString();
    }
  }

  // --------------------------------------------------------------------------
  //  Transport layer methods
  // --------------------------------------------------------------------------

  private ExchangeHttpResponse sendPublicRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (params == null) {
      params = createRequestParamMap(); // no params, so empty query string
    }

    final Map<String, String> requestHeaders = createHeaderParamMap();

    try {
      final StringBuilder queryString = new StringBuilder();
      if (!params.isEmpty()) {
        queryString.append("?");
        for (final Map.Entry<String, String> param : params.entrySet()) {
          if (queryString.length() > 1) {
            queryString.append("&");
          }
          queryString.append(param.getKey());
          queryString.append("=");
          queryString.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }

        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
      }

      final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + queryString);
      return makeNetworkRequest(url, "GET", null, requestHeaders);

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  /*
   * Makes an authenticated API call to the OkCoin exchange.
   *
   * A tricky one to build!
   *
   * POST payload generation:
   *
   * All parameters except for "sign" must be signed. The parameters must be re-ordered according
   * to the initials of the parameter name, alphabetically. For example, if the request parameters
   * are string[] parameters=
   *
   * {"api_key=c821db84-6fbd-11e4-a9e3-c86000d26d7c","symbol=btc_usd","type=buy","price=680",
   * "amount=1.0"};
   *
   * The result string is:
   * amount=1.0&api_key=c821db84-6fbd-11e4-a9e3-c86000d26d7c&price=680&symbol=btc_usd&type=buy
   *
   * Signature creation:
   *
   * 'secretKey' is required to generate MD5 signature. Add the 'secret_Key' to the above string to
   * generate the final string to be signed, such as:
   *
   * amount=1.0&api_key=c821db84-6fbd-11e4-a9e3-c86000d26d7c&price=680&symbol=btc_usd
   * &type=buy&secret_key=secretKey
   *
   * Note: '&secret_key=secretKey' is a must.
   * Use 32 bit MD5 encryption function to sign the string. Pass the encrypted string to 'sign'
   * parameter. Letters of the encrypted string must be in upper case.
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedSecureMessagingLayer) {
      final String errorMsg = "Message security layer has not been initialized.";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        params = createRequestParamMap();
      }

      // we always need the API key
      params.put("api_key", key);

      String sortedQueryString = createAlphabeticallySortedQueryString(params);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Sorted Query String without secret: {}", sortedQueryString);
      }

      // Add secret key to Query String
      sortedQueryString += "&secret_key=" + secret;

      final String signature = createMd5HashAndReturnAsUpperCaseString(sortedQueryString);
      params.put("sign", signature);

      // Build the payload with all the param args in it
      final StringBuilder payload = new StringBuilder();
      for (final Map.Entry<String, String> param : params.entrySet()) {
        if (payload.length() > 0) {
          payload.append("&");
        }
        payload.append(param.getKey());
        payload.append("=");
        payload.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
      }
      LOG.debug(() -> "Using following URL encoded POST payload for API call: " + payload);

      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

      final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
      return makeNetworkRequest(url, "POST", payload.toString(), requestHeaders);

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  private String createMd5HashAndReturnAsUpperCaseString(String stringToHash) {
    final char[] hexDigits = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    if (stringToHash == null || stringToHash.isEmpty()) {
      return "";
    }

    messageDigest.update(stringToHash.getBytes(StandardCharsets.UTF_8));
    final byte[] md5HashInBytes = messageDigest.digest();

    final StringBuilder md5HashAsUpperCaseString = new StringBuilder();
    for (final byte md5HashByte : md5HashInBytes) {
      md5HashAsUpperCaseString
          .append(hexDigits[(md5HashByte & 0xf0) >> 4])
          .append(hexDigits[md5HashByte & 0xf]);
    }
    return md5HashAsUpperCaseString.toString();
  }

  /*
   * Initialises the secure messaging layer.
   * Sets up the MAC to safeguard the data we send to the exchange.
   * Used to encrypt the hash of the entire message with the private key to ensure message
   * integrity. We fail hard n fast if any of this stuff blows.
   */
  private void initSecureMessageLayer() {
    try {
      messageDigest = MessageDigest.getInstance("MD5");
      initializedSecureMessagingLayer = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg =
          "Failed to setup MessageDigest for secure message layer. Details: " + e.getMessage();
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods
  // --------------------------------------------------------------------------

  private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
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
  }

  // --------------------------------------------------------------------------
  //  Util methods
  // --------------------------------------------------------------------------

  /** Initialises the GSON layer. */
  private void initGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gson = gsonBuilder.create();
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
