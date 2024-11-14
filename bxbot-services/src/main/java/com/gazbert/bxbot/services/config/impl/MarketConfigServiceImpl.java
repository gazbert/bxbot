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

import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.MarketConfigRepository;
import com.gazbert.bxbot.services.config.MarketConfigService;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Market config service.
 *
 * @author gazbert
 */
@Service("marketConfigService")
@Transactional
@ComponentScan(basePackages = {"com.gazbert.bxbot.repository"})
@Log4j2
public class MarketConfigServiceImpl implements MarketConfigService {

  private final MarketConfigRepository marketConfigRepository;

  /**
   * Creates the MarketConfigService.
   *
   * @param marketConfigRepository the market config repo.
   */
  @Autowired
  public MarketConfigServiceImpl(
      @Qualifier("marketConfigYamlRepository") MarketConfigRepository marketConfigRepository) {
    this.marketConfigRepository = marketConfigRepository;
  }

  @Override
  public List<MarketConfig> getAllMarketConfig() {
    return marketConfigRepository.findAll();
  }

  @Override
  public MarketConfig getMarketConfig(String id) {
    log.info("Fetching Market config for id: {}", id);
    return marketConfigRepository.findById(id);
  }

  @Override
  public MarketConfig updateMarketConfig(MarketConfig config) {
    log.info("About to update Market config: {}", config);
    return marketConfigRepository.save(config);
  }

  @Override
  public MarketConfig createMarketConfig(MarketConfig config) {
    log.info("About to create Market config: {}", config);
    return marketConfigRepository.save(config);
  }

  @Override
  public MarketConfig deleteMarketConfig(String id) {
    log.info("About to delete Market config for id: {}", id);
    return marketConfigRepository.delete(id);
  }
}
