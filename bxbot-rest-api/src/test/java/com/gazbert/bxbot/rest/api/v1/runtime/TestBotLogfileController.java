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

package com.gazbert.bxbot.rest.api.v1.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.services.runtime.BotLogfileService;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the Bot Logfile controller behaviour.
 ** 测试 Bot Logfile 控制器行为。
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TestBotLogfileController extends AbstractRuntimeControllerTest {

  private static final String LOGFILE_ENDPOINT_URI = RUNTIME_ENDPOINT_BASE_URI + "/logfile";
  private static final String LOGFILE_DOWNLOAD_URI = LOGFILE_ENDPOINT_URI + "/download";

  // This value must be the same as maxLogfileLines in test/resources/application.properties
  // 此值必须与 test/resources/application.properties 中的 maxLogfileLines 相同
  private static final int MAX_LOGFILE_LINES = 2;

  // This value must be the same as maxLogfileDownloadSize in test/resources/application.properties
  // 此值必须与 test/resources/application.properties 中的 maxLogfileDownloadSize 相同
  private static final int MAX_LOGFILE_DOWNLOAD_SIZE = 100;

  private static final String LOGFILE_LINE_1 = "4981 [main] 2019-07-20 17:30:20,429 INFO  Line 1";
  private static final String LOGFILE_LINE_2 = "4482 [main] 2019-07-20 17:30:21,429 INFO  Line 2";
  private static final String LOGFILE_LINE_3 = "4483 [main] 2019-07-20 17:30:22,429 INFO  Line 3";
  private static final String LOGFILE = LOGFILE_LINE_1 + LOGFILE_LINE_2 + LOGFILE_LINE_3;

  @MockBean private BotLogfileService botLogfileService;

  // Need these even though not used in the test directly because Spring loads it on startup...
  // 需要这些，即使没有直接在测试中使用，因为 Spring 在启动时加载它...
  @MockBean private TradingEngine tradingEngine;
  @MockBean private EmailAlerter emailAlerter;
  @MockBean private RestartEndpoint restartEndpoint;
  @MockBean private AuthenticationManager authenticationManager;

  @BeforeEach
  void setupBeforeEachTest() {
    mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
  }

  @Test
  void testDownloadLogfile() throws Exception {
    final Resource resource = new ByteArrayResource(LOGFILE.getBytes(Charset.forName("UTF-8")));
    given(botLogfileService.getLogfileAsResource(MAX_LOGFILE_DOWNLOAD_SIZE)).willReturn(resource);

    mockMvc
        .perform(
            get(LOGFILE_DOWNLOAD_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/octet-stream"))
        .andExpect(jsonPath("$").value(LOGFILE));

    verify(botLogfileService, times(1)).getLogfileAsResource(MAX_LOGFILE_DOWNLOAD_SIZE);
  }

  @Test
  void testDownloadLogfileReturnsInternalServerErrorForIoException() throws Exception {
    given(botLogfileService.getLogfileAsResource(MAX_LOGFILE_DOWNLOAD_SIZE))
        .willThrow(new IOException("Oops!"));

    mockMvc
        .perform(
            get(LOGFILE_DOWNLOAD_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().is5xxServerError());

    verify(botLogfileService, times(1)).getLogfileAsResource(MAX_LOGFILE_DOWNLOAD_SIZE);
  }

  @Test
  void testGetLogfile() throws Exception {
    given(botLogfileService.getLogfile(MAX_LOGFILE_LINES))
        .willReturn(LOGFILE_LINE_1 + System.lineSeparator() + LOGFILE_LINE_2);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_1 + System.lineSeparator() + LOGFILE_LINE_2));

    verify(botLogfileService, times(1)).getLogfile(MAX_LOGFILE_LINES);
  }

  @Test
  void testGetLogfileHead() throws Exception {
    final int headLineCount = 1;
    given(botLogfileService.getLogfileHead(headLineCount)).willReturn(LOGFILE_LINE_1);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI + "?head=" + headLineCount)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_1));

    verify(botLogfileService, times(1)).getLogfileHead(headLineCount);
  }

  @Test
  void testGetLogfileHeadWithTailParamSetToZero() throws Exception {
    final int headLineCount = 0;
    given(botLogfileService.getLogfile(MAX_LOGFILE_LINES))
        .willReturn(LOGFILE_LINE_2 + System.lineSeparator() + LOGFILE_LINE_3);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI + "?head=" + headLineCount)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_2 + System.lineSeparator() + LOGFILE_LINE_3));

    verify(botLogfileService, times(1)).getLogfile(MAX_LOGFILE_LINES);
  }

  @Test
  void testGetLogfileHeadWhenRequestedLineCountExceedsMaxAllowed() throws Exception {
    final int headLineCount = MAX_LOGFILE_LINES + 1;
    given(botLogfileService.getLogfileHead(MAX_LOGFILE_LINES))
        .willReturn(LOGFILE_LINE_1 + System.lineSeparator() + LOGFILE_LINE_2);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI + "?head=" + headLineCount)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_1 + System.lineSeparator() + LOGFILE_LINE_2));

    verify(botLogfileService, times(1)).getLogfileHead(MAX_LOGFILE_LINES);
  }

  @Test
  void testGetLogfileTail() throws Exception {
    final int tailLineCount = 1;
    given(botLogfileService.getLogfileTail(tailLineCount)).willReturn(LOGFILE_LINE_3);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI + "?tail=" + tailLineCount)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_3));

    verify(botLogfileService, times(1)).getLogfileTail(tailLineCount);
  }

  @Test
  void testGetLogfileTailWithTailParamSetToZero() throws Exception {
    final int tailLineCount = 0;
    given(botLogfileService.getLogfile(MAX_LOGFILE_LINES))
        .willReturn(LOGFILE_LINE_2 + System.lineSeparator() + LOGFILE_LINE_3);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI + "?tail=" + tailLineCount)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_2 + System.lineSeparator() + LOGFILE_LINE_3));

    verify(botLogfileService, times(1)).getLogfile(MAX_LOGFILE_LINES);
  }

  @Test
  void testGetLogfileTailWhenRequestedLineCountExceedsMaxAllowed() throws Exception {
    final int tailLineCount = MAX_LOGFILE_LINES + 1;
    given(botLogfileService.getLogfileTail(MAX_LOGFILE_LINES))
        .willReturn(LOGFILE_LINE_2 + System.lineSeparator() + LOGFILE_LINE_3);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI + "?tail=" + tailLineCount)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE_LINE_2 + System.lineSeparator() + LOGFILE_LINE_3));

    verify(botLogfileService, times(1)).getLogfileTail(MAX_LOGFILE_LINES);
  }

  @Test
  void testGetLogfileReturnsInternalServerErrorForIoException() throws Exception {
    given(botLogfileService.getLogfile(MAX_LOGFILE_LINES))
        .willThrow(new IOException("Something bad happened!"));

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().is5xxServerError());

    verify(botLogfileService, times(1)).getLogfile(MAX_LOGFILE_LINES);
  }

  @Test
  void testGetLogfileWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetLogfileWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(get(LOGFILE_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
