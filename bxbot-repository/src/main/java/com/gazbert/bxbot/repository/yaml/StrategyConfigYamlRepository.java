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

import static com.gazbert.bxbot.datastore.yaml.FileLocations.STRATEGIES_CONFIG_YAML_FILENAME;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.strategy.StrategiesType;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.repository.StrategyConfigRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Strategy config repo that uses a YAML backed datastore.
 *
 * @author gazbert
 */
@Repository("strategyConfigYamlRepository")
@Transactional
@Log4j2
public class StrategyConfigYamlRepository implements StrategyConfigRepository {

  private final ConfigurationManager configurationManager;

  /**
   * Creates the Strategy config YAML repo.
   *
   * @param configurationManager the config manager.
   */
  public StrategyConfigYamlRepository(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
  }

  @Override
  public List<StrategyConfig> findAll() {
    log.info("Fetching all Strategy configs...");
    return configurationManager
        .loadConfig(StrategiesType.class, STRATEGIES_CONFIG_YAML_FILENAME)
        .getStrategies();
  }

  @Override
  public StrategyConfig findById(String id) {
    log.info("Fetching config for Strategy id: {}", id);

    final StrategiesType strategiesType =
        configurationManager.loadConfig(StrategiesType.class, STRATEGIES_CONFIG_YAML_FILENAME);

    return adaptInternalToExternalConfig(
        strategiesType.getStrategies().stream()
            .filter(item -> item.getId().equals(id))
            .distinct()
            .collect(Collectors.toList()));
  }

  @Override
  public StrategyConfig save(StrategyConfig config) {
    final StrategiesType strategiesType =
        configurationManager.loadConfig(StrategiesType.class, STRATEGIES_CONFIG_YAML_FILENAME);

    final List<StrategyConfig> strategyConfigs =
        strategiesType.getStrategies().stream()
            .filter(item -> item.getId().equals(config.getId()))
            .distinct()
            .toList();

    if (config.getId() == null || config.getId().isEmpty()) {
      log.info("About to create StrategyConfig: {}", config);
      if (strategyConfigs.isEmpty()) {

        final StrategyConfig newStrategyConfig = new StrategyConfig(config);
        newStrategyConfig.setId(generateUuid());

        strategiesType.getStrategies().add(newStrategyConfig);
        configurationManager.saveConfig(
            StrategiesType.class, strategiesType, STRATEGIES_CONFIG_YAML_FILENAME);

        return newStrategyConfig;
      } else {
        throw new IllegalStateException(
            "Trying to create new StrategyConfig but null/empty id already exists. "
                + "StrategyConfig: "
                + config
                + " Existing StrategyConfigs: "
                + strategiesType.getStrategies());
      }
    } else {
      log.info("About to update StrategyConfig: {}", config);

      if (!strategyConfigs.isEmpty()) {
        strategiesType
            .getStrategies()
            .remove(strategyConfigs.get(0)); // will only be 1 unique strat
        strategiesType.getStrategies().add(config);
        configurationManager.saveConfig(
            StrategiesType.class, strategiesType, STRATEGIES_CONFIG_YAML_FILENAME);

        final StrategiesType updatedStrategiesType =
            configurationManager.loadConfig(StrategiesType.class, STRATEGIES_CONFIG_YAML_FILENAME);

        return adaptInternalToExternalConfig(
            updatedStrategiesType.getStrategies().stream()
                .filter(item -> item.getId().equals(config.getId()))
                .distinct()
                .collect(Collectors.toList()));
      } else {
        log.warn(
            "Trying to update StrategyConfig but id does not exist StrategyConfig: "
                + config
                + " Existing StrategyConfig: "
                + strategiesType.getStrategies());
        return null;
      }
    }
  }

  @Override
  public StrategyConfig delete(String id) {
    log.info("Deleting Strategy config for id: {}", id);

    final StrategiesType strategiesType =
        configurationManager.loadConfig(StrategiesType.class, STRATEGIES_CONFIG_YAML_FILENAME);

    final List<StrategyConfig> strategyConfigs =
        strategiesType.getStrategies().stream()
            .filter(item -> item.getId().equals(id))
            .distinct()
            .toList();

    if (!strategyConfigs.isEmpty()) {
      final StrategyConfig strategyToRemove = strategyConfigs.get(0); // will only be 1 unique strat
      strategiesType.getStrategies().remove(strategyToRemove);
      configurationManager.saveConfig(
          StrategiesType.class, strategiesType, STRATEGIES_CONFIG_YAML_FILENAME);
      return adaptInternalToExternalConfig(Collections.singletonList(strategyToRemove));
    } else {
      log.warn(
          "Trying to delete StrategyConfig but id does not exist. StrategyConfig id: "
              + id
              + " Existing StrategyConfig: "
              + strategiesType.getStrategies());
      return null;
    }
  }

  // --------------------------------------------------------------------------
  // Adapter methods
  // --------------------------------------------------------------------------

  private static StrategyConfig adaptInternalToExternalConfig(
      List<StrategyConfig> internalStrategyConfigItems) {
    if (!internalStrategyConfigItems.isEmpty()) {
      // Should only ever be 1 unique Strategy id
      return internalStrategyConfigItems.get(0);
    }
    return null;
  }

  // --------------------------------------------------------------------------
  // Util methods
  // --------------------------------------------------------------------------

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }
}
