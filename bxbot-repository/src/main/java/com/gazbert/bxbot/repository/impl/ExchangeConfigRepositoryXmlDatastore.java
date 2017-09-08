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

package com.gazbert.bxbot.repository.impl;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.exchange.generated.*;
import com.gazbert.bxbot.domain.exchange.AuthenticationConfig;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.domain.exchange.OptionalConfig;
import com.gazbert.bxbot.repository.ExchangeConfigRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.gazbert.bxbot.datastore.FileLocations.EXCHANGE_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.EXCHANGE_CONFIG_XSD_FILENAME;

/**
 * An XML datastore implementation of the Exchange config repository.
 *
 * @author gazbert
 */
@Repository("exchangeConfigRepository")
@Transactional
public class ExchangeConfigRepositoryXmlDatastore implements ExchangeConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public ExchangeConfig get() {

        LOG.info(() -> "Fetching ExchangeConfig...");

        final ExchangeType internalEngineConfig = ConfigurationManager.loadConfig(ExchangeType.class,
                EXCHANGE_CONFIG_XML_FILENAME, EXCHANGE_CONFIG_XSD_FILENAME);
        return adaptInternalToExternalConfig(internalEngineConfig);
    }

    @Override
    public ExchangeConfig save(ExchangeConfig config) {

        LOG.info(() -> "About to save ExchangeConfig: " + config);

        final ExchangeType internalExchangeConfig = adaptExternalToInternalConfig(config);
        ConfigurationManager.saveConfig(ExchangeType.class, internalExchangeConfig, EXCHANGE_CONFIG_XML_FILENAME);

        final ExchangeType internalEngineConfig = ConfigurationManager.loadConfig(ExchangeType.class,
                EXCHANGE_CONFIG_XML_FILENAME, EXCHANGE_CONFIG_XSD_FILENAME);

        return adaptInternalToExternalConfig(internalEngineConfig);
    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static ExchangeConfig adaptInternalToExternalConfig(ExchangeType internalExchangeConfig) {

        final AuthenticationConfig authenticationConfig = new AuthenticationConfig();
        internalExchangeConfig.getAuthenticationConfig().getConfigItems()
                .forEach(item -> authenticationConfig.getItems().put(item.getName(), item.getValue()));

        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setConnectionTimeout(internalExchangeConfig.getNetworkConfig().getConnectionTimeout());
        networkConfig.setNonFatalErrorCodes(internalExchangeConfig.getNetworkConfig().getNonFatalErrorCodes().getCodes());
        networkConfig.setNonFatalErrorMessages(internalExchangeConfig.getNetworkConfig().getNonFatalErrorMessages().getMessages());

        final OptionalConfig optionalConfig = new OptionalConfig();
        final OptionalConfigType internalOptionalConfig = internalExchangeConfig.getOptionalConfig();
        if (internalOptionalConfig != null) { // it's optional
            internalExchangeConfig.getOptionalConfig().getConfigItems()
                    .forEach(item -> optionalConfig.getItems().put(item.getName(), item.getValue()));
        }

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setExchangeName(internalExchangeConfig.getName());
        exchangeConfig.setExchangeAdapter(internalExchangeConfig.getAdapter());
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOptionalConfig(optionalConfig);
        return exchangeConfig;
    }

    private static ExchangeType adaptExternalToInternalConfig(ExchangeConfig externalExchangeConfig) {

        final NonFatalErrorCodesType nonFatalErrorCodes = new NonFatalErrorCodesType();
        nonFatalErrorCodes.getCodes().addAll(externalExchangeConfig.getNetworkConfig().getNonFatalErrorCodes());
        final NonFatalErrorMessagesType nonFatalErrorMessages = new NonFatalErrorMessagesType();
        nonFatalErrorMessages.getMessages().addAll(externalExchangeConfig.getNetworkConfig().getNonFatalErrorMessages());
        final NetworkConfigType networkConfig = new NetworkConfigType();
        networkConfig.setConnectionTimeout(externalExchangeConfig.getNetworkConfig().getConnectionTimeout());
        networkConfig.setNonFatalErrorCodes(nonFatalErrorCodes);
        networkConfig.setNonFatalErrorMessages(nonFatalErrorMessages);

        final OptionalConfigType optionalConfig = new OptionalConfigType();
        externalExchangeConfig.getOptionalConfig().getItems().forEach((key, value) -> {
            final ConfigItemType configItem = new ConfigItemType();
            configItem.setName(key);
            configItem.setValue(value);
            optionalConfig.getConfigItems().add(configItem);
        });

        final ExchangeType exchangeConfig = new ExchangeType();
        exchangeConfig.setName(externalExchangeConfig.getExchangeName());
        exchangeConfig.setAdapter(externalExchangeConfig.getExchangeAdapter());
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOptionalConfig(optionalConfig);

        // TODO - Currently, we don't accept AuthenticationConfig - security risk?
        // We load the existing auth config and merge it in with the updated stuff...
        final ExchangeType existingExchangeConfig = ConfigurationManager.loadConfig(ExchangeType.class,
                EXCHANGE_CONFIG_XML_FILENAME, EXCHANGE_CONFIG_XSD_FILENAME);
        exchangeConfig.setAuthenticationConfig(existingExchangeConfig.getAuthenticationConfig());

        return exchangeConfig;
    }
}
