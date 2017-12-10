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

package com.gazbert.bxbot.core.config.exchange;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OptionalConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests Exchange Config exchange API config object behaves as expected.
 *
 * @author gazbert
 */
public class TestExchangeConfigImpl {

    private static final String EXCHANGE_NAME = "Bitstamp";
    private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";
    private static final AuthenticationConfig AUTHENTICATION_CONFIG = new AuthenticationConfigImpl();
    private static final NetworkConfig NETWORK_CONFIG = new NetworkConfigImpl();
    private static final OptionalConfig OTHER_CONFIG = new OptionalConfigImpl();


    @Test
    public void testInitialisationWorksAsExpected() {

        final ExchangeConfigImpl exchangeConfig = new ExchangeConfigImpl();
        assertEquals(null, exchangeConfig.getExchangeName());
        assertEquals(null, exchangeConfig.getExchangeAdapter());
        assertEquals(null, exchangeConfig.getAuthenticationConfig());
        assertEquals(null, exchangeConfig.getNetworkConfig());
        assertEquals(null, exchangeConfig.getOptionalConfig());
    }

    @Test
    public void testSettersWorkAsExpected() {

        final ExchangeConfigImpl exchangeConfig = new ExchangeConfigImpl();

        exchangeConfig.setExchangeName(EXCHANGE_NAME);
        assertEquals(EXCHANGE_NAME, exchangeConfig.getExchangeName());

        exchangeConfig.setExchangeAdapter(EXCHANGE_ADAPTER);
        assertEquals(EXCHANGE_ADAPTER, exchangeConfig.getExchangeAdapter());

        exchangeConfig.setAuthenticationConfig(AUTHENTICATION_CONFIG);
        assertEquals(AUTHENTICATION_CONFIG, exchangeConfig.getAuthenticationConfig());

        exchangeConfig.setNetworkConfig(NETWORK_CONFIG);
        assertEquals(NETWORK_CONFIG, exchangeConfig.getNetworkConfig());

        exchangeConfig.setOptionalConfig(OTHER_CONFIG);
        assertEquals(OTHER_CONFIG, exchangeConfig.getOptionalConfig());
    }
}
