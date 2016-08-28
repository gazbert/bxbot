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

package com.gazbert.bxbot.trading.api;

import static org.junit.Assert.*;
import org.junit.Test;

/*
 * Tests a Market domain object behaves as expected.
 */
public class TestMarket {

    private static final String MARKET_NAME = "LTC_BTC";
    private static final String MARKET_ID = "3";
    private static final String BASE_CURRENCY = "LTC";
    private static final String COUNTER_CURRENCY = "BTC";


    @Test
    public void testMarketIsInitialisedAsExpected() {

        final Market market = new Market(MARKET_NAME, MARKET_ID, BASE_CURRENCY, COUNTER_CURRENCY);
        assertEquals(MARKET_NAME, market.getName());
        assertEquals(MARKET_ID, market.getId());
        assertEquals(BASE_CURRENCY, market.getBaseCurrency());
        assertEquals(COUNTER_CURRENCY, market.getCounterCurrency());
    }

    @Test
    public void testSettersWorkAsExpected() {

        final Market market = new Market(null, null, null, null);
        assertEquals(null, market.getName());
        assertEquals(null, market.getId());
        assertEquals(null, market.getBaseCurrency());
        assertEquals(null, market.getCounterCurrency());

        market.setName(MARKET_NAME);
        assertEquals(MARKET_NAME, market.getName());

        market.setId(MARKET_ID);
        assertEquals(MARKET_ID, market.getId());

        market.setBaseCurrency(BASE_CURRENCY);
        assertEquals(BASE_CURRENCY, market.getBaseCurrency());

        market.setCounterCurrency(COUNTER_CURRENCY);
        assertEquals(COUNTER_CURRENCY, market.getCounterCurrency());
    }
}
