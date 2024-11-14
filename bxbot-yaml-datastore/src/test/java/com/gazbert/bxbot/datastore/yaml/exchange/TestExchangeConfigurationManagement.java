/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.datastore.yaml.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests the Exchange Adapter configuration is loaded as expected.
 *
 * @author gazbert
 */
class TestExchangeConfigurationManagement {

  private static final String VALID_YAML_CONFIG_FILENAME =
      "src/test/config/exchange/valid-exchange.yaml";

  private static final String INVALID_YAML_CONFIG_FILENAME =
      "src/test/config/exchange/invalid-exchange.yaml";

  private static final String MISSING_XML_CONFIG_FILENAME =
      "src/test/config/exchange-/missing-exchange.yaml";

  private static final String YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/exchange/saved-exchange.yaml";

  private static final String INVALID_YAML_CONFIG_TO_SAVE_FILENAME =
      "src/test/config/not-here/saved-exchange.yaml";

  private static final String EXCHANGE_NAME = "Bitstamp";
  private static final String EXCHANGE_ADAPTER =
      "com.gazbert.bxbot.exchanges.BitstampExchangeAdapter";

  private static final String CLIENT_ID_CONFIG_ITEM_KEY = "client-id";
  private static final String CLIENT_ID_CONFIG_ITEM_VALUE = "your-client-id";
  private static final String API_KEY_CONFIG_ITEM_KEY = "key";
  private static final String API_KEY_CONFIG_ITEM_VALUE = "your-api-key";
  private static final String SECRET_CONFIG_ITEM_KEY = "secret";
  private static final String SECRET_CONFIG_ITEM_VALUE = "your-secret-key";

  private static final Integer CONNECTION_TIMEOUT = 30;

  private static final List<Integer> NON_FATAL_ERROR_CODES =
      Arrays.asList(502, 503, 504, 520, 522, 525);

  private static final List<String> NON_FATAL_ERROR_MESSAGES =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake",
          "Unexpected end of file from server");

  private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
  private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.5";
  private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
  private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.5";

  @Test
  void testLoadingValidYamlConfigFileIsSuccessful() {
    final ConfigurationManager configurationManager = new ConfigurationManager();
    final ExchangeType exchangeType =
        configurationManager.loadConfig(ExchangeType.class, VALID_YAML_CONFIG_FILENAME);

    assertThat(exchangeType.getExchange().getName()).isEqualTo(EXCHANGE_NAME);
    assertThat(exchangeType.getExchange().getAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(exchangeType.getExchange().getAuthenticationConfig())
        .containsEntry(CLIENT_ID_CONFIG_ITEM_KEY, CLIENT_ID_CONFIG_ITEM_VALUE);

    assertThat(exchangeType.getExchange().getAuthenticationConfig())
        .containsEntry(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);

    assertThat(exchangeType.getExchange().getAuthenticationConfig())
        .containsEntry(SECRET_CONFIG_ITEM_KEY, SECRET_CONFIG_ITEM_VALUE);

    assertThat(exchangeType.getExchange().getNetworkConfig().getConnectionTimeout())
        .isEqualTo(CONNECTION_TIMEOUT);

    assertTrue(
        exchangeType
            .getExchange()
            .getNetworkConfig()
            .getNonFatalErrorCodes()
            .containsAll(NON_FATAL_ERROR_CODES));

    assertTrue(
        exchangeType
            .getExchange()
            .getNetworkConfig()
            .getNonFatalErrorMessages()
            .containsAll(NON_FATAL_ERROR_MESSAGES));

    assertThat(exchangeType.getExchange().getOtherConfig())
        .containsEntry(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);

    assertThat(exchangeType.getExchange().getOtherConfig())
        .containsEntry(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);
  }

  @Test
  void testLoadingMissingYamlConfigFileThrowsException() {
    final ConfigurationManager configurationManager = new ConfigurationManager();
    assertThrows(
        IllegalStateException.class,
        () -> configurationManager.loadConfig(ExchangeType.class, MISSING_XML_CONFIG_FILENAME));
  }

  @Test
  void testLoadingInvalidYamlConfigFileThrowsException() {
    final ConfigurationManager configurationManager = new ConfigurationManager();
    assertThrows(
        IllegalArgumentException.class,
        () -> configurationManager.loadConfig(ExchangeType.class, INVALID_YAML_CONFIG_FILENAME));
  }

  @Test
  void testSavingConfigToYamlIsSuccessful() throws Exception {
    final Map<String, String> authenticationConfig = new HashMap<>();
    authenticationConfig.put(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);
    authenticationConfig.put(SECRET_CONFIG_ITEM_KEY, SECRET_CONFIG_ITEM_VALUE);

    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

    final ExchangeConfig exchangeConfig = getExchangeConfig(authenticationConfig, networkConfig);

    final ExchangeType exchangeType = new ExchangeType();
    exchangeType.setExchange(exchangeConfig);

    final ConfigurationManager configurationManager = new ConfigurationManager();

    // Save it
    configurationManager.saveConfig(ExchangeType.class, exchangeType, YAML_CONFIG_TO_SAVE_FILENAME);

    // Read it back in
    final ExchangeType exchangeReloaded =
        configurationManager.loadConfig(ExchangeType.class, YAML_CONFIG_TO_SAVE_FILENAME);

    assertThat(exchangeReloaded.getExchange().getName()).isEqualTo(EXCHANGE_NAME);
    assertThat(exchangeReloaded.getExchange().getAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(exchangeReloaded.getExchange().getAuthenticationConfig())
        .containsEntry(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);

    assertThat(exchangeReloaded.getExchange().getAuthenticationConfig())
        .containsEntry(SECRET_CONFIG_ITEM_KEY, SECRET_CONFIG_ITEM_VALUE);

    assertThat(exchangeReloaded.getExchange().getNetworkConfig().getConnectionTimeout())
        .isEqualTo(CONNECTION_TIMEOUT);

    assertTrue(
        exchangeReloaded
            .getExchange()
            .getNetworkConfig()
            .getNonFatalErrorCodes()
            .containsAll(NON_FATAL_ERROR_CODES));

    assertTrue(
        exchangeReloaded
            .getExchange()
            .getNetworkConfig()
            .getNonFatalErrorMessages()
            .containsAll(NON_FATAL_ERROR_MESSAGES));

    assertThat(exchangeReloaded.getExchange().getOtherConfig())
        .containsEntry(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);

    assertThat(exchangeReloaded.getExchange().getOtherConfig())
        .containsEntry(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    // cleanup
    Files.delete(FileSystems.getDefault().getPath(YAML_CONFIG_TO_SAVE_FILENAME));
  }

  @Test
  void testSavingConfigToInvalidYamlFileIsHandled() {
    final Map<String, String> authenticationConfig = new HashMap<>();
    authenticationConfig.put(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);
    authenticationConfig.put(SECRET_CONFIG_ITEM_KEY, SECRET_CONFIG_ITEM_VALUE);

    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

    final ExchangeConfig exchangeConfig = getExchangeConfig(authenticationConfig, networkConfig);

    final ExchangeType exchangeType = new ExchangeType();
    exchangeType.setExchange(exchangeConfig);

    final ConfigurationManager configurationManager = new ConfigurationManager();

    assertThrows(
        IllegalStateException.class,
        () ->
            configurationManager.saveConfig(
                ExchangeType.class, exchangeType, INVALID_YAML_CONFIG_TO_SAVE_FILENAME));
  }

  // --------------------------------------------------------------------------
  // Canned data
  // --------------------------------------------------------------------------

  private static ExchangeConfig getExchangeConfig(
      Map<String, String> authenticationConfig, NetworkConfig networkConfig) {
    final Map<String, String> otherConfig = new HashMap<>();
    otherConfig.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    otherConfig.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setAuthenticationConfig(authenticationConfig);
    exchangeConfig.setNetworkConfig(networkConfig);
    exchangeConfig.setOtherConfig(otherConfig);
    return exchangeConfig;
  }
}
