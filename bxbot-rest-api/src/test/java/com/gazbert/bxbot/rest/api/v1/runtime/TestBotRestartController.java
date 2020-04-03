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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.services.runtime.BotRestartService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the Bot restart controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestBotRestartController extends AbstractRuntimeControllerTest {

  private static final String RESTART_ENDPOINT_URI = RUNTIME_ENDPOINT_BASE_URI + "/restart";
  private static final String BOT_STATUS = "restarting";

  @MockBean private BotRestartService botRestartService;

  // Need these even though not used in the test directly because Spring loads it on startup...
  @MockBean private TradingEngine tradingEngine;
  @MockBean private EmailAlerter emailAlerter;
  @MockBean private RestartEndpoint restartEndpoint;
  @MockBean private LogFileWebEndpoint logFileWebEndpoint;
  @MockBean private AuthenticationManager authenticationManager;

  @Before
  public void setupBeforeEachTest() {
    mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
  }

  @Test
  public void testBotRestartWithAdminTokenAuthorized() throws Exception {
    given(botRestartService.restart()).willReturn(BOT_STATUS);

    mockMvc
        .perform(
            post(RESTART_ENDPOINT_URI)
                .header(
                    "Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(BOT_STATUS));

    verify(botRestartService, times(1)).restart();
  }

  @Test
  public void testBotRestartWithUserTokenForbidden() throws Exception {
    given(botRestartService.restart()).willReturn(BOT_STATUS);

    mockMvc
        .perform(
            post(RESTART_ENDPOINT_URI)
                .header(
                    "Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isForbidden());

    verify(botRestartService, times(0)).restart();
  }

  @Test
  public void testBotRestartWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(RESTART_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testBotRestartWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(get(RESTART_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
