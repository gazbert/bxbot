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

package com.gazbert.bxbot.rest.api.config;

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.services.ExchangeConfigService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * Controller for directing Exchange config requests.
 * <p>
 * Exchange config can only be fetched and updated - it cannot be deleted or created.
 * <p>
 * There is only 1 Exchange Adapter per bot.
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping("/api/config/")
public class ExchangeConfigController {

    private static final Logger LOG = LogManager.getLogger();
    private final ExchangeConfigService exchangeConfigService;

    public ExchangeConfigController(ExchangeConfigService exchangeConfigService) {
        this.exchangeConfigService = exchangeConfigService;
    }

    /**
     * Returns the Exchange configuration for the bot.
     * <p>
     * The AuthenticationConfig is stripped out and not exposed for remote consumption.
     * The API keys/credentials should not leave the bot's local machine via the REST API.
     *
     * @param user the authenticated user making the request.
     * @return the Exchange configuration.
     */
    @RequestMapping(value = "/exchange", method = RequestMethod.GET)
    public ExchangeConfig getExchange(@AuthenticationPrincipal User user) {

        LOG.info("GET /exchange - getExchange() - caller: " + user.getUsername());

        final ExchangeConfig exchangeConfig = exchangeConfigService.getExchangeConfig();
        exchangeConfig.setAuthenticationConfig(null);

        LOG.info("Response: " + exchangeConfig);
        return exchangeConfig;
    }

    /**
     * Updates the Exchange configuration for the bot.
     * <p>
     * Any AuthenticationConfig is stripped out and not updated.
     * The API keys/credentials should not enter the bot's local machine via the REST API.
     *
     * @param user   the authenticated user making the request.
     * @param config the Exchange config to update.
     * @return 200 'OK' HTTP status code with updated Exchange config in the body if update successful, some other
     * HTTP status code otherwise.
     */
    @RequestMapping(value = "/exchange", method = RequestMethod.PUT)
    public ResponseEntity<?> updateExchange(@AuthenticationPrincipal User user, @RequestBody ExchangeConfig config) {

        LOG.info("PUT /exchange - updateExchange() - caller: " + user.getUsername());
        LOG.info("Request: " + config);

        final ExchangeConfig updatedConfig = exchangeConfigService.updateExchangeConfig(
                mergeWithLocalAuthenticationConfig(config));
        return new ResponseEntity<>(updatedConfig, HttpStatus.OK);
    }

    // ------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------

    private ExchangeConfig mergeWithLocalAuthenticationConfig(ExchangeConfig remoteConfig) {
        final ExchangeConfig localConfig = exchangeConfigService.getExchangeConfig();
        remoteConfig.setAuthenticationConfig(localConfig.getAuthenticationConfig());
        return remoteConfig;
    }
}

