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

package com.gazbert.bxbot.core.util;

import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.trading.api.TradingApi;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests Exchange Adapters are created as expected.
 *
 * @author gazbert
 */
public class TestExchangeAdapterCreation {

    private static final String VALID_EXCHANGE_ADAPTER_IMPL = "com.gazbert.bxbot.core.util.adapters.ValidExchangeAdapter";
    private static final String INVALID_EXCHANGE_ADAPTER_NOT_IMPL_TRADING_API =
            "com.gazbert.bxbot.core.util.adapters.InvalidExchangeAdapterNotImplTradingApi";
    private static final String INVALID_EXCHANGE_ADAPTER_NOT_IMPL_EXCHANGE_ADAPTER =
            "com.gazbert.bxbot.core.util.adapters.InvalidExchangeAdapterNotImplExchangeAdapter";
    private static final String NONEXISTENT_EXCHANGE_ADAPTER_IMPL = "com.gazbert.bxbot.core.util.adapters.MuhInvalidExchangeAdapter";

    @Test
    public void testCreatingValidExchangeAdapter() {

        final TradingApi tradingApi = ConfigurableComponentFactory.createComponent(VALID_EXCHANGE_ADAPTER_IMPL);
        final ExchangeAdapter exchangeAdapter = ConfigurableComponentFactory.createComponent(VALID_EXCHANGE_ADAPTER_IMPL);

        assertNotNull(tradingApi);
        assertNotNull(exchangeAdapter);
        assertEquals(VALID_EXCHANGE_ADAPTER_IMPL, exchangeAdapter.getClass().getCanonicalName());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreatingExchangeAdapterThatDoesNotExistThrowsException() {

        final TradingApi tradingApi = ConfigurableComponentFactory.createComponent(NONEXISTENT_EXCHANGE_ADAPTER_IMPL);
    }

    @Test(expected = ClassCastException.class)
    public void testCreatingExchangeAdapterThatDoesNotImplementTradingApiThrowsException() {

        final TradingApi tradingApi = ConfigurableComponentFactory.createComponent(INVALID_EXCHANGE_ADAPTER_NOT_IMPL_TRADING_API);
    }

    @Test(expected = ClassCastException.class)
    public void testCreatingExchangeAdapterThatDoesNotImplementExchangeAdapterThrowsException() {

        final ExchangeAdapter exchangeAdapter = ConfigurableComponentFactory.createComponent(INVALID_EXCHANGE_ADAPTER_NOT_IMPL_EXCHANGE_ADAPTER);
    }
}
