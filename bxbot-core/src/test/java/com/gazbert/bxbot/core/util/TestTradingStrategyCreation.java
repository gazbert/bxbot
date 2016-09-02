/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.core.util;

import com.gazbert.bxbot.strategy.api.TradingStrategy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests Trading Strategies are created as expected.
 *
 * @author gazbert
 */
public class TestTradingStrategyCreation {

    private static final String VALID_TRADING_STRATEGY_IMPL = "com.gazbert.bxbot.core.util.strategies.ValidTradingStrategy";
    private static final String INVALID_TRADING_STRATEGY_IMPL = "com.gazbert.bxbot.core.util.strategies.InvalidTradingStrategy";
    private static final String MISSING_TRADING_STRATEGY_IMPL = "com.gazbert.bxbot.core.util.strategies.MissingInvalidTradingStrategy";

    @Test
    public void testCreationOfValidTradingStrategyImpl() {

        final TradingStrategy tradingStrategy = ConfigurableComponentFactory.createComponent(VALID_TRADING_STRATEGY_IMPL);

        assertNotNull(tradingStrategy);
        assertEquals(VALID_TRADING_STRATEGY_IMPL, tradingStrategy.getClass().getCanonicalName());
    }

    @Test(expected = ClassCastException.class)
    public void testCreatingTradingStrategyImplThatDoesNotImplementTradingStrategyThrowsException() {
        final TradingStrategy tradingStrategy = ConfigurableComponentFactory.createComponent(INVALID_TRADING_STRATEGY_IMPL);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreatingTradingStrategyImplThatDoesNotExistThrowsException() {
        final TradingStrategy tradingStrategy = ConfigurableComponentFactory.createComponent(MISSING_TRADING_STRATEGY_IMPL);
    }
}
