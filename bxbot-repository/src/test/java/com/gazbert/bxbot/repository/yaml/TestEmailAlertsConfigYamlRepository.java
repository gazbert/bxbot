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

package com.gazbert.bxbot.repository.yaml;

import static com.gazbert.bxbot.datastore.yaml.FileLocations.EMAIL_ALERTS_CONFIG_YAML_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.emailalerts.EmailAlertsType;
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.repository.EmailAlertsConfigRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests YAML backed Email Alerts configuration repository behaves as expected.
 * * 测试 YAML 支持的电子邮件警报配置存储库的行为是否符合预期。
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
public class TestEmailAlertsConfigYamlRepository {

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
  public void setup() {
    PowerMock.mockStatic(ConfigurationManager.class);
  }

  @Test
  public void whenGetCalledThenExpectEmailAlertsConfigToBeReturned() {
    expect(
            ConfigurationManager.loadConfig(
                eq(EmailAlertsType.class), eq(EMAIL_ALERTS_CONFIG_YAML_FILENAME)))
        .andReturn(someEmailAlertsConfig());

    PowerMock.replayAll();

    final EmailAlertsConfigRepository emailAlertsConfigRepository =
        new EmailAlertsConfigYamlRepository();
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
  public void whenSaveCalledThenExpectRepositoryToSaveItAndReturnSavedEmailAlertsConfig() {
    ConfigurationManager.saveConfig(
        eq(EmailAlertsType.class),
        anyObject(EmailAlertsType.class),
        eq(EMAIL_ALERTS_CONFIG_YAML_FILENAME));

    expect(
            ConfigurationManager.loadConfig(
                eq(EmailAlertsType.class), eq(EMAIL_ALERTS_CONFIG_YAML_FILENAME)))
        .andReturn(adaptExternalToInternalConfig(someUpdatedEmailAlertsConfig()));

    PowerMock.replayAll();

    final EmailAlertsConfigRepository emailAlertsConfigRepository =
        new EmailAlertsConfigYamlRepository();
    final EmailAlertsConfig savedConfig =
        emailAlertsConfigRepository.save(someUpdatedEmailAlertsConfig());

    assertThat(savedConfig.isEnabled()).isEqualTo(ENABLED);
    assertThat(savedConfig.getSmtpConfig().getHost()).isEqualTo(UPDATED_HOST);
    assertThat(savedConfig.getSmtpConfig().getTlsPort()).isEqualTo(UPDATED_PORT);
    assertThat(savedConfig.getSmtpConfig().getFromAddress()).isEqualTo(UPDATED_FROM_ADDRESS);
    assertThat(savedConfig.getSmtpConfig().getToAddress()).isEqualTo(UPDATED_TO_ADDRESS);
    assertThat(savedConfig.getSmtpConfig().getAccountUsername())
        .isEqualTo(UPDATED_ACCOUNT_USERNAME);
    assertThat(savedConfig.getSmtpConfig().getAccountPassword())
        .isEqualTo(UPDATED_ACCOUNT_PASSWORD);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  // Private utils
  //私有工具
  // --------------------------------------------------------------------------

  private static EmailAlertsType someEmailAlertsConfig() {
    final SmtpConfig smtpConfig = new SmtpConfig();
    smtpConfig.setHost(HOST);
    smtpConfig.setTlsPort(TLS_PORT);
    smtpConfig.setToAddress(TO_ADDRESS);
    smtpConfig.setFromAddress(FROM_ADDRESS);
    smtpConfig.setAccountUsername(ACCOUNT_USERNAME);
    smtpConfig.setAccountPassword(ACCOUNT_PASSWORD);

    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    emailAlertsConfig.setEnabled(ENABLED);
    emailAlertsConfig.setSmtpConfig(smtpConfig);

    final EmailAlertsType emailAlertsType = new EmailAlertsType();
    emailAlertsType.setEmailAlerts(emailAlertsConfig);
    return emailAlertsType;
  }

  private static EmailAlertsConfig someUpdatedEmailAlertsConfig() {
    final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
    emailAlertsConfig.setEnabled(true);

    final SmtpConfig smtpConfig =
        new SmtpConfig(
            UPDATED_HOST,
            UPDATED_PORT,
            UPDATED_ACCOUNT_USERNAME,
            UPDATED_ACCOUNT_PASSWORD,
            UPDATED_FROM_ADDRESS,
            UPDATED_TO_ADDRESS);
    emailAlertsConfig.setSmtpConfig(smtpConfig);

    return emailAlertsConfig;
  }

  private static EmailAlertsType adaptExternalToInternalConfig(
      EmailAlertsConfig externalEmailAlertsConfig) {
    final EmailAlertsType emailAlertsType = new EmailAlertsType();
    emailAlertsType.setEmailAlerts(externalEmailAlertsConfig);
    return emailAlertsType;
  }
}
