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

package com.gazbert.bxbot.domain.emailalerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests a SmtpConfig domain object behaves as expected.
 * 测试 SmtpConfig 域对象的行为是否符合预期。
 *
 * @author gazbert
 */
class TestSmtpConfig {

  private static final String HOST = "mail.google.com";
  private static final int TLS_PORT = 587;
  private static final String ACCOUNT_USERNAME = "user@google.com";
  private static final String ACCOUNT_PASSWORD = "myPass";
  private static final String FROM_ADDRESS = "from.me@google.com";
  private static final String TO_ADDRESS = "to.them@google.com";

  @Test
  void testInitialisationWorksAsExpected() {
    final SmtpConfig emailAlertsConfig =
        new SmtpConfig(
            HOST, TLS_PORT, ACCOUNT_USERNAME, ACCOUNT_PASSWORD, FROM_ADDRESS, TO_ADDRESS);
    assertEquals(HOST, emailAlertsConfig.getHost());
    assertEquals(TLS_PORT, emailAlertsConfig.getTlsPort());
    assertEquals(ACCOUNT_USERNAME, emailAlertsConfig.getAccountUsername());
    assertEquals(ACCOUNT_PASSWORD, emailAlertsConfig.getAccountPassword());
    assertEquals(FROM_ADDRESS, emailAlertsConfig.getFromAddress());
    assertEquals(TO_ADDRESS, emailAlertsConfig.getToAddress());
  }

  @Test
  void testSettersWorkAsExpected() {
    final SmtpConfig emailAlertsConfig = new SmtpConfig();
    assertNull(emailAlertsConfig.getHost());
    assertEquals(0, emailAlertsConfig.getTlsPort());
    assertNull(emailAlertsConfig.getAccountUsername());
    assertNull(emailAlertsConfig.getAccountPassword());
    assertNull(emailAlertsConfig.getFromAddress());
    assertNull(emailAlertsConfig.getToAddress());

    emailAlertsConfig.setHost(HOST);
    assertEquals(HOST, emailAlertsConfig.getHost());

    emailAlertsConfig.setTlsPort(TLS_PORT);
    assertEquals(TLS_PORT, emailAlertsConfig.getTlsPort());

    emailAlertsConfig.setAccountUsername(ACCOUNT_USERNAME);
    assertEquals(ACCOUNT_USERNAME, emailAlertsConfig.getAccountUsername());

    emailAlertsConfig.setAccountPassword(ACCOUNT_PASSWORD);
    assertEquals(ACCOUNT_PASSWORD, emailAlertsConfig.getAccountPassword());

    emailAlertsConfig.setFromAddress(FROM_ADDRESS);
    assertEquals(FROM_ADDRESS, emailAlertsConfig.getFromAddress());

    emailAlertsConfig.setToAddress(TO_ADDRESS);
    assertEquals(TO_ADDRESS, emailAlertsConfig.getToAddress());
  }

  @Test
  void testToStringWorksAsExpected() {
    final SmtpConfig emailAlertsConfig = new SmtpConfig();
    emailAlertsConfig.setHost(HOST);
    emailAlertsConfig.setTlsPort(TLS_PORT);
    emailAlertsConfig.setAccountUsername(ACCOUNT_USERNAME);
    emailAlertsConfig.setAccountPassword(ACCOUNT_PASSWORD);
    emailAlertsConfig.setFromAddress(FROM_ADDRESS);
    emailAlertsConfig.setToAddress(TO_ADDRESS);

    assertEquals(
        "SmtpConfig{host=mail.google.com, tlsPort=587, accountUsername=user@google.com,"
            + " fromAddress=from.me@google.com, toAddress=to.them@google.com}",
        emailAlertsConfig.toString());
  }
}
