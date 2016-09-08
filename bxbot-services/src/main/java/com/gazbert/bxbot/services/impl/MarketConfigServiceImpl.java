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

package com.gazbert.bxbot.services.impl;


import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.MarketConfigRepository;
import com.gazbert.bxbot.services.MarketConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * TODO Work in progress...
 *
 * Implementation of the Market config service.
 *
 * @author gazbert
 */
@Service("marketConfigService")
@Transactional
@ComponentScan(basePackages = {"com.gazbert.bxbot.repository"})
public class MarketConfigServiceImpl implements MarketConfigService {

    private final MarketConfigRepository marketConfigRepository;

    @Autowired
    public MarketConfigServiceImpl(MarketConfigRepository marketConfigRepository) {
        Assert.notNull(marketConfigRepository, "marketConfigRepository dependency cannot be null!");
        this.marketConfigRepository = marketConfigRepository;
    }

    @Override
    public List<MarketConfig> findAllMarkets() {
        return Arrays.asList(getCannedMarketConfig());
    }

    @Override
    public MarketConfig findById(String id) {
        return getCannedMarketConfig();
    }

    @Override
    public MarketConfig saveMarket(MarketConfig config) {
        return getCannedMarketConfig();
    }

    @Override
    public MarketConfig updateMarket(MarketConfig config) {
        return getCannedMarketConfig();
    }

    @Override
    public MarketConfig deleteMarketById(String id) {
        return getCannedMarketConfig();
    }

    /*
     * TODO Hard code these for now - will come from Repository later...
     */
    private static MarketConfig getCannedMarketConfig() {
        final MarketConfig marketConfig = new MarketConfig("BTC/USD", "btc_usd", "BTC", "USD", true, "scalper-strategy");
        return marketConfig;
    }
}
