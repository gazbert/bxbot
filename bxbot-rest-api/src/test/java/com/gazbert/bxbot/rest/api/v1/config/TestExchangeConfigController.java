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
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.services.config.ExchangeConfigService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Tests the Exchange config controller behaviour.
 * * 测试 Exchange 配置控制器的行为。
 *
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TestExchangeConfigController extends AbstractConfigControllerTest {

  private static final String EXCHANGE_CONFIG_ENDPOINT_URI = CONFIG_ENDPOINT_BASE_URI + "/exchange";

  private static final String EXCHANGE_NAME = "Bitstamp";
  private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";

  private static final Integer CONNECTION_TIMEOUT = 30;

  private static final int HTTP_STATUS_502 = 502;
  private static final int HTTP_STATUS_503 = 503;
  private static final int HTTP_STATUS_504 = 504;
  private static final List<Integer> NON_FATAL_ERROR_CODES =
      Arrays.asList(HTTP_STATUS_502, HTTP_STATUS_503, HTTP_STATUS_504);

  private static final String ERROR_MESSAGE_REFUSED = "Connection refused";
  private static final String ERROR_MESSAGE_RESET = "Connection reset";
  private static final String ERROR_MESSAGE_CLOSED =
      "Remote host closed connection during handshake";
  private static final List<String> NON_FATAL_ERROR_MESSAGES =
      Arrays.asList(ERROR_MESSAGE_REFUSED, ERROR_MESSAGE_RESET, ERROR_MESSAGE_CLOSED);

  private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
  private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";
  private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
  private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";

  @MockBean private ExchangeConfigService exchangeConfigService;

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
  void testGetExchangeConfigWithValidToken() throws Exception {
    given(exchangeConfigService.getExchangeConfig()).willReturn(someExchangeConfig());

    mockMvc
        .perform(
            get(EXCHANGE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(EXCHANGE_NAME))
        .andExpect(jsonPath("$.adapter").value(EXCHANGE_ADAPTER))

        // REST API does not expose AuthenticationConfig by design.
            // REST API 不按设计公开 AuthenticationConfig。
        .andExpect(jsonPath("$.authenticationConfig").doesNotExist())
        .andExpect(jsonPath("$.networkConfig.connectionTimeout").value(CONNECTION_TIMEOUT))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[0]").value(HTTP_STATUS_502))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[1]").value(HTTP_STATUS_503))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[2]").value(HTTP_STATUS_504))
        .andExpect(
            jsonPath("$.networkConfig.nonFatalErrorMessages[0]").value(ERROR_MESSAGE_REFUSED))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[1]").value(ERROR_MESSAGE_RESET))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[2]").value(ERROR_MESSAGE_CLOSED))
        .andExpect(jsonPath("$.otherConfig.buy-fee").value(BUY_FEE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.otherConfig.sell-fee").value(SELL_FEE_CONFIG_ITEM_VALUE));

    verify(exchangeConfigService, times(1)).getExchangeConfig();
  }

  @Test
  void testGetExchangeConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(get(EXCHANGE_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetExchangeConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(EXCHANGE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateExchangeConfigWithAdminTokenAuthorized() throws Exception {
    given(exchangeConfigService.getExchangeConfig()).willReturn(someExchangeConfig());
    given(exchangeConfigService.updateExchangeConfig(any())).willReturn(someExchangeConfig());

    mockMvc
        .perform(
            put(EXCHANGE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someExchangeConfig())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(EXCHANGE_NAME))
        .andExpect(jsonPath("$.adapter").value(EXCHANGE_ADAPTER))

        // REST API does not expose AuthenticationConfig by design.
            // REST API 不按设计公开 AuthenticationConfig。
        .andExpect(jsonPath("$.authenticationConfig").doesNotExist())
        .andExpect(jsonPath("$.networkConfig.connectionTimeout").value(CONNECTION_TIMEOUT))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[0]").value(HTTP_STATUS_502))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[1]").value(HTTP_STATUS_503))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[2]").value(HTTP_STATUS_504))
        .andExpect(
            jsonPath("$.networkConfig.nonFatalErrorMessages[0]").value(ERROR_MESSAGE_REFUSED))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[1]").value(ERROR_MESSAGE_RESET))
        .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[2]").value(ERROR_MESSAGE_CLOSED))
        .andExpect(jsonPath("$.otherConfig.buy-fee").value(BUY_FEE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.otherConfig.sell-fee").value(SELL_FEE_CONFIG_ITEM_VALUE));

    verify(exchangeConfigService, times(1)).getExchangeConfig();
    verify(exchangeConfigService, times(1)).updateExchangeConfig(any());
  }

  @Test
  void testUpdateExchangeConfigWithUserTokenForbidden() throws Exception {
    given(exchangeConfigService.getExchangeConfig()).willReturn(someExchangeConfig());
    given(exchangeConfigService.updateExchangeConfig(any())).willReturn(someExchangeConfig());

    mockMvc
        .perform(
            put(EXCHANGE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someExchangeConfig())))
        .andExpect(status().isForbidden());

    verify(exchangeConfigService, times(0)).getExchangeConfig();
    verify(exchangeConfigService, times(0)).updateExchangeConfig(any());
  }

  @Test
  void testUpdateExchangeConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(put(EXCHANGE_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateExchangeConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            put(EXCHANGE_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  // --------------------------------------------------------------------------
  // Private utils
  // 私有工具
  // --------------------------------------------------------------------------

  private static ExchangeConfig someExchangeConfig() {
    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

    final Map<String, String> otherConfig = new HashMap<>();
    otherConfig.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    otherConfig.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setNetworkConfig(networkConfig);
    exchangeConfig.setOtherConfig(otherConfig);
    // Don't include any AuthenticationConfig
    // 不要包含任何 AuthenticationConfig

    return exchangeConfig;
  }
}
