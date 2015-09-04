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

package com.gazbert.bxbot.core.config.emailalerts;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.core.config.emailalerts.generated.SmtpConfigType;
import com.gazbert.bxbot.core.config.engine.generated.EngineType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/*
 * Tests the Email Alerter configuration is loaded as expected.
 * We're not testing the JAXB impl here - cherry pick the important stuff.
 */
public class TestEmailAlertsConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "config/schemas/email-alerts.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/emailalerts/valid-email-alerts.xml";
    private static final String VALID_XML_CONFIG_WITHOUT_EMAIL_ALERTS_FILENAME =
            "src/test/config/emailalerts/valid-email-alerts-without-smtp-config.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/emailalerts/missing-email-alerts.xml";


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final EmailAlertsType emailAlertsType = ConfigurationManager.loadConfig(EmailAlertsType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

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
                VALID_XML_CONFIG_WITHOUT_EMAIL_ALERTS_FILENAME, XML_SCHEMA_FILENAME);
        assertFalse(emailAlertsType.isEnabled());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigThrowsException() {
        ConfigurationManager.loadConfig(EngineType.class, MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }
}
