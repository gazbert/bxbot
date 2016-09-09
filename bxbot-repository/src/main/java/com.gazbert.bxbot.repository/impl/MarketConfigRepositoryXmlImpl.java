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
import java.util.List;

import static com.gazbert.bxbot.datastore.FileLocations.MARKETS_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.MARKETS_CONFIG_XSD_FILENAME;

/**
 * Implementation of the Market config repository.
 *
 * @author gazbert
 */
@Repository("marketConfigRepository")
@Transactional
public class MarketConfigRepositoryXmlImpl implements MarketConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override

    public List<MarketConfig> findAllMarkets() {

        final MarketsType internalMarketsConfig = ConfigurationManager.loadConfig(MarketsType.class,
                MARKETS_CONFIG_XML_FILENAME, MARKETS_CONFIG_XSD_FILENAME);
        return adaptAllInternalToAllExternalConfig(internalMarketsConfig);
    }

    @Override
    public MarketConfig findById(String id) {
        throw new UnsupportedOperationException("findById not coded yet!");
    }

    @Override
    public MarketConfig createMarket(MarketConfig config) {
        throw new UnsupportedOperationException("createMarket not coded yet!");
    }

    @Override
    public MarketConfig updateMarket(MarketConfig config) {
        throw new UnsupportedOperationException("updateMarket not coded yet!");
    }

    @Override
    public MarketConfig deleteMarketById(String id) {
        throw new UnsupportedOperationException("deleteMarketById not coded yet!");
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
            marketConfig.setLabel(item.getLabel());
            marketConfig.setEnabled(item.isEnabled());
            marketConfig.setBaseCurrency(item.getBaseCurrency());
            marketConfig.setCounterCurrency(item.getCounterCurrency());
            marketConfig.setTradingStrategy(item.getTradingStrategy());

            marketConfigItems.add(marketConfig);
        });

        return marketConfigItems;
    }
}
