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
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Tests Bot logfile service behaves as expected.
 * 测试 Bot 日志文件服务按预期运行。
 *
 * @author gazbert
 */
class TestBotLogfileService {

  @Test
  void whenGetLogfileCalledThenExpectLogfileContentToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get()  4981 [主要] 2019-07-20 17:30:20,429 信息 EngineConfigYamlRepository get()"
            + "- Fetching EngineConfig... - 获取引擎配置..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() 4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get()  4983 [主要] 2019-07-20 17:30:22,429 信息 EngineConfigYamlRepository get()"
            + "- Config is good - 配置不错"
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
  void whenLogfileCalledWith2ThenExpectOnlyLast2LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() 4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get()  4983 [主要] 2019-07-20 17:30:22,429 信息 EngineConfigYamlRepository get()"
            + " - Config is good - 配置不错"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfile(2); // 2 lines only 仅 2 行

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  void whenLogfileTailCalledWith2ThenExpectOnlyLast2LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get()  4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get() 4983 [主要] 2019-07-20 17:30:22,429 信息 EngineConfigYamlRepository get()"
            + "- Config is good - 配置不错"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileTail(2); // tail 2 lines only 仅尾部 2 行

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  void whenLogfileTailCalledWith4ThenExpectOnly3LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get()  4981 [主要] 2019-07-20 17:30:20,429 信息 EngineConfigYamlRepository get()"
            + "- Fetching EngineConfig... - 获取引擎配置..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get()  4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get()  4983 [主要] 2019-07-20 17:30:22,429 信息 EngineConfigYamlRepository get()"
            + " - Config is good - 配置不错"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileTail(4); // attempt 4 lines // 尝试 4 行

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent); // expect last 3 期待最后 3
    verify(logFileWebEndpoint);
  }

  @Test
  void whenLogfileHeadCalledWith2ThenExpectOnlyLast2LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() 4981 [主要] 2019-07-20 17:30:20,429 信息 EngineConfigYamlRepository get()"
            + "- Fetching EngineConfig... - 获取引擎配置..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get()  4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileHead(2); // head 2 lines only 仅头 2 行

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent);
    verify(logFileWebEndpoint);
  }

  @Test
  void whenLogfileHeadCalledWith4ThenExpectOnly3LinesToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get()  4981 [主要] 2019-07-20 17:30:20,429 信息 EngineConfigYamlRepository get()"
            + "- Fetching EngineConfig... - 获取引擎配置..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get() 4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get()  4983 [主要] 2019-07-20 17:30:22,429 信息 EngineConfigYamlRepository get()"
            + "- Config is good - 配置不错"
            + System.lineSeparator();

    final Path path = FileSystems.getDefault().getPath(logfilePath);
    final Resource resource = new FileSystemResource(path);
    final LogFileWebEndpoint logFileWebEndpoint = EasyMock.createMock(LogFileWebEndpoint.class);

    expect(logFileWebEndpoint.logFile()).andReturn(resource);
    replay(logFileWebEndpoint);

    final BotLogfileServiceImpl botLogfileService = new BotLogfileServiceImpl(logFileWebEndpoint);
    final String fetchedLogfile = botLogfileService.getLogfileHead(4); // attempt 4 lines 尝试 4 行

    assertThat(fetchedLogfile).isEqualTo(expectedLogfileContent); // expect first 3 期待前 3
    verify(logFileWebEndpoint);
  }

  @Test
  void whenGetLogfileAsResourceCalledThenExpectLogfileToBeReturned() throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";
    final String expectedLogfileContent =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get()  4981 [主要] 2019-07-20 17:30:20,429 信息 EngineConfigYamlRepository get()"
            + "- Fetching EngineConfig... - 获取引擎配置..."
            + System.lineSeparator()
            + "4982 [main] 2019-07-20 17:30:21,429 INFO  EngineConfigYamlRepository get()  4982 [主要] 2019-07-20 17:30:21,429 信息 EngineConfigYamlRepository get()"
            + "- Validating config... - 验证配置..."
            + System.lineSeparator()
            + "4983 [main] 2019-07-20 17:30:22,429 INFO  EngineConfigYamlRepository get()  4983 [主要] 2019-07-20 17:30:22,429 信息 EngineConfigYamlRepository get()"
            + "- Config is good - 配置不错";

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
  void whenGetLogfileAsResourceCalledAndMaxSizeExceededThenExpectLogfileToBeTruncated()
      throws Exception {
    final String logfilePath = "src/test/logfiles/logfile.log";

    final String firstLineOfLogfile =
        "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get()  4981 [主要] 2019-07-20 17:30:20,429 信息 EngineConfigYamlRepository get()"
            + "- Fetching EngineConfig... - 获取引擎配置...";

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
