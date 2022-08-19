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

package com.gazbert.bxbot.repository.yaml;

import static com.gazbert.bxbot.datastore.yaml.FileLocations.EXCHANGE_CONFIG_YAML_FILENAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.exchange.ExchangeType;
import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import com.gazbert.bxbot.repository.ExchangeConfigRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests YAML backed Exchange configuration repository behaves as expected.
 * * 测试 YAML 支持的 Exchange 配置存储库的行为是否符合预期。
 *
 * @author gazbert
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
@PowerMockIgnore({
    "javax.crypto.*",
    "javax.management.*",
    "com.sun.org.apache.xerces.*",
    "javax.xml.parsers.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
public class TestExchangeConfigYamlRepository {

  private static final String EXCHANGE_NAME = "Bitstamp";
  private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";

  private static final String API_KEY_CONFIG_ITEM_KEY = "api-key";
  private static final String API_KEY_CONFIG_ITEM_VALUE = "apiKey--123";

  private static final String SECRET_CONFIG_ITEM_KEY = "secret";
  private static final String SECRET_CONFIG_ITEM_VALUE = "secret-key";

  private static final Integer CONNECTION_TIMEOUT = 30;
  private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503, 504);
  private static final List<String> NON_FATAL_ERROR_MESSAGES =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake");

  private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
  private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";
  private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
  private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";

  @Before
  public void setup() {
    PowerMock.mockStatic(ConfigurationManager.class);
  }

  @Test
  public void whenGetCalledThenReturnExchangeConfig() {
    expect(
            ConfigurationManager.loadConfig(
                eq(ExchangeType.class), eq(EXCHANGE_CONFIG_YAML_FILENAME)))
        .andReturn(someInternalExchangeConfig());

    PowerMock.replayAll();

    final ExchangeConfigRepository exchangeConfigRepository = new ExchangeConfigYamlRepository();
    final ExchangeConfig exchangeConfig = exchangeConfigRepository.get();

    assertThat(exchangeConfig.getName()).isEqualTo(EXCHANGE_NAME);
    assertThat(exchangeConfig.getAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(exchangeConfig.getAuthenticationConfig().get(API_KEY_CONFIG_ITEM_KEY))
        .isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
    assertThat(exchangeConfig.getAuthenticationConfig().get(SECRET_CONFIG_ITEM_KEY))
        .isEqualTo(SECRET_CONFIG_ITEM_VALUE);

    assertThat(exchangeConfig.getNetworkConfig().getConnectionTimeout())
        .isEqualTo(CONNECTION_TIMEOUT);
    assertThat(exchangeConfig.getNetworkConfig().getNonFatalErrorCodes())
        .isEqualTo(NON_FATAL_ERROR_CODES);
    assertThat(exchangeConfig.getNetworkConfig().getNonFatalErrorMessages())
        .isEqualTo(NON_FATAL_ERROR_MESSAGES);
    assertThat(exchangeConfig.getOtherConfig().get(BUY_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
    assertThat(exchangeConfig.getOtherConfig().get(SELL_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);

    PowerMock.verifyAll();
  }

  @Test
  public void whenSaveCalledThenExpectRepositoryToSaveItAndReturnSavedExchangeConfig() {
    ConfigurationManager.saveConfig(
        eq(ExchangeType.class), anyObject(ExchangeType.class), eq(EXCHANGE_CONFIG_YAML_FILENAME));

    expect(
            ConfigurationManager.loadConfig(
                eq(ExchangeType.class), eq(EXCHANGE_CONFIG_YAML_FILENAME)))
        .andReturn(someInternalExchangeConfig());

    PowerMock.replayAll();

    final ExchangeConfigRepository exchangeConfigRepository = new ExchangeConfigYamlRepository();
    final ExchangeConfig savedExchangeConfig =
        exchangeConfigRepository.save(someExternalExchangeConfig());

    assertThat(savedExchangeConfig.getName()).isEqualTo(EXCHANGE_NAME);
    assertThat(savedExchangeConfig.getAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(savedExchangeConfig.getAuthenticationConfig().get(API_KEY_CONFIG_ITEM_KEY))
        .isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
    assertThat(savedExchangeConfig.getAuthenticationConfig().get(SECRET_CONFIG_ITEM_KEY))
        .isEqualTo(SECRET_CONFIG_ITEM_VALUE);

    assertThat(savedExchangeConfig.getNetworkConfig().getConnectionTimeout())
        .isEqualTo(CONNECTION_TIMEOUT);
    assertThat(savedExchangeConfig.getNetworkConfig().getNonFatalErrorCodes())
        .isEqualTo(NON_FATAL_ERROR_CODES);
    assertThat(savedExchangeConfig.getNetworkConfig().getNonFatalErrorMessages())
        .isEqualTo(NON_FATAL_ERROR_MESSAGES);
    assertThat(savedExchangeConfig.getOtherConfig().get(BUY_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
    assertThat(savedExchangeConfig.getOtherConfig().get(SELL_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);

    PowerMock.verifyAll();
  }

  // --------------------------------------------------------------------------
  // Private utils
  // 私有工具
  // --------------------------------------------------------------------------

  private static ExchangeType someInternalExchangeConfig() {
    final Map<String, String> authenticationConfig = new HashMap<>();
    authenticationConfig.put(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);
    authenticationConfig.put(SECRET_CONFIG_ITEM_KEY, SECRET_CONFIG_ITEM_VALUE);

    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

    final Map<String, String> otherConfig = new HashMap<>();
    otherConfig.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    otherConfig.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setAuthenticationConfig(authenticationConfig);
    exchangeConfig.setNetworkConfig(networkConfig);
    exchangeConfig.setOtherConfig(otherConfig);

    final ExchangeType exchangeType = new ExchangeType();
    exchangeType.setExchange(exchangeConfig);
    return exchangeType;
  }

  private static ExchangeConfig someExternalExchangeConfig() {
    final Map<String, String> authenticationConfig = new HashMap<>();
    authenticationConfig.put(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);
    authenticationConfig.put(SECRET_CONFIG_ITEM_KEY, SECRET_CONFIG_ITEM_VALUE);

    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

    final Map<String, String> optionalConfig = new HashMap<>();
    optionalConfig.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    optionalConfig.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);

    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setAuthenticationConfig(authenticationConfig);
    exchangeConfig.setNetworkConfig(networkConfig);
    exchangeConfig.setOtherConfig(optionalConfig);

    return exchangeConfig;
  }
}
