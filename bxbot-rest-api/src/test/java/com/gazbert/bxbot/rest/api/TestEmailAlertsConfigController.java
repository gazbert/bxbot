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
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.services.EmailAlertsConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
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

    // This must match a user's login_id in the user table in src/test/resources/import.sql
    private static final String VALID_USER_LOGINID = "user1";

    // This must match a user's password in the user table in src/test/resources/import.sql
    private static final String VALID_USER_PASSWORD = "user1-password";

    // Canned data
    private static final boolean ENABLED = true;
    private static final String HOST = "smtp.host.deathstar.com";
    private static final int TLS_PORT = 573;
    private static final String ACCOUNT_USERNAME = "boba@google.com";
    private static final String FROM_ADDRESS = "boba.fett@Mandalore.com";
    private static final String TO_ADDRESS = "darth.vader@deathstar.com";

    @MockBean
    EmailAlertsConfigService emailAlertsConfigService;

    @MockBean
    private EmailAlerter emailAlerter;

    @MockBean
    private TradingEngine tradingEngine;


    @Before
    public void setupBeforeEachTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilter(springSecurityFilterChain).build();
    }

    @Test
    public void testGetEmailAlertsConfig() throws Exception {

        given(emailAlertsConfigService.getConfig()).willReturn(someEmailAlertsConfig());
        tradingEngine.start();

        mockMvc.perform(get("/api/config/emailalerts")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smtpConfig.host").value(HOST))
                .andExpect(jsonPath("$.smtpConfig.tlsPort").value(TLS_PORT))
                .andExpect(jsonPath("$.enabled").value(ENABLED))
                .andExpect(jsonPath("$.smtpConfig.fromAddress").value(FROM_ADDRESS))
                .andExpect(jsonPath("$.smtpConfig.toAddress").value(TO_ADDRESS))
                .andExpect(jsonPath("$.smtpConfig.accountUsername").value(ACCOUNT_USERNAME))

                // REST API does not expose email account password - potential security risk
                .andExpect(jsonPath("$.smtpConfig.accountPassword").doesNotExist()
                );
    }

    @Test
    public void testGetEmailAlertsConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(get("/api/config/emailalerts")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    public void testUpdateEmailAlertsConfig() throws Exception {

        final String configJson = jsonify(someEmailAlertsConfig());
        mockMvc.perform(put("/api/config/emailalerts")
                .header("Authorization", "Bearer " + getAccessToken(VALID_USER_LOGINID, VALID_USER_PASSWORD))
                .contentType(CONTENT_TYPE)
                .content(configJson))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testUpdateEmailAlertsConfigWhenUnauthorized() throws Exception {

        mockMvc.perform(put("/api/config/emailalerts")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static EmailAlertsConfig someEmailAlertsConfig() {

        // REST API does not expose email account password - potential security risk
        final SmtpConfig smtpConfig = new SmtpConfig(
                HOST, TLS_PORT, ACCOUNT_USERNAME, null, FROM_ADDRESS, TO_ADDRESS);

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(true);
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }
}
