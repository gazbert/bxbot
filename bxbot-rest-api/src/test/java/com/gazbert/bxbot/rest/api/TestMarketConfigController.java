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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private static final String MARKET_2_STRATEGY = "scalper-strategy";

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

    @Test
    public void testGetAllMarketConfig() throws Exception {

        given(this.marketConfigService.findAllMarkets()).willReturn(allMarketConfig());
        this.tradingEngine.start();

        this.mockMvc.perform(get("/api/config/market/")
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

    @Ignore("FIXME test needs fixing once service wired up")
    @Test
    public void testUpdateMarketConfig() throws Exception {

        final MarketConfig marketConfig = someMarketConfig();
        final String marketConfigJson = jsonify(marketConfig);
        this.mockMvc.perform(put("/api/config/market/" + marketConfig.getId())
                .contentType(CONTENT_TYPE)
                .content(marketConfigJson))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static MarketConfig someMarketConfig() {
        final MarketConfig marketConfig = new MarketConfig(MARKET_1_LABEL, MARKET_1_ID, MARKET_1_BASE_CURRENCY,
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
}
