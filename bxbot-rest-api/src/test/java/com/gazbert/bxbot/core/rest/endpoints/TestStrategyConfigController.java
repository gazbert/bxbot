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

package com.gazbert.bxbot.core.rest.endpoints;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the Strategy config controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestStrategyConfigController extends AbstractConfigControllerTest {

    // This must match a user's login_id in the user table in src/test/resources/import.sql
    private static final String VALID_USER_LOGINID = "user1";

    // This must match a user's password in the user table in src/test/resources/import.sql
    private static final String VALID_USER_PASSWORD = "user1-password";

    // Canned data
    private static final String STRAT_ID_1 = "macd-long-position";
    private static final String STRAT_LABEL_1 = "MACD Long Position Algo";
    private static final String STRAT_DESCRIPTION_1 = "Uses MACD as indicator and takes long position in base currency.";
    private static final String STRAT_CLASSNAME_1 = "com.gazbert.nova.algos.MacdLongBase";

    private static final String STRAT_ID_2 = "long-scalper";
    private static final String STRAT_LABEL_2 = "Long Position Scalper Algo";
    private static final String STRAT_DESCRIPTION_2 = "Scalps and goes long...";
    private static final String STRAT_CLASSNAME_2 = "com.gazbert.nova.algos.LongScalper";

    private static final String BUY_PRICE_CONFIG_ITEM_KEY = "buy-price";
    private static final String BUY_PRICE_CONFIG_ITEM_VALUE = "671.15";
    private static final String AMOUNT_TO_BUY_CONFIG_ITEM_KEY = "buy-amount";
    private static final String AMOUNT_TO_BUY_CONFIG_ITEM_VALUE = "0.5";

    @MockBean
    StrategyConfigService strategyConfigService;

    @MockBean
    private EmailAlerter emailAlerter;

    @MockBean
    private TradingEngine tradingEngine;


    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void testGetAllStrategyConfig() throws Exception {

        given(this.strategyConfigService.findAllStrategies()).willReturn(allTheStrategiesConfig());
        this.tradingEngine.start();

        this.mockMvc.perform(get("/api/config/strategy/")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.[0].label").value(STRAT_LABEL_1))
                .andExpect(jsonPath("$.[0].description").value(STRAT_DESCRIPTION_1))
                .andExpect(jsonPath("$.[0].className").value(STRAT_CLASSNAME_1))
                .andExpect(jsonPath("$.[0].configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.[0].configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))

                .andExpect(jsonPath("$.[1].label").value(STRAT_LABEL_2))
                .andExpect(jsonPath("$.[1].description").value(STRAT_DESCRIPTION_2))
                .andExpect(jsonPath("$.[1].className").value(STRAT_CLASSNAME_2))
                .andExpect(jsonPath("$.[1].configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.[1].configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE)
                );
    }

    @Test
    public void testGetAllStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/strategy")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    public void testGetStrategyConfigById() throws Exception {

        given(this.strategyConfigService.findById(STRAT_ID_1)).willReturn(someStrategyConfig());

        this.mockMvc.perform(get("/api/config/strategy/" + STRAT_ID_1)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.label").value(STRAT_LABEL_1))
                .andExpect(jsonPath("$.description").value(STRAT_DESCRIPTION_1))
                .andExpect(jsonPath("$.className").value(STRAT_CLASSNAME_1))
                .andExpect(jsonPath("$.configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE)
                );
    }

    @Test
    public void testGetStrategyConfigByIdWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/strategy/" + STRAT_ID_1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    public void testGetStrategyConfigByIdWhenNotRecognized() throws Exception {

        given(this.strategyConfigService.findById("unknown-id")).willReturn(emptyStrategyConfig());

        mockMvc.perform(get("/api/config/strategy/unknown-id")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateStrategyConfig() throws Exception {

        given(this.strategyConfigService.updateStrategy(STRAT_ID_1, someStrategyConfig())).willReturn(someStrategyConfig());
        this.mockMvc.perform(put("/api/config/strategy")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testUpdateStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(put("/api/config/strategy/")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    public void testUpdateStrategyConfigWhenIdNotRecognized() throws Exception {

        given(this.strategyConfigService.updateStrategy("unknown-id", unrecognizedStrategyConfig())).willReturn(emptyStrategyConfig());
        mockMvc.perform(put("/api/config/strategy/")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(unrecognizedStrategyConfig())))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteStrategyConfig() throws Exception {

        given(this.strategyConfigService.deleteStrategyById(STRAT_ID_1)).willReturn(someStrategyConfig());

        this.mockMvc.perform(delete("/api/config/strategy/" + STRAT_ID_1)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(delete("/api/config/strategy/" + STRAT_ID_1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    public void testDeleteStrategyConfigWhenIdNotRecognized() throws Exception {

        given(this.strategyConfigService.deleteStrategyById("unknown-id")).willReturn(emptyStrategyConfig());

        mockMvc.perform(delete("/api/config/strategy/unknown-id")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveStrategyConfig() throws Exception {

        given(this.strategyConfigService.saveStrategy(someStrategyConfig())).willReturn(someStrategyConfig());
        this.mockMvc.perform(post("/api/config/strategy/")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isCreated());
    }

    @Test
    public void testCreateStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(post("/api/config/strategy/")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    public void testCreateStrategyConfigWhenIdAlreadyExists() throws Exception {

        given(this.strategyConfigService.saveStrategy(someStrategyConfig())).willReturn(emptyStrategyConfig());

        mockMvc.perform(post("/api/config/strategy")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static List<StrategyConfig> allTheStrategiesConfig() {

        final Map<String, String> configItems = new HashMap<>();

        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

        final StrategyConfig strategyConfig1 = new StrategyConfig(STRAT_ID_1, STRAT_LABEL_1, STRAT_DESCRIPTION_1, STRAT_CLASSNAME_1, configItems);
        final StrategyConfig strategyConfig2 = new StrategyConfig(STRAT_ID_2, STRAT_LABEL_2, STRAT_DESCRIPTION_2, STRAT_CLASSNAME_2, configItems);

        final List<StrategyConfig> allStrategies = new ArrayList<>();
        allStrategies.add(strategyConfig1);
        allStrategies.add(strategyConfig2);
        return allStrategies;
    }

    private static StrategyConfig someStrategyConfig() {

        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig(STRAT_ID_1, STRAT_LABEL_1, STRAT_DESCRIPTION_1, STRAT_CLASSNAME_1, configItems);
    }

    private static StrategyConfig unrecognizedStrategyConfig() {

        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig("unknown-id", STRAT_LABEL_1, STRAT_DESCRIPTION_1, STRAT_CLASSNAME_1, configItems);
    }

    private static StrategyConfig emptyStrategyConfig() {
        return new StrategyConfig();
    }
}
