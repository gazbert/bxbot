/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
 * Copyright (c) 2019 David Huertas
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
import java.net.URLEncoder;
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
 * Exchange Adapter for integrating with the CoinbasePro exchange. The CoinbasePro API is documented
 * <a href="https://docs.pro.coinbase.com">here</a>.
 * 用于与 CoinbasePro 交换集成的交换适配器。 CoinbasePro API 已记录在案
 *  * <a href="https://docs.pro.coinbase.com">这里</a>。
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
  could lose money. Despite running live on COINBASE PRO, it has only been unit tested up until the
  point of calling the {@link #sendPublicRequestToExchange(String, Map)} and {@link
  #sendAuthenticatedRequestToExchange(String, String, Map)} methods. Use it at our own risk!
 </strong>
 <p><strong> 免责声明：此交换适配器按原样提供；它可能有错误，你
 可能会赔钱。尽管在 COINBASE PRO 上实时运行，但它只进行了单元测试，直到
 调用 {@link #sendPublicRequestToExchange(String, Map)} 和 {@link
#sendAuthenticatedRequestToExchange(String, String, Map)} 方法。使用它需要我们自担风险！
 </strong>

 *
 * <p>This adapter only supports the CoinbasePro <a href="https://docs.pro.coinbase.com/#api">REST
 API</a>. The design of the API and documentation is excellent.
 <p>此适配器仅支持 CoinbasePro <a href="https://docs.pro.coinbase.com/#api">REST
 API</a>。 API 和文档的设计非常出色。
 *
 * <p>The adapter currently only supports <a
  href="https://docs.pro.coinbase.com/#place-a-new-order">Limit Orders</a>. It was originally
  developed and tested for BTC-GBP market, but it should work for BTC-USD or BTC-EUR.
 <p>适配器目前只支持<a
 href="https://docs.pro.coinbase.com/#place-a-new-order">限价订单</a>。它原本是
 为 BTC-GBP 市场开发和测试，但它应该适用于 BTC-USD 或 BTC-EUR。
 *

 * <p>Exchange fees are loaded from the exchange.yaml file on startup; they are not fetched from the
  exchange at runtime as the CoinbasePro REST API does not support this. The fees are used across
  all markets. Make sure you keep an eye on the <a
  href="https://docs.pro.coinbase.com/#fees">exchange fees</a> and update the config accordingly.
 <p>交易所费用在启动时从 exchange.yaml 文件中加载；它们不是从
 在运行时进行交换，因为 CoinbasePro REST API 不支持这一点。费用用于
 所有市场。请务必留意 <a
 href="https://docs.pro.coinbase.com/#fees">兑换费</a> 并相应地更新配置。
 *
 * <p>NOTE: CoinbasePro requires all price values to be limited to 2 decimal places when creating
  orders. This adapter truncates any prices with more than 2 decimal places and rounds using {@link
  java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be sent to the exchange as 250.18.
 <p>注意：CoinbasePro 在创建时要求所有价格值限制为 2 位小数
 订单。此适配器使用 {@link 截断任何超过 2 位小数和四舍五入的价格
  java.math.RoundingMode#HALF_EVEN}，例如250.176 将作为 250.18 发送到交易所。
 *
 * <p>The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single
  thread in order to preserve trade execution order. The {@link URLConnection} achieves this by
  blocking/waiting on the input stream (response) for each API call.
 <p>交换适配器<em>不是</em>线程安全的。它期望使用单个调用
 线程以保持交易执行顺序。 {@link URLConnection} 通过
 阻塞/等待每个 API 调用的输入流（响应）。

 *
 * <p>The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error
  occurs trying to connect to the exchange. A {@link TradingApiException} is thrown for
  <em>all</em> other failures.
 <p>如果网络错误，{@link TradingApi} 调用将抛出 {@link ExchangeNetworkException}
 尝试连接到交易所时发生。抛出 {@link TradingApiException}
 <em>所有</em>其他故障。
 *
 * @author davidhuertas
 * @since 1.0
 */
public final class CoinbaseProExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String PUBLIC_API_BASE_URL = "https://api.pro.coinbase.com/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in COINBASE PRO Exchange Adapter. COINBASE PRO 交换适配器发生意外错误。";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error. 由于意外 IO 错误，无法连接到 Exchange。";

  private static final String PRODUCTS = "products/";
  private static final String PRICE = "price";

  private static final String PASSPHRASE_PROPERTY_NAME = "passphrase";
  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
  private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";
  private static final String SERVER_TIME_BIAS_PROPERTY_NAME = "time-server-bias";

  private BigDecimal buyFeePercentage;
  private BigDecimal sellFeePercentage;
  private Long timeServerBias;

  private String passphrase = "";
  private String key = "";
  private String secret = "";

  private Mac mac;
  private boolean initializedMacAuthentication = false;

  private Gson gson;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise COINBASE PRO ExchangeConfig: 即将初始化 COINBASE PRO Exchange 配置：" + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);
    setOtherConfig(config);

    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // COINBASE PRO API Calls adapted to the Trading API.    适用于交易 API 的 COINBASE PRO API 调用。
  // See https://docs.pro.coinbase.com/#api
  // --------------------------------------------------------------------------

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {
    try {
      /**
       * Build Limit Order: https://docs.pro.coinbase.com/#place-a-new-order
       * * 建立限价单：https://docs.pro.coinbase.com/#place-a-new-order
       *
       * stp param optional           - (Self-trade prevention flag) defaults to 'dc' Decrease &
                                     Cancel
       stp 参数可选 - （自我交易预防标志）默认为 'dc' 减少 &
       取消

       * post_only param optional     - defaults to 'false'
       * * post_only 参数可选 - 默认为 'false'
       *
       * time_in_force param optional - defaults to 'GTC' Good til Cancel
       * * time_in_force 参数可选 - 默认为 'GTC' 好直到取消
       *
       * client_oid param is optional - thia adapter does not use it.
       * * client_oid 参数是可选的 - thia 适配器不使用它。
       */
      final Map<String, String> params = createRequestParamMap();

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

      params.put("product_id", marketId);

      // note we need to limit price to 2 decimal places else exchange will barf
      // 请注意，我们需要将价格限制为小数点后 2 位，否则交易所将失败
      params.put(PRICE, new DecimalFormat("#.##", getDecimalFormatSymbols()).format(price));

      // note we need to limit size to 8 decimal places else exchange will barf
      // 注意我们需要将大小限制为小数点后 8 位，否则交换会出错
      params.put(
          "size", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(quantity));

      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("POST", "orders", params);
      LOG.debug(() -> "Create Order response:  创建订单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseProOrder createOrderResponse =
            gson.fromJson(response.getPayload(), CoinbaseProOrder.class);
        if (createOrderResponse != null
            && (createOrderResponse.id != null && !createOrderResponse.id.isEmpty())) {
          return createOrderResponse.id;
        } else {
          final String errorMsg = "Failed to place order on exchange. Error response: 未能在交易所下订单。错误响应：" + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }
      } else {
        final String errorMsg = "Failed to create order on exchange. Details: 未能在交易所创建订单。细节：" + response;
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

  /**
   * marketId is not needed for cancelling orders on this exchange.
   * 取消此交易所的订单不需要 marketId。
   */
  @Override
  public boolean cancelOrder(String orderId, String marketIdNotNeeded)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("DELETE", "orders/" + orderId, null);

      LOG.debug(() -> "Cancel Order response: " + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        // 1 Nov 2017 - COINBASE PRO API no longer returns cancelled orderId in array payload;
        // 2017 年 11 月 1 日 - COINBASE PRO API 不再在数组有效负载中返回取消的 orderId；
        // it returns [null]...  它返回 [null] ...
        return true;
      } else {
        final String errorMsg = "Failed to cancel order on exchange. Details: 在交易所取消订单失败。细节： ”" + response;
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
      // we use default request no-param call - only open or un-settled orders are returned.
      // 我们使用默认请求无参数调用 - 仅返回未结或未结算的订单。
      // As soon as an order is no longer open and settled, it will no longer appear in the default   request.
      // 一旦订单不再打开和结算，它将不再出现在默认请求中。
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("GET", "orders", null);

      LOG.debug(() -> "Open Orders response: 未结订单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseProOrder[] coinbaseProOpenOrders =
            gson.fromJson(response.getPayload(), CoinbaseProOrder[].class);
        final List<OpenOrder> ordersToReturn = new ArrayList<>();
        for (final CoinbaseProOrder openOrder : coinbaseProOpenOrders) {

          if (!marketId.equalsIgnoreCase(openOrder.productId)) {
            continue;
          }

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
                  "Unrecognised order type received in getYourOpenOrders(). Value: 在 getYourOpenOrders() 中收到无法识别的订单类型。价值："
                      + openOrder.side);
          }

          final OpenOrder order =
              new OpenOrderImpl(
                  openOrder.id,
                  Date.from(Instant.parse(openOrder.createdAt)),
                  marketId,
                  orderType,
                  openOrder.price,
                  openOrder.size.subtract(
                      openOrder.filledSize), // quantity remaining - not provided by COINBASE PRO 剩余数量 - 不由 COINBASE PRO 提供
                  openOrder.size, // orig quantity
                  openOrder.price.multiply(openOrder.size) // total - not provided by COINBASE PRO 总计 - 不由 COINBASE PRO 提供
                  );

          ordersToReturn.add(order);
        }
        return ordersToReturn;
      } else {
        final String errorMsg =
            "Failed to get your open orders from exchange. Details: 未能从交易所获得您的未结订单。细节：" + response;
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
      params.put("level", "2"); //  "2" = Top 50 bids and asks (aggregated) "2" = 前 50 名出价和要价（汇总）

      final ExchangeHttpResponse response =
          sendPublicRequestToExchange(PRODUCTS + marketId + "/book", params);

      LOG.debug(() -> "Market Orders response: 市价单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseProBookWrapper orderBook =
            gson.fromJson(response.getPayload(), CoinbaseProBookWrapper.class);

        final List<MarketOrder> buyOrders = new ArrayList<>();
        for (CoinbaseProMarketOrder coinbaseProBuyOrder : orderBook.bids) {
          final MarketOrder buyOrder =
              new MarketOrderImpl(
                  OrderType.BUY,
                  coinbaseProBuyOrder.get(0),
                  coinbaseProBuyOrder.get(1),
                  coinbaseProBuyOrder.get(0).multiply(coinbaseProBuyOrder.get(1)));
          buyOrders.add(buyOrder);
        }

        final List<MarketOrder> sellOrders = new ArrayList<>();
        for (CoinbaseProMarketOrder coinbaseProSellOrder : orderBook.asks) {
          final MarketOrder sellOrder =
              new MarketOrderImpl(
                  OrderType.SELL,
                  coinbaseProSellOrder.get(0),
                  coinbaseProSellOrder.get(1),
                  coinbaseProSellOrder.get(0).multiply(coinbaseProSellOrder.get(1)));
          sellOrders.add(sellOrder);
        }
        return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);

      } else {
        final String errorMsg =
            "Failed to get market order book from exchange. Details: 未能从交易所获取市场订单簿。细节：" + response;
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
  public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("GET", "accounts", null);

      LOG.debug(() -> "Balance Info response: 余额信息回复：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseProAccount[] coinbaseProAccounts =
            gson.fromJson(response.getPayload(), CoinbaseProAccount[].class);

        final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();
        final HashMap<String, BigDecimal> balancesOnHold = new HashMap<>();

        for (final CoinbaseProAccount coinbaseProAccount : coinbaseProAccounts) {
          balancesAvailable.put(coinbaseProAccount.currency, coinbaseProAccount.available);
          balancesOnHold.put(coinbaseProAccount.currency, coinbaseProAccount.hold);
        }
        return new BalanceInfoImpl(balancesAvailable, balancesOnHold);
      } else {
        final String errorMsg =
            "Failed to get your wallet balance info from exchange. Details: 无法从交易所获取您的钱包余额信息。细节：" + response;
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
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    try {
      final ExchangeHttpResponse response =
          sendPublicRequestToExchange(PRODUCTS + marketId + "/ticker", null);

      LOG.debug(() -> "Latest Market Price response: 最新市价回应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseProTicker coinbaseProTicker =
            gson.fromJson(response.getPayload(), CoinbaseProTicker.class);
        return coinbaseProTicker.price;
      } else {
        final String errorMsg = "Failed to get market ticker from exchange. Details: 无法从交易所获取市场代码。细节：" + response;
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

  /**
   * COINBASE PRO does not provide API call for fetching % buy fee; it only provides the fee
   monetary value for a given order via e.g. /orders/<order-id> API call. We load the % fee
   statically from exchange.yaml file.

   COINBASE PRO 不提供获取百分比购买费用的 API 调用；它只提供费用
   给定订单的货币价值，例如/orders/<order-id> API 调用。我们加载 % 费用
   从 exchange.yaml 文件中静态获取。
   */
  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return buyFeePercentage;
  }

  /**
   * COINBASE PRO does not provide API call for fetching % sell fee; it only provides the fee
    monetary value for a given order via e.g. /orders/<order-id> API call. We load the % fee
    statically from exchange.yaml file.
   COINBASE PRO 不提供 API 调用来获取 % 销售费用；它只提供费用
   给定订单的货币价值，例如/orders/<order-id> API 调用。我们加载 % 费用
   从 exchange.yaml 文件中静态获取。
   */
  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return sellFeePercentage;
  }

  @Override
  public String getImplName() {
    return "COINBASE PRO REST API v1";
  }

  @Override
  public Ticker getTicker(String marketId) throws ExchangeNetworkException, TradingApiException {
    try {
      final ExchangeHttpResponse tickerResponse =
          sendPublicRequestToExchange(PRODUCTS + marketId + "/ticker", null);

      LOG.debug(() -> "Ticker response: 代码响应：" + tickerResponse);

      if (tickerResponse.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseProTicker coinbaseProTicker =
            gson.fromJson(tickerResponse.getPayload(), CoinbaseProTicker.class);

        final TickerImpl ticker =
            new TickerImpl(
                coinbaseProTicker.price,
                coinbaseProTicker.bid,
                coinbaseProTicker.ask,
                null, // low,
                null, // high,
                null, // open,
                coinbaseProTicker.volume,
                null, // vwap - not supplied by COINBASE PRO  vwap - 不是由 COINBASE PRO 提供
                Date.from(Instant.parse(coinbaseProTicker.time)).getTime());

        // Now we need to call the stats operation to get the 24hr indicators  // 现在我们需要调用 stats 操作来获取 24 小时指标
        final ExchangeHttpResponse statsResponse =
            sendPublicRequestToExchange(PRODUCTS + marketId + "/stats", null);

        LOG.debug(() -> "Stats response: 统计响应：" + statsResponse);

        if (statsResponse.getStatusCode() == HttpURLConnection.HTTP_OK) {
          final CoinbaseProStats coinbaseProStats =
              gson.fromJson(statsResponse.getPayload(), CoinbaseProStats.class);
          ticker.setLow(coinbaseProStats.low);
          ticker.setHigh(coinbaseProStats.high);
          ticker.setOpen(coinbaseProStats.open);
        } else {
          final String errorMsg = "Failed to get stats from exchange. Details: 无法从交易所获取统计信息。细节：" + statsResponse;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

        return ticker;

      } else {
        final String errorMsg =
            "Failed to get market ticker from exchange. Details: 无法从交易所获取市场代码。细节：" + tickerResponse;
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

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.  JSON 响应的 GSON 类。
  //  See https://docs.pro.coinbase.com/#api
  // --------------------------------------------------------------------------

  /**
   * GSON class for COINBASE PRO '/orders' API call response.
   * * COINBASE PRO '/orders' API 调用响应的 GSON 类。
   *
   * <p>There are other critters in here different to what is spec'd:
   * <p>这里还有其他与规范不同的小动物：
   *
   * https://docs.pro.coinbase.com/#list-orders
   */
  private static class CoinbaseProOrder {

    String id;
    BigDecimal price;
    BigDecimal size;

    @SerializedName("product_id")
    String productId; // e.g. "BTC-GBP", "BTC-USD" // 例如“BTC-英镑”、“BTC-美元”

    String side; // "buy" or "sell" “买”或“卖”
    String stp; // Self-Trade Prevention flag, e.g. "dc" 自我交易预防标志，例如“直流”
    String type; // order type, e.g. "limit" 订单类型，例如“限制”

    @SerializedName("time_in_force")
    String timeInForce; // e.g. "GTC" (Good Til Cancelled) 例如“GTC”（直到取消）

    @SerializedName("post_only")
    boolean postOnly; // shows in book + provides exchange liquidity, but will not execute  显示在账簿上 + 提供交易所流动性，但不会执行

    @SerializedName("created_at")
    String createdAt; // e.g. "2014-11-14 06:39:55.189376+00"

    @SerializedName("fill_fees")
    BigDecimal fillFees;

    @SerializedName("filled_size")
    BigDecimal filledSize;

    String status; // e.g. "open" 例如“打开”
    boolean settled;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add(PRICE, price)
          .add("size", size)
          .add("productId", productId)
          .add("side", side)
          .add("stp", stp)
          .add("type", type)
          .add("timeInForce", timeInForce)
          .add("postOnly", postOnly)
          .add("createdAt", createdAt)
          .add("fillFees", fillFees)
          .add("filledSize", filledSize)
          .add("status", status)
          .add("settled", settled)
          .toString();
    }
  }

  /** GSON class for COINBASE PRO '/products/{marketId}/book' API call response.
   * /COINBASE PRO '/products/{marketId}/book' API 调用响应的 GSON 类。 */
  private static class CoinbaseProBookWrapper {

    long sequence;
    List<CoinbaseProMarketOrder> bids;
    List<CoinbaseProMarketOrder> asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("sequence", sequence)
          .add("bids", bids)
          .add("asks", asks)
          .toString();
    }
  }

  /**
   * GSON class for holding Market Orders. First element in array is price, second element is
   amount, third is number of orders.
   用于持有市价单的 GSON 类。数组中的第一个元素是价格，第二个元素是
   金额，第三是订单数量。
   */
  private static class CoinbaseProMarketOrder extends ArrayList<BigDecimal> {

    private static final long serialVersionUID = -4919711220797077759L;
  }

  /** GSON class for COINBASE PRO '/products/{marketId}/ticker' API call response.
   COINBASE PRO '/products/{marketId}/ticker' API 调用响应的 GSON 类。*/

  private static class CoinbaseProTicker {

    @SerializedName("trade_id")
    long tradeId;

    BigDecimal price;
    BigDecimal size;
    BigDecimal bid;
    BigDecimal ask;
    BigDecimal volume;
    String time; // e.g. "2015-10-14T19:19:36.604735Z"

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("tradeId", tradeId)
          .add(PRICE, price)
          .add("size", size)
          .add("bid", bid)
          .add("ask", ask)
          .add("volume", volume)
          .add("time", time)
          .toString();
    }
  }

  /** GSON class for COINBASE PRO '/products/&ltproduct-id&gt/stats' API call response.
   * COINBASE PRO '/products/&ltproduct-id&gt/stats' API 调用响应的 GSON 类。 */
  private static class CoinbaseProStats {

    BigDecimal open;
    BigDecimal high;
    BigDecimal low;
    BigDecimal volume;
    BigDecimal last;

    @SerializedName("volume_30day")
    String volume30Day;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("open", open)
          .add("high", high)
          .add("low", low)
          .add("volume", volume)
          .add("last", last)
          .add("volume30Day", volume30Day)
          .toString();
    }
  }

  /** GSON class for COINBASE PRO '/accounts' API call response.
   * COINBASE PRO '/accounts' API 调用响应的 GSON 类。*/
  private static class CoinbaseProAccount {

    String id;
    String currency;
    BigDecimal balance; // e.g. "0.0000000000000000"
    BigDecimal hold;
    BigDecimal available;

    @SerializedName("profile_id") // no idea what this is?!  不知道这是什么？！
    String profileId;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add("currency", currency)
          .add("balance", balance)
          .add("hold", hold)
          .add("available", available)
          .add("profileId", profileId)
          .toString();
    }
  }

  // --------------------------------------------------------------------------
  //  Transport layer methods // 传输层方法
  // --------------------------------------------------------------------------

  private ExchangeHttpResponse sendPublicRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {
    if (params == null) {
      params = createRequestParamMap(); // no params, so empty query string 没有参数，所以查询字符串为空
    }

    // Request headers required by Exchange  // Exchange 所需的请求标头
    final Map<String, String> requestHeaders = new HashMap<>();

    try {

      final StringBuilder queryString = new StringBuilder();
      if (params.size() > 0) {

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

  /**
   * Makes an authenticated API call to the COINBASE PRO exchange.
   *  对 COINBASE PRO 交换进行经过身份验证的 API 调用。
   *
   * The COINBASE PRO authentication process is complex, but well documented:
    https://docs.pro.coinbase.com/#creating-a-request
   COINBASE PRO 身份验证过程很复杂，但有据可查：
   https://docs.pro.coinbase.com/#creating-a-request

   *
   * All REST requests must contain the following headers:
   * * 所有 REST 请求必须包含以下标头：
   *
   * CB-ACCESS-KEY          The api key as a string. api 键作为字符串。
   * CB-ACCESS-SIGN         The base64-encoded signature (see Signing a Message).  base64 编码的签名（请参阅签署消息）。
   * CB-ACCESS-TIMESTAMP    A timestamp for your request. 您的请求的时间戳。
   * CB-ACCESS-PASSPHRASE   The passphrase you specified when creating the API key.  创建 API 密钥时指定的密码。
   *
   * The CB-ACCESS-TIMESTAMP header MUST be number of seconds since Unix Epoch in UTC.
    Decimal values are allowed.
   CB-ACCESS-TIMESTAMP 标头必须是 UTC 中自 Unix 纪元以来的秒数。
   允许使用十进制值。
   *
   * Your timestamp must be within 30 seconds of the api service time or your request will be
    considered expired and rejected. We recommend using the time endpoint to query for the API
    server time if you believe there many be time skew between your server and the API servers.
   您的时间戳必须在 api 服务时间的 30 秒内，否则您的请求将
   视为过期并被拒绝。我们建议使用时间端点来查询 API
   如果您认为您的服务器和 API 服务器之间存在很多时间偏差，请使用服务器时间。

   *
   * All request bodies should have content type application/json and be valid JSON.
   *  * 所有请求正文都应具有内容类型 application/json 并且是有效的 JSON。
   *
   * The CB-ACCESS-SIGN header is generated by creating a sha256 HMAC using the base64-decoded
    secret key on the prehash string:
   CB-ACCESS-SIGN 标头是通过使用 base64 解码创建 sha256 HMAC 生成的
   prehash 字符串上的密钥：

   *
   * timestamp + method + requestPath + body (where + represents string concatenation)
    and base64-encode the output.
   时间戳 + 方法 + requestPath + 正文（其中 + 表示字符串连接）
   并对输出进行 base64 编码。

   * The timestamp value is the same as the CB-ACCESS-TIMESTAMP header.
   * * 时间戳值与 CB-ACCESS-TIMESTAMP 标头相同。
   *
   * The body is the request body string or omitted if there is no request body
    (typically for GET requests).
   body 是请求正文字符串，如果没有请求正文，则省略
   （通常用于 GET 请求）。

   *
   * The method should be UPPER CASE.
   * 该方法应为大写。
   *
   * Remember to first base64-decode the alphanumeric secret string (resulting in 64 bytes) before
    using it as the key for HMAC. Also, base64-encode the digest output before sending in the
    header.
   记得先base64-decode字母数字的秘密字符串（产生64个字节）之前
   使用它作为 HMAC 的密钥。此外，在发送之前对摘要输出进行 base64 编码
   标题。
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String httpMethod, String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized. MAC 消息安全层尚未初始化。";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        // create empty map for non-param API calls // 为非参数 API 调用创建空映射
        params = createRequestParamMap();
      }

      // Build the request  // 构建请求
      final String invocationUrl;
      String requestBody = "";

      switch (httpMethod) {
        case "GET":
          LOG.debug(() -> "Building secure GET request... 构建安全的 GET 请求...");
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
          LOG.debug(() -> "Query param string: 查询参数字符串：" + queryParams);

          if (params.isEmpty()) {
            invocationUrl = AUTHENTICATED_API_URL + apiMethod;
          } else {
            invocationUrl = AUTHENTICATED_API_URL + apiMethod + "?" + queryParams;
          }
          break;

        case "POST":
          LOG.debug(() -> "Building secure POST request... 构建安全的 POST 请求...");
          invocationUrl = AUTHENTICATED_API_URL + apiMethod;
          requestBody = gson.toJson(params);
          break;

        case "DELETE":
          LOG.debug(() -> "Building secure DELETE request...构建安全的 DELETE 请求...");
          invocationUrl = AUTHENTICATED_API_URL + apiMethod;
          break;

        default:
          throw new IllegalArgumentException(
              "Don't know how to build secure  不知道如何构建安全[" + httpMethod + "] request! 请求!");
      }

      // Get UNIX EPOCH in secs and add the time server bias
      // 以秒为单位获取 UNIX EPOCH 并添加时间服务器偏差
      final long timeServer = Instant.now().getEpochSecond() + timeServerBias;
      final String timestamp = Long.toString(timeServer);
      LOG.debug(() -> "Server UNIX EPOCH in seconds: " + timestamp);

      // Build the signature string: timestamp + method + requestPath + body
      // 构建签名字符串：timestamp + method + requestPath + body
      final String signatureBuilder =
          timestamp + httpMethod.toUpperCase() + "/" + apiMethod + requestBody;

      // Sign the signature string and Base64 encode it
      // 对签名字符串进行签名并进行 Base64 编码
      mac.reset();
      mac.update(signatureBuilder.getBytes(StandardCharsets.UTF_8));
      final String signature = DatatypeConverter.printBase64Binary(mac.doFinal());

      // Request headers required by Exchange
      // Exchange 所需的请求标头
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("Content-Type", "application/json");
      requestHeaders.put("CB-ACCESS-KEY", key);
      requestHeaders.put("CB-ACCESS-SIGN", signature);
      requestHeaders.put("CB-ACCESS-TIMESTAMP", timestamp);
      requestHeaders.put("CB-ACCESS-PASSPHRASE", passphrase);

      final URL url = new URL(invocationUrl);
      return makeNetworkRequest(url, httpMethod, requestBody, requestHeaders);

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
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
      // COINBASE PRO secret is in Base64 so we must decode it first.
      // COINBASE PRO 机密在 Base64 中，因此我们必须先对其进行解码。

      final byte[] decodedBase64Secret = DatatypeConverter.parseBase64Binary(secret);

      final SecretKeySpec keyspec = new SecretKeySpec(decodedBase64Secret, "HmacSHA256");
      mac = Mac.getInstance("HmacSHA256");
      mac.init(keyspec);
      initializedMacAuthentication = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = " Failed to setup MAC security. HINT: Is HMAC-SHA256 installed? 设置 MAC 安全性失败。提示：是否安装了 HMAC-SHA256？";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = " Failed to setup MAC security. Secret key seems invalid! 设置 MAC 安全性失败。密钥似乎无效！";
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods // 配置方法
  // --------------------------------------------------------------------------

  private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
    passphrase = getAuthenticationConfigItem(authenticationConfig, PASSPHRASE_PROPERTY_NAME);
    key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
    secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
  }

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    final String buyFeeInConfig = getOtherConfigItem(otherConfig, BUY_FEE_PROPERTY_NAME);
    buyFeePercentage =
        new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    LOG.info(() -> "Buy fee % in BigDecimal format: BigDecimal 格式的购买费用百分比：" + buyFeePercentage);

    final String sellFeeInConfig = getOtherConfigItem(otherConfig, SELL_FEE_PROPERTY_NAME);
    sellFeePercentage =
        new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    LOG.info(() -> "Sell fee % in BigDecimal format: BigDecimal 格式的销售费用百分比：" + sellFeePercentage);

    final String serverTimeBiasInConfig =
        getOtherConfigItem(otherConfig, SERVER_TIME_BIAS_PROPERTY_NAME);
    timeServerBias = Long.parseLong(serverTimeBiasInConfig);
    LOG.info(() -> "Time server bias in long format: 长格式的时间服务器偏差：" + timeServerBias);
  }

  // --------------------------------------------------------------------------
  //  Util methods // 实用方法
  // --------------------------------------------------------------------------

  private void initGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gson = gsonBuilder.create();
  }

  /**
   * Hack for unit-testing request params passed to transport layer.
   * * Hack 用于传递给传输层的单元测试请求参数。
   */
  private Map<String, String> createRequestParamMap() {
    return new HashMap<>();
  }

  /**
   * Hack for unit-testing header params passed to transport layer.
   * * 用于传递给传输层的单元测试标头参数的 Hack。
   *
   */
  private Map<String, String> createHeaderParamMap() {
    return new HashMap<>();
  }

  /**
   * Hack for unit-testing transport layer.
   * * 用于单元测试传输层的 Hack。
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
