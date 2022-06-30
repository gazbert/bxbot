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

package com.gazbert.bxbot.trading.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * BX-bot's Trading API.
 * * BX-bot 的交易 API。
 *
 * <p>This is what Trading Strategies use to trade.
 * <p>这是交易策略用于交易的内容。
 *
 *
 * <p>Exchange Adapters provide their own implementation of the API for the exchange they wish to
  integrate with.
 <p>交换适配器为他们希望交换的 API 提供自己的实现
 与整合。
 *
 * <p>This version of the Trading API only supports <a
  href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> traded at the <a
  href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>. It does not support
  futures or margin trading.
 <p>此版本的交易 API 仅支持 <a
 href="http://www.investopedia.com/terms/l/limitorder.asp">限价单</a>在<a
 href="http://www.investopedia.com/terms/s/spotprice.asp">现货价格</a>。它不支持
 期货或保证金交易。
 *
 * @author gazbert
 * @since 1.0
 */
public interface TradingApi {

  /**
   * Returns the current version of the API.
   * 返回 API 的当前版本。
   *
   * @return the API version.
   * @since 1.0
   */
  default String getVersion() {
    return "1.1";
  }

  /**
   * Returns the API implementation name.
   * 返回 API 实现名称。
   *
   * @return the API implementation name.
   * * @return API 实现名称。
   * @since 1.0
   */
  String getImplName();

  /**
   * Fetches latest <em>market</em> orders for a given market.
   * * 获取给定市场的最新 <em>market</em> 订单。
   *
   * @param marketId the id of the market.
   *                 * @param marketId 市场ID。
   * @return the market order book.
   *  * @return 市场订单簿。
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *  @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
   *
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
  这是每个 Exchange 适配器的特定实现 - 请参阅文档
  您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
  让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.0
   */
  MarketOrderBook getMarketOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Fetches <em>your</em> current open orders, i.e. the orders placed by the bot.
   * * 获取 <em>您的</em> 当前未结订单，即机器人下的订单。
   *
   * @param marketId the id of the market.
   *                 * @param marketId 市场ID。
   *
   * @return your current open orders.
   *  * @return 您当前的未结订单。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
   这是每个 Exchange 适配器的特定实现 - 请参阅文档
   您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
   让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.0
   */
  List<OpenOrder> getYourOpenOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Places an order on the exchange.
   * 在交易所下订单。
   *
   * @param marketId the id of the market.
   *                 * @param marketId 市场ID。
   *
   * @param orderType Value must be {@link OrderType#BUY} or {@link OrderType#SELL}.
   *                  * @param orderType 值必须是 {@link OrderType#BUY} 或 {@link OrderType#SELL}。
   *
   * @param quantity amount of units you are buying/selling in this order.
   *                  * @param quantity 您在此订单中购买/出售的单位数量。
   *
   * @param price the price per unit you are buying/selling at.
   *              * @param price 您购买/出售的每单位价格。
   *
   * @return the id of the order.
   * * @return 订单的id。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
   这是每个 Exchange 适配器的特定实现 - 请参阅文档
   您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
   让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.0
   */
  String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Cancels your existing order on the exchange.
   * 取消您在交易所的现有订单。
   *
   * @param orderId your order Id.
   *                您的订单编号。
   *
   * @param marketId the id of the market the order was placed on, e.g. btc_usd
   *                 * @param marketId 下单的市场ID，例如： btc_usd
   *
   * @return true if order cancelled ok, false otherwise.
   * * @return 如果订单取消成功，则返回 true，否则返回 false。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
       adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
   这是每个 Exchange 适配器的特定实现 - 请参阅文档
   您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
   让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.0
   */
  boolean cancelOrder(String orderId, String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Fetches the latest price for a given market. This is usually in BTC for altcoin markets and USD
    for BTC/USD markets - see the Exchange Adapter documentation.
   获取给定市场的最新价格。这通常在 BTC 中用于山寨币市场和美元
   对于 BTC/USD 市场 - 请参阅 Exchange Adapter 文档。

   *
   * @param marketId the id of the market.
   *                 市场的 id。
   *
   * @return the latest market price.
   *        最新的市场价格。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
   这是每个 Exchange 适配器的特定实现 - 请参阅文档
   您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
   让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   * @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
  这意味着发生了不好的事情；您可能希望将此异常包装在
  StrategyException 并让交易引擎立即关闭机器人以防止
  意外的损失。
   * @since 1.0
   */
  BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Fetches the balance of your wallets on the exchange.
   * 在交易所获取您的钱包余额。
   *
   * @return your wallet balance info.
   * * @return 你的钱包余额信息。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
  这是每个 Exchange 适配器的特定实现 - 请参阅文档
  您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
  让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.0
   */
  BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException;

  /**
    Returns the exchange BUY order fee for a given market id. The returned value is the % of the
    BUY order that the exchange uses to calculate its fee as a {@link BigDecimal}. If the fee is
    0.33%, then the {@link BigDecimal} value returned is 0.0033.
   返回给定市场 id 的交易所买单费用。返回值是百分比
   交易所用来计算其费用的买入订单为 {@link BigDecimal}。如果费用是
   0.33%，则返回的 {@link BigDecimal} 值为 0.0033。

   *
   * @param marketId the id of the market.
   *                 市场的 id。
   *
   * @return the % of the BUY order that the exchange uses to calculate its fee as a {@link BigDecimal}.
   * * @return 交易所用来计算其费用的买入订单百分比 {@link BigDecimal}。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
  这是每个交易所的具体实现 - 请参阅文档
  您正在使用您的交易协议。您可以重试 API 调用，或策略并策略
  让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.0
   */
  BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException;

  /**
   * Returns the exchange SELL order fee for a given market id. The returned value is the % of the
    SELL order that the exchange uses to calculate its fee as a {@link BigDecimal}. If the fee is
    0.33%, then the {@link BigDecimal} value returned is 0.0033.
   返回给定市场 id 的交易所卖出订单费用。返回值是百分比
   交易所用来计算其费用的卖出订单为 {@link BigDecimal}。如果费用是
   0.33%，则返回的 {@link BigDecimal} 值为 0.0033。
   *
   * @param marketId the id of the market.
   *                 市场的 id。
   *
   * @return the % of the SELL order that the exchange uses to calculate its fee as a {@link  BigDecimal}.
   *    * @return 交易所用来计算其费用的卖出订单百分比 {@link BigDecimal}。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
  这是每个 Exchange 适配器的特定实现 - 请参阅文档
  您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
  让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   * @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
  这意味着发生了不好的事情；您可能希望将此异常包装在
  StrategyException 并让交易引擎立即关闭机器人以防止
  意外的损失。
   * @since 1.0
   */
  BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException;

  /**
   * Returns the exchange Ticker a given market id.
   * 返回给定市场 id 的交易所代码。
   *
   * <p>Not all exchanges provide the information returned in the Ticker methods - you'll need to
    check the relevant Exchange Adapter code/Javadoc and online Exchange API documentation.
   <p>并非所有交易所都提供在 Ticker 方法中返回的信息 - 您需要
   检查相关的 Exchange 适配器代码/Javadoc 和在线 Exchange API 文档。
   *
   * <p>If the exchange does not provide the information, a null value is returned.
   * <p>如果交易所不提供信息，则返回空值。
   *
   * @param marketId the id of the market.
   *                     * @param marketId the id of the market.
   *
   * @return the exchange Ticker for a given market.
   * * @return 给定市场的交易所代码。
   *
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
        This is implementation specific for each Exchange Adapter - see the documentation for the
        adapter you are using. You could retry the API call, or exit from your Trading Strategy and
        let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws ExchangeNetworkException 如果在尝试连接到 Exchange 时发生网络错误。
  这是每个 Exchange 适配器的特定实现 - 请参阅文档
  您正在使用的适配器。您可以重试 API 调用，或退出您的交易策略并
  让交易引擎在下一个交易周期执行您的交易策略。

   * @throws TradingApiException if the API call failed for any reason other than a network error.
        This means something bad as happened; you would probably want to wrap this exception in a
        StrategyException and let the Trading Engine shutdown the bot immediately to prevent
        unexpected losses.
   @throws TradingApiException 如果 API 调用因网络错误以外的任何原因而失败。
   这意味着发生了不好的事情；您可能希望将此异常包装在
   StrategyException 并让交易引擎立即关闭机器人以防止
   意外的损失。
   * @since 1.1
   */
  default Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {

    return new Ticker() {
      @Override
      public BigDecimal getLast() {
        return null;
      }

      @Override
      public BigDecimal getBid() {
        return null;
      }

      @Override
      public BigDecimal getAsk() {
        return null;
      }

      @Override
      public BigDecimal getLow() {
        return null;
      }

      @Override
      public BigDecimal getHigh() {
        return null;
      }

      @Override
      public BigDecimal getOpen() {
        return null;
      }

      @Override
      public BigDecimal getVolume() {
        return null;
      }

      @Override
      public BigDecimal getVwap() {
        return null;
      }

      @Override
      public Long getTimestamp() {
        return null;
      }
    };
  }
}
