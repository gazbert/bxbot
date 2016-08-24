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

package com.gazbert.bxbot.core.admin.services;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.strategy.StrategyConfig;
import com.gazbert.bxbot.core.config.strategy.generated.StrategyType;
import com.gazbert.bxbot.core.config.strategy.generated.TradingStrategiesType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.gazbert.bxbot.core.config.strategy.StrategyConfig.STRATEGIES_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.core.config.strategy.StrategyConfig.STRATEGIES_CONFIG_XSD_FILENAME;

/**
 * TODO Work in progress...
 *
 * @author gazbert
 * @since 12/08/2016
 */
@Service("strategyConfigService")
@Transactional
public class StrategyConfigServiceImpl implements StrategyConfigService {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public List<StrategyConfig> findAllStrategies() {

        final TradingStrategiesType internalStrategiesConfig = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                STRATEGIES_CONFIG_XML_FILENAME, STRATEGIES_CONFIG_XSD_FILENAME);
        return adaptInternalToExternalConfig(internalStrategiesConfig);
    }

    @Override
    public StrategyConfig findById(String id) {
        throw new UnsupportedOperationException("findById not coded yet");
    }

    @Override
    public StrategyConfig findByName(String name) {
        throw new UnsupportedOperationException("findByName not coded yet");
    }

    @Override
    public StrategyConfig saveStrategy(StrategyConfig config) {
        throw new UnsupportedOperationException("saveStrategy not coded yet");
    }

    @Override
    public StrategyConfig updateStrategy(StrategyConfig config) {
        throw new UnsupportedOperationException("updateStrategy not coded yet");
    }

    @Override
    public StrategyConfig deleteStrategyById(String id) {
        throw new UnsupportedOperationException("deleteStrategyById not coded yet");
    }

    @Override
    public List<StrategyConfig> deleteAllStrategies() {
        throw new UnsupportedOperationException("deleteAllStrategies not coded yet");
    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static List<StrategyConfig> adaptInternalToExternalConfig(TradingStrategiesType internalStrategiesConfig) {

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
}