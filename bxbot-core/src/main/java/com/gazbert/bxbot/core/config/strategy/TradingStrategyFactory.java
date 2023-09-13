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

import com.gazbert.bxbot.core.util.ConfigurableComponentFactory;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory class for instantiating a Trading Strategy class.
 *
 * @author gazbert
 */
@Component
@Log4j2
public class TradingStrategyFactory {

  private final ConfigurableComponentFactory configurableComponentFactory;

  private ApplicationContext springContext;

  /**
   * Creates the Trading Strategy Factory.
   *
   * @param configurableComponentFactory the configurable Component Factory.
   */
  public TradingStrategyFactory(ConfigurableComponentFactory configurableComponentFactory) {
    this.configurableComponentFactory = configurableComponentFactory;
  }

  /**
   * Sets the Application Context.
   *
   * @param springContext the Application Context.
   */
  @Autowired
  public void setSpringContext(ApplicationContext springContext) {
    this.springContext = springContext;
  }

  /**
   * Creates the Trading Strategy instance.
   *
   * @param strategyConfig the strategy config.
   * @return the trading strategy.
   */
  TradingStrategy createTradingStrategy(StrategyConfig strategyConfig) {
    final String tradingStrategyClassname = strategyConfig.getClassName();
    final String tradingStrategyBeanName = strategyConfig.getBeanName();

    TradingStrategy strategyImpl = null;
    if (tradingStrategyBeanName != null) {
      // if beanName is configured, try and get the bean first
      try {
        strategyImpl = (TradingStrategy) springContext.getBean(tradingStrategyBeanName);

      } catch (NullPointerException e) {
        final String errorMsg =
            "Failed to obtain bean [" + tradingStrategyBeanName + "] from spring context";
        log.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }
    }

    if (strategyImpl == null) {
      // if beanName not configured use className
      strategyImpl = configurableComponentFactory.createComponent(tradingStrategyClassname);
    }
    return strategyImpl;
  }
}
