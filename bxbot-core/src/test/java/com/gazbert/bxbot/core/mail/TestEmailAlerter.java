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

import static org.easymock.EasyMock.expect;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.services.config.EmailAlertsConfigService;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test the Email Alerter behaves as expected.
 *
 * @author gazbert
 */
class TestEmailAlerter {

  private static final String EMAIL_SUBJECT = "CRITICAL Alert message from BX-bot";
  private static final String EMAIL_MSG = "The exchange has blown up!";

  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final int SMTP_TLS_PORT = 587;
  private static final String ACCOUNT_USERNAME = "your.account.username@gmail.com";
  private static final String ACCOUNT_PASSWORD = "le-password";
  private static final String FROM_ADDRESS = "bxbot.alerts@gmail.com";
  private static final String TO_ADDRESS = "some-destination@gmail.com";

  private EmailAlertsConfigService emailAlertsConfigService;

  @BeforeEach
  public void setup() {
    emailAlertsConfigService = EasyMock.createMock(EmailAlertsConfigService.class);
  }

  @Test
  void testEmailAlerterInitialisedSuccessfully() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig());
    EasyMock.replay(emailAlertsConfigService);

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    EasyMock.verify(emailAlertsConfigService);
  }

  @Test
  void testEmailAlerterInitialisedSuccessfullyWhenAlertsDisabledAndNoSmtpConfigSupplied() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsDisabledAndNoSmtpConfig());
    EasyMock.replay(emailAlertsConfigService);

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    EasyMock.verify(emailAlertsConfigService);
  }

  @Test
  void testEmailAlerterInitialisedSuccessfullyWhenAlertsDisabledAndSmtpConfigSupplied() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsDisabledAndSmtpConfig());
    EasyMock.replay(emailAlertsConfigService);

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    EasyMock.verify(emailAlertsConfigService);
  }

  @Test
  void testEmailAlerterInitialisationFailsWhenAlertsEnabledButNoSmtpConfigSupplied() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsEnabledAndNoSmtpConfig());
    EasyMock.replay(emailAlertsConfigService);

    assertThrows(IllegalStateException.class, () -> new EmailAlerter(emailAlertsConfigService));

    EasyMock.verify(emailAlertsConfigService);
  }

  /**
   * Requires real credentials to run test. Will actually send email out. Good for testing that
   * you're all setup before deployment.
   *
   * <ol>
   *   <li>Remove the @Disabled.
   *   <li>Change the [project-root]/config/email-alerts.yaml to use your account SMTP settings.
   *   <li>Run this test on its own.
   * </ol>
   */
  @Test
  @Disabled("Uncomment me for a live email test!")
  void testEmailAlerterReallySendsMailSuccessfully() {
    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);
    // expect to send message - check your inbox!
    assertNotNull(EMAIL_SUBJECT); // shut Sonar up ;-)
  }

  // ------------------------------------------------------------------------
  // Private utils
  // ------------------------------------------------------------------------

  private static EmailAlertsConfig someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig() {
    final SmtpConfig smtpConfig =
        new SmtpConfig(
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

  private static EmailAlertsConfig someEmailAlertsConfigWithAlertsDisabledAndSmtpConfig() {
    final SmtpConfig smtpConfig =
        new SmtpConfig(
            SMTP_HOST, SMTP_TLS_PORT, ACCOUNT_USERNAME, ACCOUNT_PASSWORD, FROM_ADDRESS, TO_ADDRESS);

    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    emailAlertsConfig.setEnabled(false);
    emailAlertsConfig.setSmtpConfig(smtpConfig);
    return emailAlertsConfig;
  }
}
