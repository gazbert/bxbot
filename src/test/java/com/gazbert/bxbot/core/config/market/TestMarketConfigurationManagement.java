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

package com.gazbert.bxbot.core.config.market;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.engine.generated.Engine;
import com.gazbert.bxbot.core.config.market.generated.MarketsType;
import org.junit.Test;

import static org.junit.Assert.*;

/*
 * Tests the Market configuration is loaded as expected.
 * We're not testing the JAXB impl here - cherry pick the important stuff.
 */
public class TestMarketConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "com/gazbert/bxbot/core/config/market/markets.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/markets/valid-markets.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/markets/missing-markets.xml";


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final MarketsType marketsType = ConfigurationManager.loadConfig(MarketsType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        assertEquals(2, marketsType.getMarkets().size());

        assertEquals("BTC/USD", marketsType.getMarkets().get(0).getLabel());
        assertEquals("btc_usd", marketsType.getMarkets().get(0).getId());
        assertEquals("BTC", marketsType.getMarkets().get(0).getBaseCurrency());
        assertEquals("USD", marketsType.getMarkets().get(0).getCounterCurrency());
        assertTrue(marketsType.getMarkets().get(0).isEnabled());
        assertEquals("scalping-strategy", marketsType.getMarkets().get(0).getTradingStrategy());

        assertEquals("LTC/BTC", marketsType.getMarkets().get(1).getLabel());
        assertEquals("ltc_usd", marketsType.getMarkets().get(1).getId());
        assertEquals("LTC", marketsType.getMarkets().get(1).getBaseCurrency());
        assertEquals("BTC", marketsType.getMarkets().get(1).getCounterCurrency());
        assertFalse(marketsType.getMarkets().get(1).isEnabled());
        assertEquals("scalping-strategy", marketsType.getMarkets().get(1).getTradingStrategy());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(Engine.class,
                MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }
}
