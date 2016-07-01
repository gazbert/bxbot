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

package com.gazbert.bxbot.core.config.exchange;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.exchange.generated.ExchangeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 * Tests the Exchange Adapter configuration is loaded as expected.
 * We're not testing the JAXB impl here - cherry pick the important stuff.
 */
public class TestExchangeConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "config/schemas/exchange.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/exchange/valid-exchange.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/exchange-/missing-exchange.xml";

    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final ExchangeType exchangeType = ConfigurationManager.loadConfig(ExchangeType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        // basic check - we're not testing JAXB here.
        assertNotNull(exchangeType);
        assertEquals("com.gazbert.bxbot.core.exchanges.BtceExchangeAdapter", exchangeType.getAdapter());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(ExchangeType.class, MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }
}
