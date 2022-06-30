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

import static junit.framework.TestCase.assertNotNull;
import static org.easymock.EasyMock.expect;

import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.services.config.EmailAlertsConfigService;
import javax.mail.Message;
import javax.mail.Transport;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test the Email Alerter behaves as expected.
 * 测试电子邮件警报器的行为是否符合预期。
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Transport.class})
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
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
  public void setup() {
    emailAlertsConfigService = PowerMock.createMock(EmailAlertsConfigService.class);
  }

  @Test
  public void testEmailAlerterInitialisedSuccessfully() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig());
    PowerMock.replayAll();

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    PowerMock.verifyAll();
  }

  @Test
  public void testEmailAlerterInitialisedSuccessfullyWhenAlertsDisabledAndNoSmtpConfigSupplied() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsDisabledAndNoSmtpConfig());
    PowerMock.replayAll();

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    PowerMock.verifyAll();
  }

  @Test
  public void testEmailAlerterInitialisedSuccessfullyWhenAlertsDisabledAndSmtpConfigSupplied() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsDisabledAndSmtpConfig());
    PowerMock.replayAll();

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    PowerMock.verifyAll();
  }

  @Test(expected = IllegalStateException.class)
  public void testEmailAlerterInitialisationFailsWhenAlertsEnabledButNoSmtpConfigSupplied() {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsEnabledAndNoSmtpConfig());
    PowerMock.replayAll();

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    assertNotNull(emailAlerter);

    PowerMock.verifyAll();
  }

  /**
   * Can safely run this test without 'real' credentials.
    Crude use of mocks to test behaviour.
    It does not send anything down the wire.
   无需“真实”凭据即可安全运行此测试。
   粗略地使用模拟来测试行为。
   它不会发送任何东西。
   */
  @Test
  public void testEmailAlerterSendsMailSuccessfullyUsingMockTransport() throws Exception {
    expect(emailAlertsConfigService.getEmailAlertsConfig())
        .andReturn(someEmailAlertsConfigWithAlertsEnabledAndSmtpConfig());

    PowerMock.mockStatic(Transport.class);
    Transport.send(EasyMock.anyObject(Message.class));

    PowerMock.replayAll();

    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);

    PowerMock.verifyAll();
  }

  /**
   * Requires real credentials to run test. Will actually send email out. Good for testing that
    you're all setup before deployment.
   需要真实凭据才能运行测试。实际上会发送电子邮件。适合测试
   你在部署之前都已经设置好了。
   *
   * <ol>
   *   <li>Uncomment @Test.
   *   <li>取消注释@Test。
   *
   *   <li>Change the [project-root]/config/email-alerts.yaml to use your account SMTP settings.
   *   <li>更改 [project-root]/config/email-alerts.yaml 以使用您的帐户 SMTP 设置。
   *
   *   <li>Comment out @RunWith(PowerMockRunner.class) and @PrepareForTest(Transport.class) at top
          of class - they mess with the SSLContext and the test will fail - no time to debug why
          but related to: https://code.google.com/p/powermock/issues/detail?id=288
      <li>Run this test on its own.
   <li>注释掉顶部的@RunWith(PowerMockRunner.class) 和@PrepareForTest(Transport.class)
   类的-他们弄乱了 SSLContext 并且测试将失败-没有时间调试原因
   但相关：https://code.google.com/p/powermock/issues/detail?id=288
   <li>自行运行此测试。
   * </ol>
   */
  // @Test
  public void testEmailAlerterReallySendsMailSuccessfully() {
    final EmailAlerter emailAlerter = new EmailAlerter(emailAlertsConfigService);
    emailAlerter.sendMessage(EMAIL_SUBJECT, EMAIL_MSG);

    // expect to send message - check your inbox!
    // 期望发送消息 - 检查您的收件箱！
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
