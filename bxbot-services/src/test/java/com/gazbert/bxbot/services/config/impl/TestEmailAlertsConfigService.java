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

import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.repository.EmailAlertsConfigRepository;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

/**
 * Tests Email Alerts configuration service behaves as expected.
 * * 测试电子邮件警报配置服务的行为是否符合预期。
 *
 * @author gazbert
 */
class TestEmailAlertsConfigService {

  @Test
  void whenGetEmailAlertsConfigCalledThenExpectConfigToBeReturned() {
    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    final EmailAlertsConfigRepository emailAlertsConfigRepository =
        EasyMock.createMock(EmailAlertsConfigRepository.class);
    expect(emailAlertsConfigRepository.get()).andReturn(emailAlertsConfig);
    replay(emailAlertsConfigRepository);

    final EmailAlertsConfigServiceImpl emailAlertsConfigService =
        new EmailAlertsConfigServiceImpl(emailAlertsConfigRepository);

    assertThat(emailAlertsConfigService.getEmailAlertsConfig()).isEqualTo(emailAlertsConfig);
    verify(emailAlertsConfigRepository);
  }

  @Test
  void whenUpdateEmailAlertsConfigCalledThenExpectUpdatedConfigToBeReturned() {
    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    final EmailAlertsConfigRepository emailAlertsConfigRepository =
        EasyMock.createMock(EmailAlertsConfigRepository.class);
    expect(emailAlertsConfigRepository.save(emailAlertsConfig)).andReturn(emailAlertsConfig);
    replay(emailAlertsConfigRepository);

    final EmailAlertsConfigServiceImpl emailAlertsConfigService =
        new EmailAlertsConfigServiceImpl(emailAlertsConfigRepository);

    assertThat(emailAlertsConfigService.updateEmailAlertsConfig(emailAlertsConfig))
        .isEqualTo(emailAlertsConfig);
    verify(emailAlertsConfigRepository);
  }
}
