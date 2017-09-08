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

package com.gazbert.bxbot.repository;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.datastore.emailalerts.generated.SmtpConfigType;
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.repository.impl.EmailAlertsConfigRepositoryXmlDatastore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.gazbert.bxbot.datastore.FileLocations.EMAIL_ALERTS_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.EMAIL_ALERTS_CONFIG_XSD_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.*;

/**
 * Tests Email Alerts configuration repository behaves as expected.
 *
 * @author gazbert
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

    private static final String UPDATED_HOST = "updated.smtp.host.deathstar.com";
    private static final int UPDATED_PORT = 588;
    private static final String UPDATED_ACCOUNT_USERNAME = "updated-boba@google.com";
    private static final String UPDATED_ACCOUNT_PASSWORD = "updated-b0b4InD4H0u53";
    private static final String UPDATED_FROM_ADDRESS = "updated-boba.fett@Mandalore.com";
    private static final String UPDATED_TO_ADDRESS = "updated-darth.vader@deathstar.com";


    @Before
    public void setup() throws Exception {
        PowerMock.mockStatic(ConfigurationManager.class);
    }

    @Test
    public void whenGetCalledThenExpectEmailAlertsConfigToBeReturned() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(EmailAlertsType.class),
                eq(EMAIL_ALERTS_CONFIG_XML_FILENAME),
                eq(EMAIL_ALERTS_CONFIG_XSD_FILENAME))).
                andReturn(someInternalEmailAlertsConfig());

        PowerMock.replayAll();

        final EmailAlertsConfigRepository emailAlertsConfigRepository = new EmailAlertsConfigRepositoryXmlDatastore();
        final EmailAlertsConfig emailAlertsConfig = emailAlertsConfigRepository.get();
        assertThat(emailAlertsConfig.isEnabled()).isEqualTo(ENABLED);
        assertThat(emailAlertsConfig.getSmtpConfig().getHost()).isEqualTo(HOST);
        assertThat(emailAlertsConfig.getSmtpConfig().getTlsPort()).isEqualTo(TLS_PORT);
        assertThat(emailAlertsConfig.getSmtpConfig().getFromAddress()).isEqualTo(FROM_ADDRESS);
        assertThat(emailAlertsConfig.getSmtpConfig().getToAddress()).isEqualTo(TO_ADDRESS);
        assertThat(emailAlertsConfig.getSmtpConfig().getAccountUsername()).isEqualTo(ACCOUNT_USERNAME);
        assertThat(emailAlertsConfig.getSmtpConfig().getAccountPassword()).isEqualTo(ACCOUNT_PASSWORD);

        PowerMock.verifyAll();
    }

    @Test
    public void whenSaveCalledThenExpectRepositoryToSaveItAndReturnSavedEmailAlertsConfig() throws Exception {

        ConfigurationManager.saveConfig(eq(EmailAlertsType.class), anyObject(EmailAlertsType.class), eq(EMAIL_ALERTS_CONFIG_XML_FILENAME));

        expect(ConfigurationManager.loadConfig(
                eq(EmailAlertsType.class),
                eq(EMAIL_ALERTS_CONFIG_XML_FILENAME),
                eq(EMAIL_ALERTS_CONFIG_XSD_FILENAME))).
                andReturn(adaptExternalToInternalConfig(withSomeExternalEmailAlertsConfig()));

        PowerMock.replayAll();

        final EmailAlertsConfigRepository emailAlertsConfigRepository = new EmailAlertsConfigRepositoryXmlDatastore();
        final EmailAlertsConfig saveConfig = emailAlertsConfigRepository.save(withSomeExternalEmailAlertsConfig());

        assertThat(saveConfig.isEnabled()).isEqualTo(ENABLED);
        assertThat(saveConfig.getSmtpConfig().getHost()).isEqualTo(UPDATED_HOST);
        assertThat(saveConfig.getSmtpConfig().getTlsPort()).isEqualTo(UPDATED_PORT);
        assertThat(saveConfig.getSmtpConfig().getFromAddress()).isEqualTo(UPDATED_FROM_ADDRESS);
        assertThat(saveConfig.getSmtpConfig().getToAddress()).isEqualTo(UPDATED_TO_ADDRESS);
        assertThat(saveConfig.getSmtpConfig().getAccountUsername()).isEqualTo(UPDATED_ACCOUNT_USERNAME);
        assertThat(saveConfig.getSmtpConfig().getAccountPassword()).isEqualTo(UPDATED_ACCOUNT_PASSWORD);

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

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(true);

        final SmtpConfig smtpConfig = new SmtpConfig(UPDATED_HOST, UPDATED_PORT, UPDATED_ACCOUNT_USERNAME,
                UPDATED_ACCOUNT_PASSWORD, UPDATED_FROM_ADDRESS, UPDATED_TO_ADDRESS);
        emailAlertsConfig.setSmtpConfig(smtpConfig);

        return emailAlertsConfig;
    }

    private static EmailAlertsType adaptExternalToInternalConfig(EmailAlertsConfig externalEmailAlertsConfig) {

        final SmtpConfigType smtpConfig = new SmtpConfigType();
        smtpConfig.setSmtpHost(externalEmailAlertsConfig.getSmtpConfig().getHost());
        smtpConfig.setSmtpTlsPort(externalEmailAlertsConfig.getSmtpConfig().getTlsPort());
        smtpConfig.setAccountUsername(externalEmailAlertsConfig.getSmtpConfig().getAccountUsername());
        smtpConfig.setAccountPassword(externalEmailAlertsConfig.getSmtpConfig().getAccountPassword());
        smtpConfig.setFromAddr(externalEmailAlertsConfig.getSmtpConfig().getFromAddress());
        smtpConfig.setToAddr(externalEmailAlertsConfig.getSmtpConfig().getToAddress());

        final EmailAlertsType emailAlertsConfig = new EmailAlertsType();
        emailAlertsConfig.setEnabled(externalEmailAlertsConfig.isEnabled());
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }
}
