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
import com.gazbert.bxbot.datastore.market.generated.MarketType;
import com.gazbert.bxbot.datastore.market.generated.MarketsType;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.MarketConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.gazbert.bxbot.datastore.FileLocations.MARKETS_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.MARKETS_CONFIG_XSD_FILENAME;

/**
 * An XML datastore implementation of the Market config repository.
 *
 * @author gazbert
 */
@Repository("marketConfigRepository")
@Transactional
public class MarketConfigRepositoryXmlDatastore implements MarketConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override

    public List<MarketConfig> findAll() {

        LOG.info(() -> "Fetching all Market configs...");

        final MarketsType internalMarketsConfig = ConfigurationManager.loadConfig(MarketsType.class,
                MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);
        return adaptAllInternalToAllExternalConfig(internalMarketsConfig);
    }

    @Override
    public MarketConfig findById(String id) {

        LOG.info(() -> "Fetching Market config for id: " + id);

        final MarketsType internalMarketsConfig = ConfigurationManager.loadConfig(MarketsType.class,
                MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);

        return adaptInternalToExternalConfig(
                internalMarketsConfig.getMarkets()
                        .stream()
                        .filter((item) -> item.getId().equals(id))
                        .distinct()
                        .collect(Collectors.toList()));
    }

    @Override
    public MarketConfig save(MarketConfig config) {

        final MarketsType internalMarketsConfig = ConfigurationManager.loadConfig(MarketsType.class,
                MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);

        final List<MarketType> marketTypes = internalMarketsConfig.getMarkets()
                .stream()
                .filter((item) -> item.getId().equals(config.getId()))
                .distinct()
                .collect(Collectors.toList());

        if (config.getId() == null || config.getId().isEmpty()) {

            LOG.info(() -> "About to create MarketConfig: " + config);

            if (marketTypes.isEmpty()) {

                final MarketConfig newMarketConfig = new MarketConfig(config);
                newMarketConfig.setId(generateUuid());

                internalMarketsConfig.getMarkets().add(adaptExternalToInternalConfig(newMarketConfig));
                ConfigurationManager.saveConfig(MarketsType.class, internalMarketsConfig,
                        MARKETS_CONFIG_XML_FILENAME);

                final MarketsType updatedInternalMarketsConfig = ConfigurationManager.loadConfig(
                        MarketsType.class, MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);

                return adaptInternalToExternalConfig(
                        updatedInternalMarketsConfig.getMarkets()
                                .stream()
                                .filter((item) -> item.getId().equals(newMarketConfig.getId()))
                                .distinct()
                                .collect(Collectors.toList()));
            } else {
                throw new IllegalStateException("Trying to create new MarketConfig but null/empty id already exists. " +
                        "MarketConfig: " + config + " Existing MarketConfig: "
                        + adaptAllInternalToAllExternalConfig(internalMarketsConfig));
            }

        } else {

            LOG.info(() -> "About to update MarketConfig: " + config);

            if (!marketTypes.isEmpty()) {

                internalMarketsConfig.getMarkets().remove(marketTypes.get(0)); // will only be 1 unique strat
                internalMarketsConfig.getMarkets().add(adaptExternalToInternalConfig(config));
                ConfigurationManager.saveConfig(MarketsType.class, internalMarketsConfig,
                        MARKETS_CONFIG_XML_FILENAME);

                final MarketsType updatedInternalMarketsConfig = ConfigurationManager.loadConfig(
                        MarketsType.class, MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);

                return adaptInternalToExternalConfig(
                        updatedInternalMarketsConfig.getMarkets()
                                .stream()
                                .filter((item) -> item.getId().equals(config.getId()))
                                .distinct()
                                .collect(Collectors.toList()));
            } else {
                LOG.warn("Trying to update MarketConfig but id does not exist MarketConfig: " + config +
                        " Existing MarketConfig: " + adaptAllInternalToAllExternalConfig(internalMarketsConfig));
                return new MarketConfig();
            }
        }
    }

    @Override
    public MarketConfig delete(String id) {

        LOG.info(() -> "Deleting Market config for id: " + id);

        final MarketsType internalMarketsConfig = ConfigurationManager.loadConfig(MarketsType.class,
                MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);

        final List<MarketType> marketTypes = internalMarketsConfig.getMarkets()
                .stream()
                .filter((item) -> item.getId().equals(id))
                .distinct()
                .collect(Collectors.toList());

        if (!marketTypes.isEmpty()) {

            final MarketType marketToRemove = marketTypes.get(0); // will only be 1 unique strat
            internalMarketsConfig.getMarkets().remove(marketToRemove);
            ConfigurationManager.saveConfig(MarketsType.class, internalMarketsConfig,
                    MARKETS_CONFIG_XML_FILENAME);

            return adaptInternalToExternalConfig(Collections.singletonList(marketToRemove));
        } else {
            // no matching id :-(
            return new MarketConfig();
        }

    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static List<MarketConfig> adaptAllInternalToAllExternalConfig(MarketsType internalMarketsConfig) {

        final List<MarketConfig> marketConfigItems = new ArrayList<>();

        final List<MarketType> internalMarketConfigItems = internalMarketsConfig.getMarkets();
        internalMarketConfigItems.forEach((item) -> {

            final MarketConfig marketConfig = new MarketConfig();
            marketConfig.setId(item.getId());
            marketConfig.setName(item.getName());
            marketConfig.setEnabled(item.isEnabled());
            marketConfig.setBaseCurrency(item.getBaseCurrency());
            marketConfig.setCounterCurrency(item.getCounterCurrency());
            marketConfig.setTradingStrategyId(item.getTradingStrategyId());

            marketConfigItems.add(marketConfig);
        });

        return marketConfigItems;
    }

    private static MarketConfig adaptInternalToExternalConfig(List<MarketType> internalMarketConfigItems) {

        final MarketConfig marketConfig = new MarketConfig();

        if (!internalMarketConfigItems.isEmpty()) {

            // Should only ever be 1 unique Market id
            final MarketType internalMarketConfig = internalMarketConfigItems.get(0);
            marketConfig.setId(internalMarketConfig.getId());
            marketConfig.setName(internalMarketConfig.getName());
            marketConfig.setEnabled(internalMarketConfig.isEnabled());
            marketConfig.setBaseCurrency(internalMarketConfig.getBaseCurrency());
            marketConfig.setCounterCurrency(internalMarketConfig.getCounterCurrency());
            marketConfig.setTradingStrategyId(internalMarketConfig.getTradingStrategyId());
        }
        return marketConfig;
    }

    private static MarketType adaptExternalToInternalConfig(MarketConfig externalMarketConfig) {

        final MarketType marketType = new MarketType();
        marketType.setId(externalMarketConfig.getId());
        marketType.setName(externalMarketConfig.getName());
        marketType.setEnabled(externalMarketConfig.isEnabled());
        marketType.setBaseCurrency(externalMarketConfig.getBaseCurrency());
        marketType.setCounterCurrency(externalMarketConfig.getCounterCurrency());
        marketType.setTradingStrategyId(externalMarketConfig.getTradingStrategyId());
        return marketType;
    }

    // ------------------------------------------------------------------------------------------------
    // Util methods
    // ------------------------------------------------------------------------------------------------

    private String generateUuid() {
        return UUID.randomUUID().toString();
    }
}
