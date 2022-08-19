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

package com.gazbert.bxbot.strategy.api;

import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.TradingApi;

/**
 * All user defined Trading Strategies must implement this interface.
 *  所有用户定义的交易策略都必须实现这个接口。
 *
 * <p>The Trading Engine will send only 1 thread through your strategy code at a time - you do not have to code for concurrency.
 *    * <p>交易引擎一次只会通过您的策略代码发送 1 个线程 - 您不必为并发编写代码。
 *
 * @author gazbert
 * @since 1.0
 */
public interface TradingStrategy {

  /**
   * Called once by the Trading Engine when it starts up.
   *  交易引擎启动时调用一次。
   *
   * @param tradingApi the Trading API.
   *                   交易 API。
   *
   * @param market the market for this strategy.
   *               这种策略的市场。
   *
   * @param config optional configuration for the strategy.
   *               策略的可选配置。
   */
  void init(TradingApi tradingApi, Market market, StrategyConfig config);

  /**
   * Called by the Trading Engine during each trade cycle.
   *   由交易引擎在每个交易周期内所有。
   *
   * <p>Here, you can create some orders, cancel some, buy some beer... do whatever you want.
   *  * <p>在这里，您可以创建一些订单、取消一些订单、购买一些啤酒……随心所欲。
   *
   * @throws StrategyException if something goes bad. Trading Strategy implementations should throw  this exception if they want the Trading Engine to shutdown the bot immediately.
   * @throws StrategyException 如果出现问题。如果交易策略实现希望交易引擎立即关闭机器人，则应抛出此异常。
   */
  void execute() throws StrategyException;
}
