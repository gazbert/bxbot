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
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.rest.api.AbstractConfigControllerTest;
import com.gazbert.bxbot.services.EmailAlertsConfigService;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the Email Alerts config controller behaviour.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TestEmailAlertsConfigController extends AbstractConfigControllerTest {

    // Canned data
    private static final boolean ENABLED = true;
    private static final String HOST = "smtp.host.deathstar.com";
    private static final int TLS_PORT = 573;
    private static final String ACCOUNT_USERNAME = "boba@google.com";
    private static final String ACCOUNT_PASSWORD = "bounty";
    private static final String FROM_ADDRESS = "boba.fett@Mandalore.com";
    private static final String TO_ADDRESS = "darth.vader@deathstar.com";

    @MockBean
    EmailAlertsConfigService emailAlertsConfigService;

    // Need this even though not used in the test directly because Spring loads it on startup...
    @MockBean
    private TradingEngine tradingEngine;

    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void testGetEmailAlertsConfig() throws Exception {

        given(emailAlertsConfigService.getEmailAlertsConfig()).willReturn(someEmailAlertsConfig());

        mockMvc.perform(get("/api/config/email-alerts")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smtpConfig.host").value(HOST))
                .andExpect(jsonPath("$.smtpConfig.tlsPort").value(TLS_PORT))
                .andExpect(jsonPath("$.enabled").value(ENABLED))
                .andExpect(jsonPath("$.smtpConfig.fromAddress").value(FROM_ADDRESS))
                .andExpect(jsonPath("$.smtpConfig.toAddress").value(TO_ADDRESS))
                .andExpect(jsonPath("$.smtpConfig.accountUsername").value(ACCOUNT_USERNAME))
                .andExpect(jsonPath("$.smtpConfig.accountPassword").value(ACCOUNT_PASSWORD));
    }

    @Test
    public void testGetEmailAlertsConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(get("/api/config/email-alerts")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetEmailAlertsConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(get("/api/config/email-alerts")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateEmailAlertsConfig() throws Exception {

        given(emailAlertsConfigService.updateEmailAlertsConfig(someEmailAlertsConfig())).willReturn(someEmailAlertsConfig());

        final MvcResult result = mockMvc.perform(put("/api/config/email-alerts")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(jsonify(someEmailAlertsConfig())))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // FIXME - response body is empty?!
//        assertEquals(jsonify(someEmailAlertsConfig()), result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdateEmailAlertsConfigWhenUnauthorizedWithMissingCredentials() throws Exception {

        mockMvc.perform(put("/api/config/email-alerts")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateEmailAlertsConfigWhenUnauthorizedWithInvalidCredentials() throws Exception {

        mockMvc.perform(put("/api/config/email-alerts")
                .header("Authorization", buildAuthorizationHeaderValue(VALID_USER_LOGINID, INVALID_USER_PASSWORD))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static EmailAlertsConfig someEmailAlertsConfig() {
        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        final SmtpConfig smtpConfig = new SmtpConfig(
                HOST, TLS_PORT, ACCOUNT_USERNAME, ACCOUNT_PASSWORD, FROM_ADDRESS, TO_ADDRESS);
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        emailAlertsConfig.setEnabled(true);
        return emailAlertsConfig;
    }
}
