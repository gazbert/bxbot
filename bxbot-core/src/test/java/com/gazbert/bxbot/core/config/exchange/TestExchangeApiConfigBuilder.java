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

package com.gazbert.bxbot.core.config.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests the behaviour of the Exchange API Config Builder is as expected.
 * 测试 Exchange API 配置生成器的行为是否符合预期。
 *
 * @author gazbert
 */
class TestExchangeApiConfigBuilder {

  private static final String EXCHANGE_NAME = "Bitstamp";
  private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";

  private static final String API_KEY_CONFIG_ITEM_KEY = "api-key";
  private static final String API_KEY_CONFIG_ITEM_VALUE = "apiKey--123";
  private static final String SECRET_CONFIG_ITEM_KEY = "secret";
  private static final String SECRET_FEE_CONFIG_ITEM_VALUE = "secret-key";

  private static final Integer CONNECTION_TIMEOUT = 30;
  private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503);
  private static final List<String> NON_FATAL_ERROR_MESSAGES =
      Arrays.asList("Connection refused", "Remote host closed connection during handshake 握手期间远程主机关闭连接");

  private static final String BUY_FEE_CONFIG_ITEM_KEY = "buy-fee";
  private static final String BUY_FEE_CONFIG_ITEM_VALUE = "0.20";
  private static final String SELL_FEE_CONFIG_ITEM_KEY = "sell-fee";
  private static final String SELL_FEE_CONFIG_ITEM_VALUE = "0.25";

  @Test
  void testBuildingConfig() {
    final com.gazbert.bxbot.exchange.api.ExchangeConfig exchangeApiConfig =
        ExchangeApiConfigBuilder.buildConfig(buildExchangeConfig());

    assertThat(exchangeApiConfig.getExchangeName()).isEqualTo(EXCHANGE_NAME);
    assertThat(exchangeApiConfig.getExchangeAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(exchangeApiConfig.getAuthenticationConfig().getItem(API_KEY_CONFIG_ITEM_KEY))
        .isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
    assertThat(exchangeApiConfig.getAuthenticationConfig().getItem(SECRET_CONFIG_ITEM_KEY))
        .isEqualTo(SECRET_FEE_CONFIG_ITEM_VALUE);

    assertThat(exchangeApiConfig.getNetworkConfig().getConnectionTimeout())
        .isEqualTo(CONNECTION_TIMEOUT);
    assertThat(exchangeApiConfig.getNetworkConfig().getNonFatalErrorCodes())
        .isEqualTo(NON_FATAL_ERROR_CODES);
    assertThat(exchangeApiConfig.getNetworkConfig().getNonFatalErrorMessages())
        .isEqualTo(NON_FATAL_ERROR_MESSAGES);

    assertThat(exchangeApiConfig.getOtherConfig().getItem(BUY_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
    assertThat(exchangeApiConfig.getOtherConfig().getItem(SELL_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);
  }

  @Test
  void testBuildingConfigWithMandatoryConfigOnly() {
    final com.gazbert.bxbot.exchange.api.ExchangeConfig exchangeApiConfig =
        ExchangeApiConfigBuilder.buildConfig(buildExchangeConfigWithMandatoryConfigOnly());

    assertThat(exchangeApiConfig.getExchangeName()).isEqualTo(EXCHANGE_NAME);
    assertThat(exchangeApiConfig.getExchangeAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(exchangeApiConfig.getAuthenticationConfig()).isNull();
    assertThat(exchangeApiConfig.getNetworkConfig()).isNull();
    assertThat(exchangeApiConfig.getOtherConfig()).isNull();
  }

  @Test
  void testBuildingConfigWithoutOptionalNetworkConfig() {
    final com.gazbert.bxbot.exchange.api.ExchangeConfig exchangeApiConfig =
        ExchangeApiConfigBuilder.buildConfig(buildExchangeConfigWithoutOptionalNetworkConfig());

    assertThat(exchangeApiConfig.getExchangeName()).isEqualTo(EXCHANGE_NAME);
    assertThat(exchangeApiConfig.getExchangeAdapter()).isEqualTo(EXCHANGE_ADAPTER);

    assertThat(exchangeApiConfig.getAuthenticationConfig().getItem(API_KEY_CONFIG_ITEM_KEY))
        .isEqualTo(API_KEY_CONFIG_ITEM_VALUE);
    assertThat(exchangeApiConfig.getAuthenticationConfig().getItem(SECRET_CONFIG_ITEM_KEY))
        .isEqualTo(SECRET_FEE_CONFIG_ITEM_VALUE);

    assertThat(exchangeApiConfig.getNetworkConfig().getConnectionTimeout())
        .isEqualTo(CONNECTION_TIMEOUT);
    assertThat(exchangeApiConfig.getNetworkConfig().getNonFatalErrorCodes()).isEmpty();
    assertThat(exchangeApiConfig.getNetworkConfig().getNonFatalErrorMessages()).isEmpty();

    assertThat(exchangeApiConfig.getOtherConfig().getItem(BUY_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(BUY_FEE_CONFIG_ITEM_VALUE);
    assertThat(exchangeApiConfig.getOtherConfig().getItem(SELL_FEE_CONFIG_ITEM_KEY))
        .isEqualTo(SELL_FEE_CONFIG_ITEM_VALUE);
  }

  private static Map<String, String> buildAuthenticationConfig() {
    final Map<String, String> authenticationConfig = new HashMap<>();
    authenticationConfig.put(API_KEY_CONFIG_ITEM_KEY, API_KEY_CONFIG_ITEM_VALUE);
    authenticationConfig.put(SECRET_CONFIG_ITEM_KEY, SECRET_FEE_CONFIG_ITEM_VALUE);
    return authenticationConfig;
  }

  private static NetworkConfig buildNetworkConfig() {
    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);
    return networkConfig;
  }

  private static NetworkConfig buildNetworkConfigWithoutErrorCodesAndMessages() {
    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    return networkConfig;
  }

  private static Map<String, String> buildOtherConfig() {
    final Map<String, String> otherConfig = new HashMap<>();
    otherConfig.put(BUY_FEE_CONFIG_ITEM_KEY, BUY_FEE_CONFIG_ITEM_VALUE);
    otherConfig.put(SELL_FEE_CONFIG_ITEM_KEY, SELL_FEE_CONFIG_ITEM_VALUE);
    return otherConfig;
  }

  private static ExchangeConfig buildExchangeConfig() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setAuthenticationConfig(buildAuthenticationConfig());
    exchangeConfig.setNetworkConfig(buildNetworkConfig());
    exchangeConfig.setOtherConfig(buildOtherConfig());
    return exchangeConfig;
  }

  private static ExchangeConfig buildExchangeConfigWithMandatoryConfigOnly() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    return exchangeConfig;
  }

  private static ExchangeConfig buildExchangeConfigWithoutOptionalNetworkConfig() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setAuthenticationConfig(buildAuthenticationConfig());
    exchangeConfig.setNetworkConfig(buildNetworkConfigWithoutErrorCodesAndMessages());
    exchangeConfig.setOtherConfig(buildOtherConfig());
    return exchangeConfig;
  }
}
