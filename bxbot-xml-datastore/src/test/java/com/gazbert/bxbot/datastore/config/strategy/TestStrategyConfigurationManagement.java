/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015. Gareth Jon Lynch
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

package com.gazbert.bxbot.datastore.config.strategy;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.strategy.generated.TradingStrategiesType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the Trading Strategy configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestStrategyConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "com/gazbert/bxbot/core/config/strategy/strategies.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/strategies/valid-strategies.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/strategies/missing-strategies.xml";


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final TradingStrategiesType tradingStrategiesType = ConfigurationManager.loadConfig(TradingStrategiesType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        assertEquals(3, tradingStrategiesType.getStrategies().size());

        /*
         * Strat 1
         */
        assertEquals("scalping-strategy", tradingStrategiesType.getStrategies().get(0).getId());
        assertEquals("Basic Scalping Strat", tradingStrategiesType.getStrategies().get(0).getLabel());
        assertTrue(tradingStrategiesType.getStrategies().get(0).getDescription().trim().equals(
                "A simple trend following scalper that buys at current BID price and sells at current ASK price, " +
                        "taking profit from the spread. The exchange fees are factored in."));
        assertEquals("com.gazbert.bxbot.strategies.ExampleScalpingStrategy", tradingStrategiesType.getStrategies().get(0).getClassName());

        assertTrue(2 == tradingStrategiesType.getStrategies().get(0).getConfiguration().getConfigItem().size());
        assertEquals("btc-buy-order-amount", tradingStrategiesType.getStrategies().get(0).getConfiguration().getConfigItem().get(0).getName());
        assertEquals("0.5", tradingStrategiesType.getStrategies().get(0).getConfiguration().getConfigItem().get(0).getValue());
        assertEquals("minimumPercentageGain", tradingStrategiesType.getStrategies().get(0).getConfiguration().getConfigItem().get(1).getName());
        assertEquals("1", tradingStrategiesType.getStrategies().get(0).getConfiguration().getConfigItem().get(1).getValue());

        /*
         * Strat 2
         */
        assertEquals("ema-shorting-strategy", tradingStrategiesType.getStrategies().get(1).getId());
        assertEquals("EMA Based Shorting Strat", tradingStrategiesType.getStrategies().get(1).getLabel());
        assertNull(tradingStrategiesType.getStrategies().get(1).getDescription()); // optional element check
        assertEquals("com.gazbert.bxbot.strategies.YourEmaShortingStrategy", tradingStrategiesType.getStrategies().get(1).getClassName());

        assertTrue(4 == tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().size());

        assertEquals("btc-sell-order-amount", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(0).getName());
        assertEquals("0.5", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(0).getValue());

        assertEquals("shortEmaInterval", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(1).getName());
        assertEquals("5", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(1).getValue());

        assertEquals("mediumEmaInterval", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(2).getName());
        assertEquals("10", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(2).getValue());

        assertEquals("longEmaInterval", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(3).getName());
        assertEquals("20", tradingStrategiesType.getStrategies().get(1).getConfiguration().getConfigItem().get(3).getValue());

        /*
         * Strat 3
         */
        assertEquals("macd-strategy", tradingStrategiesType.getStrategies().get(2).getId());
        assertEquals("MACD Based Strat", tradingStrategiesType.getStrategies().get(2).getLabel());
        assertTrue(tradingStrategiesType.getStrategies().get(2).getDescription().trim().equals(
                "Strat uses MACD data to take long position in USD."));
        assertEquals("com.gazbert.bxbot.strategies.YourMacdStrategy", tradingStrategiesType.getStrategies().get(2).getClassName());
        assertNull(tradingStrategiesType.getStrategies().get(2).getConfiguration()); // optional element check
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(TradingStrategiesType.class,
                MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }
}
