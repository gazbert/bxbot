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

package com.gazbert.bxbot.rest.api.v1.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.services.config.EmailAlertsConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the Email Alerts config controller behaviour.
 * * 测试电子邮件警报配置控制器的行为。
 *
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TestEmailAlertsConfigController extends AbstractConfigControllerTest {

  private static final String EMAIL_ALERTS_CONFIG_ENDPOINT_URI =
      CONFIG_ENDPOINT_BASE_URI + "/email-alerts";

  private static final boolean ENABLED = true;
  private static final String HOST = "smtp.host.deathstar.com";
  private static final int TLS_PORT = 573;
  private static final String ACCOUNT_USERNAME = "boba@google.com";
  private static final String ACCOUNT_PASSWORD = "bounty";
  private static final String FROM_ADDRESS = "boba.fett@Mandalore.com";
  private static final String TO_ADDRESS = "darth.vader@deathstar.com";

  @MockBean private EmailAlertsConfigService emailAlertsConfigService;

  // Need these even though not used in the test directly because Spring loads it on startup...
  // 需要这些，即使没有直接在测试中使用，因为 Spring 在启动时加载它...
  @MockBean private TradingEngine tradingEngine;
  @MockBean private RestartEndpoint restartEndpoint;
  @MockBean private LogFileWebEndpoint logFileWebEndpoint;
  @MockBean private AuthenticationManager authenticationManager;

  @BeforeEach
  void setupBeforeEachTest() {
    mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
  }

  @Test
  void testGetEmailAlertsConfigWithValidToken() throws Exception {
    given(emailAlertsConfigService.getEmailAlertsConfig()).willReturn(someEmailAlertsConfig());

    mockMvc
        .perform(
            get(EMAIL_ALERTS_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.smtpConfig.host").value(HOST))
        .andExpect(jsonPath("$.smtpConfig.tlsPort").value(TLS_PORT))
        .andExpect(jsonPath("$.enabled").value(ENABLED))
        .andExpect(jsonPath("$.smtpConfig.fromAddress").value(FROM_ADDRESS))
        .andExpect(jsonPath("$.smtpConfig.toAddress").value(TO_ADDRESS))
        .andExpect(jsonPath("$.smtpConfig.accountUsername").value(ACCOUNT_USERNAME))
        .andExpect(jsonPath("$.smtpConfig.accountPassword").value(ACCOUNT_PASSWORD));

    verify(emailAlertsConfigService, atLeastOnce()).getEmailAlertsConfig();
  }

  @Test
  void testGetEmailAlertsConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(get(EMAIL_ALERTS_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetEmailAlertsConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(EMAIL_ALERTS_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateEmailAlertsConfigWithAdminTokenAuthorized() throws Exception {
    given(emailAlertsConfigService.updateEmailAlertsConfig(any()))
        .willReturn(someEmailAlertsConfig());

    mockMvc
        .perform(
            put(EMAIL_ALERTS_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someEmailAlertsConfig())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.smtpConfig.host").value(HOST))
        .andExpect(jsonPath("$.smtpConfig.tlsPort").value(TLS_PORT))
        .andExpect(jsonPath("$.enabled").value(ENABLED))
        .andExpect(jsonPath("$.smtpConfig.fromAddress").value(FROM_ADDRESS))
        .andExpect(jsonPath("$.smtpConfig.toAddress").value(TO_ADDRESS))
        .andExpect(jsonPath("$.smtpConfig.accountUsername").value(ACCOUNT_USERNAME))
        .andExpect(jsonPath("$.smtpConfig.accountPassword").value(ACCOUNT_PASSWORD));

    verify(emailAlertsConfigService, times(1)).updateEmailAlertsConfig(any());
  }

  @Test
  void testUpdateEmailAlertsConfigWithUserTokenForbidden() throws Exception {
    given(emailAlertsConfigService.updateEmailAlertsConfig(any()))
        .willReturn(someEmailAlertsConfig());

    mockMvc
        .perform(
            put(EMAIL_ALERTS_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someEmailAlertsConfig())))
        .andExpect(status().isForbidden());

    verify(emailAlertsConfigService, times(0)).updateEmailAlertsConfig(any());
  }

  @Test
  void testUpdateEmailAlertsConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(put(EMAIL_ALERTS_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateEmailAlertsConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            put(EMAIL_ALERTS_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  // --------------------------------------------------------------------------
  // Private utils
  // 私有工具
  // --------------------------------------------------------------------------

  private static EmailAlertsConfig someEmailAlertsConfig() {
    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    final SmtpConfig smtpConfig =
        new SmtpConfig(
            HOST, TLS_PORT, ACCOUNT_USERNAME, ACCOUNT_PASSWORD, FROM_ADDRESS, TO_ADDRESS);
    emailAlertsConfig.setSmtpConfig(smtpConfig);
    emailAlertsConfig.setEnabled(true);
    return emailAlertsConfig;
  }
}
