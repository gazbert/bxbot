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
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Util class that loads and initialises the Trading Strategies to execute.
 *
 * @author gazbert
 */
@Component
@Log4j2
public class TradingStrategiesBuilder {

  private TradingStrategyFactory tradingStrategyFactory;

  /** Creates the Trading Strategies Builder. */
  public TradingStrategiesBuilder() {
    // No extra init needed.
  }

  /**
   * Sets the trading strategy factory.
   *
   * @param tradingStrategyFactory the trading strategy factory.
   */
  @Autowired
  public void setTradingStrategyFactory(TradingStrategyFactory tradingStrategyFactory) {
    this.tradingStrategyFactory = tradingStrategyFactory;
  }

  /**
   * Builds the Trading Strategy execution list.
   *
   * @param strategies the strategies.
   * @param markets the markets.
   * @param exchangeAdapter the Exchange Adapter.
   * @return the Trading Strategy list.
   */
  public List<TradingStrategy> buildStrategies(
      List<StrategyConfig> strategies,
      List<MarketConfig> markets,
      ExchangeAdapter exchangeAdapter) {

    final List<TradingStrategy> tradingStrategiesToExecute = new ArrayList<>();

    // Register the strategies
    final Map<String, StrategyConfig> tradingStrategyConfigs = new HashMap<>();
    for (final StrategyConfig strategy : strategies) {
      tradingStrategyConfigs.put(strategy.getId(), strategy);
      log.info("Registered Trading Strategy with Trading Engine: Id={}", strategy.getId());
    }

    // Set logic only as crude mechanism for checking for duplicate Markets.
    final Set<Market> loadedMarkets = new HashSet<>();

    // Load em up and create the Strategies
    for (final MarketConfig market : markets) {
      final String marketName = market.getName();
      if (!market.isEnabled()) {
        log.info("{} market is NOT enabled for trading - skipping to next market...", marketName);
        continue;
      }

      final Market tradingMarket =
          new MarketImpl(
              marketName, market.getId(), market.getBaseCurrency(), market.getCounterCurrency());
      final boolean wasAdded = loadedMarkets.add(tradingMarket);
      if (!wasAdded) {
        final String errorMsg = "Found duplicate Market! Market details: " + market;
        log.fatal(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      } else {
        log.info(
            "Registered Market with Trading Engine: Id={}, Name={}", market.getId(), marketName);
      }

      // Get the strategy to use for this Market
      final String strategyToUse = market.getTradingStrategyId();
      log.info("Market Trading Strategy Id to use: {}", strategyToUse);

      if (tradingStrategyConfigs.containsKey(strategyToUse)) {
        final StrategyConfig tradingStrategy = tradingStrategyConfigs.get(strategyToUse);
        final StrategyConfigItems tradingStrategyConfig = new StrategyConfigItems();
        final Map<String, String> configItems = tradingStrategy.getConfigItems();
        if (configItems != null && !configItems.isEmpty()) {
          tradingStrategyConfig.setItems(configItems);
        } else {
          log.info(
              "No (optional) configuration has been set for Trading Strategy: {}", strategyToUse);
        }
        log.info("StrategyConfigImpl (optional): {}", tradingStrategyConfig);

        /*
         * Load the Trading Strategy impl, instantiate it, set its config, and store in the
         * Trading Strategy execution list.
         */
        final TradingStrategy strategyImpl =
            tradingStrategyFactory.createTradingStrategy(tradingStrategy);
        strategyImpl.init(exchangeAdapter, tradingMarket, tradingStrategyConfig);

        log.info(
            "Initialized trading strategy successfully. Name: [{}] Class: [{}] Bean: [{}]",
            tradingStrategy.getName(),
            tradingStrategy.getClassName(),
            tradingStrategy.getBeanName());

        tradingStrategiesToExecute.add(strategyImpl);
      } else {

        // Game over. Config integrity blown - we can't find strategy.
        final String errorMsg =
            "Failed to find matching Strategy for Market "
                + market
                + " - The Strategy "
                + "["
                + strategyToUse
                + "] cannot be found in the "
                + " Strategy Descriptions map: "
                + tradingStrategyConfigs;
        log.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }
    }
    return tradingStrategiesToExecute;
  }
}
