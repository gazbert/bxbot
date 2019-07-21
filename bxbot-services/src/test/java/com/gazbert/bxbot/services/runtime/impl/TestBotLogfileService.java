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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.core.io.Resource;

/**
 * Tests Bot logfile service behaves as expected.
 *
 * @author gazbert
 */
public class TestBotLogfileService {

  @Test
  public void whenGetLogfileCalledThenExpectLogfileContentToBeReturned() throws Exception {
    final String logfile =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() - Fetching EngineConfig...";

    final InputStream inputStream = EasyMock.createMock(InputStream.class);
    final Resource resource = EasyMock.createMock(Resource.class);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    expect(resource.getInputStream()).andReturn(inputStream);
    expect(inputStream.readAllBytes()).andReturn(logfile.getBytes(StandardCharsets.UTF_8));

    replay(resource);
    replay(inputStream);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfile();

    assertThat(fetchedLogfile).isEqualTo(logfile);

    verify(resource);
    verify(inputStream);
    verify(logFileWebEndpoint);
  }
}
