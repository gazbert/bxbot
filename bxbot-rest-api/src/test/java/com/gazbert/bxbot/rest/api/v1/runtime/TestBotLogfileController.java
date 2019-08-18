/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.services.runtime.BotLogfileService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the Bot Logfile controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestBotLogfileController extends AbstractRuntimeControllerTest {

  private static final String LOGFILE_ENDPOINT_URI = RUNTIME_ENDPOINT_BASE_URI + "/logfile";

  private static final int MAX_LOGFILE_LINES = 1000;
  private static final String LOGFILE =
      "4981 [main] 2019-07-20 17:30:20,429 INFO  EngineConfigYamlRepository get() - "
          + "Fetching EngineConfig...";

  @MockBean private BotLogfileService botLogfileService;

  // Need these even though not used in the test directly because Spring loads it on startup...
  @MockBean private TradingEngine tradingEngine;
  @MockBean private EmailAlerter emailAlerter;
  @MockBean private RestartEndpoint restartEndpoint;

  @Before
  public void setupBeforeEachTest() {
    mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
  }

  @Ignore("FIXME: Ignore for now - code still being developed")
  @Test
  public void testGetBotLogfile() throws Exception {
    given(botLogfileService.getLogfile(MAX_LOGFILE_LINES)).willReturn(LOGFILE);

    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI)
                .header(
                    "Authorization",
                    buildAuthorizationHeaderValue(VALID_USER_LOGIN_ID, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(LOGFILE));

    verify(botLogfileService, times(1)).getLogfile(MAX_LOGFILE_LINES);
  }

  @Test
  public void testGetBotLogfileWhenUnauthorizedWithBadCredentials() throws Exception {
    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI)
                .header(
                    "Authorization",
                    buildAuthorizationHeaderValue(VALID_USER_LOGIN_ID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testGetBotLogfileWhenUnauthorizedWithMissingCredentials() throws Exception {
    mockMvc
        .perform(
            get(LOGFILE_ENDPOINT_URI)
                .header(
                    "Authorization",
                    buildAuthorizationHeaderValue(VALID_USER_LOGIN_ID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
