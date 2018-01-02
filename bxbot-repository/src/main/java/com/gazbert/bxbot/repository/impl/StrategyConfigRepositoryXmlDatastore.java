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
import com.gazbert.bxbot.datastore.strategy.generated.OptionalConfigType;
import com.gazbert.bxbot.datastore.strategy.generated.StrategyType;
import com.gazbert.bxbot.datastore.strategy.generated.TradingStrategiesType;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.repository.StrategyConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.gazbert.bxbot.datastore.FileLocations.STRATEGIES_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.STRATEGIES_CONFIG_XSD_FILENAME;

/**
 * An XML datastore implementation of the Strategy config repository.
 *
 * @author gazbert
 */
@Repository("strategyConfigRepository")
@Transactional
public class StrategyConfigRepositoryXmlDatastore implements StrategyConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public List<StrategyConfig> findAll() {

        LOG.info(() -> "Fetching all Strategy configs...");

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
    public StrategyConfig save(StrategyConfig config) {

        final TradingStrategiesType internalStrategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

        final List<StrategyType> strategyTypes = internalStrategiesConfig.getStrategies()
                .stream()
                .filter((item) -> item.getId().equals(config.getId()))
                .distinct()
                .collect(Collectors.toList());

        if (config.getId() == null || config.getId().isEmpty()) {

            LOG.info(() -> "About to create StrategyConfig: " + config);

            if (strategyTypes.isEmpty()) {

                final StrategyConfig newStrategyConfig = new StrategyConfig(config);
                newStrategyConfig.setId(generateUuid());

                internalStrategiesConfig.getStrategies().add(adaptExternalToInternalConfig(newStrategyConfig));
                ConfigurationManager.saveConfig(TradingStrategiesType.class, internalStrategiesConfig,
                        STRATEGIES_CONFIG_XML_FILENAME);

                final TradingStrategiesType updatedInternalStrategiesConfig = ConfigurationManager.loadConfig(
                        TradingStrategiesType.class, STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

                return adaptInternalToExternalConfig(
                        updatedInternalStrategiesConfig.getStrategies()
                                .stream()
                                .filter((item) -> item.getId().equals(newStrategyConfig.getId()))
                                .distinct()
                                .collect(Collectors.toList()));
            } else {
                throw new IllegalStateException("Trying to create new StrategyConfig but null/empty id already exists. " +
                        "StrategyConfig: " + config + " Existing StrategyConfigs: "
                        + adaptAllInternalToAllExternalConfig(internalStrategiesConfig));
            }

        } else {

            LOG.info(() -> "About to update StrategyConfig: " + config);

            if (!strategyTypes.isEmpty()) {

                internalStrategiesConfig.getStrategies().remove(strategyTypes.get(0)); // will only be 1 unique strat
                internalStrategiesConfig.getStrategies().add(adaptExternalToInternalConfig(config));
                ConfigurationManager.saveConfig(TradingStrategiesType.class, internalStrategiesConfig, STRATEGIES_CONFIG_XML_FILENAME);

                final TradingStrategiesType updatedInternalStrategiesConfig = ConfigurationManager.loadConfig(
                        TradingStrategiesType.class, STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);

                return adaptInternalToExternalConfig(
                        updatedInternalStrategiesConfig.getStrategies()
                                .stream()
                                .filter((item) -> item.getId().equals(config.getId()))
                                .distinct()
                                .collect(Collectors.toList()));
            } else {
                LOG.warn("Trying to update StrategyConfig but id does not exist StrategyConfig: " + config +
                        " Existing StrategyConfig: " + adaptAllInternalToAllExternalConfig(internalStrategiesConfig));
                return null;
            }
        }
    }

    @Override
    public StrategyConfig delete(String id) {

        LOG.info(() -> "Deleting Strategy config for id: " + id);

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
            LOG.warn("Trying to delete StrategyConfig but id does not exist. StrategyConfig id: " + id
                    + " Existing StrategyConfig: " + adaptAllInternalToAllExternalConfig(internalStrategiesConfig));
            return null;
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
            strategyConfig.setName(item.getName());
            strategyConfig.setDescription(item.getDescription());
            strategyConfig.setClassName(item.getClassName());
            strategyConfig.setBeanName(item.getBeanName());

            item.getOptionalConfig().getConfigItem().forEach(internalConfigItem ->
                    strategyConfig.getConfigItems().put(internalConfigItem.getName(), internalConfigItem.getValue()));

            strategyConfigItems.add(strategyConfig);
        });

        return strategyConfigItems;
    }

    private static StrategyConfig adaptInternalToExternalConfig(List<StrategyType> internalStrategyConfigItems) {

        if (!internalStrategyConfigItems.isEmpty()) {

            final StrategyConfig strategyConfig = new StrategyConfig();

            // Should only ever be 1 unique Strategy id
            final StrategyType internalStrategyConfig = internalStrategyConfigItems.get(0);
            strategyConfig.setId(internalStrategyConfig.getId());
            strategyConfig.setName(internalStrategyConfig.getName());
            strategyConfig.setDescription(internalStrategyConfig.getDescription());
            strategyConfig.setClassName(internalStrategyConfig.getClassName());
            strategyConfig.setBeanName(internalStrategyConfig.getBeanName());


            internalStrategyConfig.getOptionalConfig().getConfigItem().forEach(internalConfigItem ->
                    strategyConfig.getConfigItems().put(internalConfigItem.getName(), internalConfigItem.getValue()));

            return strategyConfig;
        }
        return null;
    }

    private static StrategyType adaptExternalToInternalConfig(StrategyConfig externalStrategyConfig) {

        final OptionalConfigType configurationType = new OptionalConfigType();
        externalStrategyConfig.getConfigItems().forEach((key, value) -> {
            final ConfigItemType configItem = new ConfigItemType();
            configItem.setName(key);
            configItem.setValue(value);
            configurationType.getConfigItem().add(configItem);
        });

        final StrategyType strategyType = new StrategyType();
        strategyType.setId(externalStrategyConfig.getId());
        strategyType.setName(externalStrategyConfig.getName());
        strategyType.setDescription(externalStrategyConfig.getDescription());
        strategyType.setClassName(externalStrategyConfig.getClassName());
        strategyType.setBeanName(externalStrategyConfig.getBeanName());
        strategyType.setOptionalConfig(configurationType);
        return strategyType;
    }

    // ------------------------------------------------------------------------------------------------
    // Util methods
    // ------------------------------------------------------------------------------------------------

    private String generateUuid() {
        return UUID.randomUUID().toString();
    }
}

