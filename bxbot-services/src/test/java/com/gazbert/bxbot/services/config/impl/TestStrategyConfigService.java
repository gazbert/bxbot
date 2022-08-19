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

package com.gazbert.bxbot.services.config.impl;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.repository.StrategyConfigRepository;
import java.util.ArrayList;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

/**
 * Tests Strategy configuration service behaves as expected.
 * * 测试策略配置服务按预期运行。
 *
 * @author gazbert
 */
class TestStrategyConfigService {

  @Test
  void whenGetStrategyConfigCalledThenExpectConfigToBeReturned() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    strategyConfig.setId("id-123");
    final StrategyConfigRepository strategyConfigRepository =
        EasyMock.createMock(StrategyConfigRepository.class);
    expect(strategyConfigRepository.findById(strategyConfig.getId())).andReturn(strategyConfig);
    replay(strategyConfigRepository);

    final StrategyConfigServiceImpl strategyConfigService =
        new StrategyConfigServiceImpl(strategyConfigRepository);

    assertThat(strategyConfigService.getStrategyConfig(strategyConfig.getId()))
        .isEqualTo(strategyConfig);
    verify(strategyConfigRepository);
  }

  @Test
  void whenGetAllStrategyConfigCalledThenExpectConfigToBeReturned() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    final List<StrategyConfig> strategyConfigs = new ArrayList<>();
    strategyConfigs.add(strategyConfig);
    strategyConfig.setId("id-123");
    final StrategyConfigRepository strategyConfigRepository =
        EasyMock.createMock(StrategyConfigRepository.class);
    expect(strategyConfigRepository.findAll()).andReturn(strategyConfigs);
    replay(strategyConfigRepository);

    final StrategyConfigServiceImpl strategyConfigService =
        new StrategyConfigServiceImpl(strategyConfigRepository);

    assertThat(strategyConfigService.getAllStrategyConfig()).isEqualTo(strategyConfigs);
    verify(strategyConfigRepository);
  }

  @Test
  void whenUpdateStrategyConfigCalledThenExpectUpdatedConfigToBeReturned() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    strategyConfig.setId("id-123");
    final StrategyConfigRepository strategyConfigRepository =
        EasyMock.createMock(StrategyConfigRepository.class);
    expect(strategyConfigRepository.save(strategyConfig)).andReturn(strategyConfig);
    replay(strategyConfigRepository);

    final StrategyConfigServiceImpl strategyConfigService =
        new StrategyConfigServiceImpl(strategyConfigRepository);

    assertThat(strategyConfigService.updateStrategyConfig(strategyConfig))
        .isEqualTo(strategyConfig);
    verify(strategyConfigRepository);
  }

  @Test
  void whenDeleteStrategyConfigCalledThenExpectDeletedConfigToBeReturned() {
    final StrategyConfig strategyConfig = new StrategyConfig();
    strategyConfig.setId("id-123");
    final StrategyConfigRepository strategyConfigRepository =
        EasyMock.createMock(StrategyConfigRepository.class);
    expect(strategyConfigRepository.delete(strategyConfig.getId())).andReturn(strategyConfig);
    replay(strategyConfigRepository);

    final StrategyConfigServiceImpl strategyConfigService =
        new StrategyConfigServiceImpl(strategyConfigRepository);

    assertThat(strategyConfigService.deleteStrategyConfig(strategyConfig.getId()))
        .isEqualTo(strategyConfig);
    verify(strategyConfigRepository);
  }
}
