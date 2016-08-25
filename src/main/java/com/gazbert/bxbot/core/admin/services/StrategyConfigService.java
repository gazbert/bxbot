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

import java.util.List;

/**
 * TODO Work in progress...
 *
 * @author gazbert
 * @since 12/08/2016
 */
public interface StrategyConfigService {

    /**
     * Returns all the Strategy Config items.
     *
     * TODO doc what is exposed and what is not
     *
     * @return all the strategy config items
     */
    List<StrategyConfig> findAllStrategies();

    /**
     * Returns the Strategy config for a given Strategy id.
     *
     * @param id the strategy id.
     * @return the Strategy config.
     */
    StrategyConfig findById(String id);


    StrategyConfig findByName(String name);

    StrategyConfig saveStrategy(StrategyConfig config);

    /**
     * TODO doc what can and cannot be updated
     *
     * @param config
     * @return
     */
    StrategyConfig updateStrategy(StrategyConfig config);

    StrategyConfig deleteStrategyById(String id);

    List<StrategyConfig> deleteAllStrategies();
}
