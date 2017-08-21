/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.datastore.config.exchange;

import com.gazbert.bxbot.datastore.ConfigurationManager;
import com.gazbert.bxbot.datastore.exchange.generated.*;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests the Exchange Adapter configuration is loaded as expected.
 *
 * @author gazbert
 */
public class TestExchangeConfigurationManagement {

    /* Production XSD */
    private static final String XML_SCHEMA_FILENAME = "com/gazbert/bxbot/datastore/config/exchange.xsd";

    /* Test XML config */
    private static final String VALID_XML_CONFIG_FILENAME = "src/test/config/exchange/valid-exchange.xml";
    private static final String INVALID_XML_CONFIG_FILENAME = "src/test/config/exchange/invalid-exchange.xml";
    private static final String MISSING_XML_CONFIG_FILENAME = "src/test/config/exchange-/missing-exchange.xml";
    private static final String XML_CONFIG_TO_SAVE_FILENAME = "src/test/config/exchange/saved-exchange.xml";

    private static final String EXCHANGE_NAME = "Bitstamp";
    private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.BitstampExchangeAdapter";

    private static final String CLIENT_ID_CONFIG_ITEM_KEY = "client-id";
    private static final String CLIENT_ID_CONFIG_ITEM_VALUE = "your-client-id";
    private static final String API_KEY_CONFIG_ITEM_KEY = "key";
    private static final String API_KEY_CONFIG_ITEM_VALUE = "your-api-key";
    private static final String SECRET_CONFIG_ITEM_KEY = "secret";
    private static final String SECRET_CONFIG_ITEM_VALUE = "your-secret-key";

    private static final Integer CONNECTION_TIMEOUT = 30;
    private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503, 504, 520, 522, 525);
    private static final List<String> NON_FATAL_ERROR_MESSAGES = Arrays.asList(
            "Connection refused",
            "Connection reset",
            "Remote host closed connection during handshake",
            "Unexpected end of file from server");

    private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
    private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.5";
    private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
    private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.5";


    @Test
    public void testLoadingValidXmlConfigFileIsSuccessful() {

        final ExchangeType exchangeType = ConfigurationManager.loadConfig(ExchangeType.class,
                VALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);

        assertThat(exchangeType.getName()).isEqualTo(EXCHANGE_NAME);
        assertThat(exchangeType.getAdapter()).isEqualTo(EXCHANGE_ADAPTER);

        assertThat(exchangeType.getAuthenticationConfig().getConfigItems().get(0).getName()).isEqualTo(CLIENT_ID_CONFIG_ITEM_KEY);
        assertThat(exchangeType.getAuthenticationConfig().getConfigItems().get(0).getValue()).isEqualTo(CLIENT_ID_CONFIG_ITEM_VALUE);
        assertThat(exchangeType.getAuthenticationConfig().getConfigItems().get(1).getName()).isEqualTo(API_KEY_CONFIG_ITEM_KEY);
        assertThat(exchangeType.getAuthenticationConfig().getConfigItems().get(1).getValue()).isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
        assertThat(exchangeType.getAuthenticationConfig().getConfigItems().get(2).getName()).isEqualTo(SECRET_CONFIG_ITEM_KEY);
        assertThat(exchangeType.getAuthenticationConfig().getConfigItems().get(2).getValue()).isEqualTo(SECRET_CONFIG_ITEM_VALUE);

        assertThat(exchangeType.getNetworkConfig().getConnectionTimeout()).isEqualTo(CONNECTION_TIMEOUT);
        assertTrue(exchangeType.getNetworkConfig().getNonFatalErrorCodes().getCodes().containsAll(NON_FATAL_ERROR_CODES));
        assertTrue(exchangeType.getNetworkConfig().getNonFatalErrorMessages().getMessages().containsAll(NON_FATAL_ERROR_MESSAGES));

        assertThat(exchangeType.getOptionalConfig().getConfigItems().get(0).getName()).isEqualTo(BUY_FEE_CONFIG_ITEM_KEY);
        assertThat(exchangeType.getOptionalConfig().getConfigItems().get(0).getValue()).isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
        assertThat(exchangeType.getOptionalConfig().getConfigItems().get(1).getName()).isEqualTo(SELL_FEE_CONFIG_ITEM_KEY);
        assertThat(exchangeType.getOptionalConfig().getConfigItems().get(1).getValue()).isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadingMissingXmlConfigFileThrowsException() {
        ConfigurationManager.loadConfig(ExchangeType.class, MISSING_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadingInvalidXmlConfigFileThrowsException() {

        ConfigurationManager.loadConfig(ExchangeType.class,
                INVALID_XML_CONFIG_FILENAME, XML_SCHEMA_FILENAME);
    }

    /*
     * Painful, but necessary... ;-/
     */
    @Test
    public void testSavingConfigToXmlIsSuccessful() throws Exception {

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
        final OptionalConfigType optionalConfig = new OptionalConfigType();
        optionalConfig.getConfigItems().add(buyFee);
        optionalConfig.getConfigItems().add(sellFee);

        final ExchangeType exchangeConfig = new ExchangeType();
        exchangeConfig.setName(EXCHANGE_NAME);
        exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
        exchangeConfig.setAuthenticationConfig(authenticationConfig);
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOptionalConfig(optionalConfig);

        // Save it!
        ConfigurationManager.saveConfig(ExchangeType.class, exchangeConfig, XML_CONFIG_TO_SAVE_FILENAME);

        // Read it back in
        final ExchangeType exchangeReloaded = ConfigurationManager.loadConfig(ExchangeType.class,
                XML_CONFIG_TO_SAVE_FILENAME, XML_SCHEMA_FILENAME);

        assertThat(exchangeReloaded.getName()).isEqualTo(EXCHANGE_NAME);
        assertThat(exchangeReloaded.getAdapter()).isEqualTo(EXCHANGE_ADAPTER);

        assertThat(exchangeReloaded.getAuthenticationConfig().getConfigItems().get(0).getName()).isEqualTo(API_KEY_CONFIG_ITEM_KEY);
        assertThat(exchangeReloaded.getAuthenticationConfig().getConfigItems().get(0).getValue()).isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
        assertThat(exchangeReloaded.getAuthenticationConfig().getConfigItems().get(1).getName()).isEqualTo(SECRET_CONFIG_ITEM_KEY);
        assertThat(exchangeReloaded.getAuthenticationConfig().getConfigItems().get(1).getValue()).isEqualTo(SECRET_CONFIG_ITEM_VALUE);

        assertThat(exchangeReloaded.getNetworkConfig().getConnectionTimeout()).isEqualTo(CONNECTION_TIMEOUT);
        assertTrue(exchangeReloaded.getNetworkConfig().getNonFatalErrorCodes().getCodes().containsAll(NON_FATAL_ERROR_CODES));
        assertTrue(exchangeReloaded.getNetworkConfig().getNonFatalErrorMessages().getMessages().containsAll(NON_FATAL_ERROR_MESSAGES));

        assertThat(exchangeReloaded.getOptionalConfig().getConfigItems().get(0).getName()).isEqualTo(BUY_FEE_CONFIG_ITEM_KEY);
        assertThat(exchangeReloaded.getOptionalConfig().getConfigItems().get(0).getValue()).isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
        assertThat(exchangeReloaded.getOptionalConfig().getConfigItems().get(1).getName()).isEqualTo(SELL_FEE_CONFIG_ITEM_KEY);
        assertThat(exchangeReloaded.getOptionalConfig().getConfigItems().get(1).getValue()).isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);

        // cleanup
        Files.delete(FileSystems.getDefault().getPath(XML_CONFIG_TO_SAVE_FILENAME));
    }
}
