/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.datastore.yaml.emailalerts;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import org.junit.Test;

/**
 * Tests the Email Alerts configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestEmailAlertsConfigurationManagement {

  private static final String VALID_YAML_CONFIG_FILENAME =
      "src/test/config/emailalerts/valid-email-alerts.yaml";
  private static final String INVALID_YAML_CONFIG_FILENAME =
      "src/test/config/emailalerts/invalid-email-alerts.yaml";
  private static final String VALID_YAML_CONFIG_WITHOUT_EMAIL_ALERTS_FILENAME =
      "src/test/config/emailalerts/valid-email-alerts-without-smtp-config.yaml";
  private static final String MISSING_YAML_CONFIG_FILENAME =
      "src/test/config/emailalerts/missing-email-alerts.yaml";
  private static final String YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/emailalerts/saved-email-alerts.yaml";
  private static final String INVALID_YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/not-here/saved-email-alerts.yaml";

  private static final String HOST = "mail.google.com";
  private static final int TLS_PORT = 587;
  private static final String ACCOUNT_USERNAME = "user@google.com";
  private static final String ACCOUNT_PASSWORD = "myPass";
  private static final String FROM_ADDRESS = "from.me@google.com";
  private static final String TO_ADDRESS = "to.them@google.com";

  @Test
  public void testLoadingValidYamlConfigFileIsSuccessful() {
    final EmailAlertsType emailAlertsType =
        ConfigurationManager.loadConfig(EmailAlertsType.class, VALID_YAML_CONFIG_FILENAME);

    assertTrue(emailAlertsType.getEmailAlerts().isEnabled());

    final SmtpConfig smtpConfig = emailAlertsType.getEmailAlerts().getSmtpConfig();
    assertEquals("smtp.gmail.com", smtpConfig.getHost());
    assertEquals(587, smtpConfig.getTlsPort());
    assertEquals("your.account.username@gmail.com", smtpConfig.getAccountUsername());
    assertEquals("your.account.password", smtpConfig.getAccountPassword());
    assertEquals("from.addr@gmail.com", smtpConfig.getFromAddress());
    assertEquals("to.addr@gmail.com", smtpConfig.getToAddress());
  }

  @Test
  public void testLoadingValidYamlConfigFileWithoutSmtpConfigIsSuccessful() {
    final EmailAlertsType emailAlertsType =
        ConfigurationManager.loadConfig(
            EmailAlertsType.class, VALID_YAML_CONFIG_WITHOUT_EMAIL_ALERTS_FILENAME);
    assertFalse(emailAlertsType.getEmailAlerts().isEnabled());
  }

  @Test(expected = IllegalStateException.class)
  public void testLoadingMissingYamlConfigThrowsException() {
    ConfigurationManager.loadConfig(EmailAlertsType.class, MISSING_YAML_CONFIG_FILENAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLoadingInvalidYamlConfigFileThrowsException() {
    ConfigurationManager.loadConfig(EmailAlertsType.class, INVALID_YAML_CONFIG_FILENAME);
  }

  @Test
  public void testSavingConfigToXmlIsSuccessful() throws Exception {
    final SmtpConfig smtpConfig = new SmtpConfig();
    smtpConfig.setAccountUsername(ACCOUNT_USERNAME);
    smtpConfig.setAccountPassword(ACCOUNT_PASSWORD);
    smtpConfig.setHost(HOST);
    smtpConfig.setTlsPort(TLS_PORT);
    smtpConfig.setFromAddress(FROM_ADDRESS);
    smtpConfig.setToAddress(TO_ADDRESS);

    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    emailAlertsConfig.setEnabled(true);
    emailAlertsConfig.setSmtpConfig(smtpConfig);

    final EmailAlertsType emailAlertsType = new EmailAlertsType();
    emailAlertsType.setEmailAlerts(emailAlertsConfig);

    ConfigurationManager.saveConfig(
        EmailAlertsType.class, emailAlertsType, YAML_CONFIG_TO_SAVE_FILENAME);

    // Read it back in
    final EmailAlertsType emailAlertsReloaded =
        ConfigurationManager.loadConfig(EmailAlertsType.class, YAML_CONFIG_TO_SAVE_FILENAME);

    assertThat(emailAlertsReloaded.getEmailAlerts().isEnabled()).isEqualTo(true);
    assertThat(emailAlertsReloaded.getEmailAlerts().getSmtpConfig().getAccountUsername())
        .isEqualTo(ACCOUNT_USERNAME);
    assertThat(emailAlertsReloaded.getEmailAlerts().getSmtpConfig().getAccountPassword())
        .isEqualTo(ACCOUNT_PASSWORD);
    assertThat(emailAlertsReloaded.getEmailAlerts().getSmtpConfig().getHost()).isEqualTo(HOST);
    assertThat(emailAlertsReloaded.getEmailAlerts().getSmtpConfig().getTlsPort())
        .isEqualTo(TLS_PORT);
    assertThat(emailAlertsReloaded.getEmailAlerts().getSmtpConfig().getFromAddress())
        .isEqualTo(FROM_ADDRESS);
    assertThat(emailAlertsReloaded.getEmailAlerts().getSmtpConfig().getToAddress())
        .isEqualTo(TO_ADDRESS);

    // cleanup
    Files.delete(FileSystems.getDefault().getPath(YAML_CONFIG_TO_SAVE_FILENAME));
  }

  @Test(expected = IllegalStateException.class)
  public void testSavingConfigToInvalidYamlFileIsHandled() {
    final SmtpConfig smtpConfig = new SmtpConfig();
    smtpConfig.setAccountUsername(ACCOUNT_USERNAME);
    smtpConfig.setAccountPassword(ACCOUNT_PASSWORD);
    smtpConfig.setHost(HOST);
    smtpConfig.setTlsPort(TLS_PORT);
    smtpConfig.setFromAddress(FROM_ADDRESS);
    smtpConfig.setToAddress(TO_ADDRESS);

    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    emailAlertsConfig.setEnabled(true);
    emailAlertsConfig.setSmtpConfig(smtpConfig);

    final EmailAlertsType emailAlertsType = new EmailAlertsType();
    emailAlertsType.setEmailAlerts(emailAlertsConfig);

    ConfigurationManager.saveConfig(
        EmailAlertsType.class, emailAlertsType, INVALID_YAML_CONFIG_TO_SAVE_FILENAME);
  }
}
