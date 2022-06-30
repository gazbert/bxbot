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

import static com.gazbert.bxbot.datastore.yaml.FileLocations.EXCHANGE_CONFIG_YAML_FILENAME;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.exchange.ExchangeType;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.repository.ExchangeConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * An Exchange config repo that uses a YAML backed datastore.
 * 使用 YAML 支持的数据存储的 Exchange 配置存储库。
 *
 * @author gazbert
 */
@Repository("exchangeConfigYamlRepository")
@Transactional
public class ExchangeConfigYamlRepository implements ExchangeConfigRepository {

  private static final Logger LOG = LogManager.getLogger();

  @Override
  public ExchangeConfig get() {
    LOG.info(() -> "Fetching ExchangeConfig... 正在获取 Exchange 配置...");
    return ConfigurationManager.loadConfig(ExchangeType.class, EXCHANGE_CONFIG_YAML_FILENAME)
        .getExchange();
  }

  @Override
  public ExchangeConfig save(ExchangeConfig config) {
    LOG.info(() -> "About to save ExchangeConfig: 即将保存 Exchange 配置：" + config);

    final ExchangeType exchangeType = new ExchangeType();
    exchangeType.setExchange(config);
    ConfigurationManager.saveConfig(
        ExchangeType.class, exchangeType, EXCHANGE_CONFIG_YAML_FILENAME);

    return ConfigurationManager.loadConfig(ExchangeType.class, EXCHANGE_CONFIG_YAML_FILENAME)
        .getExchange();
  }
}
