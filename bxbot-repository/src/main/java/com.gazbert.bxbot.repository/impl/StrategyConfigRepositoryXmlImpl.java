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

package com.gazbert.bxbot.repository.impl;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.strategy.generated.ConfigItemType;
import com.gazbert.bxbot.datastore.strategy.generated.ConfigurationType;
import com.gazbert.bxbot.datastore.strategy.generated.StrategyType;
import com.gazbert.bxbot.datastore.strategy.generated.TradingStrategiesType;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.repository.StrategyConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.gazbert.bxbot.datastore.FileLocations.STRATEGIES_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.STRATEGIES_CONFIG_XSD_FILENAME;

/**
 * Implementation of the Strategy config repository.
 *
 * @author gazbert
 */
@Repository("strategyConfigRepository")
@Transactional
public class StrategyConfigRepositoryXmlImpl implements StrategyConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public List<StrategyConfig> findAllStrategies() {

        final TradingStrategiesType internalStrategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);
        return adaptAllInternalToAllExternalConfig(internalStrategiesConfig);
    }

    @Override
    public StrategyConfig findById(String id) {

        LOG.info(() -> "Fetching config for Strategy id: " + id);

        final TradingStrategiesType internalStrategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

        return adaptInternalToExternalConfig(
                internalStrategiesConfig.getStrategies()
                        .stream()
                        .filter((item) -> item.getId().equals(id))
                        .distinct()
                        .collect(Collectors.toList()));
    }

    @Override
    public StrategyConfig updateStrategy(StrategyConfig config) {

        LOG.info(() -> "About to update: " + config);

        final TradingStrategiesType internalStrategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

        final List<StrategyType> strategyTypes = internalStrategiesConfig.getStrategies()
                .stream()
                .filter((item) -> item.getId().equals(config.getId()))
                .distinct()
                .collect(Collectors.toList());

        if (!strategyTypes.isEmpty()) {

            internalStrategiesConfig.getStrategies().remove(strategyTypes.get(0)); // will only be 1 unique strat
            internalStrategiesConfig.getStrategies().add(adaptExternalToInternalConfig(config));
            ConfigurationManager.saveConfig(TradingStrategiesType.class, internalStrategiesConfig,
                    STRATEGIES_CONFIG_XML_FILENAME);

            final TradingStrategiesType updatedInternalStrategiesConfig = ConfigurationManager.loadConfig(
                    TradingStrategiesType.class, STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

            return adaptInternalToExternalConfig(
                    updatedInternalStrategiesConfig.getStrategies()
                            .stream()
                            .filter((item) -> item.getId().equals(config.getId()))
                            .distinct()
                            .collect(Collectors.toList()));
        } else {
            // no matching id :-(
            return new StrategyConfig();
        }
    }

    @Override
    public StrategyConfig saveStrategy(StrategyConfig config) {
        throw new UnsupportedOperationException("saveStrategy not coded yet");
    }

    @Override
    public StrategyConfig deleteStrategyById(String id) {

        LOG.info(() -> "Deleting config for Strategy id: " + id);

        final TradingStrategiesType internalStrategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

        final List<StrategyType> strategyTypes = internalStrategiesConfig.getStrategies()
                .stream()
                .filter((item) -> item.getId().equals(id))
                .distinct()
                .collect(Collectors.toList());

        if (!strategyTypes.isEmpty()) {

            final StrategyType strategyToRemove = strategyTypes.get(0); // will only be 1 unique strat
            internalStrategiesConfig.getStrategies().remove(strategyToRemove);
            ConfigurationManager.saveConfig(TradingStrategiesType.class, internalStrategiesConfig,
                    STRATEGIES_CONFIG_XML_FILENAME);

            return adaptInternalToExternalConfig(Collections.singletonList(strategyToRemove));
        } else {
            // no matching id :-(
            return new StrategyConfig();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static List<StrategyConfig> adaptAllInternalToAllExternalConfig(TradingStrategiesType internalStrategiesConfig) {

        final List<StrategyConfig> strategyConfigItems = new ArrayList<>();

        final List<StrategyType> internalStrategyConfigItems = internalStrategiesConfig.getStrategies();
        internalStrategyConfigItems.forEach((item) -> {

            final StrategyConfig strategyConfig = new StrategyConfig();
            strategyConfig.setId(item.getId());
            strategyConfig.setLabel(item.getLabel());
            strategyConfig.setDescription(item.getDescription());
            strategyConfig.setClassName(item.getClassName());

            item.getConfiguration().getConfigItem().forEach(internalConfigItem ->
                    strategyConfig.getConfigItems().put(internalConfigItem.getName(), internalConfigItem.getValue()));

            strategyConfigItems.add(strategyConfig);
        });

        return strategyConfigItems;
    }

    private static StrategyConfig adaptInternalToExternalConfig(List<StrategyType> internalStrategyConfigItems) {

        final StrategyConfig strategyConfig = new StrategyConfig();

        if (!internalStrategyConfigItems.isEmpty()) {

            // Should only ever be 1 unique Strategy id
            final StrategyType internalStrategyConfig = internalStrategyConfigItems.get(0);
            strategyConfig.setId(internalStrategyConfig.getId());
            strategyConfig.setLabel(internalStrategyConfig.getLabel());
            strategyConfig.setDescription(internalStrategyConfig.getDescription());
            strategyConfig.setClassName(internalStrategyConfig.getClassName());

            internalStrategyConfig.getConfiguration().getConfigItem().forEach(internalConfigItem ->
                    strategyConfig.getConfigItems().put(internalConfigItem.getName(), internalConfigItem.getValue()));
        }
        return strategyConfig;
    }

    private static StrategyType adaptExternalToInternalConfig(StrategyConfig externalStrategyConfig) {

        final ConfigurationType configurationType = new ConfigurationType();
        externalStrategyConfig.getConfigItems().entrySet()
                .forEach(item -> {
                    final ConfigItemType configItem = new ConfigItemType();
                    configItem.setName(item.getKey());
                    configItem.setValue(item.getValue());
                    configurationType.getConfigItem().add(configItem);
                });

        final StrategyType strategyType = new StrategyType();
        strategyType.setId(externalStrategyConfig.getId());
        strategyType.setLabel(externalStrategyConfig.getLabel());
        strategyType.setDescription(externalStrategyConfig.getDescription());
        strategyType.setClassName(externalStrategyConfig.getClassName());
        strategyType.setConfiguration(configurationType);
        return strategyType;
    }
}