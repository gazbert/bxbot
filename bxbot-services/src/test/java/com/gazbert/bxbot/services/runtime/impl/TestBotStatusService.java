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

package com.gazbert.bxbot.services.runtime.impl;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

/**
 * Tests Bot status service behaves as expected.
 *  测试机器人状态服务按预期运行。
 * @author gazbert
 */
class TestBotStatusService {

  @Test
  void whenGetStatusCalledThenExpectBotStatusToBeReturned() {
    final String botStatus = "UP";
    final Status upStatus = new Status(botStatus);
    final HealthComponent healthComponent = EasyMock.createMock(HealthComponent.class);
    final HealthEndpoint healthEndpoint = EasyMock.createMock(HealthEndpoint.class);

    expect(healthEndpoint.health()).andReturn(healthComponent);
    expect(healthComponent.getStatus()).andReturn(upStatus);
    replay(healthEndpoint);
    replay(healthComponent);

    final BotStatusServiceImpl botStatusService = new BotStatusServiceImpl(healthEndpoint);
    final String fetchedBotStatus = botStatusService.getStatus();

    assertThat(fetchedBotStatus).isEqualTo(botStatus);

    verify(healthEndpoint);
    verify(healthComponent);
  }
}
