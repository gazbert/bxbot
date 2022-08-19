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

package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * This is a very simple
 * 这是一个非常简单的
 * <a href="http://www.investopedia.com/articles/trading/02/081902.asp">scalping strategy 剥头皮策略</a>
 * to show how to use the Trading API; you will want to code a much better algorithm! It trades using
 * * 展示如何使用交易 API；你会想要编写一个更好的算法！它使用
 * <a
 * href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> at the 在<a
 * href="http://www.investopedia.com/terms/s/spotprice.asp">spot price 现货价格</a>.
 *
 * <p><strong> DISCLAIMER: This algorithm is provided as-is; it might have bugs in it and you could
 * lose money. Use it at our own risk! </strong>
 * <p><strong> 免责声明：此算法按原样提供；它可能有错误，你可以
 *  * 赔钱。使用它需要我们自担风险！ </strong>
 *
 * <p>It was originally written to trade on <a href="https://btc-e.com">BTC-e</a>, but should work
  for any exchange.
 <p>它最初是为在 <a href="https://btc-e.com">BTC-e</a> 上交易而编写的，但应该可以使用任何交换。

 The algorithm will start by buying the base currency (BTC in this example)
  using the counter currency (USD in this example), and then sell the base currency (BTC) at a
  higher price to take profit from the spread.
 该算法将从购买基础货币开始（本例中为 BTC）
 使用相对货币（本例中为美元），然后以
 更高的价格从点差中获利。

 The algorithm expects you to have deposited sufficient counter currency (USD) into your exchange wallet in order to buy the base currency (BTC).
 该算法预计您已将足够的柜台货币 (USD) 存入您的兑换钱包，以便购买基础货币 (BTC)。
 *
 * <p>When it starts up, it places an order at the current BID price and uses x amount of counter
  currency (USD) to 'buy' the base currency (BTC).
 <p>当它启动时，它以当前 BID 价格下单并使用 x 数量的计数器
 货币 (USD) 以“购买”基础货币 (BTC)。

 * The value of x comes from the sample {project-root}/config/strategies.yaml 'counter-currency-buy-order-amount' config-item, currently
  set to 20 USD.
 x 的值来自示例 {project-root}/config/strategies.yaml 'counter-currency-buy-order-amount' config-item，目前
 设置为 20 美元。

  Make sure that the value you use for x is large enough to be able to meet the
  minimum BTC order size for the exchange you are trading on, e.g. the Bitfinex min order size is
  0.01 BTC as of 3 May 2017.
 确保用于 x 的值足够大，能够满足
 您交易的交易所的最小 BTC 订单大小，例如Bitfinex 最小订单规模为
 截至 2017 年 5 月 3 日为 0.01 比特币。

 * The algorithm then waits for the buy order to fill...
 * 然后该算法等待购买订单完成......
 *
 * <p>Once the buy order fills, it then waits until the ASK price is at least y % higher than the
  previous buy fill price.
 <p>只要买单交易，它就可以了，直到 ASK 价格必须完成 %
 的之前价格。

 The value of y comes from the sample {project-root}/config/strategies.yaml 'minimum-percentage-gain' config-item, currently set to 1%.
 y 的值来自示例 {project-root}/config/strategies.yaml 'minimum-percentage-gain' config-item，当前设置为 1%。

 * Once the % gain has been achieved, the algorithm will place a sell order at the current ASK price.
 * * 一旦获得 % 收益，算法将以当前 ASK 价格下达卖单。
 * It then waits for the sell order to fill... and the cycle repeats.
 * * 然后它等待卖单完成……然后循环重复。
 *
 * <p>The algorithm does not factor in being outbid when placing buy orders, i.e. it does not cancel
 * the current order and place a new order at a higher price;
 * <p>该算法在下达买单时不考虑出价过高，即它不会取消 当前订单并以更高的价格下新订单；
 *
 * it simply holds until the current BID price falls again. Likewise, the algorithm does not factor in being undercut when placing sell orders;
 * 它只是保持到当前的 BID 价格再次下跌。同样，该算法不会在下达卖单时考虑被削弱；
 *
 * it does not cancel the current order and place a new order at a lower price.
 * 它不会取消当前订单并以较低的价格下新订单。
 *
 * <p>Chances are you will either get a stuck buy order if the market is going up, or a stuck sell order if the market goes down.
 * * <p>如果市场上涨，您可能会收到卡住的买单，如果市场下跌，您可能会收到卡住的卖单。
 *
 * You could manually execute the trades on the exchange and restart the bot to get going again... but a much better solution would be to modify this code to deal
 with it: cancel your current buy order and place a new order matching the current BID price, or
 cancel your current sell order and place a new order matching the current ASK price. The {@link
  TradingApi} allows you to add this behaviour.
 * 您可以在交易所手动执行交易并重新启动机器人以重新开始......但更好的解决方案是修改此代码以进行交易
 使用它：取消您当前的买入订单并下一个与当前 BID 价格匹配的新订单，或
 取消您当前的卖单并下一个与当前 ASK 价格匹配的新订单。链接
 TradingApi} 允许您添加此行为。

 *
 * <p>Remember to include the correct exchange fees (both buy and sell) in your buy/sell
  calculations when you write your own algorithm. Otherwise, you'll end up bleeding fiat/crypto to
  the exchange...
 <p>请记住在您的买入/卖出中包含正确的交易费用（买入和卖出）
 编写自己的算法时的计算。否则，你最终会流血法币/加密货币
 找的零钱...
 *
 * <p>This demo algorithm relies on the {project-root}/config/strategies.yaml
 'minimum-percentage-gain' config-item value being high enough to make a profit and cover the
 exchange fees. You could tweak the algo to call the {@link com.gazbert.bxbot.trading.api.TradingApi#getPercentageOfBuyOrderTakenForExchangeFee(String)} and
 {@link  com.gazbert.bxbot.trading.api.TradingApi#getPercentageOfSellOrderTakenForExchangeFee(String)}
  when calculating the order to send to the exchange... See the sample
 {project-root}/config/samples/{exchange}/exchange.yaml files for info on the different exchange fees.
 <p>此演示算法依赖于 {project-root}/config/strategies.yaml
 'minimum-percentage-gain' 配置项值足够高以赚取利润并覆盖
 兑换费。您可以调整算法以调用 {@link com.gazbert.bxbot.trading.api.TradingApi#getPercentageOfBuyOrderTakenForExchangeFee(String)} 和
 {@link com.gazbert.bxbot.trading.api.TradingApi#getPercentageOfSellOrderTakenForExchangeFee(String)}
 在计算发送到交易所的订单时... 查看示例
 {project-root}/config/samples/{exchange}/exchange.yaml 文件以获取有关不同交换费用的信息。
 *
 * <p>You configure the loading of your strategy using either a className OR a beanName in the
  {project-root}/config/strategies.yaml config file. This example strategy is configured using the
  bean-name and by setting the @Component("exampleScalpingStrategy") annotation - this results in
  Spring injecting the bean - see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Component.html">
  Spring docs</a> for more details. Alternatively, you can load your strategy using className -
  this will use the bot's custom injection framework. The choice is yours, but beanName is the way
  to go if you want to use other Spring features in your strategy, e.g. a <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Repository.html">
  Repository</a> to store your trade data.
 <p>您可以使用 className 或 beanName 在
 {project-root}/config/strategies.yaml 配置文件。此示例策略使用
 bean-name 并通过设置 @Component("exampleScalpingStrategy") 注释 - 这会导致
 Spring 注入 bean - 参见 <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Component.html">
 Spring 文档</a> 了解更多详情。或者，您可以使用 className 加载您的策略 -
 这将使用机器人的自定义注入框架。选择是你的，但 beanName 是方式
 如果你想在你的策略中使用其他 Spring 特性，例如一个 <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Repository.html">
 存储库</a>来存储您的交易数据。
 *
 * <p>The algorithm relies on config from the sample {project-root}/config/strategies.yaml and
  {project-root}/config/markets.yaml files. You can pass additional configItems to your Strategy
 using the {project-root}/config/strategies.yaml file - you access it from the {@link
  #init(TradingApi, Market, StrategyConfig)} method via the StrategyConfigImpl argument.
 <p>您可以使用 className 或 bean 名称来配置策略的加载
 {project-root}/config/strategies.yaml 配置文件。此示例策略使用
 bean-name 并通过设置 @Component("exampleScalpingStrategy") 注释 - 这会导致
 Spring 注入 bean - 参见 <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Component.html">
 Spring 文档</a> 了解更多详情。或者，您可以使用 className 加载您的策略 -
 这将使用机器人的自定义注入框架。选择是你的，但 beanName 是方式
 如果你想在你的策略中使用其他 Spring 特性，例如一个 <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Repository.html">
 存储库</a>来存储您的交易数据。
 *
 * <p>This simple demo algorithm only manages 1 order at a time to keep things simple.
 * <p>这个简单的演示算法一次只管理 1 个订单以保持简单。
 *
 * <p>The Trading Engine will only send 1 thread through your strategy code at a time - you do not have to code for concurrency.
 * * <p>交易引擎一次只会通过您的策略代码发送 1 个线程 - 您不必为并发编写代码。
 *
 * <p>This 这<a href="http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp">site 地点</a>
 * might give you a few ideas - the {@link TradingApi} provides a basic Ticker that you might want to use. Check out the excellent [ta4j](https://github.com/ta4j/ta4j) project too.
 * * 可能会给您一些想法 - {@link TradingApi} 提供了您可能想要使用的基本代码。查看优秀的 [ta4j](https://github.com/ta4j/ta4j) 项目。
 *
 * <p>Good luck!
 * <p>祝你好运！
 *
 * @author gazbert
 */
@Component("exampleScalpingStrategy") // used to load the strategy using Spring bean injection  // 用于使用 Spring bean 注入加载策略
public class ExampleScalpingStrategy implements TradingStrategy {

  private static final Logger LOG = LogManager.getLogger();

  /** The decimal format for the logs.
   * 日志的十进制格式。*/
  private static final String DECIMAL_FORMAT = "#.########";

  /** Reference to the main Trading API.
   * 参考主要交易 API。 */
  private TradingApi tradingApi;

  /** The market this strategy is trading on.
   * 此策略交易的市场。 */
  private Market market;

  /** The state of the order.
   * 订单的状态。 */
  private OrderState lastOrder;

  /**
   * The counter currency amount to use when placing the buy order. This was loaded from the strategy entry in the {project-root}/config/strategies.yaml config file.
   * * 下订单时使用的柜台货币金额。这是从 {project-root}/config/strategies.yaml 配置文件中的策略条目加载的。
   */
  private BigDecimal counterCurrencyBuyOrderAmount;

  /**
   * The minimum % gain was to achieve before placing a SELL oder. This was loaded from the strategy entry in the {project-root}/config/strategies.yaml config file.
   * * 最低 % 收益是在下单前达到的。这是从 {project-root}/config/strategies.yaml 配置文件中的策略条目加载的。
   */
  private BigDecimal minimumPercentageGain;

  /**
   * Initialises the Trading Strategy. Called once by the Trading Engine when the bot starts up;
   * * 初始化交易策略。机器人启动时由交易引擎调用一次；
   *
   * it's a bit like a servlet init() method.
   * 它有点像 servlet init() 方法。
   *
   * @param tradingApi the Trading API. Use this to make trades and stuff.  交易 API。用它来做交易和东西。
   * @param market the market for this strategy. This is the market the strategy is currently   running on - you wire this up in the markets.yaml and strategies.yaml files.
   *               这种策略的市场。这是该策略当前正在运行的市场 - 您可以在markets.yaml 和strategy.yaml 文件中将其连接起来。
   *
   * @param config configuration for the strategy. Contains any (optional) config you set up in the  strategies.yaml file.
   *               策略的配置。包含您在 strategy.YAML 文件中设置的任何（可选）配置。
   */
  @Override
  public void init(TradingApi tradingApi, Market market, StrategyConfig config) {
    LOG.info(() -> "Initialising Trading Strategy... 正在初始化交易策略...");
    this.tradingApi = tradingApi;
    this.market = market;
    getConfigForStrategy(config);
    LOG.info(() -> "Trading Strategy initialised successfully! 交易策略初始化成功！");
  }

  /**
   * This is the main execution method of the Trading Strategy. It is where your algorithm lives.
   * * 这是交易策略的主要执行方式。这是您的算法所在的地方。
   *
   * <p>It is called by the Trading Engine during each trade cycle, e.g. every 60s. The trade cycle is configured in the {project-root}/config/engine.yaml file.
   * * <p>它在每个交易周期被交易引擎调用，例如每 60 年代。交易周期在 {project-root}/config/engine.yaml 文件中配置。
   *
   * @throws StrategyException if something unexpected occurs. This tells the Trading Engine to  shutdown the bot immediately to help prevent unexpected losses.
   * 如果发生意外情况，@throws StrategyException。这告诉交易引擎立即关闭机器人以帮助防止意外损失。
   */
  @Override
  public void execute() throws StrategyException {
    LOG.info(() -> market.getName() + " Checking order status... 正在检查订单状态...");

    try {
      // Grab the latest order book for the market. 获取市场的最新订单。
      final MarketOrderBook orderBook = tradingApi.getMarketOrders(market.getId());

      final List<MarketOrder> buyOrders = orderBook.getBuyOrders();
      if (buyOrders.isEmpty()) {
        LOG.warn(
            () ->
                "Exchange returned empty Buy Orders. Ignoring this trade window. OrderBook: 交易所返回空的买单。忽略这个交易窗口。订单簿："
                    + orderBook);
        return;
      }

      final List<MarketOrder> sellOrders = orderBook.getSellOrders();
      if (sellOrders.isEmpty()) {
        LOG.warn(
            () ->
                "Exchange returned empty Sell Orders. Ignoring this trade window. OrderBook: 交易所返回空卖单。忽略这个交易窗口。订单簿："
                    + orderBook);
        return;
      }

      // Get the current BID and ASK spot prices. 获取当前的 BID 和 ASK 现货价格。
      final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
      final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();

      LOG.info(
          () ->
              market.getName()
                  + " Current BID price= 当前投标价格="
                  + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice));
      LOG.info(
          () ->
              market.getName()
                  + " Current ASK price= 当前要价="
                  + new DecimalFormat(DECIMAL_FORMAT).format(currentAskPrice));

      // Is this the first time the Strategy has been called? If yes, we initialise the OrderState so we can keep track of orders during later trace cycles.
      // 这是第一次调用策略吗？如果是，我们初始化 OrderState，以便我们可以在以后的跟踪周期中跟踪订单。
      if (lastOrder == null) {
        LOG.info(
            () ->
                market.getName()
                    + " First time Strategy has been called - creating new OrderState object. 第一次调用 Strategy - 创建新的 OrderState 对象。");
        lastOrder = new OrderState();
      }

      // Always handy to log what the last order was during each trace cycle.
      LOG.info(() -> market.getName() + " Last Order was: 最后的订单是：" + lastOrder);

      // Execute the appropriate algorithm based on the last order type.
      //根据最后的订单类型执行适当的算法。
      if (lastOrder.type == OrderType.BUY) {
        executeAlgoForWhenLastOrderWasBuy();

      } else if (lastOrder.type == OrderType.SELL) {
        executeAlgoForWhenLastOrderWasSell(currentBidPrice, currentAskPrice);

      } else if (lastOrder.type == null) {
        executeAlgoForWhenLastOrderWasNone(currentBidPrice);
      }

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here.
      //您的超时处理代码可以放在这里。
      // We are just going to log it and swallow it, and wait for next trade cycle.
      // 我们只是要记录它并吞下它，然后等待下一个交易周期。
      LOG.error(
          () ->
              market.getName()
                  + " Failed to get market orders because Exchange threw network exception. 由于交易所抛出网络异常，未能获得市价单。"
                  + "Waiting until next trade cycle. 等到下一个交易周期。",
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...  您的错误处理代码可以放在这里...
      // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
      // 我们将重新抛出 StrategyException 以供引擎处理 - 它会关闭机器人。
      LOG.error(
          () ->
              market.getName()
                  + " Failed to get market orders because Exchange threw TradingApi exception.  由于交易所抛出 TradingApi 异常，未能获得市价单。"
                  + "Telling Trading Engine to shutdown bot! 告诉交易引擎关闭机器人！",
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with a buy order at current BID price.
   * * 首次调用交易策略时执行的算法。我们以当前 BID 价格买入订单开始。
   *
   * @param currentBidPrice the current market BID price.  当前市场 BID 价格。
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *              如果从 Exchange 适配器接收到意外异常。
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   *     抛出此异常表明我们希望交易引擎关闭机器人。
   */
  private void executeAlgoForWhenLastOrderWasNone(BigDecimal currentBidPrice)
      throws StrategyException {
    LOG.info(
        () ->
            market.getName()
                + " OrderType is NONE - placing new BUY order at [ OrderType 为 NONE - 在 ["
                + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)
                + "]");

    try {
      // Calculate the amount of base currency (BTC) to buy for given amount of counter currency (USD).
      //计算给定数量的相对货币 (USD) 购买的基础货币 (BTC) 数量。
      final BigDecimal amountOfBaseCurrencyToBuy =
          getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(counterCurrencyBuyOrderAmount);

      // Send the order to the exchange
      // 发送订单到交易所
      LOG.info(() -> market.getName() + " Sending initial BUY order to exchange ---> 发送初始买入订单以进行交换 --->");

      lastOrder.id =
          tradingApi.createOrder(
              market.getId(), OrderType.BUY, amountOfBaseCurrencyToBuy, currentBidPrice);

      LOG.info(
          () -> market.getName() + " Initial BUY Order sent successfully. ID:  初始买单发送成功。 ID：" + lastOrder.id);

      // update last order details
      // 更新最后的订单详情
      lastOrder.price = currentBidPrice;
      lastOrder.type = OrderType.BUY;
      lastOrder.amount = amountOfBaseCurrencyToBuy;

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order actually made it to the exchange? And if not, resend it...
      // 你的超时处理代码可以放在这里，例如您可能想检查订单是否真的到达了交易所？如果没有，请重新发送...

      // We are just going to log it and swallow it, and wait for next trade cycle.
      // 我们只是要记录它并吞下它，然后等待下一个交易周期。
      LOG.error(
          () ->
              market.getName()
                  + " Initial order to BUY base currency failed because Exchange threw network exception. Waiting until next trade cycle. " +
                      " 由于交易所抛出网络异常，购买基础货币的初始订单失败。等到下一个交易周期。",
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here... 您的错误处理代码可以放在这里...
      // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
      // 我们将重新抛出 StrategyException 以供引擎处理 - 它会关闭机器人。
      LOG.error(
          () ->
              market.getName()
                  + " Initial order to BUY base currency failed because Exchange threw TradingApi exception. Telling Trading Engine to shutdown bot!  " +
                      "由于交易所抛出 TradingApi 异常，购买基础货币的初始订单失败。告诉交易引擎关闭机器人！",
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchanges was a BUY.
   * * 我们在交易所下的最后一个订单是买入时执行的算法。
   *
   * <p>If last buy order filled, we try and sell at a profit.
   * <p>如果最后一个买单成交，我们会尝试卖出获利。
   *
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   * * @throws StrategyException 如果从 Exchange 适配器接收到意外异常。
   *
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   *     * 抛出此异常表明我们希望交易引擎关闭机器人。
   */
  private void executeAlgoForWhenLastOrderWasBuy() throws StrategyException {
    try {
      // Fetch our current open orders and see if the buy order is still outstanding/open on the exchange
      // 获取我们当前的未结订单并查看购买订单是否仍然未完成/在交易所未结
      final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
      boolean lastOrderFound = false;
      for (final OpenOrder myOrder : myOrders) {
        if (myOrder.getId().equals(lastOrder.id)) {
          lastOrderFound = true;
          break;
        }
      }

      // If the order is not there, it must have all filled.
      // 如果订单不存在，则必须全部成交。
      if (!lastOrderFound) {
        LOG.info(
            () ->
                market.getName()
                    + " ^^^ Yay!!! Last BUY Order Id [ ^^^耶！！！最后购买订单编号 ["
                    + lastOrder.id
                    + "] filled at [ ]填写在["
                    + lastOrder.price
                    + "]");

        /**
         * The last buy order was filled, so lets see if we can send a new sell order.
         * 最后一个买单已经成交，所以让我们看看我们是否可以发送一个新的卖单。
         *
         * IMPORTANT - new sell order ASK price must be > (last order price + exchange fees) because:
         * * 重要 - 新卖单要价必须 >（最后订单价格 + 交易费），因为：
         *
         * 1. If we put sell amount in as same amount as previous buy, the exchange barfs because we don't have enough units to cover the transaction fee.
         *          * 1. If we put sell amount in as same amount as previous buy, the exchange barfs because we don't have enough units to cover the transaction fee.
         *
         * 2. We could end up selling at a loss.
         * * 2. 我们最终可能会亏本出售。
         *
         * For this example strategy, we're just going to add 2% (taken from the 'minimum-percentage-gain' config item in the {project-root}/config/strategies.yaml config file) on top of previous bid price to make a little profit and cover the exchange fees.
         * * 对于这个示例策略，我们将在之前的投标价格之上增加 2%（取自 {project-root}/config/strategies.yaml 配置文件中的“minimum-percentage-gain”配置项）赚取一点利润并支付交易所费用。
         *
         * Your algo will have other ideas on how much profit to make and when to apply the exchange fees - you could try calling
         the TradingApi#getPercentageOfBuyOrderTakenForExchangeFee() and
          TradingApi#getPercentageOfSellOrderTakenForExchangeFee() when calculating the order to send to the exchange...
         * 您的算法会对赚取多少利润以及何时应用交易费有其他想法 - 您可以尝试调用
         TradingApi#getPercentageOfBuyOrderTakenForExchangeFee() 和
         TradingApi#getPercentageOfSellOrderTakenForExchangeFee() 在计算发送到交易所的订单时...
         */
        LOG.info(
            () ->
                market.getName()
                    + " Percentage profit (in decimal) to make for the sell order is: 卖单的利润百分比（十进制）是："
                    + minimumPercentageGain);

        final BigDecimal amountToAdd = lastOrder.price.multiply(minimumPercentageGain);
        LOG.info(
            () -> market.getName() + " Amount to add to last buy order fill price: 添加到最后买单执行价格的金额：" + amountToAdd);

        // Most exchanges (if not all) use 8 decimal places.
        // 大多数交易所（如果不是全部）使用 8 位小数。

        // It's usually best to round up the ASK price in your calculations to maximise gains.
        // 通常最好在计算中将 ASK 价格四舍五入以最大化收益。
        final BigDecimal newAskPrice =
            lastOrder.price.add(amountToAdd).setScale(8, RoundingMode.HALF_UP);
        LOG.info(
            () ->
                market.getName()
                    + " Placing new SELL order at ask price [ 以卖价下新卖单 ["
                    + new DecimalFormat(DECIMAL_FORMAT).format(newAskPrice)
                    + "]");

        LOG.info(() -> market.getName() + " Sending new SELL order to exchange --->  发送新的 SELL 订单以进行交换 --->");

        // Build the new sell order
        // 建立新的卖单
        lastOrder.id =
            tradingApi.createOrder(market.getId(), OrderType.SELL, lastOrder.amount, newAskPrice);
        LOG.info(() -> market.getName() + " New SELL Order sent successfully. ID: 新卖单发送成功。 ID：" + lastOrder.id);

        // update last order state
        // 更新最后订单状态
        lastOrder.price = newAskPrice;
        lastOrder.type = OrderType.SELL;
      } else {

        /**
         * BUY order has not filled yet.
         * BUY 订单尚未成交。
         *
         * Could be nobody has jumped on it yet... or the order is only part filled... or market
         has gone up and we've been outbid and have a stuck buy order. In which case, we have to
         wait for the market to fall for the order to fill... or you could tweak this code to
         cancel the current order and raise your bid - remember to deal with any part-filled orders!
         可能还没有人跳上它……或者订单只完成了一部分……或者市场
         已经上涨，我们的出价被高出并且有一个卡住的买单。在这种情况下，我们必须
         等待市场下跌以完成订单...或者您可以将此代码调整为
         取消当前订单并提高您的出价 - 请记住处理任何部分完成的订单！
         */
        LOG.info(
            () ->
                market.getName()
                    + " !!! Still have BUY Order ！！！仍有购买订单"
                    + lastOrder.id
                    + " waiting to fill at [ 等待填写 ["
                    + lastOrder.price
                    + "] - holding last BUY order...  ] - 持有最后的买单...");
      }

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order actually
      // 你的超时处理代码可以放在这里，例如你可能想检查订单是否真的

      // made it to the exchange? And if not, resend it...
      // 到交易所了吗？如果没有，请重新发送...

      // We are just going to log it and swallow it, and wait for next trade cycle.
      // 我们只是要记录它并吞下它，然后等待下一个交易周期。
      LOG.error(
          () ->
              market.getName()
                  + " New Order to SELL base currency failed because Exchange threw network exception. Waiting until next trade cycle. Last Order: " +
                      "由于交易所抛出网络异常，新订单卖出基础货币失败。等到下一个交易周期。最后的订单："
                  + lastOrder,
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // 您的错误处理代码可以放在这里...

      // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
      // 我们将重新抛出 StrategyException 以供引擎处理 - 它会关闭机器人。
      LOG.error(
          () ->
              market.getName()
                  + " New order to SELL base currency failed because Exchange threw TradingApi exception. Telling Trading Engine to shutdown bot! Last Order: " +
                      " 卖出基础货币的新订单失败，因为交易所抛出 TradingApi 异常。告诉交易引擎关闭机器人！最后的订单："
                  + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchange was a SELL.
   * * 我们在交易所下的最后一个订单是卖出时执行的算法。
   *
   * <p>If last sell order filled, we send a new buy order to the exchange.
   * <p>如果最后一个卖单成交，我们会向交易所发送一个新的买单。
   *
   * @param currentBidPrice the current market BID price.  当前市场 BID 价格。
   * @param currentAskPrice the current market ASK price.  当前市场ASK价格。
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *  * @throws StrategyException 如果从 Exchange 适配器接收到意外异常。
   *
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   *     * 抛出此异常表明我们希望交易引擎关闭机器人。
   */
  private void executeAlgoForWhenLastOrderWasSell(
      BigDecimal currentBidPrice, BigDecimal currentAskPrice) throws StrategyException {
    try {
      final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
      boolean lastOrderFound = false;
      for (final OpenOrder myOrder : myOrders) {
        if (myOrder.getId().equals(lastOrder.id)) {
          lastOrderFound = true;
          break;
        }
      }

      // If the order is not there, it must have all filled.
      //如果订单不存在，则必须全部执行。
      if (!lastOrderFound) {
        LOG.info(
            () ->
                market.getName()
                    + " ^^^ Yay!!! Last SELL Order Id [ ^^^耶！！！最后卖出订单编号 ["
                    + lastOrder.id
                    + "] filled at [ ]填写在["
                    + lastOrder.price
                    + "]");

        // Get amount of base currency (BTC) we can buy for given counter currency (USD) amount.
        // 获取我们可以在给定的对应货币 (USD) 数量下购买的基础货币 (BTC) 数量。
        final BigDecimal amountOfBaseCurrencyToBuy =
            getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
                counterCurrencyBuyOrderAmount);

        LOG.info(
            () ->
                market.getName()
                    + " Placing new BUY order at bid price [ 以买入价下新买单 ["
                    + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)
                    + "]");

        LOG.info(() -> market.getName() + " Sending new BUY order to exchange --->  发送新的买单进行交换--->");

        // Send the buy order to the exchange.
        // 将买单发送到交易所。
        lastOrder.id =
            tradingApi.createOrder(
                market.getId(), OrderType.BUY, amountOfBaseCurrencyToBuy, currentBidPrice);
        LOG.info(() -> market.getName() + " New BUY Order sent successfully. ID: 新买单发送成功。 ID：" + lastOrder.id);

        // update last order details
        // 更新最后的订单详情
        lastOrder.price = currentBidPrice;
        lastOrder.type = OrderType.BUY;
        lastOrder.amount = amountOfBaseCurrencyToBuy;
      } else {

        /**
         * SELL order not filled yet.
         * 卖单尚未成交。
         *
         * Could be nobody has jumped on it yet... or the order is only part filled... or market
          has gone down and we've been undercut and have a stuck sell order. In which case, we
          have to wait for market to recover for the order to fill... or you could tweak this
          code to cancel the current order and lower your ask - remember to deal with any   part-filled orders!
         可能还没有人跳上它……或者订单只完成了一部分……或者市场
         已经下跌，我们已经被削弱并且有一个卡住的卖单。在这种情况下，我们
         必须等待市场恢复才能完成订单......或者你可以调整这个
         取消当前订单并降低您的要求的代码 - 请记住处理任何部分填充的订单！
         */
        if (currentAskPrice.compareTo(lastOrder.price) < 0) {
          LOG.info(
              () ->
                  market.getName()
                      + " <<< Current ask price [ <<< 当前要价 ["
                      + currentAskPrice
                      + "] is LOWER then last order price [ ] 低于最后订单价格 ["
                      + lastOrder.price
                      + "] - holding last SELL order... ] - 持有最后一个卖出订单...");

        } else if (currentAskPrice.compareTo(lastOrder.price) > 0) {
          LOG.error(
              () ->
                  market.getName()
                      + " >>> Current ask price [ >>> 当前要价 ["
                      + currentAskPrice
                      + "] is HIGHER than last order price [ ] 高于最后订单价格 ["
                      + lastOrder.price
                      + "] - IMPOSSIBLE! BX-bot must have sold????? ] - 不可能的！ BX-bot 一定卖了？？？？");

        } else if (currentAskPrice.compareTo(lastOrder.price) == 0) {
          LOG.info(
              () ->
                  market.getName()
                      + " === Current ask price [ === 当前卖价 ["
                      + currentAskPrice
                      + "] is EQUAL to last order price [  等于最后订单价格 ["
                      + lastOrder.price
                      + "] - holding last SELL order... ] - 持有最后一个卖出订单...");
        }
      }
    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order actually made it to the exchange? And if not, resend it... We are just going to log it and swallow it, and wait for next trade cycle.
      // 你的超时处理代码可以放在这里，例如您可能想检查订单是否真的到达了交易所？如果没有，请重新发送...我们将记录并吞下它，然后等待下一个交易周期。
      LOG.error(
          () ->
              market.getName()
                  + " New Order to BUY base currency failed because Exchange threw network exception. Waiting until next trade cycle. Last Order:  " +
                      " 购买基础货币的新订单失败，因为交易所抛出网络异常。等到下一个交易周期。最后的订单："
                  + lastOrder,
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      //您的错误处理代码可以放在这里...
      // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
      // 我们将重新抛出 StrategyException 以供引擎处理 - 它会关闭机器人。
      LOG.error(
          () ->
              market.getName()
                  + " New order to BUY base currency failed because Exchange threw TradingApi exception. Telling Trading Engine to shutdown bot! Last Order: " +
                      "购买基础货币的新订单失败，因为交易所抛出 TradingApi 异常。告诉交易引擎关闭机器人！最后的订单："
                  + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Returns amount of base currency (BTC) to buy for a given amount of counter currency (USD) based on last market trade price.
   *返回基于最后市场交易价格购买给定数量的对应货币 (USD) 的基础货币 (BTC) 数量。
   *
   * @param amountOfCounterCurrencyToTrade the amount of counter currency (USD) we have to trade  (buy) with.
   *                                      我们必须与之交易（购买）的对应货币（美元）的数量。
   *
   * @return the amount of base currency (BTC) we can buy for the given counter currency (USD)  amount.
   * @return 我们可以为给定的相对货币 (USD) 数量购买的基础货币 (BTC) 数量。
   *
   * @throws TradingApiException if an unexpected error occurred contacting the exchange.
   *                        如果在联系交易所时发生意外错误。
   * @throws ExchangeNetworkException if a request to the exchange has timed out.  如果对交易所的请求已超时。
   */
  private BigDecimal getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
      BigDecimal amountOfCounterCurrencyToTrade)
      throws TradingApiException, ExchangeNetworkException {

    LOG.info(
        () ->
            market.getName()
                + " Calculating amount of base currency (BTC) to buy for amount of counter currency  计算要购买的基础货币（BTC）的数量以换取对应货币的数量"
                + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                + " "
                + market.getCounterCurrency());

    // Fetch the last trade price
    // 获取最后的交易价格
    final BigDecimal lastTradePriceInUsdForOneBtc = tradingApi.getLatestMarketPrice(market.getId());
    LOG.info(
        () ->
            market.getName()
                + " Last trade price for 1  最后交易价格为 1"
                + market.getBaseCurrency()
                + " was: 曾是："
                + new DecimalFormat(DECIMAL_FORMAT).format(lastTradePriceInUsdForOneBtc)
                + " "
                + market.getCounterCurrency());

    /**
     * Most exchanges (if not all) use 8 decimal places and typically round in favour of the exchange. It's usually safest to round down the order quantity in your calculations.
     * * 大多数交易所（如果不是全部）使用 8 位小数，通常四舍五入以支持交易所。在计算中将订单数量四舍五入通常是最安全的。
     */
    final BigDecimal amountOfBaseCurrencyToBuy =
        amountOfCounterCurrencyToTrade.divide(
            lastTradePriceInUsdForOneBtc, 8, RoundingMode.HALF_DOWN);

    LOG.info(
        () ->
            market.getName()
                + " Amount of base currency ( 基础货币金额（"
                + market.getBaseCurrency()
                + ") to BUY for  ) 购买"
                + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                + " "
                + market.getCounterCurrency()
                + " based on last market trade price:  基于最后的市场交易价格："
                + amountOfBaseCurrencyToBuy);

    return amountOfBaseCurrencyToBuy;
  }

  /**
   * Loads the config for the strategy. We expect the 'counter-currency-buy-order-amount' and 'minimum-percentage-gain' config items to be present in the {project-root}/config/strategies.yaml config file.
   ** 加载策略的配置。我们希望“counter-currency-buy-order-amount”和“minimum-percentage-gain”配置项出现在 {project-root}/config/strategies.yaml 配置文件中。
   *
   * @param config the config for the Trading Strategy.  交易策略的配置。
   */
  private void getConfigForStrategy(StrategyConfig config) {

    // Get counter currency buy amount...
    final String counterCurrencyBuyOrderAmountFromConfigAsString =
        config.getConfigItem("counter-currency-buy-order-amount");

    if (counterCurrencyBuyOrderAmountFromConfigAsString == null) {
      // game over
      // 游戏结束
      throw new IllegalArgumentException(
          "Mandatory counter-currency-buy-order-amount value missing in strategy.xml config. strategy.xml 配置中缺少强制的 counter-currency-buy-order-amount 值。");
    }
    LOG.info(
        () ->
            "<counter-currency-buy-order-amount> from config is:  配置中的 <counter-currency-buy-order-amount> 是："
                + counterCurrencyBuyOrderAmountFromConfigAsString);

    // Will fail fast if value is not a number
    // 如果 value 不是数字，将很快失败
    counterCurrencyBuyOrderAmount = new BigDecimal(counterCurrencyBuyOrderAmountFromConfigAsString);
    LOG.info(() -> "counterCurrencyBuyOrderAmount: counterCurrencyBuyOrderAmount:" + counterCurrencyBuyOrderAmount);

    // Get min % gain...
    final String minimumPercentageGainFromConfigAsString =
        config.getConfigItem("minimum-percentage-gain");
    if (minimumPercentageGainFromConfigAsString == null) {
      // game over
      throw new IllegalArgumentException(
          "Mandatory minimum-percentage-gain value missing in strategy.xml config.  strategy.xml 配置中缺少强制的最小百分比增益值。");
    }
    LOG.info(
        () ->
            "<minimum-percentage-gain> from config is:  来自配置的 <minimum-percentage-gain> 是：" + minimumPercentageGainFromConfigAsString);

    // Will fail fast if value is not a number
    // 如果 value 不是数字，将很快失败
    final BigDecimal minimumPercentageGainFromConfig =
        new BigDecimal(minimumPercentageGainFromConfigAsString);
    minimumPercentageGain =
        minimumPercentageGainFromConfig.divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);

    LOG.info(() -> "minimumPercentageGain in decimal is: 十进制的 minimumPercentageGain 是：" + minimumPercentageGain);
  }

  /**
   * Models the state of an Order placed on the exchange.
   * 模拟在交易所下达的订单的状态。
   *
   * <p>Typically, you would maintain order state in a database or use some other persistence method
   to recover from restarts and for audit purposes. In this example, we are storing the state in
   memory to keep it simple.
   <p>通常，您会在数据库中维护订单状态或使用其他一些持久性方法
   从重新启动中恢复并用于审计目的。在这个例子中，我们将状态存储在
   记忆保持简单。
   */
  private static class OrderState {

    /** Id - default to null.
     * Id - 默认为空。*/
    private String id = null;

    /**
     * Type: buy/sell. We default to null which means no order has been placed yet, i.e. we've just started!
     * * 类型：买/卖。我们默认为 null 表示尚未下订单，即我们刚刚开始！
     */
    private OrderType type = null;

    /** Price to buy/sell at - default to zero.
     * 买入/卖出价格 - 默认为零。*/
    private BigDecimal price = BigDecimal.ZERO;

    /** Number of units to buy/sell - default to zero.
     * 要购买/出售的单位数量 - 默认为零。*/
    private BigDecimal amount = BigDecimal.ZERO;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add("type", type)
          .add("price", price)
          .add("amount", amount)
          .toString();
    }
  }
}
