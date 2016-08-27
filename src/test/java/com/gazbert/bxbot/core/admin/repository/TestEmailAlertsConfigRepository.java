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

package com.gazbert.bxbot.core.admin.repository;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.core.config.emailalerts.SmtpConfig;
import com.gazbert.bxbot.core.config.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.core.config.emailalerts.generated.SmtpConfigType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig.EMAIL_ALERTS_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig.EMAIL_ALERTS_CONFIG_XSD_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.*;

/**
 * Tests Email Alerts configuration repository behaves as expected.
 *
 * @author gazbert
 * @since 23/08/2016
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
public class TestEmailAlertsConfigRepository {

    private static final boolean ENABLED = true;
    private static final String HOST = "smtp.host.deathstar.com";
    private static final int TLS_PORT = 573;
    private static final String ACCOUNT_USERNAME = "boba@google.com";
    private static final String ACCOUNT_PASSWORD = "b0b4InD4H0u53";
    private static final String FROM_ADDRESS = "boba.fett@Mandalore.com";
    private static final String TO_ADDRESS = "darth.vader@deathstar.com";


    @Before
    public void setup() throws Exception {
        PowerMock.mockStatic(ConfigurationManager.class);
    }

    @Test
    public void whenGetConfigCalledThenExpectRepositoryToLoadIt() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(EmailAlertsType.class),
                eq(EMAIL_ALERTS_CONFIG_XML_FILENAME),
                eq(EMAIL_ALERTS_CONFIG_XSD_FILENAME))).
                andReturn(someInternalEmailAlertsConfig());

        PowerMock.replayAll();

        final EmailAlertsConfigRepository emailAlertsConfigRepository = new EmailAlertsConfigRepositoryImpl();
        final EmailAlertsConfig emailAlertsConfig = emailAlertsConfigRepository.getConfig();
        assertThat(emailAlertsConfig.isEnabled()).isEqualTo(ENABLED);
        assertThat(emailAlertsConfig.getSmtpConfig().getHost()).isEqualTo(HOST);
        assertThat(emailAlertsConfig.getSmtpConfig().getTlsPort()).isEqualTo(TLS_PORT);
        assertThat(emailAlertsConfig.getSmtpConfig().getFromAddress()).isEqualTo(FROM_ADDRESS);
        assertThat(emailAlertsConfig.getSmtpConfig().getToAddress()).isEqualTo(TO_ADDRESS);
        assertThat(emailAlertsConfig.getSmtpConfig().getAccountUsername()).isEqualTo(ACCOUNT_USERNAME);

        // We don't expose email account password in the service - security risk
        assertThat(emailAlertsConfig.getSmtpConfig().getAccountPassword()).isNull();

        PowerMock.verifyAll();
    }

    @Test
    public void whenUpdateConfigCalledThenExpectRepositoryToSaveIt() throws Exception {

        // for loading the existing smtp config to merge with updated stuff
        expect(ConfigurationManager.loadConfig(
                eq(EmailAlertsType.class),
                eq(EMAIL_ALERTS_CONFIG_XML_FILENAME),
                eq(EMAIL_ALERTS_CONFIG_XSD_FILENAME))).
                andReturn(someInternalEmailAlertsConfig());

        ConfigurationManager.saveConfig(eq(EmailAlertsType.class), anyObject(EmailAlertsType.class), eq(EMAIL_ALERTS_CONFIG_XML_FILENAME));
        PowerMock.replayAll();

        final EmailAlertsConfigRepository emailAlertsConfigRepository = new EmailAlertsConfigRepositoryImpl();
        emailAlertsConfigRepository.updateConfig(withSomeExternalEmailAlertsConfig());

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static EmailAlertsType someInternalEmailAlertsConfig() {

        final SmtpConfigType smtpConfig = new SmtpConfigType();
        smtpConfig.setSmtpHost(HOST);
        smtpConfig.setSmtpTlsPort(TLS_PORT);
        smtpConfig.setToAddr(TO_ADDRESS);
        smtpConfig.setFromAddr(FROM_ADDRESS);
        smtpConfig.setAccountUsername(ACCOUNT_USERNAME);
        smtpConfig.setAccountPassword(ACCOUNT_PASSWORD);

        final EmailAlertsType emailAlertsConfig = new EmailAlertsType();
        emailAlertsConfig.setEnabled(ENABLED);
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }

    private static EmailAlertsConfig withSomeExternalEmailAlertsConfig() {

        // We don't permit updating of: account username, password, host, port - potential security risk
        // If caller sets it, we just ignore it.
        final SmtpConfig smtpConfig = new SmtpConfig(
                "ignoreHostUpdate", 0, "ignoreUsernameUpdate", "ignorePasswordUpdate", FROM_ADDRESS, TO_ADDRESS);

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(true);
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }
}
