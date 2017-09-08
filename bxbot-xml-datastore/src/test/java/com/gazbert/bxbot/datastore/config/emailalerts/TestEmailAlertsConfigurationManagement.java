/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.datastore.config.emailalerts;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.datastore.emailalerts.generated.SmtpConfigType;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Files;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Tests the Email Alerter configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestEmailAlertsConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_LOCATION = "com/gazbert/bxbot/datastore/config/email-alerts.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/emailalerts/valid-email-alerts.xml";
    private static final String INVALID_XML_CONFIG_FILENAME = "src/test/config/emailalerts/invalid-email-alerts.xml";
    private static final String VALID_XML_CONFIG_WITHOUT_EMAIL_ALERTS_FILENAME =
            "src/test/config/emailalerts/valid-email-alerts-without-smtp-config.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/emailalerts/missing-email-alerts.xml";
    private static final String XML_CONFIG_TO_SAVE_FILENAME = "src/test/config/emailalerts/saved-email-alerts.xml";

    private static final String HOST = "mail.google.com";
    private static final int TLS_PORT = 587;
    private static final String ACCOUNT_USERNAME = "user@google.com";
    private static final String ACCOUNT_PASSWORD = "myPass";
    private static final String FROM_ADDRESS = "from.me@google.com";
    private static final String TO_ADDRESS = "to.them@google.com";


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final EmailAlertsType emailAlertsType = ConfigurationManager.loadConfig(EmailAlertsType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_LOCATION);

        final SmtpConfigType smtpConfigType = emailAlertsType.getSmtpConfig();
        assertTrue(emailAlertsType.isEnabled());
        assertEquals("smtp.gmail.com", smtpConfigType.getSmtpHost());
        assertTrue(587 == smtpConfigType.getSmtpTlsPort());
        assertEquals("your.account.username@gmail.com", smtpConfigType.getAccountUsername());
        assertEquals("your.account.password", smtpConfigType.getAccountPassword());
        assertEquals("from.addr@gmail.com", smtpConfigType.getFromAddr());
        assertEquals("to.addr@gmail.com", smtpConfigType.getToAddr());
    }

    @Test
    public void testLoadingValidXmlConfigFileWithoutSmtpConfigIsSuccessful() {

        final EmailAlertsType emailAlertsType = ConfigurationManager.loadConfig(EmailAlertsType.class,
                VALID_XML_CONFIG_WITHOUT_EMAIL_ALERTS_FILENAME, XML_SCHEMA_LOCATION);
        assertFalse(emailAlertsType.isEnabled());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigThrowsException() {
        ConfigurationManager.loadConfig(EmailAlertsType.class, MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_LOCATION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadingInvalidXmlConfigFileThrowsException() {
        ConfigurationManager.loadConfig(EmailAlertsType.class,
                INVALID_XML_CONFIG_FILENAME, XML_SCHEMA_LOCATION);
    }

    @Test
    public void testSavingConfigToXmlIsSuccessful() throws Exception {

        final SmtpConfigType smtpConfig = new SmtpConfigType();
        smtpConfig.setAccountUsername(ACCOUNT_USERNAME);
        smtpConfig.setAccountPassword(ACCOUNT_PASSWORD);
        smtpConfig.setSmtpHost(HOST);
        smtpConfig.setSmtpTlsPort(TLS_PORT);
        smtpConfig.setFromAddr(FROM_ADDRESS);
        smtpConfig.setToAddr(TO_ADDRESS);

        final EmailAlertsType emailAlertsConfig = new EmailAlertsType();
        emailAlertsConfig.setEnabled(true);
        emailAlertsConfig.setSmtpConfig(smtpConfig);

        ConfigurationManager.saveConfig(EmailAlertsType.class, emailAlertsConfig, XML_CONFIG_TO_SAVE_FILENAME);

        // Read it back in
        final EmailAlertsType emailAlertsReloaded = ConfigurationManager.loadConfig(EmailAlertsType.class,
                XML_CONFIG_TO_SAVE_FILENAME, XML_SCHEMA_LOCATION);

        assertThat(emailAlertsReloaded.isEnabled()).isTrue();
        assertThat(emailAlertsConfig.getSmtpConfig().getAccountUsername()).isEqualTo(ACCOUNT_USERNAME);
        assertThat(emailAlertsConfig.getSmtpConfig().getAccountPassword()).isEqualTo(ACCOUNT_PASSWORD);
        assertThat(emailAlertsConfig.getSmtpConfig().getSmtpHost()).isEqualTo(HOST);
        assertThat(emailAlertsConfig.getSmtpConfig().getSmtpTlsPort()).isEqualTo(TLS_PORT);
        assertThat(emailAlertsConfig.getSmtpConfig().getFromAddr()).isEqualTo(FROM_ADDRESS);
        assertThat(emailAlertsConfig.getSmtpConfig().getToAddr()).isEqualTo(TO_ADDRESS);

        // cleanup
        Files.delete(FileSystems.getDefault().getPath(XML_CONFIG_TO_SAVE_FILENAME));
    }
}
