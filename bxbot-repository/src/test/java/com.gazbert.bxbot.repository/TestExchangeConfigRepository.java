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
import com.gazbert.bxbot.domain.exchange.OtherConfig;
import com.gazbert.bxbot.repository.impl.ExchangeConfigRepositoryXmlImpl;
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

    private static final String EXCHANGE_NAME = "BTC-e";
    private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.core.exchanges.TestExchangeAdapter";

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
    public void whenGetConfigCalledThenExpectServiceToLoadIt() throws Exception {

        expect(ConfigurationManager.loadConfig(
                eq(ExchangeType.class),
                eq(EXCHANGE_CONFIG_XML_FILENAME),
                eq(EXCHANGE_CONFIG_XSD_FILENAME))).
                andReturn(someInternalExchangeConfig());

        PowerMock.replayAll();

        final ExchangeConfigRepository exchangeConfigRepository = new ExchangeConfigRepositoryXmlImpl();
        final ExchangeConfig exchangeConfig = exchangeConfigRepository.getConfig();
        assertThat(exchangeConfig.getExchangeName()).isEqualTo(EXCHANGE_NAME);
        assertThat(exchangeConfig.getExchangeAdapter()).isEqualTo(EXCHANGE_ADAPTER);

        assertThat(exchangeConfig.getAuthenticationConfig().getItems().get(API_KEY_CONFIG_ITEM_KEY)).isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
        assertThat(exchangeConfig.getAuthenticationConfig().getItems().get(SECRET_CONFIG_ITEM_KEY)).isEqualTo(SECRET_CONFIG_ITEM_VALUE);

        assertThat(exchangeConfig.getNetworkConfig().getConnectionTimeout()).isEqualTo(CONNECTION_TIMEOUT);
        assertThat(exchangeConfig.getNetworkConfig().getNonFatalErrorCodes()).isEqualTo(NON_FATAL_ERROR_CODES);
        assertThat(exchangeConfig.getNetworkConfig().getNonFatalErrorMessages()).isEqualTo(NON_FATAL_ERROR_MESSAGES);
        assertThat(exchangeConfig.getOtherConfig().getItems().get(BUY_FEE_CONFIG_ITEM_KEY)).isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
        assertThat(exchangeConfig.getOtherConfig().getItems().get(SELL_FEE_CONFIG_ITEM_KEY)).isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);

        PowerMock.verifyAll();
    }

    @Test
    public void whenUpdateConfigCalledThenExpectServiceToSaveIt() throws Exception {

        // for loading the existing auth config to merge with updated stuff
        expect(ConfigurationManager.loadConfig(
                eq(ExchangeType.class),
                eq(EXCHANGE_CONFIG_XML_FILENAME),
                eq(EXCHANGE_CONFIG_XSD_FILENAME))).
                andReturn(someInternalExchangeConfig());

        ConfigurationManager.saveConfig(eq(ExchangeType.class), anyObject(ExchangeType.class), eq(EXCHANGE_CONFIG_XML_FILENAME));
        PowerMock.replayAll();

        final ExchangeConfigRepository exchangeConfigRepository = new ExchangeConfigRepositoryXmlImpl();
        exchangeConfigRepository.updateConfig(withSomeExternalExchangeConfig());

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
        final OtherConfigType otherConfig = new OtherConfigType();
        otherConfig.getConfigItems().add(buyFee);
        otherConfig.getConfigItems().add(sellFee);

        final ExchangeType exchangeConfig = new ExchangeType();
        exchangeConfig.setName(EXCHANGE_NAME);
        exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOtherConfig(otherConfig);
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

        final OtherConfig otherConfig = new OtherConfig();
        otherConfig.getItems().put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
        otherConfig.getItems().put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setExchangeName(EXCHANGE_NAME);
        exchangeConfig.setExchangeAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOtherConfig(otherConfig);

        return exchangeConfig;
    }
}
