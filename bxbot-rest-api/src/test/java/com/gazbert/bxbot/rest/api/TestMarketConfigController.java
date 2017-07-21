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

package com.gazbert.bxbot.rest.api;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.services.MarketConfigService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    // This must match a user's login_id in the user table in src/test/resources/import.sql
    private static final String VALID_USER_LOGINID = "user1";

    // This must match a user's password in the user table in src/test/resources/import.sql
    private static final String VALID_USER_PASSWORD = "user1-password";

    // Canned data
    private static final String UNKNOWN_MARKET_ID = "unknown-id";

    private static final String MARKET_1_ID = "btc_usd";
    private static final String MARKET_1_LABEL = "BTC/USD";
    private static final String MARKET_1_BASE_CURRENCY = "BTC";
    private static final String MARKET_1_COUNTER_CURRENCY = "USD";
    private static final boolean MARKET_1_ENABLED = true;
    private static final String MARKET_1_STRATEGY = "scalper-strategy";

    private static final String MARKET_2_ID = "btc_gbp";
    private static final String MARKET_2_LABEL = "BTC/GBP";
    private static final String MARKET_2_BASE_CURRENCY = "BTC";
    private static final String MARKET_2_COUNTER_CURRENCY = "GBP";
    private static final boolean MARKET_2_ENABLED = false;
    private static final String MARKET_2_STRATEGY = "macd-strategy";

    @MockBean
    MarketConfigService marketConfigService;

    @MockBean
    private EmailAlerter emailAlerter;

    @MockBean
    private TradingEngine tradingEngine;


    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetAllMarketConfig() throws Exception {

        given(marketConfigService.findAllMarkets()).willReturn(allMarketConfig());
        tradingEngine.start();

        mockMvc.perform(get("/api/config/market/")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.[0].id").value(MARKET_1_ID))
                .andExpect(jsonPath("$.[0].label").value(MARKET_1_LABEL))
                .andExpect(jsonPath("$.[0].baseCurrency").value(MARKET_1_BASE_CURRENCY))
                .andExpect(jsonPath("$.[0].counterCurrency").value(MARKET_1_COUNTER_CURRENCY))
                .andExpect(jsonPath("$.[0].enabled").value(MARKET_1_ENABLED))
                .andExpect(jsonPath("$.[0].tradingStrategy").value(MARKET_1_STRATEGY))

                .andExpect(jsonPath("$.[1].id").value(MARKET_2_ID))
                .andExpect(jsonPath("$.[1].label").value(MARKET_2_LABEL))
                .andExpect(jsonPath("$.[1].baseCurrency").value(MARKET_2_BASE_CURRENCY))
                .andExpect(jsonPath("$.[1].counterCurrency").value(MARKET_2_COUNTER_CURRENCY))
                .andExpect(jsonPath("$.[1].enabled").value(MARKET_2_ENABLED))
                .andExpect(jsonPath("$.[1].tradingStrategy").value(MARKET_2_STRATEGY)

                );
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetAllMarketConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/market")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetMarketConfigById() throws Exception {

        given(marketConfigService.findById(MARKET_1_ID)).willReturn(someMarketConfig());

        mockMvc.perform(get("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id").value(MARKET_1_ID))
                .andExpect(jsonPath("$.label").value(MARKET_1_LABEL))
                .andExpect(jsonPath("$.baseCurrency").value(MARKET_1_BASE_CURRENCY))
                .andExpect(jsonPath("$.counterCurrency").value(MARKET_1_COUNTER_CURRENCY))
                .andExpect(jsonPath("$.enabled").value(MARKET_1_ENABLED))
                .andExpect(jsonPath("$.tradingStrategy").value(MARKET_1_STRATEGY)
                );
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetMarketConfigByIdWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/market/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetMarketConfigByIdWhenNotRecognized() throws Exception {

        given(marketConfigService.findById(UNKNOWN_MARKET_ID)).willReturn(emptyMarketConfig());

        mockMvc.perform(get("/api/config/market/" + UNKNOWN_MARKET_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateMarketConfig() throws Exception {

        given(marketConfigService.updateMarket(someMarketConfig())).willReturn(someMarketConfig());

        mockMvc.perform(put("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isNoContent());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateMarketConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(put("/api/config/market/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateMarketConfigWhenIdNotRecognized() throws Exception {

        given(marketConfigService.updateMarket(unrecognizedMarketConfig())).willReturn(emptyMarketConfig());

        mockMvc.perform(put("/api/config/market/" + UNKNOWN_MARKET_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(unrecognizedMarketConfig())))
                .andExpect(status().isNotFound());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateMarketConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(put("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testDeleteMarketConfig() throws Exception {

        given(marketConfigService.deleteMarketById(MARKET_1_ID)).willReturn(someMarketConfig());

        mockMvc.perform(delete("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testDeleteMarketConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(delete("/api/config/market/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testDeleteMarketConfigWhenIdNotRecognized() throws Exception {

        given(marketConfigService.deleteMarketById(UNKNOWN_MARKET_ID)).willReturn(emptyMarketConfig());

        mockMvc.perform(delete("/api/config/market/" + UNKNOWN_MARKET_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateMarketConfig() throws Exception {

        given(marketConfigService.createMarket(someMarketConfig())).willReturn(someMarketConfig());

        mockMvc.perform(post("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isCreated());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateMarketConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(post("/api/config/market/" + MARKET_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateMarketConfigWhenIdAlreadyExists() throws Exception {

        given(marketConfigService.createMarket(someMarketConfig())).willReturn(emptyMarketConfig());

        mockMvc.perform(post("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfig())))
                .andExpect(status().isConflict());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateMarketConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/config/market/" + MARKET_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someMarketConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static MarketConfig someMarketConfig() {
        final MarketConfig marketConfig = new MarketConfig(MARKET_1_LABEL, MARKET_1_ID, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY);
        return marketConfig;
    }

    private static MarketConfig unrecognizedMarketConfig() {
        final MarketConfig marketConfig = new MarketConfig(MARKET_1_LABEL, UNKNOWN_MARKET_ID, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY);
        return marketConfig;
    }

    private static MarketConfig someMarketConfigWithMissingId() {
        final MarketConfig marketConfig = new MarketConfig(MARKET_1_LABEL, null, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY);
        return marketConfig;
    }

    private static List<MarketConfig> allMarketConfig() {
        final MarketConfig market1Config = new MarketConfig(MARKET_1_LABEL, MARKET_1_ID, MARKET_1_BASE_CURRENCY,
                MARKET_1_COUNTER_CURRENCY, MARKET_1_ENABLED, MARKET_1_STRATEGY);
        final MarketConfig market2Config = new MarketConfig(MARKET_2_LABEL, MARKET_2_ID, MARKET_2_BASE_CURRENCY,
                MARKET_2_COUNTER_CURRENCY, MARKET_2_ENABLED, MARKET_2_STRATEGY);

        final List<MarketConfig> allMarkets = new ArrayList<>();
        allMarkets.add(market1Config);
        allMarkets.add(market2Config);
        return allMarkets;
    }

    private static MarketConfig emptyMarketConfig() {
        return new MarketConfig();
    }
}
