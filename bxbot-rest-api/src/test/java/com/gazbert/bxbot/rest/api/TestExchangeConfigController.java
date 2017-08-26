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
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.domain.exchange.OptionalConfig;
import com.gazbert.bxbot.services.ExchangeConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the Exchange config controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestExchangeConfigController extends AbstractConfigControllerTest {

    // Canned test data
    private static final String EXCHANGE_NAME = "Bitstamp";
    private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";

    private static final Integer CONNECTION_TIMEOUT = 30;
    private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503, 504);
    private static final List<String> NON_FATAL_ERROR_MESSAGES = Arrays.asList(
            "Connection refused", "Connection reset", "Remote host closed connection during handshake");

    private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
    private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";
    private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
    private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";

    @MockBean
    ExchangeConfigService exchangeConfigService;

    @MockBean
    private TradingEngine tradingEngine;

    // Need this even though not used in the test directly because Spring loads the Email Alerts config on startup...
    @MockBean
    private EmailAlerter emailAlerter;

    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void testGetExchangeConfig() throws Exception {

        given(exchangeConfigService.getExchangeConfig()).willReturn(someExchangeConfig());
        tradingEngine.start();

        mockMvc.perform(get("/api/config/exchange")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.name").value(EXCHANGE_NAME))
                .andExpect(jsonPath("$.className").value(EXCHANGE_ADAPTER))

                // REST API currently does not expose AuthenticationConfig - potential security risk?
                .andExpect(jsonPath("$.authenticationConfig").doesNotExist())

                .andExpect(jsonPath("$.networkConfig.connectionTimeout").value(CONNECTION_TIMEOUT)

                // FIXME - JSON paths to data need sorting!
//                .andExpect(jsonPath("$.networkConfig.nonFatalErrorHttpStatusCodes").value(Arrays.asList(502, 503, 504)))
//                .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages").value(Arrays.asList(
//                        "Connection refused", "Connection reset", "Remote host closed connection during handshake")))
//                .andExpect(jsonPath("$.optionalConfig.items.buy-fee").value(BUY_FEE_CONFIG_ITEM_VALUE))
//                .andExpect(jsonPath("$.optionalConfig.items.sell-fee").value(SELL_FEE_CONFIG_ITEM_VALUE)
                );
    }

    @Test
    public void testGetExchangeConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(get("/api/config/exchange")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetExchangeConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(get("/api/config/exchange")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateExchangeConfig() throws Exception {

        mockMvc.perform(put("/api/config/exchange")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someExchangeConfig())))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testUpdateExchangeConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(put("/api/config/exchange")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateExchangeConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(put("/api/config/exchange")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static ExchangeConfig someExchangeConfig() {

        // We don't expose AuthenticationConfig in the REST API - security risk

        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
        networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
        networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

        final OptionalConfig optionalConfig = new OptionalConfig();
        optionalConfig.getItems().put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
        optionalConfig.getItems().put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setExchangeName(EXCHANGE_NAME);
        exchangeConfig.setExchangeAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOptionalConfig(optionalConfig);

        return exchangeConfig;
    }
}
