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

import java.util.Collections;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.restart.RestartEndpoint;

/**
 * Tests Bot restart service behaves as expected.
 ** 测试 Bot 重启服务的行为是否符合预期。
 * @author gazbert
 */
class TestBotRestartService {

  @Test
  void whenRestartCalledThenExpectBotStatusToBeRestarting() {
    final Map restartingStatus = Collections.singletonMap("message", "Restarting");
    final RestartEndpoint restartEndpoint = EasyMock.createMock(RestartEndpoint.class);
    expect(restartEndpoint.restart()).andReturn(restartingStatus);
    replay(restartEndpoint);

    final BotRestartServiceImpl botRestartService = new BotRestartServiceImpl(restartEndpoint);
    final String status = botRestartService.restart();

    assertThat(status).isEqualTo(restartingStatus.get(restartingStatus.keySet().iterator().next()));
    verify(restartEndpoint);
  }
}
