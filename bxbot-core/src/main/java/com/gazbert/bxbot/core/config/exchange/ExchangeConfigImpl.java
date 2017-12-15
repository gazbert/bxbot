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
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OptionalConfig;
import com.google.common.base.MoreObjects;

/**
 * Exchange API Exchange config.
 *
 * @author gazbert
 */
public class ExchangeConfigImpl implements ExchangeConfig {

    private String exchangeName;
    private String exchangeAdapter;
    private AuthenticationConfig authenticationConfig;
    private NetworkConfig networkConfig;
    private OptionalConfig optionalConfig;

    @Override
    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    @Override
    public String getExchangeAdapter() {
        return exchangeAdapter;
    }

    public void setExchangeAdapter(String exchangeAdapter) {
        this.exchangeAdapter = exchangeAdapter;
    }

    @Override
    public AuthenticationConfig getAuthenticationConfig() {
        return authenticationConfig;
    }

    public void setAuthenticationConfig(AuthenticationConfig authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    @Override
    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public OptionalConfig getOptionalConfig() {
        return optionalConfig;
    }

    public void setOptionalConfig(OptionalConfig optionalConfig) {
        this.optionalConfig = optionalConfig;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("exchangeName", exchangeName)
                .add("exchangeAdapter", exchangeAdapter)
                // WARNING - careful showing this!
                //.add("authenticationConfig", authenticationConfig)
                .add("networkConfig", networkConfig)
                .add("optionalConfig", optionalConfig)
                .toString();
    }
}
