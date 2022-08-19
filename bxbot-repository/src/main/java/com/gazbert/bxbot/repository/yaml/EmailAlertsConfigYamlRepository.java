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

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.emailalerts.EmailAlertsType;
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.repository.EmailAlertsConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * An Email Alerts config repo that uses a YAML backed datastore.
 * 使用 YAML 支持的数据存储的电子邮件警报配置存储库。
 *
 * @author gazbert
 */
@Repository("emailAlertsConfigYamlRepository")
@Transactional
public class EmailAlertsConfigYamlRepository implements EmailAlertsConfigRepository {

  private static final Logger LOG = LogManager.getLogger();

  @Override
  public EmailAlertsConfig get() {
    LOG.info(() -> "Fetching EmailAlertsConfig... 正在获取电子邮件警报配置...");
    return ConfigurationManager.loadConfig(EmailAlertsType.class, EMAIL_ALERTS_CONFIG_YAML_FILENAME)
        .getEmailAlerts();
  }

  @Override
  public EmailAlertsConfig save(EmailAlertsConfig config) {
    LOG.info(() -> "About to save EmailAlertsConfig: 关于保存电子邮件警报配置：" + config);

    final EmailAlertsType emailAlertsType = new EmailAlertsType();
    emailAlertsType.setEmailAlerts(config);
    ConfigurationManager.saveConfig(
        EmailAlertsType.class, emailAlertsType, EMAIL_ALERTS_CONFIG_YAML_FILENAME);

    return ConfigurationManager.loadConfig(EmailAlertsType.class, EMAIL_ALERTS_CONFIG_YAML_FILENAME)
        .getEmailAlerts();
  }
}
