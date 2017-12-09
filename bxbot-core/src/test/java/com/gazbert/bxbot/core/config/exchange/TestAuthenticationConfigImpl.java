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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests Authentication Config exchange API config object behaves as expected.
 *
 * @author gazbert
 */
public class TestAuthenticationConfigImpl {

    private static final String API_KEY_CONFIG_ITEM_KEY = "api-key";
    private static final String API_KEY_CONFIG_ITEM_VALUE = "apiKey--123";

    private static final String SECRET_CONFIG_ITEM_KEY = "secret";
    private static final String SECRET_FEE_CONFIG_ITEM_VALUE = "secret-key";


    @Test
    public void testAddingAndFetchingAuthenticationConfig() throws Exception {

        final AuthenticationConfigImpl authenticationConfig = new AuthenticationConfigImpl();
        authenticationConfig.getItems().put(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);
        authenticationConfig.getItems().put(SECRET_CONFIG_ITEM_KEY, SECRET_FEE_CONFIG_ITEM_VALUE);

        assertEquals(2, authenticationConfig.getItems().size());
        assertEquals(API_KEY_CONFIG_ITEM_VALUE, authenticationConfig.getItems().get(API_KEY_CONFIG_ITEM_KEY));
        assertEquals(SECRET_FEE_CONFIG_ITEM_VALUE, authenticationConfig.getItems().get(SECRET_CONFIG_ITEM_KEY));
    }
}
