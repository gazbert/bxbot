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

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gazbert.bxbot.core.config.strategy.strategies.TradingStrategyForBeanNameInstantiation;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Tests the Trading Strategy Factory behaves as expected.
 * 测试交易策略工厂的行为是否符合预期。
 *
 * @author gazbert
 */
class TestStrategyConfigFactory {

  private static final String STRATEGY_ID = "strat-123";
  private static final String STRATEGY_NAME = "ema-strat";
  private static final String STRATEGY_DESCRIPTION = "This is a great strat!";
  private static final String STRATEGY_CLASSNAME =
      "com.gazbert.bxbot.core.config.strategy.strategies.TradingStrategyForClassnameInstantiation";
  private static final String STRATEGY_BEAN_NAME = "emaStrat";
  private static final String INVALID_STRATEGY_BEAN_NAME = "invalid-bean";

  @Test
  void testCreatingStrategyUsingClassname() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    strategyConfig.setId(STRATEGY_ID);
    strategyConfig.setName(STRATEGY_NAME);
    strategyConfig.setDescription(STRATEGY_DESCRIPTION);
    strategyConfig.setClassName(STRATEGY_CLASSNAME);

    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    final TradingStrategy tradingStrategy =
        tradingStrategyFactory.createTradingStrategy(strategyConfig);

    assertThat(tradingStrategy).isNotNull();
  }

  @Test
  void testCreatingStrategyUsingBeanName() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    strategyConfig.setId(STRATEGY_ID);
    strategyConfig.setName(STRATEGY_NAME);
    strategyConfig.setDescription(STRATEGY_DESCRIPTION);
    strategyConfig.setBeanName(STRATEGY_BEAN_NAME);

    final ApplicationContext applicationContext = EasyMock.createMock(ApplicationContext.class);
    expect(applicationContext.getBean(STRATEGY_BEAN_NAME))
        .andReturn(new TradingStrategyForBeanNameInstantiation());
    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    tradingStrategyFactory.setSpringContext(applicationContext);
    EasyMock.replay(applicationContext);

    final TradingStrategy tradingStrategy =
        tradingStrategyFactory.createTradingStrategy(strategyConfig);

    assertThat(tradingStrategy).isNotNull();
    EasyMock.verify(applicationContext);
  }

  @Test
  void testCreatingStrategyUsingInvalidName() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    strategyConfig.setId(STRATEGY_ID);
    strategyConfig.setName(STRATEGY_NAME);
    strategyConfig.setDescription(STRATEGY_DESCRIPTION);
    strategyConfig.setBeanName(INVALID_STRATEGY_BEAN_NAME);

    final ApplicationContext applicationContext = EasyMock.createMock(ApplicationContext.class);
    expect(applicationContext.getBean(INVALID_STRATEGY_BEAN_NAME))
        .andThrow(new NullPointerException("No such bean error! 没有这样的bean错误！"));
    final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory();
    tradingStrategyFactory.setSpringContext(applicationContext);
    EasyMock.replay(applicationContext);

    assertThrows(
        IllegalArgumentException.class,
        () -> tradingStrategyFactory.createTradingStrategy(strategyConfig));
    EasyMock.verify(applicationContext);
  }
}
