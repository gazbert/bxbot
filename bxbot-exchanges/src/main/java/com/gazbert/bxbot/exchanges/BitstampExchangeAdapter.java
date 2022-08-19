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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Exchange Adapter for integrating with the Bitstamp exchange. It uses v2 of the Bitstamp API; it
      is documented <a href="https://www.bitstamp.net/api/">here</a>.
 用于与 Bitstamp 交换集成的交换适配器。它使用了 Bitstamp API v2；它
 <a href="https://www.bitstamp.net/api/">此处</a>记录在案。
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
  could lose money. Despite running live on Bitstamp, it has only been unit tested up until the
   point of calling the {@link #sendPublicRequestToExchange(String)} and {@link
   #sendAuthenticatedRequestToExchange(String, Map)} methods. Use it at our own risk! </strong>
 <p><strong> 免责声明：此交换适配器按原样提供；它可能有错误，你
 可能会赔钱。尽管在 Bitstamp 上实时运行，但它只进行了单元测试，直到
 调用 {@link #sendPublicRequestToExchange(String)} 和 {@link
#sendAuthenticatedRequestToExchange(String, Map)} 方法。使用它需要我们自担风险！ </strong>

 *
 * <p>Note: the Bitstamp API returns 200 OK response even for errors. The response payload will be
  different though, e.g.
 <p>注意：即使出现错误，Bitstamp API 也会返回 200 OK 响应。响应负载将是
 虽然不同，例如
 *
 * <pre>
 *     {"error": "Order not found"}
 * </pre>
 *
 * <p>This Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single
  thread in order to preserve trade execution order. The {@link URLConnection} achieves this by
  blocking/waiting on the input stream (response) for each API call.
 <p>此交换适配器<em>不是</em>线程安全的。它期望使用单个调用
 线程以保留交易执行顺序。 {@link URLConnection} 通过
 阻塞/等待每个 API 调用的输入流（响应）。
 *
 * <p>The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error
  occurs trying to connect to the exchange. A {@link TradingApiException} is thrown for
  <em>all</em> other failures.
 <p>如果网络错误，{@link TradingApi} 调用将抛出 {@link ExchangeNetworkException}
 尝试连接到交易所时发生。抛出 {@link TradingApiException}
 <em>所有</em>其他故障。
 *
 * <p>NOTE: Bitstamp requires all price values to be limited to 2 decimal places when creating
  orders. This adapter truncates any prices with more than 2 decimal places and rounds using {@link
  java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be sent to the exchange as 250.18.
 <p>注意：Bitstamp 在创建时要求所有价格值限制为 2 位小数
 订单。此适配器使用 {@link 截断任何超过 2 位小数和四舍五入的价格
爪哇。 math.RoundingMode#HALF_EVEN}，例如250.176 将作为 250.18 发送到交易所。
 *
 * @author gazbert
 * @since 1.0
 */
public class BitstampExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String API_BASE_URL = "https://www.bitstamp.net/api/v2/";

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in Bitstamp Exchange Adapter. Bitstamp 交换适配器发生意外错误。";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error. 由于意外的 IO 错误，无法连接到 Exchange。";

  private static final String AMOUNT = "amount";
  private static final String BALANCE = "balance";
  private static final String PRICE = "price";

  private static final String CLIENT_ID_PROPERTY_NAME = "client-id";
  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private String clientId = "";
  private String key = "";
  private String secret = "";

  private Mac mac;
  private long nonce = 0;
  private boolean initializedMacAuthentication = false;

  private Gson gson;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise Bitstamp ExchangeConfig: 即将初始化 Bitstamp ExchangeConfig： " + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);

    nonce = System.currentTimeMillis() / 1000;
    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // Bitstamp API Calls adapted to the Trading API.
  // 适用于交易 API 的 Bitstamp API 调用。

  // See https://www.bitstamp.net/api/
  // --------------------------------------------------------------------------

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("order_book/" + marketId);
      LOG.debug(() -> "Market Orders response: 市价单响应：" + response);

      final BitstampOrderBook bitstampOrderBook =
          gson.fromJson(response.getPayload(), BitstampOrderBook.class);

      final List<MarketOrder> buyOrders = new ArrayList<>();
      final List<List<BigDecimal>> bitstampBuyOrders = bitstampOrderBook.bids;
      for (final List<BigDecimal> order : bitstampBuyOrders) {
        final MarketOrder buyOrder =
            new MarketOrderImpl(
                OrderType.BUY,
                order.get(0), // price  价格
                order.get(1), // quantity 数量
                order.get(0).multiply(order.get(1)));
        buyOrders.add(buyOrder);
      }

      final List<MarketOrder> sellOrders = new ArrayList<>();
      final List<List<BigDecimal>> bitstampSellOrders = bitstampOrderBook.asks;
      for (final List<BigDecimal> order : bitstampSellOrders) {
        final MarketOrder sellOrder =
            new MarketOrderImpl(
                OrderType.SELL,
                order.get(0), // price 价格
                order.get(1), // quantity  数量
                order.get(0).multiply(order.get(1)));
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
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("open_orders/" + marketId, null);
      LOG.debug(() -> "Open Orders response:  未结订单响应：" + response);

      final BitstampOrderResponse[] myOpenOrders =
          gson.fromJson(response.getPayload(), BitstampOrderResponse[].class);

      // No need to filter on marketId; exchange does this for us.
      //无需过滤marketId；交换为我们做这件事。
      final List<OpenOrder> ordersToReturn = new ArrayList<>();
      for (final BitstampOrderResponse openOrder : myOpenOrders) {
        OrderType orderType;
        if (openOrder.type == 0) {
          orderType = OrderType.BUY;
        } else if (openOrder.type == 1) {
          orderType = OrderType.SELL;
        } else {
          throw new TradingApiException(
              "Unrecognised order type received in getYourOpenOrders(). Value: 在 getYourOpenOrders() 中收到无法识别的订单类型。价值：" + openOrder.type);
        }

        final OpenOrder order =
            new OpenOrderImpl(
                Long.toString(openOrder.id),
                openOrder.datetime,
                marketId,
                orderType,
                openOrder.price,
                openOrder.amount,
                null, // orig_quantity - not provided by stamp :-(  orig_quantity - 不提供邮票 :-(
                openOrder.price.multiply(openOrder.amount) // total - not provided by stamp :-( 总计 - 邮票未提供 :-(
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
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();

      // note we need to limit price to 2 decimal places else exchange will barf
      // 请注意，我们需要将价格限制为小数点后 2 位，否则交易所将失败
      params.put(PRICE, new DecimalFormat("#.##", getDecimalFormatSymbols()).format(price));

      // note we need to limit amount to 8 decimal places else exchange will barf
      // 请注意，我们需要将金额限制为 8 位小数，否则交换将失败
      params.put(
          AMOUNT, new DecimalFormat("#.########", getDecimalFormatSymbols()).format(quantity));

      final ExchangeHttpResponse response;
      if (orderType == OrderType.BUY) {
        // buying BTC
        response = sendAuthenticatedRequestToExchange("buy/" + marketId, params);
      } else if (orderType == OrderType.SELL) {
        // selling BTC
        response = sendAuthenticatedRequestToExchange("sell/" + marketId, params);
      } else {
        final String errorMsg =
            "Invalid order type: 无效的订单类型："
                + orderType
                + " - Can only be - 只能是"
                + OrderType.BUY.getStringValue()
                + " or "
                + OrderType.SELL.getStringValue();
        LOG.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      LOG.debug(() -> "Create Order response: 创建订单响应：" + response);

      final BitstampOrderResponse createOrderResponse =
          gson.fromJson(response.getPayload(), BitstampOrderResponse.class);
      final long id = createOrderResponse.id;
      if (id == 0) {
        final String errorMsg = "Failed to place order on exchange. Error response: 未能在交易所下订单。错误响应：" + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      } else {
        return Long.toString(createOrderResponse.id);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  /**
   * marketId is not needed for cancelling orders on this exchange.
   * 取消此交易所的订单不需要市场 ID。
   */
  @Override
  public boolean cancelOrder(String orderId, String marketIdNotNeeded)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("id", orderId);

      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("cancel_order", params);
      LOG.debug(() -> "Cancel Order response: 取消订单响应：" + response);

      final BitstampCancelOrderResponse cancelOrderResponse =
          gson.fromJson(response.getPayload(), BitstampCancelOrderResponse.class);
      if (!orderId.equals(String.valueOf(cancelOrderResponse.id))) {
        final String errorMsg = "Failed to cancel order on exchange. Error response: 交易所取消订单失败。错误响应：" + response;
        LOG.error(errorMsg);
        return false;
      } else {
        return true;
      }

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
      final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker/" + marketId);
      LOG.debug(() -> "Latest Market Price response: 最新市价回应：" + response);

      final BitstampTicker bitstampTicker =
          gson.fromJson(response.getPayload(), BitstampTicker.class);
      return bitstampTicker.last;

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
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange(BALANCE, null);
      LOG.debug(() -> "Balance Info response: 余额信息回复：" + response);

      final BitstampBalance balances = gson.fromJson(response.getPayload(), BitstampBalance.class);

      final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
      balancesAvailable.put("BTC", balances.btcAvailable);
      balancesAvailable.put("USD", balances.usdAvailable);
      balancesAvailable.put("EUR", balances.eurAvailable);
      balancesAvailable.put("LTC", balances.ltcAvailable);
      balancesAvailable.put("XRP", balances.xrpAvailable);

      final Map<String, BigDecimal> balancesOnOrder = new HashMap<>();
      balancesOnOrder.put("BTC", balances.btcReserved);
      balancesOnOrder.put("USD", balances.usdReserved);
      balancesOnOrder.put("EUR", balances.eurReserved);
      balancesOnOrder.put("LTC", balances.ltcReserved);
      balancesOnOrder.put("XRP", balances.xrpReserved);

      return new BalanceInfoImpl(balancesAvailable, balancesOnOrder);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange(BALANCE, null);
      LOG.debug(() -> "Buy Fee response: 购买费用回复：" + response);

      final BitstampBalance balances = gson.fromJson(response.getPayload(), BitstampBalance.class);

      // Ouch! 哎哟!
      final Class<?> clazz = balances.getClass();
      final Field[] fields = clazz.getDeclaredFields();
      for (final Field field : fields) {
        if (field.getName().startsWith(marketId)) {
          final BigDecimal fee = (BigDecimal) field.get(balances);
          // adapt the % into BigDecimal format  // 将 % 调整为 BigDecimal 格式
          return fee.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        }
      }

      final String errorMsg =
          "Unable to map marketId to currency balances returned from the Exchange. 无法将 marketId 映射到从 Exchange 返回的货币余额。"
              + "MarketId: 市场编号："
              + marketId
              + " BitstampBalances: Bitstamp 余额："
              + balances;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange(BALANCE, null);
      LOG.debug(() -> "Sell Fee response: 销售费用回应：" + response);

      final BitstampBalance balances = gson.fromJson(response.getPayload(), BitstampBalance.class);

      // Ouch!
      final Class<?> clazz = balances.getClass();
      final Field[] fields = clazz.getDeclaredFields();
      for (final Field field : fields) {
        if (field.getName().startsWith(marketId) && field.getName().endsWith("Fee")) {
          final BigDecimal fee = (BigDecimal) field.get(balances);
          // adapt the % into BigDecimal format
          return fee.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        }
      }

      final String errorMsg =
          "Unable to map marketId to currency balances returned from the Exchange. 无法将 marketId 映射到从 Exchange 返回的货币余额。"
              + "MarketId: 市场编号："
              + marketId
              + " BitstampBalances: BitstampBalances：Bitstamp 余额："
              + balances;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public String getImplName() {
    return "Bitstamp HTTP API v2";
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {

    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("ticker/" + marketId);
      LOG.debug(() -> "Ticker response: 代码响应：" + response);

      final BitstampTicker bitstampTicker =
          gson.fromJson(response.getPayload(), BitstampTicker.class);
      return new TickerImpl(
          bitstampTicker.last,
          bitstampTicker.bid,
          bitstampTicker.ask,
          bitstampTicker.low,
          bitstampTicker.high,
          bitstampTicker.open,
          bitstampTicker.volume,
          bitstampTicker.vwap,
          bitstampTicker.timestamp);

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  //  JSON 响应的 GSON 类。

  //  See https://www.bitstamp.net/api/
  // --------------------------------------------------------------------------

  /**
   * GSON class for holding Bitstamp Balance response from balance API call. Updated for v2 API -
    markets correct as of 25 June 2017. Well this is fun - why not return a map of reserved, map of
    available, etc... ;-(
   用于保存来自 balance API 调用的 Bitstamp Balance 响应的 GSON 类。更新 v2 API -
   截至 2017 年 6 月 25 日市场正确。这很有趣 - 为什么不返回保留地图，地图
   可用等... ;-(
   */
  private static class BitstampBalance {

    @SerializedName("btc_available")
    BigDecimal btcAvailable;

    @SerializedName("btc_balance")
    BigDecimal btcBalance;

    @SerializedName("btc_reserved")
    BigDecimal btcReserved;

    @SerializedName("btceur_fee")
    BigDecimal btceurFee;

    @SerializedName("btcusd_fee")
    BigDecimal btcusdFee;

    @SerializedName("eur_available")
    BigDecimal eurAvailable;

    @SerializedName("eur_balance")
    BigDecimal eurBalance;

    @SerializedName("eur_reserved")
    BigDecimal eurReserved;

    @SerializedName("eurusd_fee")
    BigDecimal eurusdFee;

    @SerializedName("ltc_available")
    BigDecimal ltcAvailable;

    @SerializedName("ltc_balance")
    BigDecimal ltcBalance;

    @SerializedName("ltc_reserved")
    BigDecimal ltcReserved;

    @SerializedName("ltcbtc_fee")
    BigDecimal ltcbtcFee;

    @SerializedName("ltceur_fee")
    BigDecimal ltceurFee;

    @SerializedName("ltcusd_fee")
    BigDecimal ltcusdFee;

    @SerializedName("usd_available")
    BigDecimal usdAvailable;

    @SerializedName("usd_balance")
    BigDecimal usdBalance;

    @SerializedName("usd_reserved")
    BigDecimal usdReserved;

    @SerializedName("xrp_available")
    BigDecimal xrpAvailable;

    @SerializedName("xrp_balance")
    BigDecimal xrpBalance;

    @SerializedName("xrp_reserved")
    BigDecimal xrpReserved;

    @SerializedName("xrpbtc_fee")
    BigDecimal xrpbtcFee;

    @SerializedName("xrpeur_fee")
    BigDecimal xrpeurFee;

    @SerializedName("xrpusd_fee")
    BigDecimal xrpusdFee;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("btcAvailable", btcAvailable)
          .add("btcBalance", btcBalance)
          .add("btcReserved", btcReserved)
          .add("btceurFee", btceurFee)
          .add("btcusdFee", btcusdFee)
          .add("eurAvailable", eurAvailable)
          .add("eurBalance", eurBalance)
          .add("eurReserved", eurReserved)
          .add("eurusdFee", eurusdFee)
          .add("ltcAvailable", ltcAvailable)
          .add("ltcBalance", ltcBalance)
          .add("ltcReserved", ltcReserved)
          .add("ltcbtcFee", ltcbtcFee)
          .add("ltceurFee", ltceurFee)
          .add("ltcUsdFee", ltcusdFee)
          .add("usdAvailable", usdAvailable)
          .add("usdBalance", usdBalance)
          .add("usdReserved", usdReserved)
          .add("xrpAvailable", xrpAvailable)
          .add("xrpBalance", xrpBalance)
          .add("xrpReserved", xrpReserved)
          .add("xrpbtcFee", xrpbtcFee)
          .add("xrpeurFee", xrpeurFee)
          .add("xrpusdFee", xrpusdFee)
          .toString();
    }
  }

  /**
   * GSON class for holding Bitstamp Order Book response from order_book API call.
   *  用于保存来自 order_book API 调用的 Bitstamp Order Book 响应的 GSON 类。
   *
   * <p>JSON looks like:
   *  * <p>JSON 看起来像：
   *
   * <pre>
   * {
   *   "timestamp": "1400943488",
   *   "bids": [["521.86", "0.00017398"], ["519.58", "0.25100000"], ["0.01", "38820.00000000"]],
   *   "asks": [["521.88", "10.00000000"], ["522.00", "310.24504478"], ["522.13", "0.02852084"]]
   * }
   * </pre>
   *
   * <p>Each is a list of open orders and each order is represented as a list of price and amount.
   *  <p>每个都是未结订单的列表，每个订单都表示为价格和数量的列表。
   */
  private static class BitstampOrderBook {

    long timestamp; // unix timestamp
    List<List<BigDecimal>> bids;
    List<List<BigDecimal>> asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("timestamp", timestamp)
          .add("bids", bids)
          .add("asks", asks)
          .toString();
    }
  }

  /** GSON class for a Bitstamp ticker response.
   * Bitstamp 代码响应的 GSON 类。 */
  private static class BitstampTicker {

    BigDecimal high;
    BigDecimal last;
    Long timestamp;
    BigDecimal bid;
    BigDecimal vwap;
    BigDecimal volume;
    BigDecimal low;
    BigDecimal ask;
    BigDecimal open;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("high", high)
          .add("last", last)
          .add("timestamp", timestamp)
          .add("bid", bid)
          .add("vwap", vwap)
          .add("volume", volume)
          .add("low", low)
          .add("ask", ask)
          .add("open", open)
          .toString();
    }
  }

  /** GSON class for Bitstamp create order response.
   * Bitstamp 的 GSON 类创建订单响应。 */
  private static class BitstampOrderResponse {

    long id;
    Date datetime;
    int type; // 0 = buy; 1 = sell
    BigDecimal price;
    BigDecimal amount;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add("datetime", datetime)
          .add("type", type)
          .add(PRICE, price)
          .add(AMOUNT, amount)
          .toString();
    }
  }

  /** GSON class for Bitstamp cancel order response.
   * Bitstamp 取消订单响应的 GSON 类。 */
  private static class BitstampCancelOrderResponse {

    long id;
    BigDecimal price;
    BigDecimal amount;
    int type; // 0 = buy; 1 = sell

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add(PRICE, price)
          .add(AMOUNT, amount)
          .add("type", type)
          .toString();
    }
  }

  /**
   * Deserializer needed because stamp Date format is different in open_order response and causes
    default GSON parsing to barf.
   需要反序列化器，因为 open_order 响应和原因中的戳日期格式不同
   默认 GSON 解析为 barf。
   *
   * <pre>
   * [main] 2014-05-25 20:51:31,074 ERROR BitstampExchangeAdapter  - Failed to parse a Bitstamp date
   * java.text.ParseException: Unparseable date: "2014-05-25 19:50:32"
   * at java.text.DateFormat.parse(DateFormat.java:357)
   * at com.gazbert.bxbot.adapter.BitstampExchangeAdapter$DateDeserializer
   *  .deserialize(BitstampExchangeAdapter.java:596)
   * at com.gazbert.bxbot.adapter.BitstampExchangeAdapter$DateDeserializer
   *  .deserialize(BitstampExchangeAdapter.java:1)
   * at com.google.gson.TreeTypeAdapter.read(TreeTypeAdapter.java:58)
   * </pre>
   */
  private static class BitstampDateDeserializer implements JsonDeserializer<Date> {

    private final SimpleDateFormat bitstampDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    BitstampDateDeserializer() {
    }

    public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
      Date dateFromBitstamp = null;
      if (json.isJsonPrimitive()) {
        try {
          dateFromBitstamp = bitstampDateFormat.parse(json.getAsString());
        } catch (ParseException e) {
          final String errorMsg = "DateDeserializer failed to parse a Bitstamp date!";
          LOG.error(errorMsg, e);
          throw new JsonParseException(errorMsg, e);
        }
      }
      return dateFromBitstamp;
    }
  }

  // --------------------------------------------------------------------------
  //  Transport layer methods // 传输层方法
  // --------------------------------------------------------------------------

  private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod)
      throws ExchangeNetworkException, TradingApiException {
    try {
      final URL url = new URL(API_BASE_URL + apiMethod);
      return makeNetworkRequest(url, "GET", null, createHeaderParamMap());

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized.";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      // Setup common params for the API call
      // 设置 API 调用的常用参数
      if (params == null) {
        params = createRequestParamMap();
      }

      params.put("key", key);
      params.put("nonce", Long.toString(nonce));

      // Create MAC message for signature
      // 创建用于签名的 MAC 消息
      // message = nonce + client_id + api_key
      mac.reset(); // force reset 强制重置
      mac.update(String.valueOf(nonce).getBytes(StandardCharsets.UTF_8));
      mac.update(clientId.getBytes(StandardCharsets.UTF_8));
      mac.update(key.getBytes(StandardCharsets.UTF_8));

      /**
       * Signature is a HMAC-SHA256 encoded message containing: nonce, client ID and API key.
        The HMAC-SHA256 code must be generated using a secret key that was generated with your
        API key.
       签名是 HMAC-SHA256 编码的消息，包含：nonce、客户端 ID 和 API 密钥。
       HMAC-SHA256 代码必须使用由您生成的密钥生成
       API 密钥。

       * This code must be converted to it's hexadecimal representation (64 uppercase characters).
       * 此代码必须转换为十六进制表示（64 个大写字符）。
       *
       * signature = hmac.new(API_SECRET, msg=message, digestmod=hashlib.sha256).hexdigest().upper()
       */
      final String signature = toHex(mac.doFinal()).toUpperCase();
      params.put("signature", signature);

      // increment ready for next call... 增量准备下一次调用...
      nonce++;

      // Build the URL with query param args in it  // 构建带有查询参数 args 的 URL
      final StringBuilder postData = new StringBuilder();
      for (final Map.Entry<String, String> param : params.entrySet()) {
        if (postData.length() > 0) {
          postData.append("&");
        }
        postData.append(param.getKey());
        postData.append("=");
        postData.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
      }

      // Request headers required by Exchange
      // Exchange 所需的请求标头
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

      // MUST have the trailing slash else exchange barfs...
      // 必须有尾部斜线否则交换barfs ...
      final URL url = new URL(API_BASE_URL + apiMethod + "/");
      return makeNetworkRequest(url, "POST", postData.toString(), requestHeaders);

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

  /**
   * Initialises the secure messaging layer.
   Sets up the MAC to safeguard the data we send to the exchange.
    Used to encrypt the hash of the entire message with the private key to ensure message
   integrity. We fail hard n fast if any of this stuff blows.
   初始化安全消息层。
   设置 MAC 以保护我们发送到交易所的数据。
   用于用私钥对整条消息的哈希进行加密，以保证消息
   正直。如果这些东西中的任何一个发生爆炸，我们就会很快失败。
   */
  private void initSecureMessageLayer() {
    try {
      final SecretKeySpec keyspec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac = Mac.getInstance("HmacSHA256");
      mac.init(keyspec);
      initializedMacAuthentication = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA256 installed? 设置 MAC 安全性失败。提示：是否安装了 HMAC-SHA256？";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = "Failed to setup MAC security. Secret key seems invalid! 设置 MAC 安全性失败。密钥似乎无效！";
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods // 配置方法
  // --------------------------------------------------------------------------

  private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
    clientId = getAuthenticationConfigItem(authenticationConfig, CLIENT_ID_PROPERTY_NAME);
    key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
    secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
  }

  // --------------------------------------------------------------------------
  //  Util methods // 实用方法
  // --------------------------------------------------------------------------

  private void initGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Date.class, new BitstampDateDeserializer());
    gson = gsonBuilder.create();
  }

  /**
   * Hack for unit-testing map params passed to transport layer.
   * * Hack 用于传递给传输层的单元测试map参数。
   */
  private Map<String, String> createRequestParamMap() {
    return new HashMap<>();
  }

  /**
   * Hack for unit-testing header params passed to transport layer.
   * * 用于传递给传输层的单元测试标头参数的 Hack。
   */
  private Map<String, String> createHeaderParamMap() {
    return new HashMap<>();
  }

  /**
   * Hack for unit-testing transport layer.
   *  用于单元测试传输层的 Hack。
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
