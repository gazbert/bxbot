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

import com.gazbert.bxbot.core.admin.services.EngineConfigService;
import com.gazbert.bxbot.core.config.engine.EngineConfig;
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
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TODO Work in progress...
 * <p>
 * Tests the Engine config controller behaviour.
 *
 * @author gazbert
 * @since 11/08/2016
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestEngineConfigController {

    private static final MediaType CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private HttpMessageConverter mappingJackson2HttpMessageConverter;
    private MockMvc mockMvc;

    @MockBean
    EngineConfigService engineConfigService;

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
    public void testGetEngineConfig() throws Exception {

        given(this.engineConfigService.getConfig()).willReturn(mockedEngineConfig());

        this.mockMvc.perform(get("/api/config/engine"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emergencyStopCurrency").value("BTC"))
                .andExpect(jsonPath("$.emergencyStopBalance").value(0.923232))
                .andExpect(jsonPath("$.tradeCycleInterval").value(60)
                );
    }

    @Test
    public void testUpdateEngineConfig() throws Exception {

        final String configJson = jsonify(buildUpdatedEngineConfig());
        this.mockMvc.perform(put("/api/config/engine")
                .contentType(CONTENT_TYPE)
                .content(configJson))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private String jsonify(Object objectToJsonify) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(objectToJsonify, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    private static EngineConfig buildUpdatedEngineConfig() {
        final EngineConfig engineConfig = new EngineConfig();
        engineConfig.setEmergencyStopCurrency("BTC");
        engineConfig.setEmergencyStopBalance(new BigDecimal("0.923232"));
        engineConfig.setTradeCycleInterval(60);
        return engineConfig;
    }

    private static EngineConfig mockedEngineConfig() {
        final EngineConfig engineConfig = new EngineConfig();
        engineConfig.setEmergencyStopCurrency("BTC");
        engineConfig.setEmergencyStopBalance(new BigDecimal("0.923232"));
        engineConfig.setTradeCycleInterval(60);
        return engineConfig;
    }

}
