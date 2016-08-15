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

package com.gazbert.bxbot.core.config;

import com.gazbert.bxbot.core.api.trading.TradingApiException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestConfigurationException {

    private static final String ERROR_MSG = "File not found...";
    private static final RuntimeException CAUSE = new RuntimeException("The cause of the exception");

    @Test
    public void testCreationOfExceptionIsAsExpected() {
        final Exception exception = new ConfigurationException(ERROR_MSG);
        assertEquals(ERROR_MSG, exception.getMessage());
    }

    @Test
    public void testCreationOfExceptionWithCauseIsAsExpected() {
        final Exception exception = new ConfigurationException(ERROR_MSG, CAUSE);
        assertEquals(ERROR_MSG, exception.getMessage());
        assertEquals(CAUSE, exception.getCause());
    }
}