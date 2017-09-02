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

package com.gazbert.bxbot.repository;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.exchange.generated.*;
import com.gazbert.bxbot.domain.exchange.AuthenticationConfig;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.domain.exchange.OptionalConfig;
import com.gazbert.bxbot.repository.impl.ExchangeConfigRepositoryXmlDatastore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static com.gazbert.bxbot.datastore.FileLocations.EXCHANGE_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.datastore.FileLocations.EXCHANGE_CONFIG_XSD_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.*;

/**
 * Tests Exchange configuration repository behaves as expected.
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
public class TestExchangeConfigRepository {

    private static final String EXCHANGE_NAME = "Bitstamp";
    private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";

    private static final String API_KEY_CONFIG_ITEM_KEY = "api-key";
    private static final String API_KEY_CONFIG_ITEM_VALUE = "apiKey--123";

    private static final String SECRET_CONFIG_ITEM_KEY = "secret";
    private static final String SECRET_CONFIG_ITEM_VALUE = "secret-key";

    private static final Integer CONNECTION_TIMEOUT = 30;
    private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503, 504);
    private static final List<String> NON_FATAL_ERROR_MESSAGES = Arrays.asList(
            "Connection refused", "Connection reset", "Remote host closed connection during handshake");

    private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
    private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";
    private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
    private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";


    @Before
    public void setup() throws Exception {
        PowerMock.mockStatic(ConfigurationManager.class);
    }

    @Test
    public void whenGetCalledThenReturnExchangeConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(ExchangeType.class),
                eq(EXCHANGE_CONFIG_XML_FILENAME),
                eq(EXCHANGE_CONFIG_XSD_FILENAME))).
                andReturn(someInternalExchangeConfig());

        PowerMock.replayAll();

        final ExchangeConfigRepository exchangeConfigRepository = new ExchangeConfigRepositoryXmlDatastore();
        final ExchangeConfig exchangeConfig = exchangeConfigRepository.get();

        assertThat(exchangeConfig.getExchangeName()).isEqualTo(EXCHANGE_NAME);
        assertThat(exchangeConfig.getExchangeAdapter()).isEqualTo(EXCHANGE_ADAPTER);

        assertThat(exchangeConfig.getAuthenticationConfig().getItems().get(API_KEY_CONFIG_ITEM_KEY)).isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
        assertThat(exchangeConfig.getAuthenticationConfig().getItems().get(SECRET_CONFIG_ITEM_KEY)).isEqualTo(SECRET_CONFIG_ITEM_VALUE);

        assertThat(exchangeConfig.getNetworkConfig().getConnectionTimeout()).isEqualTo(CONNECTION_TIMEOUT);
        assertThat(exchangeConfig.getNetworkConfig().getNonFatalErrorCodes()).isEqualTo(NON_FATAL_ERROR_CODES);
        assertThat(exchangeConfig.getNetworkConfig().getNonFatalErrorMessages()).isEqualTo(NON_FATAL_ERROR_MESSAGES);
        assertThat(exchangeConfig.getOptionalConfig().getItems().get(BUY_FEE_CONFIG_ITEM_KEY)).isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
        assertThat(exchangeConfig.getOptionalConfig().getItems().get(SELL_FEE_CONFIG_ITEM_KEY)).isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);

        PowerMock.verifyAll();
    }

    @Test
    public void whenSaveCalledThenExpectRepositoryToSaveItAndReturnSavedExchangeConfig() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(ExchangeType.class),
                eq(EXCHANGE_CONFIG_XML_FILENAME),
                eq(EXCHANGE_CONFIG_XSD_FILENAME))).
                andReturn(someInternalExchangeConfig());

        ConfigurationManager.saveConfig(eq(ExchangeType.class), anyObject(ExchangeType.class), eq(EXCHANGE_CONFIG_XML_FILENAME));

        expect(ConfigurationManager.loadConfig(
                eq(ExchangeType.class),
                eq(EXCHANGE_CONFIG_XML_FILENAME),
                eq(EXCHANGE_CONFIG_XSD_FILENAME))).
                andReturn(someInternalExchangeConfig());

        PowerMock.replayAll();

        final ExchangeConfigRepository exchangeConfigRepository = new ExchangeConfigRepositoryXmlDatastore();
        final ExchangeConfig savedExchangeConfig = exchangeConfigRepository.save(withSomeExternalExchangeConfig());

        assertThat(savedExchangeConfig.getExchangeName()).isEqualTo(EXCHANGE_NAME);
        assertThat(savedExchangeConfig.getExchangeAdapter()).isEqualTo(EXCHANGE_ADAPTER);

        assertThat(savedExchangeConfig.getAuthenticationConfig().getItems().get(API_KEY_CONFIG_ITEM_KEY)).isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
        assertThat(savedExchangeConfig.getAuthenticationConfig().getItems().get(SECRET_CONFIG_ITEM_KEY)).isEqualTo(SECRET_CONFIG_ITEM_VALUE);

        assertThat(savedExchangeConfig.getNetworkConfig().getConnectionTimeout()).isEqualTo(CONNECTION_TIMEOUT);
        assertThat(savedExchangeConfig.getNetworkConfig().getNonFatalErrorCodes()).isEqualTo(NON_FATAL_ERROR_CODES);
        assertThat(savedExchangeConfig.getNetworkConfig().getNonFatalErrorMessages()).isEqualTo(NON_FATAL_ERROR_MESSAGES);
        assertThat(savedExchangeConfig.getOptionalConfig().getItems().get(BUY_FEE_CONFIG_ITEM_KEY)).isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
        assertThat(savedExchangeConfig.getOptionalConfig().getItems().get(SELL_FEE_CONFIG_ITEM_KEY)).isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);

        PowerMock.verifyAll();
    }

    // ------------------------------------------------------------------------------------------------
    // Private utils
    // ------------------------------------------------------------------------------------------------

    private static ExchangeType someInternalExchangeConfig() {

        final AuthenticationConfigType authenticationConfig = new AuthenticationConfigType();
        final ConfigItemType apiKey = new ConfigItemType();
        apiKey.setName(API_KEY_CONFIG_ITEM_KEY);
        apiKey.setValue(API_KEY_CONFIG_ITEM_VALUE);
        final ConfigItemType secretKey = new ConfigItemType();
        secretKey.setName(SECRET_CONFIG_ITEM_KEY);
        secretKey.setValue(SECRET_CONFIG_ITEM_VALUE);
        authenticationConfig.getConfigItems().add(apiKey);
        authenticationConfig.getConfigItems().add(secretKey);

        final NonFatalErrorCodesType nonFatalErrorCodes = new NonFatalErrorCodesType();
        nonFatalErrorCodes.getCodes().addAll(NON_FATAL_ERROR_CODES);
        final NonFatalErrorMessagesType nonFatalErrorMessages = new NonFatalErrorMessagesType();
        nonFatalErrorMessages.getMessages().addAll(NON_FATAL_ERROR_MESSAGES);
        final NetworkConfigType networkConfig = new NetworkConfigType();
        networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
        networkConfig.setNonFatalErrorCodes(nonFatalErrorCodes);
        networkConfig.setNonFatalErrorMessages(nonFatalErrorMessages);

        final ConfigItemType buyFee = new ConfigItemType();
        buyFee.setName(BUY_FEE_CONFIG_ITEM_KEY);
        buyFee.setValue(BUY_FEE_CONFIG_ITEM_VALUE);
        final ConfigItemType sellFee = new ConfigItemType();
        sellFee.setName(SELL_FEE_CONFIG_ITEM_KEY);
        sellFee.setValue(SELL_FEE_CONFIG_ITEM_VALUE);
        final OptionalConfigType optionalConfigType = new OptionalConfigType();
        optionalConfigType.getConfigItems().add(buyFee);
        optionalConfigType.getConfigItems().add(sellFee);

        final ExchangeType exchangeConfig = new ExchangeType();
        exchangeConfig.setName(EXCHANGE_NAME);
        exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOptionalConfig(optionalConfigType);
        return exchangeConfig;
    }

    private static ExchangeConfig withSomeExternalExchangeConfig() {

        // We don't permit updating of AuthenticationConfig in the service - security risk
        // If caller sets it, we just ignore it.
        final AuthenticationConfig authenticationConfig = new AuthenticationConfig();
        authenticationConfig.getItems().put("key", "updatedKeyIgnored");
        authenticationConfig.getItems().put("secret", "updatedSecretIgnored");

        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
        networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
        networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

        final OptionalConfig optionalConfig = new OptionalConfig();
        optionalConfig.getItems().put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
        optionalConfig.getItems().put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setExchangeName(EXCHANGE_NAME);
        exchangeConfig.setExchangeAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOptionalConfig(optionalConfig);

        return exchangeConfig;
    }
}
