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
import jakarta.xml.bind.DatatypeConverter;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import lombok.extern.log4j.Log4j2;

/**
 * Exchange Adapter for integrating with the Bitfinex exchange. The Bitfinex API is documented <a
 * href="https://www.bitfinex.com/pages/api">here</a>.
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
 * could lose money. Despite running live on Bitfinex, it has only been unit tested up until the
 * point of calling the {@link #sendPublicRequestToExchange(String)} and {@link
 * #sendAuthenticatedRequestToExchange(String, Map)} methods. Use it at our own risk!</strong>
 *
 * <p>The adapter uses v1 of the Bitfinex API - it is limited to 60 API calls per minute. It only
 * supports 'exchange' accounts; it does <em>not</em> support 'trading' (margin trading) accounts or
 * 'deposit' (liquidity SWAPs) accounts. Furthermore, the adapter does not support sending 'hidden'
 * orders.
 *
 * <p>There are different exchange fees for Takers and Makers - see <a
 * href="https://www.bitfinex.com/pages/fees">here.</a> This adapter will use the <em>Taker</em>
 * fees to keep things simple for now.
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
@Log4j2
public final class BitfinexExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final String BITFINEX_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://api.bitfinex.com/" + BITFINEX_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in Bitfinex Exchange Adapter. ";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error.";

  private static final String ID = "id";
  private static final String EXCHANGE = "exchange";
  private static final String SYMBOL = "symbol";
  private static final String AMOUNT = "amount";
  private static final String PRICE = "price";
  private static final String TIMESTAMP = "timestamp";
  private static final String AVG_EXECUTION_PRICE = "avgExecutionPrice";
  private static final String IS_LIVE = "isLive";
  private static final String IS_CANCELLED = "isCancelled";
  private static final String IS_HIDDEN = "isHidden";
  private static final String WAS_FORCED = "wasForced";
  private static final String ORIGINAL_AMOUNT = "originalAmount";
  private static final String REMAINING_AMOUNT = "remainingAmount";
  private static final String EXECUTED_AMOUNT = "executedAmount";

  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private String key = "";
  private String secret = "";

  private Mac mac;
  private boolean initializedMacAuthentication = false;
  private long nonce = 0;

  private Gson gson;

  /** Constructs the Exchange Adapter. */
  public BitfinexExchangeAdapter() {
    // No extra init.
  }

  @Override
  public void init(ExchangeConfig config) {
    log.info("About to initialise Bitfinex ExchangeConfig: {}", config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);

    nonce = System.currentTimeMillis() / 1000;
    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // Bitfinex API Calls adapted to the Trading API.
  // See https://www.bitfinex.com/pages/api
  // --------------------------------------------------------------------------

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("book/" + marketId);
      log.debug("Market Orders response: {}", response);

      final BitfinexOrderBook orderBook =
          gson.fromJson(response.getPayload(), BitfinexOrderBook.class);

      final List<MarketOrder> buyOrders = new ArrayList<>();
      for (BitfinexMarketOrder bitfinexBuyOrder : orderBook.bids) {
        final MarketOrder buyOrder =
            new MarketOrderImpl(
                OrderType.BUY,
                bitfinexBuyOrder.price,
                bitfinexBuyOrder.amount,
                bitfinexBuyOrder.price.multiply(bitfinexBuyOrder.amount));
        buyOrders.add(buyOrder);
      }

      final List<MarketOrder> sellOrders = new ArrayList<>();
      for (BitfinexMarketOrder bitfinexSellOrder : orderBook.asks) {
        final MarketOrder sellOrder =
            new MarketOrderImpl(
                OrderType.SELL,
                bitfinexSellOrder.price,
                bitfinexSellOrder.amount,
                bitfinexSellOrder.price.multiply(bitfinexSellOrder.amount));
        sellOrders.add(sellOrder);
      }

      return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("orders", null);
      log.debug("Open Orders response: {}", response);

      final BitfinexOpenOrders bitfinexOpenOrders =
          gson.fromJson(response.getPayload(), BitfinexOpenOrders.class);

      final List<OpenOrder> ordersToReturn = new ArrayList<>();
      for (final BitfinexOpenOrder bitfinexOpenOrder : bitfinexOpenOrders) {

        if (!marketId.equalsIgnoreCase(bitfinexOpenOrder.symbol)) {
          continue;
        }

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
                "Unrecognised order type received in getYourOpenOrders(). Value: "
                    + bitfinexOpenOrder.type);
        }

        final OpenOrder order =
            new OpenOrderImpl(
                Long.toString(bitfinexOpenOrder.id),
                // for some reason 'finex adds decimal point to long date value, e.g. "1442073766.0"
                //  - grrrr!
                Date.from(
                    Instant.ofEpochMilli(
                        Integer.parseInt(bitfinexOpenOrder.timestamp.split("\\.")[0]))),
                marketId,
                orderType,
                bitfinexOpenOrder.price,
                bitfinexOpenOrder.remainingAmount,
                bitfinexOpenOrder.originalAmount,
                bitfinexOpenOrder.price.multiply(
                    bitfinexOpenOrder.originalAmount) // total - not provided by finex :-(
                );

        ordersToReturn.add(order);
      }
      return ordersToReturn;

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, Object> params = createRequestParamMap();

      params.put(SYMBOL, marketId);

      // note we need to limit amount and price to 8 decimal places else exchange will barf
      params.put(
          AMOUNT, new DecimalFormat("#.########", getDecimalFormatSymbols()).format(quantity));
      params.put(PRICE, new DecimalFormat("#.########", getDecimalFormatSymbols()).format(price));

      params.put(EXCHANGE, "bitfinex");

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
        log.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      // 'type' is either "market" / "limit" / "stop" / "trailing-stop" / "fill-or-kill" / "exchange
      // market" /
      // "exchange limit" / "exchange stop" / "exchange trailing-stop" / "exchange fill-or-kill".
      // 'type' starting by "exchange " are exchange orders, others are margin trading orders.

      // this adapter only supports 'exchange limit orders'
      params.put("type", "exchange limit");

      // This adapter does not currently support hidden orders.
      // Exchange API notes: "true if the order should be hidden. Default is false."
      // If you try and set "is_hidden" to false, the exchange barfs and sends a 401 back. Nice.
      // params.put("is_hidden", "false");

      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("order/new", params);
      log.debug("Create Order response: {}", response);

      final BitfinexNewOrderResponse createOrderResponse =
          gson.fromJson(response.getPayload(), BitfinexNewOrderResponse.class);
      final long id = createOrderResponse.orderId;
      if (id == 0) {
        final String errorMsg = "Failed to place order on exchange. Error response: " + response;
        log.error(errorMsg);
        throw new TradingApiException(errorMsg);
      } else {
        return Long.toString(createOrderResponse.orderId);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  /*
   * marketId is not needed for cancelling orders on this exchange.
   */
  @Override
  public boolean cancelOrder(String orderId, String marketIdNotNeeded)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, Object> params = createRequestParamMap();
      params.put("order_id", Long.parseLong(orderId));

      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("order/cancel", params);
      log.debug("Cancel Order response: {}", response);

      // Exchange returns order id and other details if successful, a 400 HTTP Status if the order
      // id was not recognised.
      gson.fromJson(response.getPayload(), BitfinexCancelOrderResponse.class);
      return true;

    } catch (ExchangeNetworkException | TradingApiException e) {
      if (e.getCause() != null && e.getCause().getMessage().contains("400")) {
        final String errorMsg =
            "Failed to cancel order on exchange. Did not recognise Order Id: " + orderId;
        log.error(errorMsg, e);
        return false;
      } else {
        throw e;
      }

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("pubticker/" + marketId);
      log.debug("Latest Market Price response: {}", response);

      final BitfinexTicker ticker = gson.fromJson(response.getPayload(), BitfinexTicker.class);
      return ticker.lastPrice;

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("balances", null);
      log.debug("Balance Info response: {}", response);

      final BitfinexBalances allAccountBalances =
          gson.fromJson(response.getPayload(), BitfinexBalances.class);
      final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();

      /*
       * The adapter only fetches the 'exchange' account balance details - this is the Bitfinex
       * 'exchange' account, i.e. the limit order trading account balance.
       */
      if (allAccountBalances != null) {
        allAccountBalances.stream()
            .filter(accountBalance -> accountBalance.type.equalsIgnoreCase(EXCHANGE))
            .forEach(
                accountBalance -> {
                  if (accountBalance.currency.equalsIgnoreCase("usd")) {
                    balancesAvailable.put("USD", accountBalance.available);
                  } else if (accountBalance.currency.equalsIgnoreCase("btc")) {
                    balancesAvailable.put("BTC", accountBalance.available);
                  }
                });
      }

      // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by
      // exchange.
      return new BalanceInfoImpl(balancesAvailable, new HashMap<>());

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("account_infos", null);
      log.debug("Buy Fee response: {}", response);

      // Nightmare to adapt! Just take the top-level taker fees.
      final BitfinexAccountInfos bitfinexAccountInfos =
          gson.fromJson(response.getPayload(), BitfinexAccountInfos.class);
      final BigDecimal fee = bitfinexAccountInfos.get(0).takerFees;

      // adapt the % into BigDecimal format
      return fee.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("account_infos", null);
      log.debug("Sell Fee response: {}", response);

      // Nightmare to adapt! Just take the top-level taker fees.
      final BitfinexAccountInfos bitfinexAccountInfos =
          gson.fromJson(response.getPayload(), BitfinexAccountInfos.class);
      final BigDecimal fee = bitfinexAccountInfos.get(0).takerFees;

      // adapt the % into BigDecimal format
      return fee.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public String getImplName() {
    return "Bitfinex API v1";
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("pubticker/" + marketId);
      log.debug("Latest Market Price response: {}", response);

      final BitfinexTicker ticker = gson.fromJson(response.getPayload(), BitfinexTicker.class);
      return new TickerImpl(
          ticker.lastPrice,
          ticker.bid,
          ticker.ask,
          ticker.low,
          ticker.high,
          null, // open not supplied by Bitfinex
          ticker.volume,
          null, // vwap not supplied by Bitfinex
          // for some reason 'finex adds decimal point to long date value, e.g. "1513631756.0798516"
          //  - grrrr!
          Date.from(Instant.ofEpochMilli(Integer.parseInt(ticker.timestamp.split("\\.")[0])))
              .getTime());

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  //  See https://www.bitfinex.com/pages/api
  // --------------------------------------------------------------------------

  /** GSON class for a market Order Book. */
  private static class BitfinexOrderBook {

    BitfinexMarketOrder[] bids;
    BitfinexMarketOrder[] asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("bids", bids).add("asks", asks).toString();
    }
  }

  /** GSON class for a Market Order. */
  private static class BitfinexMarketOrder {

    BigDecimal price;
    BigDecimal amount;
    String timestamp;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add(PRICE, price)
          .add(AMOUNT, amount)
          .add(TIMESTAMP, timestamp)
          .toString();
    }
  }

  /** GSON class for receiving your open orders in 'orders' API call response. */
  private static class BitfinexOpenOrders extends ArrayList<BitfinexOpenOrder> {
    @Serial private static final long serialVersionUID = 5516523641153401953L;
  }

  /** GSON class for mapping returned order from 'orders' API call response. */
  private static class BitfinexOpenOrder {

    long id;
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // e.g. "sell"
    String type; // e.g. "exchange limit"
    String timestamp;

    @SerializedName("is_live")
    boolean isLive;

    @SerializedName("is_cancelled")
    boolean isCancelled;

    @SerializedName("is_hidden")
    boolean isHidden;

    @SerializedName("was_forced")
    boolean wasForced;

    @SerializedName("original_amount")
    BigDecimal originalAmount;

    @SerializedName("remaining_amount")
    BigDecimal remainingAmount;

    @SerializedName("executed_amount")
    BigDecimal executedAmount;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add(ID, id)
          .add(SYMBOL, symbol)
          .add(EXCHANGE, exchange)
          .add(PRICE, price)
          .add(AVG_EXECUTION_PRICE, avgExecutionPrice)
          .add("side", side)
          .add("type", type)
          .add(TIMESTAMP, timestamp)
          .add(IS_LIVE, isLive)
          .add(IS_CANCELLED, isCancelled)
          .add(IS_HIDDEN, isHidden)
          .add(WAS_FORCED, wasForced)
          .add(ORIGINAL_AMOUNT, originalAmount)
          .add(REMAINING_AMOUNT, remainingAmount)
          .add(EXECUTED_AMOUNT, executedAmount)
          .toString();
    }
  }

  /** GSON class for a Bitfinex 'pubticker' API call response. */
  private static class BitfinexTicker {

    BigDecimal mid;
    BigDecimal bid;
    BigDecimal ask;

    @SerializedName("last_price")
    BigDecimal lastPrice;

    BigDecimal low;
    BigDecimal high;
    BigDecimal volume;
    String timestamp;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("mid", mid)
          .add("bid", bid)
          .add("ask", ask)
          .add("lastPrice", lastPrice)
          .add("low", low)
          .add("high", high)
          .add("volume", volume)
          .add(TIMESTAMP, timestamp)
          .toString();
    }
  }

  /**
   * GSON class for holding Bitfinex response from 'account_infos' API call.
   *
   * <p>This is a lot of work to just get the exchange fees!
   *
   * <p>We want the taker fees.
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
   */
  private static class BitfinexAccountInfos extends ArrayList<BitfinexAccountInfo> {
    @Serial private static final long serialVersionUID = 5516521641453401953L;
  }

  /** GSON class for holding Bitfinex Account Info. */
  private static class BitfinexAccountInfo {

    @SerializedName("maker_fees")
    BigDecimal makerFees;

    @SerializedName("taker_fees")
    BigDecimal takerFees;

    BitfinexPairFees fees;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("makerFees", makerFees)
          .add("takerFees", takerFees)
          .add("fees", fees)
          .toString();
    }
  }

  /** GSON class for holding Bitfinex Pair Fees. */
  private static class BitfinexPairFees extends ArrayList<BitfinexPairFee> {
    @Serial private static final long serialVersionUID = 1516526641473401953L;
  }

  /** GSON class for holding Bitfinex Pair Fee. */
  private static class BitfinexPairFee {

    String pairs;

    @SerializedName("maker_fees")
    BigDecimal makerFees;

    @SerializedName("taker_fees")
    BigDecimal takerFees;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("pairs", pairs)
          .add("makerFees", makerFees)
          .add("takerFees", takerFees)
          .toString();
    }
  }

  /**
   * GSON class for holding Bitfinex response from 'balances' API call.
   *
   * <p>Basically an array of BitfinexAccountBalance types.
   *
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
   */
  private static class BitfinexBalances extends ArrayList<BitfinexAccountBalance> {
    @Serial private static final long serialVersionUID = 5516523641953401953L;
  }

  /**
   * GSON class for holding a Bitfinex account type balance info.
   *
   * <p>There are 3 types of account: 'deposit' (swaps), 'exchange' (limit orders), 'trading'
   * (margin).
   *
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
   */
  private static class BitfinexAccountBalance {

    String type;
    String currency;
    BigDecimal amount;
    BigDecimal available;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("type", type)
          .add("currency", currency)
          .add(AMOUNT, amount)
          .add("available", available)
          .toString();
    }
  }

  /** GSON class for Bitfinex 'order/new' response. */
  private static class BitfinexNewOrderResponse {

    long id; // same as order_id
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // e.g. "sell"
    String type; // e.g. "exchange limit"
    String timestamp;

    @SerializedName("is_live")
    boolean isLive;

    @SerializedName("is_cancelled")
    boolean isCancelled;

    @SerializedName("is_hidden")
    boolean isHidden;

    @SerializedName("was_forced")
    boolean wasForced;

    @SerializedName("original_amount")
    BigDecimal originalAmount;

    @SerializedName("remaining_amount")
    BigDecimal remainingAmount;

    @SerializedName("executed_amount")
    BigDecimal executedAmount;

    @SerializedName("order_id")
    long orderId; // same as id

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add(ID, id)
          .add(SYMBOL, symbol)
          .add(EXCHANGE, exchange)
          .add(PRICE, price)
          .add(AVG_EXECUTION_PRICE, avgExecutionPrice)
          .add("side", side)
          .add("type", type)
          .add(TIMESTAMP, timestamp)
          .add(IS_LIVE, isLive)
          .add(IS_CANCELLED, isCancelled)
          .add(IS_HIDDEN, isHidden)
          .add(WAS_FORCED, wasForced)
          .add(ORIGINAL_AMOUNT, originalAmount)
          .add(REMAINING_AMOUNT, remainingAmount)
          .add(EXECUTED_AMOUNT, executedAmount)
          .add("orderId", orderId)
          .toString();
    }
  }

  /** GSON class for Bitfinex 'order/cancel' response. */
  private static class BitfinexCancelOrderResponse {

    long id; // only get this param; there is no order_id
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // e.g. "sell"
    String type; // e.g. "exchange limit"
    String timestamp;

    @SerializedName("is_live")
    boolean isLive;

    @SerializedName("is_cancelled")
    boolean isCancelled;

    @SerializedName("is_hidden")
    boolean isHidden;

    @SerializedName("was_forced")
    boolean wasForced;

    @SerializedName("original_amount")
    BigDecimal originalAmount;

    @SerializedName("remaining_amount")
    BigDecimal remainingAmount;

    @SerializedName("executed_amount")
    BigDecimal executedAmount;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add(ID, id)
          .add(SYMBOL, symbol)
          .add(EXCHANGE, exchange)
          .add(PRICE, price)
          .add(AVG_EXECUTION_PRICE, avgExecutionPrice)
          .add("side", side)
          .add("type", type)
          .add(TIMESTAMP, timestamp)
          .add(IS_LIVE, isLive)
          .add(IS_CANCELLED, isCancelled)
          .add(IS_HIDDEN, isHidden)
          .add(WAS_FORCED, wasForced)
          .add(ORIGINAL_AMOUNT, originalAmount)
          .add(REMAINING_AMOUNT, remainingAmount)
          .add(EXECUTED_AMOUNT, executedAmount)
          .toString();
    }
  }

  // --------------------------------------------------------------------------
  //  Transport layer methods
  // --------------------------------------------------------------------------

  private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod)
      throws ExchangeNetworkException, TradingApiException {
    try {
      final URL url = new URI(PUBLIC_API_BASE_URL + apiMethod).toURL();
      return makeNetworkRequest(url, "GET", null, createHeaderParamMap());

    } catch (MalformedURLException | URISyntaxException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      log.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  /*
   * Makes an authenticated API call to the Bitfinex exchange.
   *
   * Bitfinex Example:
   *
   * POST https://api.bitfinex.com/v1/order/new
   *
   * With JSON payload of:
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
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String apiMethod, Map<String, Object> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized.";
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        // create empty map for non param API calls, e.g. "balances"
        params = createRequestParamMap();
      }

      // nonce is required by Bitfinex in every request
      params.put("nonce", Long.toString(nonce));
      nonce++; // increment ready for next call.

      // must include the method in request param too
      params.put("request", "/" + BITFINEX_API_VERSION + "/" + apiMethod);

      // JSON-ify the param dictionary
      final String paramsInJson = gson.toJson(params);

      // Need to base64 encode payload as per API
      final String base64payload =
          DatatypeConverter.printBase64Binary(paramsInJson.getBytes(StandardCharsets.UTF_8));

      // Request headers required by Exchange
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("X-BFX-APIKEY", key);
      requestHeaders.put("X-BFX-PAYLOAD", base64payload);

      // Add the signature
      mac.reset(); // force reset
      mac.update(base64payload.getBytes(StandardCharsets.UTF_8));

      /*
       * signature = HMAC-SHA384(payload, api-secret) as hexadecimal - MUST be in LOWERCASE else
       * signature fails. See:
       * http://bitcoin.stackexchange.com/questions/25835/bitfinex-api-call-returns-400-bad-request
       */
      final String signature = toHex(mac.doFinal()).toLowerCase();
      requestHeaders.put("X-BFX-SIGNATURE", signature);

      // payload is JSON for this exchange
      requestHeaders.put("Content-Type", "application/json");

      final URL url = new URI(AUTHENTICATED_API_URL + apiMethod).toURL();
      return makeNetworkRequest(url, "POST", paramsInJson, requestHeaders);

    } catch (MalformedURLException | URISyntaxException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      log.error(errorMsg, e);
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
      log.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = "Failed to setup MAC security. Secret key seems invalid!";
      log.error(errorMsg, e);
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
  private Map<String, Object> createRequestParamMap() {
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
