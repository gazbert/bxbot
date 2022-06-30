/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.core.config.strategy;

import com.gazbert.bxbot.core.config.market.MarketImpl;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.Market;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**：加载并初始化要执行的交易策略的Util类。
 * Util class that loads and initialises the Trading Strategies to execute.
 *
 * @author gazbert
 */
@Component
public class TradingStrategiesBuilder {

  private static final Logger LOG = LogManager.getLogger();
  private TradingStrategyFactory tradingStrategyFactory;

  @Autowired
  public void setTradingStrategyFactory(TradingStrategyFactory tradingStrategyFactory) {
    this.tradingStrategyFactory = tradingStrategyFactory;
  }

  /**
   * Builds the Trading Strategy execution list.
   * 建立交易策略执行列表。
   *
   * @param strategies the strategies.
   *                   策略。
   *
   * @param markets the markets.
   *                市场。
   *
   * @param exchangeAdapter the Exchange Adapter.
   *                        交换适配器。
   * @return the Trading Strategy list.
   * @return 交易策略列表。
   */
  public List<TradingStrategy> buildStrategies(
      List<StrategyConfig> strategies,
      List<MarketConfig> markets,
      ExchangeAdapter exchangeAdapter) {

    final List<TradingStrategy> tradingStrategiesToExecute = new ArrayList<>();

    // Register the strategies  // 注册策略
    final Map<String, StrategyConfig> tradingStrategyConfigs = new HashMap<>();
    for (final StrategyConfig strategy : strategies) {
      tradingStrategyConfigs.put(strategy.getId(), strategy);
      LOG.info(() -> "Registered Trading Strategy with Trading Engine: Id=使用交易引擎注册的交易策略：Id=" + strategy.getId());
    }

    // Set logic only as crude mechanism for checking for duplicate Markets.  仅将逻辑设置为检查重复市场的粗略机制。
    final Set<Market> loadedMarkets = new HashSet<>();

    // Load em up and create the Strategies   // 加载它们并创建策略
    for (final MarketConfig market : markets) {
      final String marketName = market.getName();
      if (!market.isEnabled()) {
        LOG.info(
            () -> marketName + " market is NOT enabled for trading - skipping to next market...市场未启用交易 - 跳至下一个市场...\"");
        continue;
      }

      final Market tradingMarket =
          new MarketImpl(
              marketName, market.getId(), market.getBaseCurrency(), market.getCounterCurrency());
      final boolean wasAdded = loadedMarkets.add(tradingMarket);
      if (!wasAdded) {
        final String errorMsg = "Found duplicate Market! Market details: 发现重复的市场！市场详情：" + market;
        LOG.fatal(() -> errorMsg);
        throw new IllegalArgumentException(errorMsg);
      } else {
        LOG.info(
            () ->
                "Registered Market with Trading Engine: Id=“带有交易引擎的注册市场：Id=”"
                    + market.getId()
                    + ", Name="
                    + marketName);
      }

      // Get the strategy to use for this Market      // 获取用于该市场的策略
      final String strategyToUse = market.getTradingStrategyId();
      LOG.info(() -> "Market Trading Strategy Id to use: 要使用的市场交易策略 ID：" + strategyToUse);

      if (tradingStrategyConfigs.containsKey(strategyToUse)) {
        final StrategyConfig tradingStrategy = tradingStrategyConfigs.get(strategyToUse);
        final StrategyConfigItems tradingStrategyConfig = new StrategyConfigItems();
        final Map<String, String> configItems = tradingStrategy.getConfigItems();
        if (configItems != null && !configItems.isEmpty()) {
          tradingStrategyConfig.setItems(configItems);
        } else {
          LOG.info(
              () ->
                  "No (optional) configuration has been set for Trading Strategy: 没有为交易策略设置（可选）配置："
                      + strategyToUse);
        }
        LOG.info(() -> "StrategyConfigImpl (optional): " + tradingStrategyConfig);

        /**
         * Load the Trading Strategy impl, instantiate it, set its config, and store in the Trading Strategy execution list.
         * 加载 Trading Strategy impl，实例化它，设置它的配置，并存储在 Trading Strategy 执行列表中。
         */
        final TradingStrategy strategyImpl =
            tradingStrategyFactory.createTradingStrategy(tradingStrategy);
        strategyImpl.init(exchangeAdapter, tradingMarket, tradingStrategyConfig);

        LOG.info(
            () ->
                "Initialized trading strategy successfully. Name: [ 成功初始化交易策略。姓名： ["
                    + tradingStrategy.getName()
                    + "] Class: "
                    + tradingStrategy.getClassName());

        tradingStrategiesToExecute.add(strategyImpl);
      } else {

        // Game over. Config integrity blown - we can't find strat.  // 游戏结束。配置完整性被破坏 - 我们找不到策略。
        final String errorMsg =
            "Failed to find matching Strategy for Market  “未能找到市场匹配策略”"
                + market
                + " - The Strategy “ - 战略”"
                + "["
                + strategyToUse
                + "] cannot be found in the 无法在 "
                + " Strategy Descriptions map: 策略说明图："
                + tradingStrategyConfigs;
        LOG.error(() -> errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }
    }
    return tradingStrategiesToExecute;
  }
}
