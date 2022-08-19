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

import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.MarketConfigRepository;
import java.util.ArrayList;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

/**
 * Tests Market configuration service behaves as expected.
 * * 测试市场配置服务按预期运行。
 *
 * @author gazbert
 */
class TestMarketConfigService {

  @Test
  void whenGetMarketConfigCalledThenExpectConfigToBeReturned() {
    final MarketConfig marketConfig = new MarketConfig();
    marketConfig.setId("id-123");
    final MarketConfigRepository marketConfigRepository =
        EasyMock.createMock(MarketConfigRepository.class);
    expect(marketConfigRepository.findById(marketConfig.getId())).andReturn(marketConfig);
    replay(marketConfigRepository);

    final MarketConfigServiceImpl marketConfigService =
        new MarketConfigServiceImpl(marketConfigRepository);

    assertThat(marketConfigService.getMarketConfig(marketConfig.getId())).isEqualTo(marketConfig);
    verify(marketConfigRepository);
  }

  @Test
  void whenGetAllMarketConfigCalledThenExpectConfigToBeReturned() {
    final MarketConfig marketConfig = new MarketConfig();
    final List<MarketConfig> marketConfigs = new ArrayList<>();
    marketConfigs.add(marketConfig);
    marketConfig.setId("id-123");
    final MarketConfigRepository marketConfigRepository =
        EasyMock.createMock(MarketConfigRepository.class);
    expect(marketConfigRepository.findAll()).andReturn(marketConfigs);
    replay(marketConfigRepository);

    final MarketConfigServiceImpl marketConfigService =
        new MarketConfigServiceImpl(marketConfigRepository);

    assertThat(marketConfigService.getAllMarketConfig()).isEqualTo(marketConfigs);
    verify(marketConfigRepository);
  }

  @Test
  void whenUpdateMarketConfigCalledThenExpectUpdatedConfigToBeReturned() {
    final MarketConfig marketConfig = new MarketConfig();
    marketConfig.setId("id-123");
    final MarketConfigRepository marketConfigRepository =
        EasyMock.createMock(MarketConfigRepository.class);
    expect(marketConfigRepository.save(marketConfig)).andReturn(marketConfig);
    replay(marketConfigRepository);

    final MarketConfigServiceImpl marketConfigService =
        new MarketConfigServiceImpl(marketConfigRepository);

    assertThat(marketConfigService.updateMarketConfig(marketConfig)).isEqualTo(marketConfig);
    verify(marketConfigRepository);
  }

  @Test
  void whenDeleteMarketConfigCalledThenExpectDeletedConfigToBeReturned() {
    final MarketConfig marketConfig = new MarketConfig();
    marketConfig.setId("id-123");
    final MarketConfigRepository marketConfigRepository =
        EasyMock.createMock(MarketConfigRepository.class);
    expect(marketConfigRepository.delete(marketConfig.getId())).andReturn(marketConfig);
    replay(marketConfigRepository);

    final MarketConfigServiceImpl marketConfigService =
        new MarketConfigServiceImpl(marketConfigRepository);

    assertThat(marketConfigService.deleteMarketConfig(marketConfig.getId()))
        .isEqualTo(marketConfig);
    verify(marketConfigRepository);
  }
}
