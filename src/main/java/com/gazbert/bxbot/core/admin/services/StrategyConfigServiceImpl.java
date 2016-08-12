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

import com.gazbert.bxbot.core.config.strategy.StrategyConfig;
import com.gazbert.bxbot.core.config.strategy.StrategyConfigItems;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * TODO Work in progress...
 *
 * @author gazbert
 * @since 12/08/2016
 */
@Service("strategyConfigService")
@Transactional
public class StrategyConfigServiceImpl implements StrategyConfigService {

    @Override
    public StrategyConfig findById(String id) {
        return getCannedStrategyConfig();
    }

    @Override
    public StrategyConfig findByName(String name) {
        return getCannedStrategyConfig();
    }

    @Override
    public StrategyConfig saveStrategy(StrategyConfig config) {
        return getCannedStrategyConfig();
    }

    @Override
    public StrategyConfig updateStrategy(StrategyConfig config) {
        return getCannedStrategyConfig();
    }

    @Override
    public StrategyConfig deleteStrategyById(String id) {
        return getCannedStrategyConfig();
    }

    @Override
    public List<StrategyConfig> findAllStrategies() {
        return Arrays.asList(getCannedStrategyConfig());
    }

    @Override
    public List<StrategyConfig> deleteAllStrategies() {
        return null;
    }

    /*
     * TODO Hard code these for now - will come from Repository later...
     */
    private static StrategyConfig getCannedStrategyConfig() {

        final StrategyConfigItems configItems = new StrategyConfigItems();
        configItems.addConfigItem("buy-amount", "123.09");
        configItems.addConfigItem("long-ema-interval", "20");

        final StrategyConfig config = new StrategyConfig("3-way-ema", "3 Way EMA Crossover Algo",
                "A lovely description...", "com.gazbert.bxbot.algos.nova.ThreeWayEma", configItems);
        return config;
    }
}