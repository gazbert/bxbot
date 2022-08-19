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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.services.config.EngineConfigService;
import java.math.BigDecimal;
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
 * Tests the Engine config controller behaviour.
 * * 测试引擎配置控制器的行为。
 *
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TestEngineConfigController extends AbstractConfigControllerTest {

  private static final String ENGINE_CONFIG_ENDPOINT_URI = CONFIG_ENDPOINT_BASE_URI + "/engine";

  private static final String BOT_ID = "avro-707_1";
  private static final String BOT_NAME = "Avro 707";
  private static final String ENGINE_EMERGENCY_STOP_CURRENCY = "BTC";
  private static final BigDecimal ENGINE_EMERGENCY_STOP_BALANCE = new BigDecimal("0.9232320");
  private static final int ENGINE_TRADE_CYCLE_INTERVAL = 60;

  @MockBean private EngineConfigService engineConfigService;

  // Need these even though not used in the test directly because Spring loads it on startup...
  // 需要这些，即使没有直接在测试中使用，因为 Spring 在启动时加载它...
  @MockBean private TradingEngine tradingEngine;
  @MockBean private EmailAlerter emailAlerter;
  @MockBean private RestartEndpoint restartEndpoint;
  @MockBean private LogFileWebEndpoint logFileWebEndpoint;
  @MockBean private AuthenticationManager authenticationManager;

  @BeforeEach
  void setupBeforeEachTest() {
    mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
  }

  @Test
  void testGetEngineConfigWithValidToken() throws Exception {
    given(engineConfigService.getEngineConfig()).willReturn(someEngineConfig());

    mockMvc
        .perform(
            get(ENGINE_CONFIG_ENDPOINT_URI)
                .header(
                    "Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.botId").value(BOT_ID))
        .andExpect(jsonPath("$.botName").value(BOT_NAME))
        .andExpect(jsonPath("$.emergencyStopCurrency").value(ENGINE_EMERGENCY_STOP_CURRENCY))
        .andExpect(
            jsonPath("$.emergencyStopBalance").value(ENGINE_EMERGENCY_STOP_BALANCE.doubleValue()))
        .andExpect(jsonPath("$.tradeCycleInterval").value(ENGINE_TRADE_CYCLE_INTERVAL));

    verify(engineConfigService, times(1)).getEngineConfig();
  }

  @Test
  void testGetEngineConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(get(ENGINE_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetEngineConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(ENGINE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateEngineConfigWithAdminTokenAuthorized() throws Exception {
    given(engineConfigService.updateEngineConfig(any())).willReturn(someEngineConfig());

    mockMvc
        .perform(
            put(ENGINE_CONFIG_ENDPOINT_URI)
                .header(
                    "Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someEngineConfig())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.botId").value(BOT_ID))
        .andExpect(jsonPath("$.botName").value(BOT_NAME))
        .andExpect(jsonPath("$.emergencyStopCurrency").value(ENGINE_EMERGENCY_STOP_CURRENCY))
        .andExpect(
            jsonPath("$.emergencyStopBalance").value(ENGINE_EMERGENCY_STOP_BALANCE.doubleValue()))
        .andExpect(jsonPath("$.tradeCycleInterval").value(ENGINE_TRADE_CYCLE_INTERVAL));

    verify(engineConfigService, times(1)).updateEngineConfig(any());
  }

  @Test
  void testUpdateEngineConfigWithUserTokenForbidden() throws Exception {
    given(engineConfigService.updateEngineConfig(any())).willReturn(someEngineConfig());

    mockMvc
        .perform(
            put(ENGINE_CONFIG_ENDPOINT_URI)
                .header(
                    "Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someEngineConfig())))
        .andExpect(status().isForbidden());

    verify(engineConfigService, times(0)).updateEngineConfig(any());
  }

  @Test
  void testUpdateEngineConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(put(ENGINE_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateEngineConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            put(ENGINE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  // --------------------------------------------------------------------------
  // Private utils
  // 私有工具
  // --------------------------------------------------------------------------

  private static EngineConfig someEngineConfig() {
    final EngineConfig engineConfig = new EngineConfig();
    engineConfig.setBotId(BOT_ID);
    engineConfig.setBotName(BOT_NAME);
    engineConfig.setEmergencyStopCurrency(ENGINE_EMERGENCY_STOP_CURRENCY);
    engineConfig.setEmergencyStopBalance(ENGINE_EMERGENCY_STOP_BALANCE);
    engineConfig.setTradeCycleInterval(ENGINE_TRADE_CYCLE_INTERVAL);
    return engineConfig;
  }
}
