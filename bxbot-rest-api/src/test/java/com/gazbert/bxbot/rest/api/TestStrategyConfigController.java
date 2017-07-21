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
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.services.StrategyConfigService;
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
    private static final String UNKNOWN_STRAT_ID = "unknown-id";

    private static final String STRAT_1_ID = "macd-long-position";
    private static final String STRAT_1_LABEL= "MACD Strat Algo";
    private static final String STRAT_1_DESCRIPTION = "Uses MACD as indicator and takes long position in base currency.";
    private static final String STRAT_1_CLASSNAME = "com.gazbert.nova.algos.MacdLongBase";

    private static final String STRAT_2_ID = "long-scalper";
    private static final String STRAT_2_LABEL= "Long Position Scalper Algo";
    private static final String STRAT_2_DESCRIPTION = "Scalps and goes long...";
    private static final String STRAT_2_CLASSNAME = "com.gazbert.nova.algos.LongScalper";

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

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetAllStrategyConfig() throws Exception {

        given(strategyConfigService.findAllStrategies()).willReturn(allTheStrategiesConfig());
        tradingEngine.start();

        mockMvc.perform(get("/api/config/strategy/")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.[0].id").value(STRAT_1_ID))
                .andExpect(jsonPath("$.[0].label").value(STRAT_1_LABEL))
                .andExpect(jsonPath("$.[0].description").value(STRAT_1_DESCRIPTION))
                .andExpect(jsonPath("$.[0].className").value(STRAT_1_CLASSNAME))
                .andExpect(jsonPath("$.[0].configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.[0].configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE))

                .andExpect(jsonPath("$.[1].id").value(STRAT_2_ID))
                .andExpect(jsonPath("$.[1].label").value(STRAT_2_LABEL))
                .andExpect(jsonPath("$.[1].description").value(STRAT_2_DESCRIPTION))
                .andExpect(jsonPath("$.[1].className").value(STRAT_2_CLASSNAME))
                .andExpect(jsonPath("$.[1].configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.[1].configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE)
                );
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetAllStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/strategy")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetStrategyConfigById() throws Exception {

        given(strategyConfigService.findById(STRAT_1_ID)).willReturn(someStrategyConfig());

        mockMvc.perform(get("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id").value(STRAT_1_ID))
                .andExpect(jsonPath("$.label").value(STRAT_1_LABEL))
                .andExpect(jsonPath("$.description").value(STRAT_1_DESCRIPTION))
                .andExpect(jsonPath("$.className").value(STRAT_1_CLASSNAME))
                .andExpect(jsonPath("$.configItems.buy-price").value(BUY_PRICE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.configItems.buy-amount").value(AMOUNT_TO_BUY_CONFIG_ITEM_VALUE)
                );
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetStrategyConfigByIdWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/strategy/" + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testGetStrategyConfigByIdWhenNotRecognized() throws Exception {

        given(strategyConfigService.findById(UNKNOWN_STRAT_ID)).willReturn(emptyStrategyConfig());

        mockMvc.perform(get("/api/config/strategy/" + UNKNOWN_STRAT_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateStrategyConfig() throws Exception {

        given(strategyConfigService.updateStrategy(someStrategyConfig())).willReturn(someStrategyConfig());

        mockMvc.perform(put("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isNoContent());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(put("/api/config/strategy/" + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateStrategyConfigWhenIdNotRecognized() throws Exception {

        given(strategyConfigService.updateStrategy(unrecognizedStrategyConfig())).willReturn(emptyStrategyConfig());

        mockMvc.perform(put("/api/config/strategy/" + UNKNOWN_STRAT_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(unrecognizedStrategyConfig())))
                .andExpect(status().isNotFound());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testUpdateStrategyConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(put("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testDeleteStrategyConfig() throws Exception {

        given(strategyConfigService.deleteStrategyById(STRAT_1_ID)).willReturn(someStrategyConfig());

        mockMvc.perform(delete("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testDeleteStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(delete("/api/config/strategy/" + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testDeleteStrategyConfigWhenIdNotRecognized() throws Exception {

        given(strategyConfigService.deleteStrategyById(UNKNOWN_STRAT_ID)).willReturn(emptyStrategyConfig());

        mockMvc.perform(delete("/api/config/strategy/" + UNKNOWN_STRAT_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateStrategyConfig() throws Exception {

        given(strategyConfigService.createStrategy(someStrategyConfig())).willReturn(someStrategyConfig());

        mockMvc.perform(post("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isCreated());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateStrategyConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(post("/api/config/strategy/" + STRAT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateStrategyConfigWhenIdAlreadyExists() throws Exception {

        given(strategyConfigService.createStrategy(someStrategyConfig())).willReturn(emptyStrategyConfig());

        mockMvc.perform(post("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfig())))
                .andExpect(status().isConflict());
    }

    @Ignore("Ignore tests until OAuth2 replaced with JWT")
    @Test
    public void testCreateStrategyConfigWhenIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/config/strategy/" + STRAT_1_ID)
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE)
                .content(jsonify(someStrategyConfigWithMissingId())))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static List<StrategyConfig> allTheStrategiesConfig() {

        final Map<String, String> configItems = new HashMap<>();

        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);

        final StrategyConfig strategyConfig1 = new StrategyConfig(STRAT_1_ID, STRAT_1_LABEL, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, configItems);
        final StrategyConfig strategyConfig2 = new StrategyConfig(STRAT_2_ID, STRAT_2_LABEL, STRAT_2_DESCRIPTION, STRAT_2_CLASSNAME, configItems);

        final List<StrategyConfig> allStrategies = new ArrayList<>();
        allStrategies.add(strategyConfig1);
        allStrategies.add(strategyConfig2);
        return allStrategies;
    }

    private static StrategyConfig someStrategyConfig() {

        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig(STRAT_1_ID, STRAT_1_LABEL, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, configItems);
    }

    private static StrategyConfig someStrategyConfigWithMissingId() {

        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig(null, STRAT_1_LABEL, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, configItems);
    }

    private static StrategyConfig unrecognizedStrategyConfig() {

        final Map<String, String> configItems = new HashMap<>();
        configItems.put(BUY_PRICE_CONFIG_ITEM_KEY, BUY_PRICE_CONFIG_ITEM_VALUE);
        configItems.put(AMOUNT_TO_BUY_CONFIG_ITEM_KEY, AMOUNT_TO_BUY_CONFIG_ITEM_VALUE);
        return new StrategyConfig("unknown-id", STRAT_1_LABEL, STRAT_1_DESCRIPTION, STRAT_1_CLASSNAME, configItems);
    }

    private static StrategyConfig emptyStrategyConfig() {
        return new StrategyConfig();
    }
}
