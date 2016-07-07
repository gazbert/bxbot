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

import com.gazbert.bxbot.core.api.exchange.ExchangeConfig;

import java.util.HashMap;
import java.util.Map;

/*
 * Encapsulates configuration for an Exchange Adapter.
 * The configuration is loaded from the config/exchange.xml.
 */
public class ExchangeConfigImpl implements ExchangeConfig {

    /*
     * Map of Authentication configuration for the Exchange Adapter.
     * Contains key-value String pairs.
     */
    private Map<String, String> authenticationConfig = new HashMap<>();

    /* Holds Network configuration for the Exchange Adapter */
    private NetworkConfigImpl networkConfig;


    public Map<String, String> getAuthenticationConfig() {
        return authenticationConfig;
    }

    public void setAuthenticationConfig(Map<String, String> authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    public void setNetworkConfig(NetworkConfigImpl networkConfig) {
        this.networkConfig = networkConfig;
    }

    public NetworkConfigImpl getNetworkConfig() {
        return networkConfig;
    }
}
