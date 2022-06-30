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

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.repository.ExchangeConfigRepository;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

/**
 * Tests Exchange configuration service behaves as expected.
 * * 测试 Exchange 配置服务的行为是否符合预期。
 *
 * @author gazbert
 */
class TestExchangeConfigService {

  @Test
  void whenGetExchangeConfigCalledThenExpectConfigToBeReturned() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    final ExchangeConfigRepository exchangeConfigRepository =
        EasyMock.createMock(ExchangeConfigRepository.class);
    expect(exchangeConfigRepository.get()).andReturn(exchangeConfig);
    replay(exchangeConfigRepository);

    final ExchangeConfigServiceImpl engineConfigService =
        new ExchangeConfigServiceImpl(exchangeConfigRepository);

    assertThat(engineConfigService.getExchangeConfig()).isEqualTo(exchangeConfig);
    verify(exchangeConfigRepository);
  }

  @Test
  void whenUpdateExchangeConfigCalledThenExpectUpdatedConfigToBeReturned() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    final ExchangeConfigRepository exchangeConfigRepository =
        EasyMock.createMock(ExchangeConfigRepository.class);
    expect(exchangeConfigRepository.save(exchangeConfig)).andReturn(exchangeConfig);
    replay(exchangeConfigRepository);

    final ExchangeConfigServiceImpl exchangeConfigService =
        new ExchangeConfigServiceImpl(exchangeConfigRepository);

    assertThat(exchangeConfigService.updateExchangeConfig(exchangeConfig))
        .isEqualTo(exchangeConfig);
    verify(exchangeConfigRepository);
  }
}
