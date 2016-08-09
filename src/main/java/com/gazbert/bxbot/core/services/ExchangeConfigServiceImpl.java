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

package com.gazbert.bxbot.core.services;

import com.gazbert.bxbot.core.config.exchange.AuthenticationConfig;
import com.gazbert.bxbot.core.config.exchange.ExchangeConfig;
import com.gazbert.bxbot.core.config.exchange.NetworkConfig;
import com.gazbert.bxbot.core.config.exchange.OtherConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * TODO Work in progress...
 *
 * @author gazbert
 * @since 20/07/2016
 */
@Service("exchangeConfigService")
@Transactional
public class ExchangeConfigServiceImpl implements ExchangeConfigService {

    @Override
    public ExchangeConfig getConfig() {
        return getCannedExchangeConfig();
    }

    @Override
    public ExchangeConfig updateConfig(ExchangeConfig config) {
        return getCannedExchangeConfig();
    }

    /*
     * TODO Hard code these for now - will come from Repository later...
     */
    private static ExchangeConfig getCannedExchangeConfig() {

        final AuthenticationConfig authenticationConfig = new AuthenticationConfig();
        authenticationConfig.getItems().put("api-key", "apiKey--123");
        authenticationConfig.getItems().put("secret", "my-secret-KEY");

        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setConnectionTimeout(30);
        networkConfig.setNonFatalErrorCodes(Arrays.asList(502, 503, 504));
        networkConfig.setNonFatalErrorMessages(Arrays.asList(
                "Connection refused", "Connection reset", "Remote host closed connection during handshake"));

        final OtherConfig otherConfig = new OtherConfig();
        otherConfig.getItems().put("buy-fee", "0.20");
        otherConfig.getItems().put("sell-fee", "0.25");

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOtherConfig(otherConfig);

        return exchangeConfig;
    }
}
