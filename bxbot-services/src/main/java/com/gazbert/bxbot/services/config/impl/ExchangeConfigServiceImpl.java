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

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.repository.ExchangeConfigRepository;
import com.gazbert.bxbot.services.config.ExchangeConfigService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Exchange config service.
 *
 * @author gazbert
 */
@Service("exchangeConfigService")
@Transactional
@ComponentScan(basePackages = {"com.gazbert.bxbot.repository"})
@Log4j2
public class ExchangeConfigServiceImpl implements ExchangeConfigService {

  private final ExchangeConfigRepository exchangeConfigRepository;

  /**
   * Creates the ExchangeConfigService.
   *
   * @param exchangeConfigRepository the exchange config repo.
   */
  @Autowired
  public ExchangeConfigServiceImpl(
      @Qualifier("exchangeConfigYamlRepository")
          ExchangeConfigRepository exchangeConfigRepository) {
    this.exchangeConfigRepository = exchangeConfigRepository;
  }

  @Override
  public ExchangeConfig getExchangeConfig() {
    return exchangeConfigRepository.get();
  }

  @Override
  public ExchangeConfig updateExchangeConfig(ExchangeConfig config) {
    log.info("About to update Exchange config: " + config);
    return exchangeConfigRepository.save(config);
  }
}
