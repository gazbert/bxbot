package com.gazbert.bxbot.core.config.exchange;

import com.gazbert.bxbot.domain.exchange.ExchangeConfig;
import com.gazbert.bxbot.domain.exchange.NetworkConfig;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Util class for building the Exchange Adapter config.
 *
 * @author gazbert
 */
public class ExchangeAdapterConfigBuilder {

  private static final Logger LOG = LogManager.getLogger();

  private ExchangeAdapterConfigBuilder() {}

  /**
   * TODO: does stuff
   */
  public static ExchangeConfigImpl buildConfig(ExchangeConfig domainExchangeConfig) {

    final ExchangeConfigImpl adapterExchangeConfig = new ExchangeConfigImpl();

    // Fetch optional network config
    final NetworkConfig networkConfig = domainExchangeConfig.getNetworkConfig();
    if (networkConfig != null) {
      final NetworkConfigImpl adapterNetworkConfig = new NetworkConfigImpl();
      adapterNetworkConfig.setConnectionTimeout(networkConfig.getConnectionTimeout());

      // Grab optional non-fatal error codes
      final List<Integer> nonFatalErrorCodes = networkConfig.getNonFatalErrorCodes();
      if (nonFatalErrorCodes != null) {
        adapterNetworkConfig.setNonFatalErrorCodes(nonFatalErrorCodes);
      } else {
        LOG.info(
            () ->
                "No (optional) NetworkConfiguration NonFatalErrorCodes have been set for "
                    + "Exchange Adapter: "
                    + domainExchangeConfig.getAdapter());
      }

      // Grab optional non-fatal error messages
      final List<String> nonFatalErrorMessages = networkConfig.getNonFatalErrorMessages();
      if (nonFatalErrorMessages != null) {
        adapterNetworkConfig.setNonFatalErrorMessages(nonFatalErrorMessages);
      } else {
        LOG.info(
            () ->
                "No (optional) NetworkConfiguration NonFatalErrorMessages have been set for "
                    + "Exchange Adapter: "
                    + domainExchangeConfig.getAdapter());
      }

      adapterExchangeConfig.setNetworkConfig(adapterNetworkConfig);
      LOG.info(() -> "NetworkConfiguration has been set: " + adapterNetworkConfig);

    } else {
      LOG.info(
          () ->
              "No (optional) NetworkConfiguration has been set for Exchange Adapter: "
                  + domainExchangeConfig.getAdapter());
    }

    // Fetch optional authentication config
    final Map<String, String> authenticationConfig = domainExchangeConfig.getAuthenticationConfig();
    if (authenticationConfig != null) {
      final AuthenticationConfigImpl adapterAuthenticationConfig = new AuthenticationConfigImpl();
      adapterAuthenticationConfig.setItems(authenticationConfig);
      adapterExchangeConfig.setAuthenticationConfig(adapterAuthenticationConfig);

      // We don't log the creds!
      LOG.info(() -> "AuthenticationConfiguration has been set successfully.");

    } else {
      LOG.info(
          () ->
              "No (optional) AuthenticationConfiguration has been set for Exchange Adapter: "
                  + domainExchangeConfig.getAdapter());
    }

    // Fetch optional config
    final Map<String, String> otherConfig = domainExchangeConfig.getOtherConfig();
    if (otherConfig != null) {
      final OtherConfigImpl adapterOtherConfig = new OtherConfigImpl();
      adapterOtherConfig.setItems(otherConfig);
      adapterExchangeConfig.setOtherConfig(adapterOtherConfig);
      LOG.info(() -> "Other Exchange Adapter config has been set: " + adapterOtherConfig);
    } else {
      LOG.info(
          () ->
              "No Other config has been set for Exchange Adapter: "
                  + domainExchangeConfig.getAdapter());
    }

    return adapterExchangeConfig;
  }
}
