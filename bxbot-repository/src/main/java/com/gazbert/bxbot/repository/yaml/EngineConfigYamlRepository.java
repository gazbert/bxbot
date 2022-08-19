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

import static com.gazbert.bxbot.datastore.yaml.FileLocations.ENGINE_CONFIG_YAML_FILENAME;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.engine.EngineType;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.repository.EngineConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * An Engine config repo that uses a YAML backed datastore.
 * 使用 YAML 支持的数据存储的引擎配置存储库。
 *
 * @author gazbert
 */
@Repository("engineConfigYamlRepository")
@Transactional
public class EngineConfigYamlRepository implements EngineConfigRepository {

  private static final Logger LOG = LogManager.getLogger();

  @Override
  public EngineConfig get() {
    LOG.info(() -> "Fetching EngineConfig... 正在获取引擎配置...");
    return ConfigurationManager.loadConfig(EngineType.class, ENGINE_CONFIG_YAML_FILENAME)
        .getEngine();
  }

  @Override
  public EngineConfig save(EngineConfig config) {
    LOG.info(() -> "About to save EngineConfig: 关于保存 EngineConfig：" + config);

    final EngineType engineType = new EngineType();
    engineType.setEngine(config);
    ConfigurationManager.saveConfig(EngineType.class, engineType, ENGINE_CONFIG_YAML_FILENAME);

    return ConfigurationManager.loadConfig(EngineType.class, ENGINE_CONFIG_YAML_FILENAME)
        .getEngine();
  }
}
