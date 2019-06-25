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

package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
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
 * Exchange Adapter for integrating with the Gemini exchange. The Gemini API is documented <a
 * href="https://docs.gemini.com/rest-api/">here</a>.
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
 * could lose money. Despite running live on Gemini, it has only been unit tested up until the point
 * of calling the {@link #sendPublicRequestToExchange(String)} and {@link
 * #sendAuthenticatedRequestToExchange(String, Map)} methods. Use it at our own risk! </strong>
 *
 * <p>The adapter only supports the REST implementation of the <a
 * href="https://docs.gemini.com/rest-api/">Trading API</a>.
 *
 * <p>Gemini operates <a href="https://docs.gemini.com/rest-api/#rate-limits">rate limits</a>:
 *
 * <ul>
 *   <li>For public API entry points, they limit requests to 120 requests per minute, and recommend
 *       that you do not exceed 1 request per second.
 *   <li>For private API entry points, they limit requests to 600 requests per minute, and recommend
 *       that you not exceed 5 requests per second.
 * </ul>
 *
 * <p>Exchange fees are loaded from the exchange.yaml file on startup; they are not fetched from the
 * exchange at runtime as the Gemini REST API does not support this. The fees are used across all
 * markets. Make sure you keep an eye on the <a href="https://gemini.com/fee-schedule/">exchange
 * fees</a> and update the config accordingly.
 *
 * <p>NOTE: Gemini requires "btcusd" and "ethusd" market price currency (USD) values to be limited
 * to 2 decimal places when creating orders - the adapter truncates any prices with more than 2
 * decimal places and rounds using {@link java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be
 * sent to the exchange as 250.18. For the "ethbtc" market, price currency (BTC) values are limited
 * to 5 decimal places - the adapter will truncate and round accordingly.
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
public final class GeminiExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String GEMINI_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://api.gemini.com/" + GEMINI_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in Gemini Exchange Adapter. ";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error.";

  private static final String AMOUNT = "amount";
  private static final String PRICE = "price";

  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
  private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";

  /*
   * Markets on the exchange. Used for determining order price truncation/rounding policy.
   * See: https://docs.gemini.com/rest-api/#symbols-and-minimums
   */
  private enum MarketId {
    BTC_USD("btcusd"),
    ETH_USD("ethusd"),
    ETH_BTC("ethbtc");
    private final String market;

    MarketId(String market) {
      this.market = market;
    }

    public String getStringValue() {
      return market;
    }
  }

  private BigDecimal buyFeePercentage;
  private BigDecimal sellFeePercentage;

  private String key = "";
  private String secret = "";

  private Mac mac;
  private boolean initializedMacAuthentication = false;
  private long nonce = 0;

  private Gson gson;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise Gemini ExchangeConfig: " + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);
    setOtherConfig(config);

    nonce = System.currentTimeMillis() / 1000;
    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // Gemini REST Trade API Calls adapted to the Trading API.
  // See https://docs.gemini.com/rest-api/
  // --------------------------------------------------------------------------

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();

      params.put("symbol", marketId);

      // note we need to limit amount and price to 6 decimal places else exchange will barf with 400
      // response
      params.put(
          AMOUNT, new DecimalFormat("#.######", getDecimalFormatSymbols()).format(quantity));

      // Decimal precision of price varies with market price currency
      if (marketId.equals(MarketId.BTC_USD.getStringValue())
          || marketId.equals(MarketId.ETH_USD.getStringValue())) {
        params.put(PRICE, new DecimalFormat("#.##", getDecimalFormatSymbols()).format(price));
      } else if (marketId.equals(MarketId.ETH_BTC.getStringValue())) {
        params.put(PRICE, new DecimalFormat("#.#####", getDecimalFormatSymbols()).format(price));
      } else {
        final String errorMsg =
            "Invalid market id: "
                + marketId
                + " - Can only be "
                + MarketId.BTC_USD.getStringValue()
                + " or "
                + MarketId.ETH_USD.getStringValue()
                + " or "
                + MarketId.ETH_BTC.getStringValue();
        LOG.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

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

      // this adapter only supports 'exchange limit' orders
      params.put("type", "exchange limit");

      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("order/new", params);

      LOG.debug(() -> "Create Order response: " + response);

      final GeminiOpenOrder createOrderResponse =
          gson.fromJson(response.getPayload(), GeminiOpenOrder.class);
      final long id = createOrderResponse.orderId;
      if (id == 0) {
        final String errorMsg = "Failed to place order on exchange. Error response: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      } else {
        return Long.toString(createOrderResponse.orderId);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public boolean cancelOrder(String orderId, String marketIdNotNeeded)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("order_id", orderId);

      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("order/cancel", params);

      LOG.debug(() -> "Cancel Order response: " + response);

      // Exchange returns order id and other details if successful, a 400 HTTP Status if the order
      // id was not recognised.
      gson.fromJson(response.getPayload(), GeminiOpenOrder.class);
      return true;

    } catch (ExchangeNetworkException | TradingApiException e) {
      if (e.getCause() != null && e.getCause().getMessage().contains("400")) {
        final String errorMsg =
            "Failed to cancel order on exchange. Did not recognise Order Id: " + orderId;
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
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("orders", null);

      LOG.debug(() -> "Open Orders response: " + response);

      final GeminiOpenOrders geminiOpenOrders =
          gson.fromJson(response.getPayload(), GeminiOpenOrders.class);

      final List<OpenOrder> ordersToReturn = new ArrayList<>();
      for (final GeminiOpenOrder geminiOpenOrder : geminiOpenOrders) {

        if (!marketId.equalsIgnoreCase(geminiOpenOrder.symbol)) {
          continue;
        }

        OrderType orderType;
        switch (geminiOpenOrder.side) {
          case "buy":
            orderType = OrderType.BUY;
            break;
          case "sell":
            orderType = OrderType.SELL;
            break;
          default:
            throw new TradingApiException(
                "Unrecognised order type received in getYourOpenOrders(). Value: "
                    + geminiOpenOrder.type);
        }

        final OpenOrder order =
            new OpenOrderImpl(
                Long.toString(geminiOpenOrder.orderId),
                Date.from(Instant.ofEpochMilli(geminiOpenOrder.timestampms)),
                marketId,
                orderType,
                geminiOpenOrder.price,
                geminiOpenOrder.remainingAmount,
                geminiOpenOrder.originalAmount,
                geminiOpenOrder.price.multiply(
                    geminiOpenOrder.originalAmount) // total - not provided by Gemini :-(
                );

        ordersToReturn.add(order);
      }
      return ordersToReturn;

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
      final ExchangeHttpResponse response = sendPublicRequestToExchange("book/" + marketId);

      LOG.debug(() -> "Market Orders response: " + response);

      final GeminiOrderBook orderBook = gson.fromJson(response.getPayload(), GeminiOrderBook.class);

      final List<MarketOrder> buyOrders = new ArrayList<>();
      for (GeminiMarketOrder geminiBuyOrder : orderBook.bids) {
        final MarketOrder buyOrder =
            new MarketOrderImpl(
                OrderType.BUY,
                geminiBuyOrder.price,
                geminiBuyOrder.amount,
                geminiBuyOrder.price.multiply(geminiBuyOrder.amount));
        buyOrders.add(buyOrder);
      }

      final List<MarketOrder> sellOrders = new ArrayList<>();
      for (GeminiMarketOrder geminiSellOrder : orderBook.asks) {
        final MarketOrder sellOrder =
            new MarketOrderImpl(
                OrderType.SELL,
                geminiSellOrder.price,
                geminiSellOrder.amount,
                geminiSellOrder.price.multiply(geminiSellOrder.amount));
        sellOrders.add(sellOrder);
      }

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
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("pubticker/" + marketId);

      LOG.debug(() -> "Latest Market Price response: " + response);

      final GeminiTicker ticker = gson.fromJson(response.getPayload(), GeminiTicker.class);
      return ticker.last;

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
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("balances", null);

      LOG.debug(() -> "Balance Info response: " + response);

      final GeminiBalances allAccountBalances =
          gson.fromJson(response.getPayload(), GeminiBalances.class);
      final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();

      // This adapter only supports 'exchange' account type.
      allAccountBalances.stream()
          .filter(accountBalance -> accountBalance.type.equalsIgnoreCase("exchange"))
          .forEach(
              accountBalance ->
                  balancesAvailable.put(accountBalance.currency, accountBalance.available));

      // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by
      // exchange.
      return new BalanceInfoImpl(balancesAvailable, new HashMap<>());

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  /*
   * Gemini does not provide API call for fetching % buy fee. We load the % fee statically
   * from exchange.yaml file - see https://gemini.com/fee-schedule/
   */
  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return buyFeePercentage;
  }

  /*
   * Gemini does not provide API call for fetching % sell fee. load the % fee statically
   * from exchange.yaml file - see https://gemini.com/fee-schedule/
   */
  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return sellFeePercentage;
  }

  @Override
  public String getImplName() {
    return "Gemini REST API v1";
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  //  See https://docs.gemini.com/rest-api/
  // --------------------------------------------------------------------------

  /** GSON class for a market Order Book. */
  private static class GeminiOrderBook {

    List<GeminiMarketOrder> bids;
    List<GeminiMarketOrder> asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("bids", bids).add("asks", asks).toString();
    }
  }

  /** GSON class for a Market Order. */
  private static class GeminiMarketOrder {

    BigDecimal price;
    BigDecimal amount;
    // ignore the timestamp attribute as per the API spec

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add(PRICE, price).add(AMOUNT, amount).toString();
    }
  }

  /** GSON class for Balances API call response. */
  private static class GeminiBalances extends ArrayList<GeminiAccountBalance> {

    private static final long serialVersionUID = 5516523141993401253L;
  }

  /**
   * GSON class for holding account type balance info. This adapter only supports type 'exchange'.
   */
  private static class GeminiAccountBalance {

    String type;
    String currency;
    BigDecimal amount;
    BigDecimal available;
    BigDecimal availableForWithdrawal;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("type", type)
          .add("currency", currency)
          .add(AMOUNT, amount)
          .add("available", available)
          .add("availableForWithdrawal", availableForWithdrawal)
          .toString();
    }
  }

  /** GSON class for Ticker API call response. */
  private static class GeminiTicker {

    BigDecimal bid;
    BigDecimal ask;
    BigDecimal last;
    BigDecimal low;
    GeminiVolume volume;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("bid", bid)
          .add("ask", ask)
          .add("last", last)
          .add("low", low)
          .add("volume", volume)
          .toString();
    }
  }

  /** GSON class for holding volume information in the Ticker response. */
  private static class GeminiVolume {

    @SerializedName("BTC")
    BigDecimal btc;

    @SerializedName("USD")
    BigDecimal usd;

    long timestamp;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("btc", btc)
          .add("usd", usd)
          .add("timestamp", timestamp)
          .toString();
    }
  }

  /** GSON class for holding an active orders API call response. */
  private static class GeminiOpenOrders extends ArrayList<GeminiOpenOrder> {

    private static final long serialVersionUID = 5516523611153405953L;
  }

  /** GSON class representing an open order on the exchange. */
  private static class GeminiOpenOrder {

    @SerializedName("order_id")
    long orderId; // use this value for order id as per the API spec

    long id;
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // buy|sell
    String type; // exchange limit
    String timestamp; // timestamp as a String
    long timestampms; // timestamp in millis as a long

    @SerializedName("is_live")
    boolean isLive;

    @SerializedName("is_cancelled")
    boolean isCancelled;

    @SerializedName("is_hidden")
    boolean isHidden;

    @SerializedName("was_forced")
    boolean wasForced;

    @SerializedName("remaining_amount")
    BigDecimal remainingAmount;

    @SerializedName("executed_amount")
    BigDecimal executedAmount;

    @SerializedName("original_amount")
    BigDecimal originalAmount;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("orderId", orderId)
          .add("id", id)
          .add("symbol", symbol)
          .add("exchange", exchange)
          .add(PRICE, price)
          .add("avgExecutionPrice", avgExecutionPrice)
          .add("side", side)
          .add("type", type)
          .add("timestamp", timestamp)
          .add("timestampms", timestampms)
          .add("isLive", isLive)
          .add("isCancelled", isCancelled)
          .add("isHidden", isHidden)
          .add("wasForced", wasForced)
          .add("remainingAmount", remainingAmount)
          .add("executedAmount", executedAmount)
          .add("originalAmount", originalAmount)
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
      return makeNetworkRequest(url, "GET", null, createRequestParamMap());

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  /*
   * Makes an authenticated API call to the Gemini exchange.
   *
   * E.g.
   *
   * POST https://api.gemini.com/v0/order/new
   *
   * With JSON payload of:
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
   * '337cc8b4ea692cfe65b4a85fcc9f042b2e3f702ac956fd098d600ab15705775017beae402be773ceee107170d710f'
   *
   * These are sent as HTTP headers:
   *
   * X-GEMINI-APIKEY: apiKey
   * X-GEMINI-PAYLOAD:ewogICAgInJlcXVlc3QiOiAiL3YxL29yZGVyL3N
   * X-GEMINI-SIGNATURE: 337cc8b4ea692cfe65b4a85fcc9f042b2e3f
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized.";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        // create empty map for non param API calls, e.g. "balances"
        params = createRequestParamMap();
      }

      // Add the API call method
      params.put("request", "/" + GEMINI_API_VERSION + "/" + apiMethod);

      // nonce is required by Gemini in every request
      params.put("nonce", Long.toString(nonce));
      nonce++;

      // JSON-ify the param dictionary
      final String paramsInJson = gson.toJson(params);

      // Need to base64 encode payload as per API
      final String base64payload =
          DatatypeConverter.printBase64Binary(paramsInJson.getBytes(StandardCharsets.UTF_8));

      // Create the signature
      mac.reset(); // force reset
      mac.update(base64payload.getBytes(StandardCharsets.UTF_8));
      final String signature = toHex(mac.doFinal()).toLowerCase();

      // Request headers required by Exchange
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("X-GEMINI-APIKEY", key);
      requestHeaders.put("X-GEMINI-PAYLOAD", base64payload);
      requestHeaders.put("X-GEMINI-SIGNATURE", signature);

      // payload is JSON for this exchange
      requestHeaders.put("Content-Type", "application/json");

      final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
      return makeNetworkRequest(url, "POST", paramsInJson, requestHeaders);

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  private String toHex(byte[] byteArrayToConvert) {
    final StringBuilder hexString = new StringBuilder();
    for (final byte aByte : byteArrayToConvert) {
      hexString.append(String.format("%02x", aByte & 0xff));
    }
    return hexString.toString();
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
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA384");
      mac = Mac.getInstance("HmacSHA384");
      mac.init(keyspec);
      initializedMacAuthentication = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA384 installed?";
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
