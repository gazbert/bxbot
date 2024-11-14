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

package com.gazbert.bxbot.services.config.impl;

import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.repository.EmailAlertsConfigRepository;
import com.gazbert.bxbot.services.config.EmailAlertsConfigService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Email Alerts configuration service.
 *
 * @author gazbert
 */
@Service("emailAlertsConfigService")
@Transactional
@ComponentScan(basePackages = {"com.gazbert.bxbot.repository"})
@Log4j2
public class EmailAlertsConfigServiceImpl implements EmailAlertsConfigService {

  private final EmailAlertsConfigRepository emailAlertsConfigRepository;

  /**
   * Creates the EmailAlertsConfigService.
   *
   * @param emailAlertsConfigRepository the email alerts config repo.
   */
  @Autowired
  public EmailAlertsConfigServiceImpl(
      @Qualifier("emailAlertsConfigYamlRepository")
          EmailAlertsConfigRepository emailAlertsConfigRepository) {
    this.emailAlertsConfigRepository = emailAlertsConfigRepository;
  }

  @Override
  public EmailAlertsConfig getEmailAlertsConfig() {
    return emailAlertsConfigRepository.get();
  }

  @Override
  public EmailAlertsConfig updateEmailAlertsConfig(EmailAlertsConfig config) {
    log.info("About to update Email Alerts config: {}", config);
    return emailAlertsConfigRepository.save(config);
  }
}
