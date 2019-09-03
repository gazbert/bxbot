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

import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Tests Bot logfile service behaves as expected.
 *
 * @author gazbert
 */
public class TestBotLogfileService {

  @Test
  public void whenGetLogfileCalledThenExpectLogfileContentToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() "
            + "- Fetching EngineConfig..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() "
            + "- Config is good"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfile(3);

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenLogfileCalledWith2ThenExpectOnlyLast2LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() "
            + "- Config is good"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfile(2); // 2 lines only

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenLogfileTailCalledWith2ThenExpectOnlyLast2LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() "
            + "- Config is good"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileTail(2); // tail 2 lines only

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenLogfileTailCalledWith4ThenExpectOnly3LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() "
            + "- Fetching EngineConfig..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() "
            + "- Config is good"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileTail(4); // attempt 4 lines

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent); // expect last 3
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenLogfileHeadCalledWith2ThenExpectOnlyLast2LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() "
            + "- Fetching EngineConfig..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileHead(2); // head 2 lines only

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenLogfileHeadCalledWith4ThenExpectOnly3LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() "
            + "- Fetching EngineConfig..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() "
            + "- Config is good"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileHead(4); // attempt 4 lines

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent); // expect first 3
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenGetLogfileAsResourceCalledThenExpectLogfileToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() "
            + "- Fetching EngineConfig..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() "
            + "- Validating config..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() "
            + "- Config is good";

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final int maxLogfileSizeInBytes = 1024;
    final Resource logfileAsResource =
        botLogfileService.getLogfileAsResource(maxLogfileSizeInBytes);
    final byte[] logfileInBytes = logfileAsResource.getInputStream().readAllBytes();

    assertThat(new String(logfileInBytes, Charset.forName("UTF-8")))
        .isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  public void whenGetLogfileAsResourceCalledAndMaxSizeExceededThenExpectLogfileToBeTruncated()
      throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";

    final String firstLineOfLogfile =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() "
            + "- Fetching EngineConfig...";

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final int maxLogfileSizeInBytes = firstLineOfLogfile.length();
    final Resource logfileAsResource =
        botLogfileService.getLogfileAsResource(maxLogfileSizeInBytes);
    final byte[] logfileInBytes = logfileAsResource.getInputStream().readAllBytes();

    assertThat(new String(logfileInBytes, Charset.forName("UTF-8"))).isEqualTo(firstLineOfLogfile);
    verify(logFileWebEndpoint);
  }
}
