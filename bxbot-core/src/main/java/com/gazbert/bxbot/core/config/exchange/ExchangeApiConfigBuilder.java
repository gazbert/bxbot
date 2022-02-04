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

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Util class for building the Exchange API config.
 *
 * @author gazbert
 */
public final class ExchangeApiConfigBuilder {

  private static final Logger LOG = LogManager.getLogger();

  private ExchangeApiConfigBuilder() {
  }

  /**
   * Builds Exchange API config.
   *
   * @param exchangeConfig the raw Exchange config.
   * @return the API Exchange config.
   */
  public static ExchangeConfigImpl buildConfig(ExchangeConfig exchangeConfig) {

    final ExchangeConfigImpl exchangeApiConfig = new ExchangeConfigImpl();
    exchangeApiConfig.setExchangeName(exchangeConfig.getName());
    exchangeApiConfig.setExchangeAdapter(exchangeConfig.getAdapter());

    final NetworkConfig networkConfig = exchangeConfig.getNetworkConfig();
    if (networkConfig != null) {
      final NetworkConfigImpl exchangeApiNetworkConfig = new NetworkConfigImpl();
      exchangeApiNetworkConfig.setConnectionTimeout(networkConfig.getConnectionTimeout());

      final List<Integer> nonFatalErrorCodes = networkConfig.getNonFatalErrorCodes();
      if (nonFatalErrorCodes != null && !nonFatalErrorCodes.isEmpty()) {
        exchangeApiNetworkConfig.setNonFatalErrorCodes(nonFatalErrorCodes);
      } else {
        LOG.info(
            () ->
                "No (optional) NetworkConfiguration NonFatalErrorCodes have been set for "
                    + "Exchange Adapter: "
                    + exchangeConfig.getAdapter());
      }

      final List<String> nonFatalErrorMessages = networkConfig.getNonFatalErrorMessages();
      if (nonFatalErrorMessages != null && !nonFatalErrorMessages.isEmpty()) {
        exchangeApiNetworkConfig.setNonFatalErrorMessages(nonFatalErrorMessages);
      } else {
        LOG.info(
            () ->
                "No (optional) NetworkConfiguration NonFatalErrorMessages have been set for "
                    + "Exchange Adapter: "
                    + exchangeConfig.getAdapter());
      }

      exchangeApiConfig.setNetworkConfig(exchangeApiNetworkConfig);
      LOG.info(() -> "NetworkConfiguration has been set: " + exchangeApiNetworkConfig);

    } else {
      LOG.info(
          () ->
              "No (optional) NetworkConfiguration has been set for Exchange Adapter: "
                  + exchangeConfig.getAdapter());
    }

    final Map<String, String> authenticationConfig = exchangeConfig.getAuthenticationConfig();
    if (authenticationConfig != null) {
      final AuthenticationConfigImpl exchangeApiAuthenticationConfig =
          new AuthenticationConfigImpl();
      exchangeApiAuthenticationConfig.setItems(authenticationConfig);
      exchangeApiConfig.setAuthenticationConfig(exchangeApiAuthenticationConfig);

      // We don't log the creds!
      LOG.info(() -> "AuthenticationConfiguration has been set successfully.");

    } else {
      LOG.info(
          () ->
              "No (optional) AuthenticationConfiguration has been set for Exchange Adapter: "
                  + exchangeConfig.getAdapter());
    }

    final Map<String, String> otherConfig = exchangeConfig.getOtherConfig();
    if (otherConfig != null) {
      final OtherConfigImpl exchangeApiOtherConfig = new OtherConfigImpl();
      exchangeApiOtherConfig.setItems(otherConfig);
      exchangeApiConfig.setOtherConfig(exchangeApiOtherConfig);
      LOG.info(() -> "Other Exchange Adapter config has been set: " + exchangeApiOtherConfig);
    } else {
      LOG.info(
          () ->
              "No Other config has been set for Exchange Adapter: " + exchangeConfig.getAdapter());
    }

    return exchangeApiConfig;
  }
}
