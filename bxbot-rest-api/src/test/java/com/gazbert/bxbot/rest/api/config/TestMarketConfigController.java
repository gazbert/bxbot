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

package com.gazbert.bxbot.rest.api.config;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.rest.api.AbstractConfigControllerTest;
import com.gazbert.bxbot.services.MarketConfigService;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the Market config controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestMarketConfigController extends AbstractConfigControllerTest {

    // Canned data
    private static final String UNKNOWN_MARKET_ID = "unknown-id";

    private static final String MARKET_1_ID = "btc_usd";
    private static final String MARKET_1_NAME = "BTC/USD";
    private static final String MARKET_1_BASE_CURRENCY = "BTC";
    private static final String MARKET_1_COUNTER_CURRENCY = "USD";
    private static final boolean MARKET_1_ENABLED = true;
    private static final String MARKET_1_STRATEGY_ID = "scalper-strategy";

    private static final String MARKET_2_ID = "btc_gbp";
    private static final String MARKET_2_NAME = "BTC/GBP";
    private static final String MARKET_2_BASE_CURRENCY = "BTC";
    private static final String MARKET_2_COUNTER_CURRENCY = "GBP";
    private static final boolean MARKET_2_ENABLED = false;
    private static final String MARKET_2_STRATEGY_ID = "macd-strategy";

    @MockBean
    MarketConfigService marketConfigService;

    // Need this even though not used in the test directly because Spring loads it on startup...
    @MockBean
    private EmailAlerter emailAlerter;

    // Need this even though not used in the test directly because Spring loads it on startup...
    @MockBean
    private TradingEngine tradingEngine;


    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void testGetAllMarketConfig() throws Exception {

        given(marketConfigService.getAllMarketConfig()).willReturn(allMarketConfig());

        mockMvc.perform(get("/api/config/markets/")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.[0].id").value(MARKET_1_ID))
                .andExpect(jsonPath("$.[0].name").value(MARKET_1_NAME))
                .andExpect(jsonPath("$.[0].baseCurrency").value(MARKET_1_BASE_CURRENCY))
                .andExpect(jsonPath("$.[0].counterCurrency").value(MARKET_1_COUNTER_CURRENCY))
                .andExpect(jsonPath("$.[0].enabled").value(MARKET_1_ENABLED))
                .andExpect(jsonPath("$.[0].tradingStrategyId").value(MARKET_1_STRATEGY_ID))

                .andExpect(jsonPath("$.[1].id").value(MARKET_2_ID))
                .andExpect(jsonPath("$.[1].name").value(MARKET_2_NAME))
                .andExpect(jsonPath("$.[1].baseCurrency").value(MARKET_2_BASE_CURRENCY))
                .andExpect(jsonPath("$.[1].counterCurrency").value(MARKET_2_COUNTER_CURRENCY))
                .andExpect(jsonPath("$.[1].enabled").value(MARKET_2_ENABLED))
                .andExpect(jsonPath("$.[1].tradingStrategyId").value(MARKET_2_STRATEGY_ID)

                );
    }

    @Test
    public void testGetAllMarketConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(get("/api/config/markets")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAllMarketConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(get("/api/config/markets")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetMarketConfig() throws Exception {

        given(marketConfigService.getMarketConfig(MARKET_1_ID)).willReturn(someMarketConfig());

        mockMvc.perform(get("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id").value(MARKET_1_ID))
                .andExpect(jsonPath("$.name").value(MARKET_1_NAME))
                .andExpect(jsonPath("$.baseCurrency").value(MARKET_1_BASE_CURRENCY))
                .andExpect(jsonPath("$.counterCurrency").value(MARKET_1_COUNTER_CURRENCY))
                .andExpect(jsonPath("$.enabled").value(MARKET_1_ENABLED))
                .andExpect(jsonPath("$.tradingStrategyId").value(MARKET_1_STRATEGY_ID)
                );
    }

    @Test
    public void testGetMarketConfigByIdWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(get("/api/config/markets/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetMarketConfigByIdWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(get("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetMarketConfigByIdWhenNotRecognized() throws Exception {

        given(marketConfigService.getMarketConfig(UNKNOWN_MARKET_ID)).willReturn(emptyMarketConfig());

        mockMvc.perform(get("/api/config/markets/" + UNKNOWN_MARKET_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateMarketConfig() throws Exception {

        given(marketConfigService.updateMarketConfig(someMarketConfig())).willReturn(someMarketConfig());

        final MvcResult result = mockMvc.perform(put("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(jsonify(someMarketConfig()), result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdateMarketConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(put("/api/config/markets/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateMarketConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(put("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateMarketConfigWhenIdNotRecognized() throws Exception {

        given(marketConfigService.updateMarketConfig(unrecognizedMarketConfig())).willReturn(emptyMarketConfig());

        mockMvc.perform(put("/api/config/markets/" + UNKNOWN_MARKET_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(unrecognizedMarketConfig())))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateMarketConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(put("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteMarketConfig() throws Exception {

        given(marketConfigService.deleteMarketConfig(MARKET_1_ID)).willReturn(someMarketConfig());

        mockMvc.perform(delete("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteMarketConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(delete("/api/config/markets/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteMarketConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(delete("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteMarketConfigWhenIdNotRecognized() throws Exception {

        given(marketConfigService.deleteMarketConfig(UNKNOWN_MARKET_ID)).willReturn(emptyMarketConfig());

        mockMvc.perform(delete("/api/config/markets/" + UNKNOWN_MARKET_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateMarketConfig() throws Exception {

        given(marketConfigService.createMarketConfig(someMarketConfig())).willReturn(someMarketConfig());

        final MvcResult result = mockMvc.perform(post("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        assertEquals(jsonify(someMarketConfig()), result.getResponse().getContentAsString());
    }

    @Test
    public void testCreateMarketConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(post("/api/config/markets/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateMarketConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(post("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateMarketConfigWhenIdAlreadyExists() throws Exception {

        given(marketConfigService.createMarketConfig(someMarketConfig())).willReturn(emptyMarketConfig());

        mockMvc.perform(post("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isConflict());
    }

    @Test
    public void testCreateMarketConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/config/markets/" + MARKET_1_ID)
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static MarketConfig someMarketConfig() {
        return new MarketConfig(MARKET_1_ID, MARKET_1_NAME, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY_ID);
    }

    private static MarketConfig unrecognizedMarketConfig() {
        return new MarketConfig(UNKNOWN_MARKET_ID, MARKET_1_NAME, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY_ID);
    }

    private static MarketConfig someMarketConfigWithMissingId() {
        return new MarketConfig(null, MARKET_1_NAME, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY_ID);
    }

    private static List<MarketConfig> allMarketConfig() {
        final MarketConfig market1Config = new MarketConfig(MARKET_1_ID, MARKET_1_NAME, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY_ID);
        final MarketConfig market2Config = new MarketConfig(MARKET_2_ID, MARKET_2_NAME, MARKET_2_BASE_CURRENCY,
                MARKET_2_COUNTER_CURRENCY, MARKET_2_ENABLED, MARKET_2_STRATEGY_ID);

        final List<MarketConfig> allMarkets = new ArrayList<>();
        allMarkets.add(market1Config);
        allMarkets.add(market2Config);
        return allMarkets;
    }

    private static MarketConfig emptyMarketConfig() {
        return new MarketConfig();
    }
}
