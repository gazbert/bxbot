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

package com.gazbert.bxbot.core.admin.controllers;

import com.gazbert.bxbot.core.admin.services.ExchangeConfigService;
import com.gazbert.bxbot.core.config.exchange.AuthenticationConfig;
import com.gazbert.bxbot.core.config.exchange.ExchangeConfig;
import com.gazbert.bxbot.core.config.exchange.NetworkConfig;
import com.gazbert.bxbot.core.config.exchange.OtherConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
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
 * @since 20/07/2016
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestExchangeConfigController {

    private static final String EXCHANGE_NAME = "BTC-e";
    private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.core.exchanges.TestExchangeAdapter";

    private static final Integer CONNECTION_TIMEOUT = 30;
    private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503, 504);
    private static final List<String> NON_FATAL_ERROR_MESSAGES = Arrays.asList(
            "Connection refused", "Connection reset", "Remote host closed connection during handshake");

    private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
    private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";
    private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
    private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";

    private static final MediaType CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private HttpMessageConverter mappingJackson2HttpMessageConverter;
    private MockMvc mockMvc;

    @MockBean
    ExchangeConfigService exchangeConfigService;

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {
        this.mappingJackson2HttpMessageConverter =
                Arrays.stream(converters)
                        .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter).findAny().get();

        Assert.assertNotNull("The JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setupBeforeEachTest() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    public void testGetExchangeConfig() throws Exception {

        given(this.exchangeConfigService.getConfig()).willReturn(someExchangeConfig());

        this.mockMvc.perform(get("/api/config/exchange"))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.exchangeName").value(EXCHANGE_NAME))
                .andExpect(jsonPath("$.exchangeAdapter").value(EXCHANGE_ADAPTER))

                .andExpect(jsonPath("$.authenticationConfig.items").isEmpty())

                .andExpect(jsonPath("$.networkConfig.connectionTimeout").value(CONNECTION_TIMEOUT))
                .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[0]").value(502))
                .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[1]").value(503))
                .andExpect(jsonPath("$.networkConfig.nonFatalErrorCodes[2]").value(504))
                .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[0]").value("Connection refused"))
                .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[1]").value("Connection reset"))
                .andExpect(jsonPath("$.networkConfig.nonFatalErrorMessages[2]").value("Remote host closed connection during handshake"))

                .andExpect(jsonPath("$.otherConfig.items.buy-fee").value(BUY_FEE_CONFIG_ITEM_VALUE))
                .andExpect(jsonPath("$.otherConfig.items.sell-fee").value(SELL_FEE_CONFIG_ITEM_VALUE)

                );
    }

    @Test
    public void testUpdateExchangeConfig() throws Exception {

        final String exchangeConfigJson = jsonify(someExchangeConfig());
        this.mockMvc.perform(put("/api/config/exchange")
                .contentType(CONTENT_TYPE)
                .content(exchangeConfigJson))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private String jsonify(Object objectToJsonify) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(objectToJsonify, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    private static ExchangeConfig someExchangeConfig() {

        final AuthenticationConfig authenticationConfig = new AuthenticationConfig();
        // we don't send auth config in GET request - security risk

        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
        networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
        networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

        final OtherConfig otherConfig = new OtherConfig();
        otherConfig.getItems().put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
        otherConfig.getItems().put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setExchangeName(EXCHANGE_NAME);
        exchangeConfig.setExchangeAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOtherConfig(otherConfig);

        return exchangeConfig;
    }
}
