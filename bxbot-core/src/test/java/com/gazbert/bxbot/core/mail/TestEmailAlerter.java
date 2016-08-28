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

import com.gazbert.bxbot.core.config.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.core.config.emailalerts.generated.SmtpConfigType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.mail.Message;
import javax.mail.Transport;

import static junit.framework.TestCase.assertNotNull;
import static org.easymock.EasyMock.expect;

/*
 * Test the Email Alerter behaves as expected.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Transport.class, EmailAlerter.class})
public class TestEmailAlerter {

    private static final String EMAIL_SUBJECT = "CRITICAL Alert message from BX-bot";
    private static final String EMAIL_MSG = "The exchange has blown up!";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_TLS_PORT = 587;
    private static final String ACCOUNT_USERNAME = "your.account.username@gmail.com";
    private static final String ACCOUNT_PASSWORD = "le-password";
    private static final String FROM_ADDRESS = "bxbot.alerts@gmail.com";
    private static final String TO_ADDRESS = "some-destination@gmail.com";


    @Before
    public void doNastyHackToTestSingleton() {
        Whitebox.setInternalState(EmailAlerter.getInstance(), "EMAIL_ALERTER_SINGLETON", null, EmailAlerter.class);
    }

    @Ignore("FIX me!")
    @Test
    public void testEmailAlerterInitialisedSuccessfully() throws Exception {

        // Partial mock Email Alerter - we don't need to test the config loading; it has its own tests.
        final EmailAlertsType emailAlertsType = PowerMock.createMock(EmailAlertsType.class);
        PowerMock.mockStaticPartial(EmailAlerter.class, "loadEmailAlerterConfig");
        PowerMock.expectPrivate(EmailAlerter.class, "loadEmailAlerterConfig").andReturn(emailAlertsType);

        // expect to load full config
        expect(emailAlertsType.isEnabled()).andReturn(true);
        final SmtpConfigType smtpConfigType = PowerMock.createMock(SmtpConfigType.class);
        expect(emailAlertsType.getSmtpConfig()).andReturn(smtpConfigType);
        expect(smtpConfigType.getSmtpHost()).andReturn(SMTP_HOST);
        expect(smtpConfigType.getSmtpTlsPort()).andReturn(SMTP_TLS_PORT);
        expect(smtpConfigType.getAccountUsername()).andReturn(ACCOUNT_USERNAME);
        expect(smtpConfigType.getAccountPassword()).andReturn(ACCOUNT_PASSWORD);
        expect(smtpConfigType.getFromAddr()).andReturn(FROM_ADDRESS);
        expect(smtpConfigType.getToAddr()).andReturn(TO_ADDRESS);

        PowerMock.replayAll();

        final EmailAlerter emailAlerter = EmailAlerter.getInstance();
        assertNotNull(emailAlerter);

        PowerMock.verifyAll();
    }

    @Ignore("FIX me!")
    @Test
    public void testEmailAlerterInitialisedSuccessfullyWithoutSmtpConfig() throws Exception {

        // Partial mock Email Alerter - we don't need to test the config loading; it has its own tests.
        final EmailAlertsType emailAlertsType = PowerMock.createMock(EmailAlertsType.class);
        PowerMock.mockStaticPartial(EmailAlerter.class, "loadEmailAlerterConfig");
        PowerMock.expectPrivate(EmailAlerter.class, "loadEmailAlerterConfig").andReturn(emailAlertsType);

        // expect to load only 'enabled' flag from config
        expect(emailAlertsType.isEnabled()).andReturn(false);

        PowerMock.replayAll();

        final EmailAlerter emailAlerter = EmailAlerter.getInstance();
        assertNotNull(emailAlerter);

        PowerMock.verifyAll();
    }

    @Ignore("FIX me!")
    @Test (expected = IllegalStateException.class)
    public void testEmailAlerterInitialisationFailsWhenAlertsEnabledButNoSmtpConfigSupplied() throws Exception {

        // Partial mock Email Alerter - we don't need to test the config loading; it has its own tests.
        final EmailAlertsType emailAlertsType = PowerMock.createMock(EmailAlertsType.class);
        PowerMock.mockStaticPartial(EmailAlerter.class, "loadEmailAlerterConfig");
        PowerMock.expectPrivate(EmailAlerter.class, "loadEmailAlerterConfig").andReturn(emailAlertsType);

        // expect to try and load full config
        expect(emailAlertsType.isEnabled()).andReturn(true);
        expect(emailAlertsType.getSmtpConfig()).andReturn(null); // missing in XML config

        PowerMock.replayAll();

        final EmailAlerter emailAlerter = EmailAlerter.getInstance();
        assertNotNull(emailAlerter);

        PowerMock.verifyAll();
    }

    /*
     * Can safely run this test without 'real' credentials.
     * Crude use of mocks to test behaviour.
     * It does not send anything down the wire.
     */
    @Ignore("FIX me!")
    @Test
    public void testEmailAlerterSendsMailSuccessfullyUsingMockTransport() throws Exception {

        // Partial mock Email Alerter - we don't need to test the config loading; it has its own tests.
        final EmailAlertsType emailAlertsType = PowerMock.createMock(EmailAlertsType.class);
        PowerMock.mockStaticPartial(EmailAlerter.class, "loadEmailAlerterConfig");
        PowerMock.expectPrivate(EmailAlerter.class, "loadEmailAlerterConfig").andReturn(emailAlertsType);

        // expect to load full config
        expect(emailAlertsType.isEnabled()).andReturn(true);
        final SmtpConfigType smtpConfigType = PowerMock.createMock(SmtpConfigType.class);
        expect(emailAlertsType.getSmtpConfig()).andReturn(smtpConfigType);
        expect(smtpConfigType.getSmtpHost()).andReturn(SMTP_HOST);
        expect(smtpConfigType.getSmtpTlsPort()).andReturn(SMTP_TLS_PORT);
        expect(smtpConfigType.getAccountUsername()).andReturn(ACCOUNT_USERNAME);
        expect(smtpConfigType.getAccountPassword()).andReturn(ACCOUNT_PASSWORD);
        expect(smtpConfigType.getFromAddr()).andReturn(FROM_ADDRESS);
        expect(smtpConfigType.getToAddr()).andReturn(TO_ADDRESS);

        // expect to send message
        PowerMock.mockStatic(Transport.class);
        Transport.send(EasyMock.anyObject(Message.class));

        PowerMock.replayAll();

        // actual test
        final EmailAlerter emailAlerter = EmailAlerter.getInstance();
        emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);

        // all as expected?
        PowerMock.verifyAll();
    }

    /*
     * Requires real credentials to run test. Will actually send email out.
     * Good for testing that you're all setup before deployment.
     *
     * 1. Uncomment @Test.
     * 2. Change the ./config/email-alerts.xml to use your account SMTP settings.
     * 3. Comment out @RunWith(PowerMockRunner.class) and @PrepareForTest(Transport.class) at top of class - they mess
     *    with the SSLContext and the test will fail - no time to debug why but related to:
     *    https://code.google.com/p/powermock/issues/detail?id=288
     * 4. Run this test on its own.
     */
    //@Test
    public void testEmailAlerterReallySendsMailSuccessfully() throws Exception {

        final EmailAlerter emailAlerter = EmailAlerter.getInstance();
        emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);

        // expect to send message - check your inbox!
    }
}
