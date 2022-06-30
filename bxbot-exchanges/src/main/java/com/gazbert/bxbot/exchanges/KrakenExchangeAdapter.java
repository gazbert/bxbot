/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
 * Copyright (c) 2021 maiph
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

import static java.util.Collections.emptyMap;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchange.api.PairPrecisionConfig;
import com.gazbert.bxbot.exchanges.config.PairPrecisionConfigImpl;
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
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Exchange Adapter for integrating with the Kraken exchange. The Kraken API is documented <a href="https://www.kraken.com/en-gb/help/api">here</a>.
 * * 用于与 Kraken 交换集成的交换适配器。 Kraken API 记录在<a href="https://www.kraken.com/en-gb/help/api">这里</a>。
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
  could lose money. Despite running live on Kraken, it has only been unit tested up until the point
  of calling the {@link #sendPublicRequestToExchange(String, Map)} and {@link #sendAuthenticatedRequestToExchange(String, Map)} methods. Use it at our own risk! </strong>
 <p><strong> 免责声明：此交换适配器按原样提供；它可能有错误，你
 可能会赔钱。尽管在 Kraken 上实时运行，但到目前为止它只进行了单元测试
 调用 {@link #sendPublicRequestToExchange(String, Map)} 和 {@link #sendAuthenticatedRequestToExchange(String, Map)} 方法。使用它需要我们自担风险！ </strong>
 *
 * <p>It only supports <a
  href="https://support.kraken.com/hc/en-us/articles/203325783-Market-and-Limit-Orders">limit
  orders</a> at the spot price; it does not support <a
  href="https://support.kraken.com/hc/en-us/sections/200560633-Leverage-and-Margin">leverage and
  margin</a> trading.
 <p>它只支持<a
 href="https://support.kraken.com/hc/en-us/articles/203325783-Market-and-Limit-Orders">限制
 以现货价格下单</a>；它不支持 <a
 href="https://support.kraken.com/hc/en-us/sections/200560633-Leverage-and-Margin">杠杆和
 保证金</a> 交易。
 *
 * <p>Exchange fees are loaded from the exchange.yaml file on startup; they are not fetched from the
  exchange at runtime as the Kraken REST API does not support this. The fees are used across all
  markets. Make sure you keep an eye on the <a href="https://www.kraken.com/help/fees">exchange
  fees</a> and update the config accordingly.
 <p>Exchange fees are loaded from the exchange.yaml file on startup; they are not fetched from the
 exchange at runtime as the Kraken REST API does not support this. The fees are used across all
 markets. Make sure you keep an eye on the <a href="https://www.kraken.com/help/fees">exchange
 fees</a> and update the config accordingly.
 *
 * <p>The Kraken API has call rate limits - see <a
  href="https://www.kraken.com/en-gb/help/api#api-call-rate-limit">API Call Rate Limit</a> for
  details.
 <p>Kraken API 有调用率限制 - 请参阅 <a
 href="https://www.kraken.com/en-gb/help/api#api-call-rate-limit">API 调用速率限制</a>
 细节。
 *
 * <p>Kraken markets assets (e.g. currencies) can be referenced using their ISO4217-A3 names in the
  case of ISO registered names, their 3 letter commonly used names in the case of unregistered
  names, or their X-ISO4217-A3 code (see http://www.ifex-project.org/).
 <p>Kraken 市场资产（例如货币）可以使用其 ISO4217-A3 名称在
 ISO注册名称的情况下，未注册的情况下它们的3个字母常用名称
 名称或它们的 X-ISO4217-A3 代码（参见 http://www.ifex-project.org/）。
 *
 * <p>This adapter expects the market id to use the 3 letter commonly used names, e.g. you access
  the XBT/USD market using 'XBTUSD'. Note: the exchange always returns the market id back in the
  X-ISO4217-A3 format, i.e. 'XXBTZUSD'. The reason for doing this is because the Open Order
  response contains the asset pair in the 3 letter format ('XBTUSD'), and we need to be able to
  filter only the orders for the given market id.
 <p>此适配器希望市场 id 使用 3 个字母的常用名称，例如你访问
 使用“XBTUSD”的 XBT/USD 市场。注意：交易所总是在
 X-ISO4217-A3 格式，即“XXBTZUSD”。这样做的原因是因为未结订单
 响应包含 3 个字母格式的资产对 ('XBTUSD')，我们需要能够
 仅过滤给定市场 id 的订单。
 *
 * <p>The exchange regularly goes down for maintenance. If the keep-alive-during-maintenance
  config-item is set to true in the exchange.yaml config file, the bot will stay alive and wait
  until the next trade cycle.
 *<p>交易所定期停机进行维护。如果keep-alive-during-maintenance
 *   config-item 在 exchange.yaml 配置文件中设置为 true，bot 将保持活动状态并等待
 *   直到下一个交易周期。
 *
 *
 * <p>The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single
  thread in order to preserve trade execution order. The {@link URLConnection} achieves this by
  blocking/waiting on the input stream (response) for each API call.
 <p>Exchange 适配器<em>不是</em>线程安全的。它期望使用单个调用
 线程以保留交易执行顺序。 {@link URLConnection} 通过
 阻塞/等待每个 API 调用的输入流（响应）。
 *
 * <p>The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error
  occurs trying to connect to the exchange. A {@link TradingApiException} is thrown for
  <em>all</em> other failures.
 *<p>如果网络错误，{@link TradingApi} 调用将抛出 {@link ExchangeNetworkException}
 *   尝试连接到交易所时发生。抛出 {@link TradingApiException}
 *   <em>所有</em>其他故障。
 *
 * @author gazbert
 * @since 1.0
 */
public final class KrakenExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String KRAKEN_BASE_URI = "https://api.kraken.com/";
  private static final String KRAKEN_API_VERSION = "0";
  private static final String KRAKEN_PUBLIC_PATH = "/public/";
  private static final String KRAKEN_PRIVATE_PATH = "/private/";
  private static final String PUBLIC_API_BASE_URL =
      KRAKEN_BASE_URI + KRAKEN_API_VERSION + KRAKEN_PUBLIC_PATH;
  private static final String AUTHENTICATED_API_URL =
      KRAKEN_BASE_URI + KRAKEN_API_VERSION + KRAKEN_PRIVATE_PATH;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in Kraken Exchange Adapter. Kraken 交换适配器发生意外错误。";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error. 由于意外 IO 错误，无法连接到 Exchange。";

  private static final String UNDER_MAINTENANCE_WARNING_MESSAGE =
      "Exchange is undergoing maintenance - keep alive is true. Exchange 正在进行维护 - 保持活力是真的。";
  private static final String FAILED_TO_GET_MARKET_ORDERS =
      "Failed to get Market Order Book from exchange. Details: 从交易所获取市价单失败。细节：";
  private static final String FAILED_TO_GET_BALANCE =
      "Failed to get Balance from exchange. Details: 无法从交易所获得余额。细节：";
  private static final String FAILED_TO_GET_TICKER =
      "Failed to get Ticker from exchange. Details: 无法从交易所获取 Ticker。细节：";

  private static final String FAILED_TO_GET_OPEN_ORDERS =
      "Failed to get Open Orders from exchange. Details: 未能从交易所获取未结订单。细节：";
  private static final String FAILED_TO_ADD_ORDER = "Failed to Add Order on exchange. Details: 在交易所添加订单失败。细节：";
  private static final String FAILED_TO_CANCEL_ORDER =
      "Failed to Cancel Order on exchange. Details: 交易所取消订单失败。细节：";

  private static final String PRICE = "price";

  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
  private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";

  private static final String KEEP_ALIVE_DURING_MAINTENANCE_PROPERTY_NAME =
      "keep-alive-during-maintenance";
  private static final String EXCHANGE_UNDERGOING_MAINTENANCE_RESPONSE = "EService:Unavailable";

  private PairPrecisionConfig pairPrecisionConfig;

  private long nonce = 0;

  private BigDecimal buyFeePercentage;
  private BigDecimal sellFeePercentage;

  private boolean keepAliveDuringMaintenance;

  private String key = "";
  private String secret = "";

  private Mac mac;
  private boolean initializedMacAuthentication = false;

  private Gson gson;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise Kraken ExchangeConfig: 即将初始化 Kraken ExchangeConfig：" + config);
    initGson();
    setAuthenticationConfig(config);
    setNetworkConfig(config);
    loadPairPrecisionConfig();
    setOtherConfig(config);

    nonce = System.currentTimeMillis();
    initSecureMessageLayer();
  }

  // --------------------------------------------------------------------------
  // Kraken API Calls adapted to the Trading API. 适用于交易 API 的 Kraken API 调用。
  // See https://www.kraken.com/en-gb/help/api
  // --------------------------------------------------------------------------

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response;

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("pair", marketId);

      response = sendPublicRequestToExchange("Depth", params);
      LOG.debug(() -> "Market Orders response: 市价单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final Type resultType =
            new TypeToken<KrakenResponse<KrakenMarketOrderBookResult>>() {}.getType();
        final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);

        final List errors = krakenResponse.error;
        if (errors == null || errors.isEmpty()) {
          return adaptKrakenOrderBook(krakenResponse, marketId);

        } else {
          if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
            LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
            throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
          }

          final String errorMsg = FAILED_TO_GET_MARKET_ORDERS + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

      } else {
        final String errorMsg = FAILED_TO_GET_MARKET_ORDERS + response;
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
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response;

    try {
      response = sendAuthenticatedRequestToExchange("OpenOrders", null);
      LOG.debug(() -> "Open Orders response: 未结订单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

        final Type resultType = new TypeToken<KrakenResponse<KrakenOpenOrderResult>>() {}.getType();
        final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);

        final List errors = krakenResponse.error;
        if (errors == null || errors.isEmpty()) {
          return adaptKrakenOpenOrders(krakenResponse, marketId);

        } else {
          if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
            LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
            throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
          }

          final String errorMsg = FAILED_TO_GET_OPEN_ORDERS + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

      } else {
        final String errorMsg = FAILED_TO_GET_OPEN_ORDERS + response;
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
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response;

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("pair", marketId);

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

      String pricePrecision = "#." + "#".repeat(pairPrecisionConfig.getPricePrecision(marketId));
      String volumePrecision = "#." + "#".repeat(pairPrecisionConfig.getVolumePrecision(marketId));

      params.put("ordertype", "limit"); // this exchange adapter only supports limit orders // 此交易所适配器仅支持限价单
      params.put(PRICE, new DecimalFormat(pricePrecision, getDecimalFormatSymbols()).format(price));
      params.put(
          "volume", new DecimalFormat(volumePrecision, getDecimalFormatSymbols()).format(quantity));

      response = sendAuthenticatedRequestToExchange("AddOrder", params);
      LOG.debug(() -> "Create Order response: 创建订单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

        final Type resultType = new TypeToken<KrakenResponse<KrakenAddOrderResult>>() {}.getType();
        final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);

        final List errors = krakenResponse.error;
        if (errors == null || errors.isEmpty()) {

          // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
          // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
          final KrakenAddOrderResult krakenAddOrderResult =
              (KrakenAddOrderResult) krakenResponse.result;

          // Just return the first one. Why an array?
          // 只返回第一个。为什么是数组？
          return krakenAddOrderResult.txid.get(0);

        } else {
          if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
            LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
            throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
          }

          final String errorMsg = FAILED_TO_ADD_ORDER + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

      } else {
        final String errorMsg = FAILED_TO_ADD_ORDER + response;
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
  public boolean cancelOrder(String orderId, String marketIdNotNeeded)
      throws TradingApiException, ExchangeNetworkException {
    ExchangeHttpResponse response;

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("txid", orderId);

      response = sendAuthenticatedRequestToExchange("CancelOrder", params);
      LOG.debug(() -> "Cancel Order response: 取消订单响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

        final Type resultType =
            new TypeToken<KrakenResponse<KrakenCancelOrderResult>>() {}.getType();
        final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);

        final List errors = krakenResponse.error;
        if (errors == null || errors.isEmpty()) {
          return adaptKrakenCancelOrderResult(krakenResponse);

        } else {
          if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
            LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
            throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
          }

          final String errorMsg = FAILED_TO_CANCEL_ORDER + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

      } else {
        final String errorMsg = FAILED_TO_CANCEL_ORDER + response;
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
      throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response;

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("pair", marketId);

      response = sendPublicRequestToExchange("Ticker", params);
      LOG.debug(() -> "Latest Market Price response: 最新市价回应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

        final Type resultType = new TypeToken<KrakenResponse<KrakenTickerResult>>() {}.getType();
        final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);

        final List errors = krakenResponse.error;
        if (errors == null || errors.isEmpty()) {

          // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
          // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
          final KrakenTickerResult tickerResult = (KrakenTickerResult) krakenResponse.result;

          // 'c' key into map is the last market price: last trade closed array(<price>, <lot volume>)
          // 映射中的 'c' 键是最后的市场价格：最后交易关闭数组（<price>, <lot volume>）
          return new BigDecimal(tickerResult.get("c"));

        } else {

          if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
            LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
            throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
          }

          final String errorMsg = FAILED_TO_GET_TICKER + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

      } else {
        final String errorMsg = FAILED_TO_GET_TICKER + response;
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

    ExchangeHttpResponse response;

    try {
      response = sendAuthenticatedRequestToExchange("Balance", null);
      LOG.debug(() -> "Balance Info response: 余额信息回复：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final Type resultType = new TypeToken<KrakenResponse<KrakenBalanceResult>>() {}.getType();
        return adaptKrakenBalanceInfo(response, resultType);

      } else {
        final String errorMsg = FAILED_TO_GET_BALANCE + response;
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
   * Kraken does not provide API call for fetching % buy fee; it only provides the fee monetary
    value for a given order via the OpenOrders API call. We load the % fee statically from
    exchange.yaml file.
   Kraken 不提供获取 % 购买费用的 API 调用；它只提供货币费用
   通过 OpenOrders API 调用给定订单的价值。我们从静态加载 % 费用
   exchange.yaml 文件。
   */
  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return buyFeePercentage;
  }

  /**
   * Kraken does not provide API call for fetching % sell fee; it only provides the fee monetary
    value for a given order via the OpenOrders API call. We load the % fee statically from
    exchange.yaml file.
   *Kraken 不提供获取 % 销售费用的 API 调用；它只提供货币费用
   *     通过 OpenOrders API 调用给定订单的价值。我们从静态加载 % 费用
   *     exchange.yaml 文件。
   */
  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return sellFeePercentage;
  }

  @Override
  public String getImplName() {
    return "Kraken API v1";
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {

    ExchangeHttpResponse response;

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put("pair", marketId);

      response = sendPublicRequestToExchange("Ticker", params);
      LOG.debug(() -> "Ticker response: 代码响应：" + response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {

        final Type resultType = new TypeToken<KrakenResponse<KrakenTickerResult>>() {}.getType();
        final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);

        final List errors = krakenResponse.error;
        if (errors == null || errors.isEmpty()) {

          // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
          // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
          final KrakenTickerResult tickerResult = (KrakenTickerResult) krakenResponse.result;

          // ouch!
          // 哎哟！
          return new TickerImpl(
              new BigDecimal(tickerResult.get("c")), // last trade // 最后一笔交易
              new BigDecimal(tickerResult.get("b")), // bid 投标
              new BigDecimal(tickerResult.get("a")), // ask 问
              new BigDecimal(tickerResult.get("l")), // low 24h 低24小时
              new BigDecimal(tickerResult.get("h")), // high 24hr  高24小时
              new BigDecimal(tickerResult.get("o")), // open 打开
              new BigDecimal(tickerResult.get("v")), // volume 24hr 卷 24 小时
              new BigDecimal(tickerResult.get("p")), // vwap 24hr vwap 24小时
              null); // timestamp not supplied by Kraken  // Kraken 不提供时间戳

        } else {
          if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
            LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
            throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
          }

          final String errorMsg = FAILED_TO_GET_TICKER + response;
          LOG.error(errorMsg);
          throw new TradingApiException(errorMsg);
        }

      } else {
        final String errorMsg = FAILED_TO_GET_TICKER + response;
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
  //  GSON classes for JSON responses. JSON 响应的 GSON 类。
  //  See https://www.kraken.com/en-gb/help/api
  // --------------------------------------------------------------------------

  /**
   * GSON base class for all Kraken responses.
   * 所有 Kraken 响应的 GSON 基类。
   *
   * <p>All Kraken responses have the following format:
   * * <p>所有 Kraken 响应都具有以下格式：
   *
   * <pre>
   *
   * error = array of error messages in the format of:
   * * error = 错误消息数组，格式为：
   *
   * {char-severity code}{string-error category}:{string-error type}[:{string-extra info}] - severity code can be E for error or W for warning
   * * {char-severity code}{string-error category}:{string-error type}[:{string-extra info}] - 严重性代码可以是 E 表示错误或 W 表示警告
   *
   * result = result of API call (may not be present if errors occur)
   * * 结果 = API 调用的结果（如果发生错误，可能不存在）
   *
   * </pre>
   *
   * <p>The result Type is what varies with each API call.
   * <p>结果类型因每个 API 调用而异。
   */
  private static class KrakenResponse<T> {

    List<String> error;
    T result;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("error", error).add("result", result).toString();
    }
  }

  /** GSON class that wraps Depth API call result - the Market Order Book.
   * 包装 Depth API 调用结果的 GSON 类 - 市场订单簿。
   * */
  private static class KrakenMarketOrderBookResult extends HashMap<String, KrakenOrderBook> {

    private static final long serialVersionUID = -4913711010647027721L;
  }

  /** GSON class that wraps a Balance API call result.
   * ** 包装 Balance API 调用结果的 GSON 类。
   *
   * */
  private static class KrakenBalanceResult extends HashMap<String, BigDecimal> {

    private static final long serialVersionUID = -4919711010747027759L;
  }

  /** GSON class that wraps a Ticker API call result.
   * ** 包装 Ticker API 调用结果的 GSON 类。
   *
   * */
  private static class KrakenTickerResult extends HashMap<String, String> {

    private static final long serialVersionUID = -4913711010647027759L;

    KrakenTickerResult() {
    }
  }

  private static class KrakenAssetPairsConfig extends HashMap<String, Object> {

    private static final long serialVersionUID = -9226840830768795L;

    PairPrecisionConfig loadPrecisionConfig() {
      Gson gson = new Gson();
      Map<String, Integer> prices = new HashMap<>();
      Map<String, Integer> volumes = new HashMap<>();

      for (Entry<String, Object> entry : this.entrySet()) {
        JsonElement jsonElement = gson.toJsonTree(entry);

        JsonObject jsonObject = jsonElement.getAsJsonObject().get("value").getAsJsonObject();
        String name = jsonObject.get("altname").getAsString();
        int price = jsonObject.get("pair_decimals").getAsInt();
        int volume = jsonObject.get("lot_decimals").getAsInt();

        prices.put(name, price);
        volumes.put(name, volume);
      }

      return new PairPrecisionConfigImpl(prices, volumes);
    }
  }

  /** GSON class that wraps an Open Order API call result - your open orders.
   * ** 封装 Open Order API 调用结果的 GSON 类 - 您的未结订单。
   * */
  private static class KrakenOpenOrderResult {

    Map<String, KrakenOpenOrder> open;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("open", open).toString();
    }
  }

  /** GSON class the represents a Kraken Open Order.
   * GSON 类代表 Kraken Open Order。
   * */
  private static class KrakenOpenOrder {

    String refid;
    String userref;
    String status;
    double opentm;
    double starttm;
    double expiretm;
    KrakenOpenOrderDescription descr;
    BigDecimal vol;

    @SerializedName("vol_exec")
    BigDecimal volExec;

    BigDecimal cost;
    BigDecimal fee;
    BigDecimal price;
    String misc;
    String oflags;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("refid", refid)
          .add("userref", userref)
          .add("status", status)
          .add("opentm", opentm)
          .add("starttm", starttm)
          .add("expiretm", expiretm)
          .add("descr", descr)
          .add("vol", vol)
          .add("volExec", volExec)
          .add("cost", cost)
          .add("fee", fee)
          .add(PRICE, price)
          .add("misc", misc)
          .add("oflags", oflags)
          .toString();
    }
  }

  /** GSON class the represents a Kraken Open Order description.
   * GSON 类表示 Kraken Open Order 描述。 */
  private static class KrakenOpenOrderDescription {

    String pair;
    String type;
    String ordertype;
    BigDecimal price;
    BigDecimal price2;
    String leverage;
    String order;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("pair", pair)
          .add("type", type)
          .add("ordertype", ordertype)
          .add(PRICE, price)
          .add("price2", price2)
          .add("leverage", leverage)
          .add("order", order)
          .toString();
    }
  }

  /** GSON class representing an AddOrder result.
   * 表示 AddOrder 结果的 GSON 类。 */
  private static class KrakenAddOrderResult {

    KrakenAddOrderResultDescription descr;
    List<String> txid; // why is this a list/array? 为什么这是一个列表/数组？

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("descr", descr).add("txid", txid).toString();
    }
  }

  /** GSON class representing an AddOrder result description.
   * 表示 AddOrder 结果描述的 GSON 类。 */
  private static class KrakenAddOrderResultDescription {

    String order;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("order", order).toString();
    }
  }

  /** GSON class representing a CancelOrder result.
   * 表示 CancelOrder 结果的 GSON 类。
   * */
  private static class KrakenCancelOrderResult {

    int count;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("count", count).toString();
    }
  }

  /** GSON class for a Market Order Book.
   * 市场订单簿的 GSON 类。 */
  private static class KrakenOrderBook {

    List<KrakenMarketOrder> bids;
    List<KrakenMarketOrder> asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("bids", bids).add("asks", asks).toString();
    }
  }

  /**
   * GSON class for holding Market Orders. First element in array is price, second element is amount, 3rd is UNIX time.
   * * 持有市价单的 GSON 类。数组中的第一个元素是价格，第二个元素是金额，第三个是 UNIX 时间。
   */
  private static class KrakenMarketOrder extends ArrayList<BigDecimal> {

    private static final long serialVersionUID = -4959711260742077759L;
  }

  /**
   * Custom GSON Deserializer for Ticker API call result.
   * Ticker API 调用结果的自定义 GSON 反序列化器。
   *
   * <p>Have to do this because last entry in the Ticker param map is a String, not an array like the rest of 'em!
   * * <p>必须这样做，因为 Ticker 参数映射中的最后一个条目是字符串，而不是像其余部分那样的数组！
   */
  private static class KrakenTickerResultDeserializer
      implements JsonDeserializer<KrakenTickerResult> {

    KrakenTickerResultDeserializer() {
    }

    public KrakenTickerResult deserialize(
        JsonElement json, Type type, JsonDeserializationContext context) {

      final KrakenTickerResult krakenTickerResult = new KrakenTickerResult();
      if (json.isJsonObject()) {

        final JsonObject jsonObject = json.getAsJsonObject();

        // assume 1 (KV) entry as per API spec - the K is the market id, the V is a Map of ticker params
        // 根据 API 规范假设 1 (KV) 条目 - K 是市场 id，V 是股票参数的 Map
        final JsonElement tickerParams = jsonObject.entrySet().iterator().next().getValue();

        final JsonObject tickerMap = tickerParams.getAsJsonObject();
        for (Map.Entry<String, JsonElement> jsonTickerParam : tickerMap.entrySet()) {

          final String key = jsonTickerParam.getKey();
          switch (key) {
            case "c":
              final List<String> lastTradeDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("c", lastTradeDetails.get(0));
              break;

            case "b":
              final List<String> bidDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("b", bidDetails.get(0));
              break;

            case "a":
              final List<String> askDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("a", askDetails.get(0));
              break;

            case "l":
              final List<String> lowDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("l", lowDetails.get(1));
              break;

            case "h":
              final List<String> highDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("h", highDetails.get(1));
              break;

            case "o":
              final String openDetails =
                  context.deserialize(jsonTickerParam.getValue(), String.class);
              krakenTickerResult.put("o", openDetails);
              break;

            case "v":
              final List<String> volumeDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("v", volumeDetails.get(1));
              break;

            case "p":
              final List<String> vWapDetails =
                  context.deserialize(jsonTickerParam.getValue(), List.class);
              krakenTickerResult.put("p", vWapDetails.get(1));
              break;

            default:
              LOG.warn(() -> "Received unexpected Ticker param - ignoring: 收到意外的 Ticker 参数 - 忽略：" + key);
          }
        }
      }
      return krakenTickerResult;
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

  /**
   * Makes an authenticated API call to the Kraken exchange.
   * 对 Kraken 交换进行经过身份验证的 API 调用。
   *
   * Kraken requires the following HTTP headers to bet set:
   * * Kraken 需要设置以下 HTTP 标头：
   *
   * API-Key = API key
   * API-Sign = Message signature using HMAC-SHA512 of (URI path + SHA256(nonce + POST data))  and base64 decoded secret API key
   * * API-Sign = 使用 HMAC-SHA512 of (URI path + SHA256(nonce + POST data)) 和 base64 解码的秘密 API 密钥的消息签名
   *
   * The nonce must always increasing unsigned 64 bit integer.
   * * 随机数必须始终增加无符号 64 位整数。
   *
   * Note: Sometimes requests can arrive out of order or NTP can cause your clock to rewind, resulting in nonce issues. If you encounter this issue, you can change the nonce window in
   your account API settings page. The amount to set it to depends upon how you increment the nonce. Depending on your connectivity, a setting that would accommodate 3-15 seconds of
   network issues is suggested.
   注意：有时请求可能会无序到达，或者 NTP 可能会导致您的时钟倒带，从而导致 nonce 问题。如果遇到此问题，可以在中更改 nonce 窗口
   您的帐户 API 设置页面。设置它的数量取决于你如何增加随机数。根据您的连接性，设置可以容纳 3-15 秒
   建议网络问题。
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized. MAC 消息安全层尚未初始化。";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        // create empty map for non param API calls, e.g. "trades"
        // 为非参数 API 调用创建空映射，例如“交易”
        params = createRequestParamMap();
      }

      // The nonce is required by Kraken in every request.   Kraken 在每个请求中都需要随机数。
      // It MUST be incremented each time and the nonce param MUST match the value used in signature.  它必须每次递增，并且 nonce 参数必须与签名中使用的值匹配。
      nonce++;
      params.put("nonce", Long.toString(nonce));

      // Build the URL with query param args in it - yuk!  // 使用其中的查询参数 args 构建 URL - yuk！
      final StringBuilder postData = new StringBuilder();
      for (final Map.Entry<String, String> param : params.entrySet()) {
        if (postData.length() > 0) {
          postData.append("&");
        }
        postData.append(param.getKey());
        postData.append("=");
        postData.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
      }

      // And now the tricky part... ;-o  // 现在是棘手的部分... ;-o
      final byte[] pathInBytes =
          ("/" + KRAKEN_API_VERSION + KRAKEN_PRIVATE_PATH + apiMethod)
              .getBytes(StandardCharsets.UTF_8);
      final String noncePrependedToPostData = Long.toString(nonce) + postData;

      // Create sha256 hash of nonce and post data:  创建随机数的 sha256 哈希并发布数据：
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(noncePrependedToPostData.getBytes(StandardCharsets.UTF_8));
      final byte[] messageHash = md.digest();

      // Create hmac_sha512 digest of path and previous sha256 hash  // 创建路径的 hmac_sha512 摘要和之前的 sha256 哈希
      mac.reset(); // force reset
      mac.update(pathInBytes);
      mac.update(messageHash);

      // Signature in Base64  // Base64 中的签名
      final String signature = Base64.getEncoder().encodeToString(mac.doFinal());

      // Request headers required by Exchange  // Exchange 所需的请求标头
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
      requestHeaders.put("API-Key", key);
      requestHeaders.put("API-Sign", signature);

      final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
      return makeNetworkRequest(url, "POST", postData.toString(), requestHeaders);

    } catch (MalformedURLException | NoSuchAlgorithmException e) {
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
      // Kraken secret key is in Base64, so we need to decode it first
      // Kraken 密钥在 Base64 中，所以我们需要先对其进行解码
      final byte[] base64DecodedSecret = Base64.getDecoder().decode(secret);

      final SecretKeySpec keyspec = new SecretKeySpec(base64DecodedSecret, "HmacSHA512");
      mac = Mac.getInstance("HmacSHA512");
      mac.init(keyspec);
      initializedMacAuthentication = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to setup MAC security. HINT: Is HmacSHA512 installed? 设置 MAC 安全性失败。提示：是否安装了 HmacSHA512？";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = " Failed to setup MAC security. Secret key seems invalid! 设置 MAC 安全性失败。密钥似乎无效！";
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods  // 配置方法
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
    LOG.info(() -> "Buy fee % in BigDecimal format: BigDecimal 格式的购买费用百分比：" + buyFeePercentage);

    final String sellFeeInConfig = getOtherConfigItem(otherConfig, SELL_FEE_PROPERTY_NAME);
    sellFeePercentage =
        new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    LOG.info(() -> "Sell fee % in BigDecimal format: BigDecimal 格式的销售费用百分比：" + sellFeePercentage);

    final String keepAliveDuringMaintenanceConfig =
        getOtherConfigItem(otherConfig, KEEP_ALIVE_DURING_MAINTENANCE_PROPERTY_NAME);
    if (!keepAliveDuringMaintenanceConfig.isEmpty()) {
      keepAliveDuringMaintenance = Boolean.valueOf(keepAliveDuringMaintenanceConfig);
      LOG.info(() -> "Keep Alive During Maintenance: 维护期间保持活力：" + keepAliveDuringMaintenance);
    } else {
      LOG.info(() -> KEEP_ALIVE_DURING_MAINTENANCE_PROPERTY_NAME + " is not set in exchange.yaml 未在 exchange.yaml 中设置");
    }
  }

  private void loadPairPrecisionConfig() {
    ExchangeHttpResponse response;

    try {
      response = sendPublicRequestToExchange("AssetPairs", emptyMap());

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        Type type = new TypeToken<KrakenResponse<KrakenAssetPairsConfig>>() {}.getType();
        KrakenResponse<KrakenAssetPairsConfig> krakenResponse = gson.fromJson(
            response.getPayload(), type);

        if (krakenResponse.error != null && !krakenResponse.error.isEmpty()) {
          LOG.error(
              () -> String.format("Error when fetching pair precision: %s 获取对精度时出错：%s", krakenResponse.error));
          return;
        }

        this.pairPrecisionConfig = krakenResponse.result.loadPrecisionConfig();
      }
    } catch (ExchangeNetworkException | TradingApiException e) {
      final String errorMsg = "Failed to load price precision config 无法加载价格精度配置";
      LOG.error(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Util methods  // 实用方法
  // --------------------------------------------------------------------------

  private List<OpenOrder> adaptKrakenOpenOrders(KrakenResponse krakenResponse, String marketId)
      throws TradingApiException {
    final List<OpenOrder> openOrders = new ArrayList<>();

    // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
    // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
    final KrakenOpenOrderResult krakenOpenOrderResult =
        (KrakenOpenOrderResult) krakenResponse.result;

    final Map<String, KrakenOpenOrder> krakenOpenOrders = krakenOpenOrderResult.open;
    if (krakenOpenOrders != null) {
      for (final Map.Entry<String, KrakenOpenOrder> openOrder : krakenOpenOrders.entrySet()) {

        OrderType orderType;
        final KrakenOpenOrder krakenOpenOrder = openOrder.getValue();
        final KrakenOpenOrderDescription krakenOpenOrderDescription = krakenOpenOrder.descr;

        if (!marketId.equalsIgnoreCase(krakenOpenOrderDescription.pair)) {
          continue;
        }

        switch (krakenOpenOrderDescription.type) {
          case "buy":
            orderType = OrderType.BUY;
            break;
          case "sell":
            orderType = OrderType.SELL;
            break;
          default:
            throw new TradingApiException(
                "Unrecognised order type received in getYourOpenOrders(). Value: 在 getYourOpenOrders() 中收到无法识别的订单类型。价值："
                    + openOrder.getValue().descr.ordertype);
        }

        final OpenOrder order =
            new OpenOrderImpl(
                openOrder.getKey(),
                new Date((long) krakenOpenOrder.opentm), // opentm == creationDate  opentm == 创建日期
                marketId,
                orderType,
                krakenOpenOrderDescription.price,
                // vol_exec == amount of order that has been executed
                // vol_exec == 已执行的订单数量
                (krakenOpenOrder.vol.subtract(krakenOpenOrder.volExec)),
                krakenOpenOrder.vol, // vol == orig order amount  // vol == 原始订单金额
                // krakenOpenOrder.cost, // cost == total value of order in API docs, but it's always 0 :-(
                // krakenOpenOrder.cost, // 成本 == API 文档中订单的总价值，但始终为 0 :-(
                krakenOpenOrderDescription.price.multiply(krakenOpenOrder.vol));

        openOrders.add(order);
      }
    }
    return openOrders;
  }

  private MarketOrderBookImpl adaptKrakenOrderBook(KrakenResponse krakenResponse, String marketId)
      throws TradingApiException {

    // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
    // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
    final KrakenMarketOrderBookResult krakenOrderBookResult =
        (KrakenMarketOrderBookResult) krakenResponse.result;
    final Optional<KrakenOrderBook> first = krakenOrderBookResult.values().stream().findFirst();
    if (first.isPresent()) {
      final KrakenOrderBook krakenOrderBook = first.get();

      final List<MarketOrder> buyOrders = new ArrayList<>();
      for (KrakenMarketOrder krakenBuyOrder : krakenOrderBook.bids) {
        final MarketOrder buyOrder =
            new MarketOrderImpl(
                OrderType.BUY,
                krakenBuyOrder.get(0),
                krakenBuyOrder.get(1),
                krakenBuyOrder.get(0).multiply(krakenBuyOrder.get(1)));
        buyOrders.add(buyOrder);
      }

      final List<MarketOrder> sellOrders = new ArrayList<>();
      for (KrakenMarketOrder krakenSellOrder : krakenOrderBook.asks) {
        final MarketOrder sellOrder =
            new MarketOrderImpl(
                OrderType.SELL,
                krakenSellOrder.get(0),
                krakenSellOrder.get(1),
                krakenSellOrder.get(0).multiply(krakenSellOrder.get(1)));
        sellOrders.add(sellOrder);
      }
      return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);
    } else {
      final String errorMsg = FAILED_TO_GET_MARKET_ORDERS + krakenResponse;
      LOG.error(errorMsg);
      throw new TradingApiException(errorMsg);
    }
  }

  private boolean adaptKrakenCancelOrderResult(KrakenResponse krakenResponse) {
    // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
    // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
    final KrakenCancelOrderResult krakenCancelOrderResult =
        (KrakenCancelOrderResult) krakenResponse.result;
    if (krakenCancelOrderResult != null) {
      if (krakenCancelOrderResult.count > 0) {
        return true;
      } else {
        final String errorMsg = FAILED_TO_CANCEL_ORDER + krakenResponse;
        LOG.error(errorMsg);
        return false;
      }
    } else {
      final String errorMsg = FAILED_TO_CANCEL_ORDER + krakenResponse;
      LOG.error(errorMsg);
      return false;
    }
  }

  private BalanceInfoImpl adaptKrakenBalanceInfo(ExchangeHttpResponse response, Type resultType)
      throws ExchangeNetworkException, TradingApiException {
    final KrakenResponse krakenResponse = gson.fromJson(response.getPayload(), resultType);
    if (krakenResponse != null) {
      final List errors = krakenResponse.error;
      if (errors == null || errors.isEmpty()) {
        // Assume we'll always get something here if errors array is empty; else blow fast wih NPE
        // 假设如果错误数组为空，我们总是会在这里得到一些东西；否则用 NPE 吹得很快
        final KrakenBalanceResult balanceResult = (KrakenBalanceResult) krakenResponse.result;
        final Map<String, BigDecimal> balancesAvailable = new HashMap<>();
        final Set<Map.Entry<String, BigDecimal>> entries = balanceResult.entrySet();
        for (final Map.Entry<String, BigDecimal> entry : entries) {
          balancesAvailable.put(entry.getKey(), entry.getValue());
        }

        // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by exchange.
        // 交易所不提供用于保留/保留余额的 BalanceInfo 构造函数的第二个参数。
        return new BalanceInfoImpl(balancesAvailable, new HashMap<>());

      } else {
        if (isExchangeUndergoingMaintenance(response) && keepAliveDuringMaintenance) {
          LOG.warn(() -> UNDER_MAINTENANCE_WARNING_MESSAGE);
          throw new ExchangeNetworkException(UNDER_MAINTENANCE_WARNING_MESSAGE);
        }

        final String errorMsg = FAILED_TO_GET_BALANCE + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }
    } else {
      final String errorMsg = FAILED_TO_GET_BALANCE + response;
      LOG.error(errorMsg);
      throw new TradingApiException(errorMsg);
    }
  }

  private void initGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(KrakenTickerResult.class, new KrakenTickerResultDeserializer());
    gson = gsonBuilder.create();
  }

  private static boolean isExchangeUndergoingMaintenance(ExchangeHttpResponse response) {
    if (response != null) {
      final String payload = response.getPayload();
      return payload != null && payload.contains(EXCHANGE_UNDERGOING_MAINTENANCE_RESPONSE);
    }
    return false;
  }

  /**
   * Hack for unit-testing map params passed to transport layer.
   * * Hack 用于传递给传输层的单元测试地图参数。
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
   * * 用于单元测试传输层的 Hack。
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
