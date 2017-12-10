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

package com.gazbert.bxbot.core.mail;


import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.services.EmailAlertsConfigService;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.mail.Message;
import javax.mail.Transport;

import static junit.framework.TestCase.assertNotNull;
import static org.easymock.EasyMock.expect;

/**
 * Test the Email Alerter behaves as expected.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Transport.class})
@PowerMockIgnore({"javax.management.*"})
public class TestEmailAlerter {

    private static final String EMAIL_SUBJECT = "CRITICAL Alert message from BX-bot";
    private static final String EMAIL_MSG = "The exchange has blown up!";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_TLS_PORT = 587;
    private static final String ACCOUNT_USERNAME = "your.account.username@gmail.com";
    private static final String ACCOUNT_PASSWORD = "le-password";
    private static final String FROM_ADDRESS = "bxbot.alerts@gmail.com";
    private static final String TO_ADDRESS = "some-destination@gmail.com";

    private EmailAlertsConfigService emailAlertsConfigService;


    @Before
    public void setup() throws Exception {
        emailAlertsConfigService = PowerMock.createMock(EmailAlertsConfigService.class);
    }

    @Test
    public void testEmailAlerterInitialisedSuccessfully() throws Exception {

        expect(emailAlertsConfigService.getEmailAlertsConfig()).andReturn(someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig());
        PowerMock.replayAll();

        final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
        assertNotNull(emailAlerter);

        PowerMock.verifyAll();
    }

    @Test
    public void testEmailAlerterInitialisedSuccessfullyWhenAlertsDisabledAndNoSmtpConfigSupplied() throws Exception {

        expect(emailAlertsConfigService.getEmailAlertsConfig()).andReturn(someEmailAlertsConfigWithAlertsDisabledAndNoSmtpConfig());
        PowerMock.replayAll();

        final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
        assertNotNull(emailAlerter);

        PowerMock.verifyAll();
    }

    @Test(expected = IllegalStateException.class)
    public void testEmailAlerterInitialisationFailsWhenAlertsEnabledButNoSmtpConfigSupplied() throws Exception {

        expect(emailAlertsConfigService.getEmailAlertsConfig()).andReturn(someEmailAlertsConfigWithAlertsEnabledAndNoSmtpConfig());
        PowerMock.replayAll();

        final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
        assertNotNull(emailAlerter);

        PowerMock.verifyAll();
    }

    /*
     * Can safely run this test without 'real' credentials.
     * Crude use of mocks to test behaviour.
     * It does not send anything down the wire.
     */
    @Test
    public void testEmailAlerterSendsMailSuccessfullyUsingMockTransport() throws Exception {

        expect(emailAlertsConfigService.getEmailAlertsConfig()).andReturn(someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig());

        PowerMock.mockStatic(Transport.class);
        Transport.send(EasyMock.anyObject(Message.class));

        PowerMock.replayAll();

        final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
        emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);

        PowerMock.verifyAll();
    }

    /*
     * Requires real credentials to run test. Will actually send email out.
     * Good for testing that you're all setup before deployment.
     *
     * 1. Uncomment @Test.
     * 2. Change the <project-root>/config/email-alerts.xml to use your account SMTP settings.
     * 3. Comment out @RunWith(PowerMockRunner.class) and @PrepareForTest(Transport.class) at top of class - they mess
     *    with the SSLContext and the test will fail - no time to debug why but related to:
     *    https://code.google.com/p/powermock/issues/detail?id=288
     * 4. Run this test on its own.
     */
    //@Test
    public void testEmailAlerterReallySendsMailSuccessfully() throws Exception {

        final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
        emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);

        // expect to send message - check your inbox!
    }

    // ------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------

    private static EmailAlertsConfig someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig() {

        final SmtpConfig smtpConfig = new SmtpConfig(
                SMTP_HOST, SMTP_TLS_PORT, ACCOUNT_USERNAME, ACCOUNT_PASSWORD, FROM_ADDRESS, TO_ADDRESS);

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(true);
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }

    private static EmailAlertsConfig someEmailAlertsConfigWithAlertsDisabledAndNoSmtpConfig() {

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(false);
        return emailAlertsConfig;
    }

    private static EmailAlertsConfig someEmailAlertsConfigWithAlertsEnabledAndNoSmtpConfig() {

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(true);
        return emailAlertsConfig;
    }
}
