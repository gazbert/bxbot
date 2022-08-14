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
 * Exchange Adapter for integrating with the Bitfinex exchange. The Bitfinex API is documented <a
 * href="https://www.bitfinex.com/pages/api">here</a>.
 * 用于与 Bitfinex 交换集成的交换适配器。 Bitfinex API 记录在案 <a
 *  * href="https://www.bitfinex.com/pages/api">这里</a>。
 *
 * <p><strong> DISCLAIMER: This Exchange Adapter is provided as-is; it might have bugs in it and you
could lose money. Despite running live on Bitfinex, it has only been unit tested up until the
point of calling the {@link #sendPublicRequestToExchange(String)} and {@link
#sendAuthenticatedRequestToExchange(String, Map)} methods. Use it at our own risk!</strong>
 <p><strong> 免责声明：此交换适配器按原样提供；它可能有错误，你
 可能会赔钱。尽管在 Bitfinex 上实时运行，但它只进行了单元测试，直到
 调用 {@link #sendPublicRequestToExchange(String)} 和 {@link
#sendAuthenticatedRequestToExchange(String, Map)} 方法。使用它需要我们自担风险！</strong>
 *
 * <p>The adapter uses v1 of the Bitfinex API - it is limited to 60 API calls per minute. It only
supports 'exchange' accounts; it does <em>not</em> support 'trading' (margin trading) accounts or
'deposit' (liquidity SWAPs) accounts. Furthermore, the adapter does not support sending 'hidden'
orders.
 <p>适配器使用 Bitfinex API v1 - 每分钟限制为 60 个 API 调用。它只是
 支持“交换”账户；它<em>不</em>支持“交易”（保证金交易）账户或
 “存款”（流动性 SWAP）账户。此外，适配器不支持发送“隐藏”
 订单。
 *
 * <p>There are different exchange fees for Takers and Makers - see <a
  href="https://www.bitfinex.com/pages/fees">here.</a> This adapter will use the <em>Taker</em>
   fees to keep things simple for now.
 <p>Taker 和 Maker 有不同的交易费用 - 请参阅 <a
 href="https://www.bitfinex.com/pages/fees">这里。</a> 这个适配器将使用 <em>Taker</em>
 暂时保持简单的费用。

 * <p>The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single
 thread in order to preserve trade execution order. The {@link URLConnection} achieves this by
 blocking/waiting on the input stream (response) for each API call.
 <p>交换适配器<em>不是</em>线程安全的。它期望使用单个调用
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
 * @author gazbert
 * @since 1.0
 */
public final class BitfinexExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String BITFINEX_API_VERSION = "v1";
  private static final String PUBLIC_API_BASE_URL =
      "https://api.bitfinex.com/" + BITFINEX_API_VERSION + "/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in Bitfinex Exchange Adapter. Bitfinex 交换适配器发生意外错误。";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error. 由于意外的 IO 错误，无法连接到 Exchange。";

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

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise Bitfinex ExchangeConfig:  即将初始化 Bitfinex ExchangeConfig：" + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);

    nonce = System.currentTimeMillis() / 1000;
    initSecureMessageLayer();
    initGson();
  }

  // --------------------------------------------------------------------------
  // Bitfinex API Calls adapted to the Trading API.
//    适用于交易 API 的 Bitfinex API 调用。

  // See https://www.bitfinex.com/pages/api
  // --------------------------------------------------------------------------

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("book/" + marketId);
      LOG.debug(() -> "Market Orders response: 市价单响应：" + response);

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
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("orders", null);
      LOG.debug(() -> "Open Orders response:  未结订单响应：" + response);

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
                "Unrecognised order type received in getYourOpenOrders(). Value: 在 getYourOpenOrders() 中收到无法识别的订单类型。价值："
                    + bitfinexOpenOrder.type);
        }

        final OpenOrder order =
            new OpenOrderImpl(
                Long.toString(bitfinexOpenOrder.id),
                // for some reason 'finex adds decimal point to long date value, e.g. "1442073766.0" // 出于某种原因 'finex 将小数点添加到长日期值，例如“1442073766.0”
                //  - grrrr! - 咕咕咕
                Date.from(
                    Instant.ofEpochMilli(
                        Integer.parseInt(bitfinexOpenOrder.timestamp.split("\\.")[0]))),
                marketId,
                orderType,
                bitfinexOpenOrder.price,
                bitfinexOpenOrder.remainingAmount,
                bitfinexOpenOrder.originalAmount,
                bitfinexOpenOrder.price.multiply(
                    bitfinexOpenOrder.originalAmount) // total - not provided by finex :-( 总计 - Finex 不提供 :-(
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
      final Map<String, Object> params = createRequestParamMap();

      params.put(SYMBOL, marketId);

      // note we need to limit amount and price to 8 decimal places else exchange will barf // 请注意，我们需要将金额和价格限制为小数点后 8 位，否则交易所将失败
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
        LOG.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      // 'type' is either 或者是 "market" / "limit" / "stop" / "trailing-stop" / "fill-or-kill" / "exchange
      // market" /
      // "exchange limit" / "exchange stop" / "exchange trailing-stop" / "exchange fill-or-kill".
      // (type starting by "exchange " are exchange orders, others are margin trading orders)
//      （以“exchange”开头的类型为交易所订单，其他为保证金交易订单）

      // this adapter only supports 'exchange limit orders'// 这个适配器只支持'exchange limit orders'
      params.put("type", "exchange limit");

      // This adapter does not currently support hidden orders. 此适配器当前不支持隐藏订单。
      // Exchange API notes: "true if the order should be hidden. Default is false." // Exchange API 注释：“如果订单应该被隐藏，则为 true。默认为 false。”
      // If you try and set "is_hidden" to false, the exchange barfs and sends a 401 back. Nice. // 如果您尝试将“is_hidden”设置为 false，则交易所会拒绝并返回 401。好的。
      // params.put("is_hidden", "false");

      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("order/new", params);
      LOG.debug(() -> "Create Order response: 创建订单响应：" + response);

      final BitfinexNewOrderResponse createOrderResponse =
          gson.fromJson(response.getPayload(), BitfinexNewOrderResponse.class);
      final long id = createOrderResponse.orderId;
      if (id == 0) {
        final String errorMsg = "Failed to place order on exchange. Error response: 未能在交易所下订单。错误响应：" + response;
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

  /**
   * 取消此交易所的订单不需要 marketId。
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
      LOG.debug(() -> "Cancel Order response: 取消订单响应：" + response);

      // Exchange returns order id and other details if successful, a 400 HTTP Status if the order id was not recognised.
      // 如果成功，Exchange 返回订单 ID 和其他详细信息，如果订单 ID 未被识别，则返回 400 HTTP 状态。
      gson.fromJson(response.getPayload(), BitfinexCancelOrderResponse.class);
      return true;

    } catch (ExchangeNetworkException | TradingApiException e) {
      if (e.getCause() != null && e.getCause().getMessage().contains("400")) {
        final String errorMsg =
            "Failed to cancel order on exchange. Did not recognise Order Id: 交易所取消订单失败。无法识别订单 ID：" + orderId;
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
  public BigDecimal getLatestMarketPrice(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("pubticker/" + marketId);
      LOG.debug(() -> "Latest Market Price response: 最新市价回应：" + response);

      final BitfinexTicker ticker = gson.fromJson(response.getPayload(), BitfinexTicker.class);
      return ticker.lastPrice;

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
      LOG.debug(() -> "Balance Info response: 余额信息回复：" + response);

      final BitfinexBalances allAccountBalances =
          gson.fromJson(response.getPayload(), BitfinexBalances.class);
      final HashMap<String, BigDecimal> balancesAvailable = new HashMap<>();

      /**
       * The adapter only fetches the 'exchange' account balance details - this is the Bitfinex 'exchange' account, i.e. the limit order trading account balance.
       * * 适配器仅获取“交易所”账户余额详情——这是 Bitfinex 的“交易所”账户，即限价单交易账户余额。
       */
      /*TODO 此处为真逻辑*/
//      if (allAccountBalances != null) {
//        allAccountBalances.stream()
//            .filter(accountBalance -> accountBalance.type.equalsIgnoreCase(EXCHANGE))
//            .forEach(
//                accountBalance -> {
//                  if (accountBalance.currency.equalsIgnoreCase("usd")) {
//                    balancesAvailable.put("USD", accountBalance.available);
//                  } else if (accountBalance.currency.equalsIgnoreCase("btc")) {
//                    balancesAvailable.put("BTC", accountBalance.available);
//                  }
//                });
//      }
      /*TODO 此处为模拟账号测试逻辑*/
      if (allAccountBalances != null) {
        allAccountBalances.stream()
            .filter(accountBalance -> accountBalance.type.equalsIgnoreCase(EXCHANGE))
            .forEach(
                accountBalance -> {
                  if (accountBalance.currency.equalsIgnoreCase("AAA")) {
                    balancesAvailable.put("TESTAAA", accountBalance.available);
                  } else if (accountBalance.currency.equalsIgnoreCase("BBB")) {
                    balancesAvailable.put("TESTBBB", accountBalance.available);
                  } else if (accountBalance.currency.equalsIgnoreCase("TESTBTC")) {
                    balancesAvailable.put("TESTBTC", accountBalance.available);
                  }else if (accountBalance.currency.equalsIgnoreCase("TESTUSD")) {
                    balancesAvailable.put("TESTUSD", accountBalance.available);
                  }else if (accountBalance.currency.equalsIgnoreCase("TESTUSDT")) {
                    balancesAvailable.put("TESTUSDT", accountBalance.available);
                  }
                });
      }

      // 2nd arg of BalanceInfo constructor for reserved/on-hold balances is not provided by
      // exchange. 交易所不提供用于保留/保留余额的余额信息构造函数的第二个参数。
      return new BalanceInfoImpl(balancesAvailable, new HashMap<>());

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
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("account_infos", null);
      LOG.debug(() -> "Buy Fee response: 购买费用回复：" + response);

      // Nightmare to adapt! Just take the top-level taker fees. //噩梦适应！只需收取顶级接受者费用。
      final BitfinexAccountInfos bitfinexAccountInfos =
          gson.fromJson(response.getPayload(), BitfinexAccountInfos.class);
      final BigDecimal fee = bitfinexAccountInfos.get(0).takerFees;

      // adapt the % into BigDecimal format // 将 % 调整为 BigDecimal 格式
      return fee.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);

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
      final ExchangeHttpResponse response =
          sendAuthenticatedRequestToExchange("account_infos", null);
      LOG.debug(() -> "Sell Fee response: 销售费用回复：" + response);

      // Nightmare to adapt! Just take the top-level taker fees.
      //噩梦适应！只需收取顶级接受者费用。
      final BitfinexAccountInfos bitfinexAccountInfos =
          gson.fromJson(response.getPayload(), BitfinexAccountInfos.class);
      final BigDecimal fee = bitfinexAccountInfos.get(0).takerFees;

      // adapt the % into BigDecimal format // 将 % 调整为 BigDecimal 格式
      return fee.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);

    } catch (ExchangeNetworkException | TradingApiException e) {
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

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("pubticker/" + marketId);
      LOG.debug(() -> "Latest Market Price response: 最新市价回应：" + response);

      final BitfinexTicker ticker = gson.fromJson(response.getPayload(), BitfinexTicker.class);
      return new TickerImpl(
          ticker.lastPrice,
          ticker.bid,
          ticker.ask,
          ticker.low,
          ticker.high,
          null, // open not supplied by Bitfinex // 打开不是由 Bitfinex 提供的
          ticker.volume,
          null, // vwap not supplied by Bitfinex Bitfinex 不提供 vwap
          // for some reason 'finex adds decimal point to long date value, e.g. "1513631756.0798516" 出于某种原因，'finex 将小数点添加到长日期值，例如“1513631756.0798516”
          //  - grrrr!
          Date.from(Instant.ofEpochMilli(Integer.parseInt(ticker.timestamp.split("\\.")[0])))
              .getTime());

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses. JSON 响应的 GSON 类。
  //  See https://www.bitfinex.com/pages/api
  // --------------------------------------------------------------------------

  /** GSON class for a market Order Book. 市场订单簿的 GSON 类。*/
  private static class BitfinexOrderBook {

    BitfinexMarketOrder[] bids;
    BitfinexMarketOrder[] asks;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("bids", bids).add("asks", asks).toString();
    }
  }

  /** GSON class for a Market Order.
   * 市价单的 GSON 类。 */
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

  /** GSON class for receiving your open orders in 'orders' API call response.
   *  GSON 类，用于在“订单”API 调用响应中接收您的未结订单。*/
  private static class BitfinexOpenOrders extends ArrayList<BitfinexOpenOrder> {
    private static final long serialVersionUID = 5516523641153401953L;
  }

  /** GSON class for mapping returned order from 'orders' API call response.
   * 用于映射从“订单”API 调用响应返回的订单的 GSON 类。 */
  private static class BitfinexOpenOrder {

    long id;
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // e.g. "sell" 例如“卖”
    String type; // e.g. "exchange limit" 例如“兑换限制”
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

  /** GSON class for a Bitfinex 'pubticker' API call response.  Bitfinex 'pubticker'
   * API 调用响应的 GSON 类。*/
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
   *  用于保存来自“account_infos”API 调用的 Bitfinex 响应的 GSON 类。
   *
   * <p>This is a lot of work to just get the exchange fees!
   *  <p>这只是为了获得交换费用而做的很多工作！
   *
   * <p>We want the taker fees.
   * <p>我们想要接受者费用。
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
    private static final long serialVersionUID = 5516521641453401953L;
  }

  /** GSON class for holding Bitfinex Account Info.  用于保存 Bitfinex 帐户信息的 GSON 类。*/
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

  /** GSON class for holding Bitfinex Pair Fees. 用于持有 Bitfinex 对费用的 GSON 类。*/
  private static class BitfinexPairFees extends ArrayList<BitfinexPairFee> {
    private static final long serialVersionUID = 1516526641473401953L;
  }

  /** GSON class for holding Bitfinex Pair Fee.持有 Bitfinex 对费的 GSON 类。 */
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
   *  用于保存来自“余额”API 调用的 Bitfinex 响应的 GSON 类。
   *
   * <p>Basically an array of BitfinexAccountBalance types.
   *  <p>基本上是 BitfinexAccountBalance 类型的数组。
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
    private static final long serialVersionUID = 5516523641953401953L;
  }

  /**
   * GSON class for holding a Bitfinex account type balance info.
   *  用于保存 Bitfinex 账户类型余额信息的 GSON 类。
   *
   * <p>There are 3 types of account: 'deposit' (swaps), 'exchange' (limit orders), 'trading'
   (margin).
   *<p>有 3 种账户类型：“存款”（掉期）、“交易所”（限价订单）、“交易”
   *    （利润）。
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

  /** GSON class for Bitfinex 'order/new' response.
   * Bitfinex“订单/新”响应的 GSON 类。
   * */
  private static class BitfinexNewOrderResponse {

    long id; // same as order_id  与 order_id 相同
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // e.g. "sell" 例如“卖”
    String type; // e.g. "exchange limit"  例如“兑换限制”
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
    long orderId; // same as id 和id一样

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

  /**
   * GSON class for Bitfinex 'order/cancel' response.
   * * Bitfinex“订单/取消”响应的 GSON 类。
   * */
  private static class BitfinexCancelOrderResponse {

    long id; // only get this param; there is no order_id 只得到这个参数；没有order_id
    String symbol;
    String exchange;
    BigDecimal price;

    @SerializedName("avg_execution_price")
    BigDecimal avgExecutionPrice;

    String side; // e.g. "sell" 例如“卖”
    String type; // e.g. "exchange limit" 例如“兑换限制”
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
  //  Transport layer methods  // 传输层方法
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

  /**
   * Makes an authenticated API call to the Bitfinex exchange.
   *  对 Bitfinex 交易所进行经过身份验证的 API 调用。
   *
   * Bitfinex Example:
   * Bitfinex 示例：
   *
   * POST https://api.bitfinex.com/v1/order/new
   *
   * With JSON payload of:
   * 使用 JSON 有效负载：
   * {
   *    "request": "/v1/<request-type>
   *    "nonce": "1234",
   *    "other-params : "for the request if any..."
   * }
   *
   * To authenticate a request, we must calculate the following:
   * 要对请求进行身份验证，我们必须计算以下内容：
   *
   * payload = request-parameters-dictionary -> JSON encode -> base64
   * 有效负载 = 请求参数字典 -> JSON 编码 -> base64
   *
   * signature = HMAC-SHA384(payload, api-secret) as hexadecimal in lowercase (MUST be lowercase)
       send (api-key, payload, signature)
   signature = HMAC-SHA384(payload, api-secret) 小写十六进制（必须小写）
   发送（api-key、payload、签名）
   *
   * These are sent as HTTP headers named:
   * 这些作为 HTTP 标头发送，命名为：
   *
   * X-BFX-APIKEY
   * X-BFX-PAYLOAD
   * X-BFX-SIGNATURE
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
      String apiMethod, Map<String, Object> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedMacAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized. MAC 消息安全层尚未初始化。";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        // create empty map for non param API calls, e.g. "balances"
        // 为非参数 API 调用创建空映射，例如“余额”
        params = createRequestParamMap();
      }

      // nonce is required by Bitfinex in every request
      // Bitfinex 在每个请求中都需要随机数
      params.put("nonce", Long.toString(nonce));
      nonce++; // increment ready for next call. // 为下一次调用做准备。

      // must include the method in request param too
      // 也必须在请求参数中包含方法
      params.put("request", "/" + BITFINEX_API_VERSION + "/" + apiMethod);

      // JSON-ify the param dictionary
      // JSON-ify 参数字典
      final String paramsInJson = gson.toJson(params);

      // Need to base64 encode payload as per API
      // 需要根据 API 对有效负载进行 base64 编码
      final String base64payload =
          DatatypeConverter.printBase64Binary(paramsInJson.getBytes(StandardCharsets.UTF_8));

      // Request headers required by Exchange
      // Exchange 所需的请求标头
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("X-BFX-APIKEY", key);
      requestHeaders.put("X-BFX-PAYLOAD", base64payload);

      // Add the signature  // 添加签名
      mac.reset(); // force reset  // 强制重置
      mac.update(base64payload.getBytes(StandardCharsets.UTF_8));

      /**
       * signature = HMAC-SHA384(payload, api-secret) as hexadecimal - MUST be in LOWERCASE else signature fails. See:
       * * signature = HMAC-SHA384(payload, api-secret) as hexadecimal - 必须小写，否则签名失败。看：
       * http://bitcoin.stackexchange.com/questions/25835/bitfinex-api-call-returns-400-bad-request
       */
      final String signature = toHex(mac.doFinal()).toLowerCase();
      requestHeaders.put("X-BFX-SIGNATURE", signature);

      // payload is JSON for this exchange
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

  /**
   * Initialises the secure messaging layer.
   *  初始化安全消息层。
   *
   * Sets up the MAC to safeguard the data we send to the exchange.
   *  设置 MAC 以保护我们发送到交易所的数据。
   *
   * Used to encrypt the hash of the entire message with the private key to ensure message
   integrity. We fail hard n fast if any of this stuff blows.
   用于用私钥对整条消息的哈希进行加密，以保证消息
   正直。如果这些东西中的任何一个发生爆炸，我们就会很快失败。
   */
  private void initSecureMessageLayer() {
    try {
      final SecretKeySpec keyspec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA384");
      mac = Mac.getInstance("HmacSHA384");
      mac.init(keyspec);
      initializedMacAuthentication = true;
    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA384 installed? 设置 MAC 安全性失败。提示：是否安装了 HMAC-SHA384？";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = "Failed to setup MAC security. Secret key seems invalid! 设置 MAC 安全性失败。密钥似乎无效！";
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods 配置方法
  // --------------------------------------------------------------------------

  private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
    key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
    secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
  }

  // --------------------------------------------------------------------------
  //  Util methods 实用方法
  // --------------------------------------------------------------------------

  private void initGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gson = gsonBuilder.create();
  }

  /**
   * Hack for unit-testing map params passed to transport layer.
   * * Hack 用于传递给传输层的单元测试地图参数。
   */
  private Map<String, Object> createRequestParamMap() {
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
   * 用于单元测试传输层的 Hack。
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
