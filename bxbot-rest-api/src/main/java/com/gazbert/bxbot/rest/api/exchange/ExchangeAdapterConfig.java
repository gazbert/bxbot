/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Gareth Jon Lynch
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

package com.gazbert.bxbot.rest.api.exchange;

import com.google.common.base.MoreObjects;

/**
 * Domain object representing the Exchange Adapter config.
 * <p>
 * For now, decision taken not to expose AuthenticationConfig (API key + secret) through REST API - changes have to be
 * made on the local bot node. Might revisit this in the future.
 *
 * @author gazbert
 */
public class ExchangeAdapterConfig {

    private String name;
    private String className;
    private NetworkConfig networkConfig;
    private OptionalConfig optionalConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

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
                .add("name", name)
                .add("className", className)
                .add("networkConfig", networkConfig)
                .add("optionalConfig", optionalConfig)
                .toString();
    }
}
