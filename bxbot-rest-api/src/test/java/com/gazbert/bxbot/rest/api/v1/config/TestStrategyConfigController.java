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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.services.config.StrategyConfigService;
import java.util.ArrayList;
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
 * Tests the Strategies config controller behaviour.
 * * 测试策略配置控制器的行为。
 *
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TestStrategyConfigController extends AbstractConfigControllerTest {

  private static final String STRATEGIES_CONFIG_ENDPOINT_URI =
      CONFIG_ENDPOINT_BASE_URI + "/strategies/";

  private static final String UNKNOWN_STRAT_ID = "unknown-id";

  private static final String STRAT_1_ID = "macd-long-position";
  private static final String STRAT_1_NAME = "MACD Strat Algo";
  private static final String STRAT_1_DESCRIPTION =
      "Uses MACD as indicator and takes long position in base currency.";
  private static final String STRAT_1_CLASSNAME = "com.gazbert.nova.algos.MacdLongBase";
  private static final String STRAT_1_BEAN_NAME = "macdLongBase";

  private static final String STRAT_2_ID = "long-scalper";
  private static final String STRAT_2_NAME = "Long Position Scalper Algo";
  private static final String STRAT_2_DESCRIPTION = "Scalps and goes long...";
  private static final String STRAT_2_CLASSNAME = "com.gazbert.nova.algos.LongScalper";
  private static final String STRAT_2_BEAN_NAME = "longScalper";

  private static final String BUY_PRICE_CONFIG_ITEM_KEY = "buy-price";
  private static final String BUY_PRICE_CONFIG_ITEM_VALUE = "671.15";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_KEY = "buy-amount";
  private static final String AMOUNT_TO_BUY_CONFIG_ITEM_VALUE = "0.5";

  @MockBean private StrategyConfigService strategyConfigService;

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
  void testGetAllStrategyConfigWithValidToken() throws Exception {
    given(strategyConfigService.getAllStrategyConfig()).willReturn(allTheStrategiesConfig());

    mockMvc
        .perform(
            get(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0].id").value(STRAT_1_ID))
        .andExpect(jsonPath("$.[0].name").value(STRAT_1_NAME))
        .andExpect(jsonPath("$.[0].description").value(STRAT_1_DESCRIPTION))
        .andExpect(jsonPath("$.[0].className").value(STRAT_1_CLASSNAME))
        .andExpect(jsonPath("$.[0].configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.[0].configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.[1].id").value(STRAT_2_ID))
        .andExpect(jsonPath("$.[1].name").value(STRAT_2_NAME))
        .andExpect(jsonPath("$.[1].description").value(STRAT_2_DESCRIPTION))
        .andExpect(jsonPath("$.[1].className").value(STRAT_2_CLASSNAME))
        .andExpect(jsonPath("$.[1].configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.[1].configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE));

    verify(strategyConfigService, times(1)).getAllStrategyConfig();
  }

  @Test
  void testGetAllStrategyConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(get(STRATEGIES_CONFIG_ENDPOINT_URI).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetAllStrategyConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetStrategyConfigWithValidToken() throws Exception {
    given(strategyConfigService.getStrategyConfig(STRAT_1_ID)).willReturn(someStrategyConfig());

    mockMvc
        .perform(
            get(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(STRAT_1_ID))
        .andExpect(jsonPath("$.name").value(STRAT_1_NAME))
        .andExpect(jsonPath("$.description").value(STRAT_1_DESCRIPTION))
        .andExpect(jsonPath("$.className").value(STRAT_1_CLASSNAME))
        .andExpect(jsonPath("$.configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE));

    verify(strategyConfigService, times(1)).getStrategyConfig(STRAT_1_ID);
  }

  @Test
  void testGetStrategyConfigByIdWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(
            get(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetStrategyConfigByIdWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            get(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetStrategyConfigByIdWhenNotRecognized() throws Exception {
    given(strategyConfigService.getStrategyConfig(UNKNOWN_STRAT_ID)).willReturn(null);

    mockMvc
        .perform(
            get(STRATEGIES_CONFIG_ENDPOINT_URI + UNKNOWN_STRAT_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void testUpdateStrategyConfigWithAdminTokenAuthorized() throws Exception {
    given(strategyConfigService.updateStrategyConfig(someStrategyConfig()))
        .willReturn(someStrategyConfig());

    mockMvc
        .perform(
            put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(STRAT_1_ID))
        .andExpect(jsonPath("$.name").value(STRAT_1_NAME))
        .andExpect(jsonPath("$.description").value(STRAT_1_DESCRIPTION))
        .andExpect(jsonPath("$.className").value(STRAT_1_CLASSNAME))
        .andExpect(jsonPath("$.configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE));

    verify(strategyConfigService, times(1)).updateStrategyConfig(any());
  }

  @Test
  void testUpdateStrategyConfigWithUserTokenForbidden() throws Exception {
    given(strategyConfigService.updateStrategyConfig(someStrategyConfig()))
        .willReturn(someStrategyConfig());

    mockMvc
        .perform(
            put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andExpect(status().isForbidden());

    verify(strategyConfigService, times(0)).updateStrategyConfig(any());
  }

  @Test
  void testUpdateStrategyConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(
            put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateStrategyConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUpdateStrategyConfigWhenIdNotRecognized() throws Exception {
    given(strategyConfigService.updateStrategyConfig(unrecognizedStrategyConfig()))
        .willReturn(null);

    mockMvc
        .perform(
            put(STRATEGIES_CONFIG_ENDPOINT_URI + UNKNOWN_STRAT_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(unrecognizedStrategyConfig())))
        .andExpect(status().isNotFound());
  }

  @Test
  void testUpdateStrategyConfigWhenIdIsMissing() throws Exception {
    mockMvc
        .perform(
            put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfigWithMissingId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testDeleteStrategyConfigWithAdminTokenAuthorized() throws Exception {
    given(strategyConfigService.deleteStrategyConfig(STRAT_1_ID)).willReturn(someStrategyConfig());

    mockMvc
        .perform(
            delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header(
                    "Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD)))
        .andExpect(status().isNoContent());

    verify(strategyConfigService, times(1)).deleteStrategyConfig(STRAT_1_ID);
  }

  @Test
  void testDeleteStrategyConfigWithUserTokenForbidden() throws Exception {
    given(strategyConfigService.deleteStrategyConfig(STRAT_1_ID)).willReturn(someStrategyConfig());

    mockMvc
        .perform(
            delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD)))
        .andExpect(status().isForbidden());

    verify(strategyConfigService, times(0)).deleteStrategyConfig(STRAT_1_ID);
  }

  @Test
  void testDeleteStrategyConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(
            delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testDeleteStrategyConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testDeleteStrategyConfigWhenIdNotRecognized() throws Exception {
    given(strategyConfigService.deleteStrategyConfig(UNKNOWN_STRAT_ID)).willReturn(null);

    mockMvc
        .perform(
            delete(STRATEGIES_CONFIG_ENDPOINT_URI + UNKNOWN_STRAT_ID)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void testCreateStrategyConfigWithAdminTokenAuthorized() throws Exception {
    given(strategyConfigService.createStrategyConfig(someStrategyConfig()))
        .willReturn(someStrategyConfig());

    mockMvc
        .perform(
            post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(STRAT_1_ID))
        .andExpect(jsonPath("$.name").value(STRAT_1_NAME))
        .andExpect(jsonPath("$.description").value(STRAT_1_DESCRIPTION))
        .andExpect(jsonPath("$.className").value(STRAT_1_CLASSNAME))
        .andExpect(jsonPath("$.configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
        .andExpect(jsonPath("$.configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE));

    verify(strategyConfigService, times(1)).createStrategyConfig(any());
  }

  @Test
  void testCreateStrategyConfigWithUserTokenForbidden() throws Exception {
    given(strategyConfigService.createStrategyConfig(someStrategyConfig()))
        .willReturn(someStrategyConfig());

    mockMvc
        .perform(
            post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_USER_NAME, VALID_USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andExpect(status().isForbidden());

    verify(strategyConfigService, times(0)).createStrategyConfig(any());
  }

  @Test
  void testCreateStrategyConfigWhenUnauthorizedWithMissingToken() throws Exception {
    mockMvc
        .perform(
            post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testCreateStrategyConfigWhenUnauthorizedWithInvalidToken() throws Exception {
    mockMvc
        .perform(
            post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer junk.web.token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfig())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testCreateInvalidStrategyConfigWithAdminTokenAuthorized() throws Exception {
    given(strategyConfigService.createStrategyConfig(someStrategyConfig()))
        .willReturn(someStrategyConfig());

    mockMvc
        .perform(
            post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", "Bearer " + getJwt(VALID_ADMIN_NAME, VALID_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonify(someStrategyConfigWithMissingId())))
        .andDo(print())
        .andExpect(status().isBadRequest());

    verify(strategyConfigService, times(1)).createStrategyConfig(any());
  }

  // --------------------------------------------------------------------------
  // Private utils
  // 私有工具
  // --------------------------------------------------------------------------

  private static List<StrategyConfig> allTheStrategiesConfig() {
    final Map<String, String> configItems = new HashMap<>();

    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

    final StrategyConfig strategyConfig1 =
        new StrategyConfig(
            STRAT_1_ID,
            STRAT_1_NAME,
            STRAT_1_DESCRIPTION,
            STRAT_1_CLASSNAME,
            STRAT_1_BEAN_NAME,
            configItems);
    final StrategyConfig strategyConfig2 =
        new StrategyConfig(
            STRAT_2_ID,
            STRAT_2_NAME,
            STRAT_2_DESCRIPTION,
            STRAT_2_CLASSNAME,
            STRAT_2_BEAN_NAME,
            configItems);

    final List<StrategyConfig> allStrategies = new ArrayList<>();
    allStrategies.add(strategyConfig1);
    allStrategies.add(strategyConfig2);
    return allStrategies;
  }

  private static StrategyConfig someStrategyConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    return new StrategyConfig(
        STRAT_1_ID,
        STRAT_1_NAME,
        STRAT_1_DESCRIPTION,
        STRAT_1_CLASSNAME,
        STRAT_1_BEAN_NAME,
        configItems);
  }

  private static StrategyConfig someStrategyConfigWithMissingId() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    return new StrategyConfig(
        null, STRAT_1_NAME, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, STRAT_1_BEAN_NAME, configItems);
  }

  private static StrategyConfig unrecognizedStrategyConfig() {
    final Map<String, String> configItems = new HashMap<>();
    configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
    configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
    return new StrategyConfig(
        UNKNOWN_STRAT_ID,
        STRAT_1_NAME,
        STRAT_1_DESCRIPTION,
        STRAT_1_CLASSNAME,
        STRAT_1_BEAN_NAME,
        configItems);
  }
}
