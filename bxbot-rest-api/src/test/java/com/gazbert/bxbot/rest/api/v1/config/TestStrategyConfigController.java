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

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.services.StrategyConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the Strategies config controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestStrategyConfigController extends AbstractConfigControllerTest {

    private static final String STRATEGIES_CONFIG_ENDPOINT_URI = CONFIG_ENDPOINT_BASE_URI + "/strategies/";
    
    private static final String UNKNOWN_STRAT_ID = "unknown-id";

    private static final String STRAT_1_ID = "macd-long-position";
    private static final String STRAT_1_NAME = "MACD Strat Algo";
    private static final String STRAT_1_DESCRIPTION = "Uses MACD as indicator and takes long position in base currency.";
    private static final String STRAT_1_CLASSNAME = "com.gazbert.nova.algos.MacdLongBase";
    private static final String STRAT_1_BEANNAME = "macdLongBase";

    private static final String STRAT_2_ID = "long-scalper";
    private static final String STRAT_2_NAME = "Long Position Scalper Algo";
    private static final String STRAT_2_DESCRIPTION = "Scalps and goes long...";
    private static final String STRAT_2_CLASSNAME = "com.gazbert.nova.algos.LongScalper";
    private static final String STRAT_2_BEANNAME = "longScalper";

    private static final String BUY_PRICE_CONFIG_ITEM_KEY = "buy-price";
    private static final String BUY_PRICE_CONFIG_ITEM_VALUE = "671.15";
    private static final String AMOUNT_TO_BUY_CONFIG_ITEM_KEY = "buy-amount";
    private static final String AMOUNT_TO_BUY_CONFIG_ITEM_VALUE = "0.5";

    @MockBean
    StrategyConfigService strategyConfigService;

    // Need this even though not used in the test directly because Spring loads it on startup...
    @MockBean
    private TradingEngine tradingEngine;

    // Need this even though not used in the test directly because Spring loads it on startup...
    @MockBean
    private EmailAlerter emailAlerter;

    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void testGetAllStrategyConfig() throws Exception {

        given(strategyConfigService.getAllStrategyConfig()).willReturn(allTheStrategiesConfig());

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
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
    public void testGetAllStrategyConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAllStrategyConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetStrategyConfig() throws Exception {

        given(strategyConfigService.getStrategyConfig(STRAT_1_ID)).willReturn(someStrategyConfig());

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
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
    public void testGetStrategyConfigByIdWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetStrategyConfigByIdWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetStrategyConfigByIdWhenNotRecognized() throws Exception {

        given(strategyConfigService.getStrategyConfig(UNKNOWN_STRAT_ID)).willReturn(null);

        mockMvc.perform(get(STRATEGIES_CONFIG_ENDPOINT_URI + UNKNOWN_STRAT_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateStrategyConfig() throws Exception {

        given(strategyConfigService.updateStrategyConfig(someStrategyConfig())).willReturn(someStrategyConfig());

        final MvcResult result = mockMvc.perform(put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(jsonify(someStrategyConfig()), result.getResponse().getContentAsString());
        verify(strategyConfigService, times(1)).updateStrategyConfig(any());
    }

    @Test
    public void testUpdateStrategyConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateStrategyConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateStrategyConfigWhenIdNotRecognized() throws Exception {

        given(strategyConfigService.updateStrategyConfig(unrecognizedStrategyConfig())).willReturn(null);

        mockMvc.perform(put(STRATEGIES_CONFIG_ENDPOINT_URI + UNKNOWN_STRAT_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(unrecognizedStrategyConfig())))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateStrategyConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(put(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteStrategyConfig() throws Exception {

        given(strategyConfigService.deleteStrategyConfig(STRAT_1_ID)).willReturn(someStrategyConfig());

        mockMvc.perform(delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andExpect(status().isNoContent());

        verify(strategyConfigService, times(1)).deleteStrategyConfig(STRAT_1_ID);
    }

    @Test
    public void testDeleteStrategyConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteStrategyConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(delete(STRATEGIES_CONFIG_ENDPOINT_URI + STRAT_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteStrategyConfigWhenIdNotRecognized() throws Exception {

        given(strategyConfigService.deleteStrategyConfig(UNKNOWN_STRAT_ID)).willReturn(null);

        mockMvc.perform(delete(STRATEGIES_CONFIG_ENDPOINT_URI + UNKNOWN_STRAT_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateStrategyConfig() throws Exception {

        given(strategyConfigService.createStrategyConfig(someStrategyConfig())).willReturn(someStrategyConfig());

        final MvcResult result = mockMvc.perform(post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        assertEquals(jsonify(someStrategyConfig()), result.getResponse().getContentAsString());
        verify(strategyConfigService, times(1)).createStrategyConfig(any());
    }

    @Test
    public void testCreateStrategyConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateStrategyConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(post(STRATEGIES_CONFIG_ENDPOINT_URI)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static List<StrategyConfig> allTheStrategiesConfig() {

        final Map<String, String> configItems = new HashMap<>();

        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

        final StrategyConfig strategyConfig1 = new StrategyConfig(STRAT_1_ID, STRAT_1_NAME, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, STRAT_1_BEANNAME, configItems);
        final StrategyConfig strategyConfig2 = new StrategyConfig(STRAT_2_ID, STRAT_2_NAME, STRAT_2_DESCRIPTION, STRAT_2_CLASSNAME, STRAT_2_BEANNAME, configItems);

        final List<StrategyConfig> allStrategies = new ArrayList<>();
        allStrategies.add(strategyConfig1);
        allStrategies.add(strategyConfig2);
        return allStrategies;
    }

    private static StrategyConfig someStrategyConfig() {
        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig(STRAT_1_ID, STRAT_1_NAME, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, STRAT_1_BEANNAME, configItems);
    }

    private static StrategyConfig someStrategyConfigWithMissingId() {
        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig(null, STRAT_1_NAME, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, STRAT_1_BEANNAME, configItems);
    }

    private static StrategyConfig unrecognizedStrategyConfig() {
        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig(UNKNOWN_STRAT_ID, STRAT_1_NAME, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, STRAT_1_BEANNAME, configItems);
    }
}
