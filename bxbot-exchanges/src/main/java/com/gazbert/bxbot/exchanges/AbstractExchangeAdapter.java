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
package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OptionalConfig;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Base class for shared Exchange Adapter functionality.
 *
 * @author gazbert
 * @since 1.0
 */
abstract class AbstractExchangeAdapter {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * IO Socket Timeout error message for logging.
     */
    private static final String IO_SOCKET_TIMEOUT_ERROR_MSG = "Failed to connect to Exchange due to socket timeout.";

    /**
     * IO 5xx Timeout error message for logging.
     */
    private static final String IO_5XX_TIMEOUT_ERROR_MSG = "Failed to connect to Exchange due to 5xx timeout.";

    /**
     * Fatal error message for when AuthenticationConfig is missing in the exchange.xml config file.
     */
    private static final String AUTHENTICATION_CONFIG_MISSING = "AuthenticationConfig is missing for adapter in exchange.xml file.";

    /**
     * Fatal error message for when NetworkConfig is missing in the exchange.xml config file.
     */
    private static final String NETWORK_CONFIG_MISSING = "NetworkConfig is missing for adapter in exchange.xml file.";

    /**
     * Fatal error message for when OptionalConfig is missing in the exchange.xml config file.
     */
    private static final String OTHER_CONFIG_MISSING = "OptionalConfig is missing for adapter in exchange.xml file.";

    /**
     * Used for building error messages for missing config.
     */
    private static final String CONFIG_IS_NULL_OR_ZERO_LENGTH = " cannot be null or zero length! HINT: is the value set in the ";

    /**
     * Name of connection timeout property in config file.
     */
    private static final String CONNECTION_TIMEOUT_PROPERTY_NAME = "connection-timeout";

    /**
     * Name of non-fatal-error-codes property in config file.
     */
    private static final String NON_FATAL_ERROR_CODES_PROPERTY_NAME = "non-fatal-error-codes";

    /**
     * Name of non-fatal-error-messages property in config file.
     */
    private static final String NON_FATAL_ERROR_MESSAGES_PROPERTY_NAME = "non-fatal-error-messages";

    /**
     * Exchange Adapter config file location.
     */
    private static final String EXCHANGE_CONFIG_FILE = "config/exchange.xml";

    /**
     * The connection timeout in SECONDS for terminating hung connections to the exchange.
     */
    private int connectionTimeout;

    /**
     * HTTP status codes for non-fatal network connection failures.
     * Used to decide to throw {@link ExchangeNetworkException}.
     */
    private final Set<Integer> nonFatalNetworkErrorCodes;

    /**
     * java.io exception messages for non-fatal network connection failures.
     * Used to decide to throw {@link ExchangeNetworkException}.
     */
    private final Set<String> nonFatalNetworkErrorMessages;


    /**
     * Constructor set some sensible defaults for the network config.
     */
    AbstractExchangeAdapter() {
        connectionTimeout = 30;
        nonFatalNetworkErrorCodes = new HashSet<>();
        nonFatalNetworkErrorMessages = new HashSet<>();
    }

    /**
     * Makes a request to the Exchange.
     *
     * @param url            the URL to invoke.
     * @param postData       optional post data to send. This can be null.
     * @param httpMethod     the HTTP method to use, e.g. GET, POST, DELETE
     * @param requestHeaders optional request headers to set on the {@link URLConnection} used to invoke the Exchange.
     * @return the response from the Exchange.
     * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
     *                                  This exception allows for recovery from temporary network issues.
     * @throws TradingApiException      if the API call failed for any reason other than a network error. This means something
     *                                  really bad as happened.
     */
    ExchangeHttpResponse sendNetworkRequest(URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
            throws TradingApiException, ExchangeNetworkException {

        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

        try {

            LOG.debug(() -> "Using following URL for API call: " + url);

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);
            exchangeConnection.setRequestMethod(httpMethod); // GET|POST|DELETE

            // Er, perhaps, I need to be a bit more stealth here... this was needed for some exchanges back in the day!
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.78 Safari/537.36");

            if (requestHeaders != null) {
                for (final Map.Entry<String, String> requestHeader : requestHeaders.entrySet()) {
                    exchangeConnection.setRequestProperty(requestHeader.getKey(), requestHeader.getValue());
                    LOG.debug(() -> "Setting following request header: " + requestHeader);
                }
            }

            // Add a timeout so we don't get blocked indefinitely; timeout on URLConnection is in millis.
            final int timeoutInMillis = connectionTimeout * 1000;
            exchangeConnection.setConnectTimeout(timeoutInMillis);
            exchangeConnection.setReadTimeout(timeoutInMillis);

            if (httpMethod.equalsIgnoreCase("POST") && postData != null) {
                LOG.debug(() -> "Doing POST with request body: " + postData);
                final OutputStreamWriter outputPostStream = new OutputStreamWriter(exchangeConnection.getOutputStream(), "UTF-8");
                outputPostStream.write(postData);
                outputPostStream.close();
            }

            // Grab the response - we just block here as per Connection API
            final BufferedReader responseInputStream = new BufferedReader(new InputStreamReader(
                    exchangeConnection.getInputStream(), "UTF-8"));

            // Read the JSON response lines into our response buffer
            String responseLine;
            while ((responseLine = responseInputStream.readLine()) != null) {
                exchangeResponse.append(responseLine);
            }
            responseInputStream.close();

            return new ExchangeHttpResponse(exchangeConnection.getResponseCode(), exchangeConnection.getResponseMessage(),
                    exchangeResponse.toString());

        } catch (MalformedURLException e) {
            final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new TradingApiException(errorMsg, e);

        } catch (SocketTimeoutException e) {
            final String errorMsg = IO_SOCKET_TIMEOUT_ERROR_MSG;
            LOG.error(errorMsg, e);
            throw new ExchangeNetworkException(errorMsg, e);

        } catch (FileNotFoundException | UnknownHostException e) {
            // Huobi started throwing FileNotFoundException as of 8 Nov 2015 :-/
            // EC2 started throwing UnknownHostException for BTC-e, GDAX, as of 14 July 2016 :-/
            final String errorMsg = "Failed to connect to Exchange. It's dead Jim!";
            LOG.error(errorMsg, e);
            throw new ExchangeNetworkException(errorMsg, e);

        } catch (IOException e) {

            // Check if this is a non-fatal network error
            try {

                if (e.getMessage() != null && nonFatalNetworkErrorMessages.contains(e.getMessage())) {

                    final String errorMsg = "Failed to connect to Exchange. SSL Connection was refused or reset by the server.";
                    LOG.error(errorMsg, e);
                    throw new ExchangeNetworkException(errorMsg, e);

                } else if (exchangeConnection != null && nonFatalNetworkErrorCodes.contains(exchangeConnection.getResponseCode())) {

                    final String errorMsg = IO_5XX_TIMEOUT_ERROR_MSG;
                    LOG.error(errorMsg, e);
                    throw new ExchangeNetworkException(errorMsg, e);

                } else {
                    String errorMsg = UNEXPECTED_IO_ERROR_MSG;

                    // Check for any clue in the response...
                    if (exchangeConnection != null) {
                        final InputStream rawErrorStream = exchangeConnection.getErrorStream();
                        if (rawErrorStream != null) {
                            final BufferedReader errorInputStream = new BufferedReader(new InputStreamReader(rawErrorStream, "UTF-8"));
                            final StringBuilder errorResponse = new StringBuilder();
                            String errorLine;
                            while ((errorLine = errorInputStream.readLine()) != null) {
                                errorResponse.append(errorLine);
                            }
                            errorInputStream.close();
                            errorMsg += " ErrorStream Response: " + errorResponse;
                        }
                    }
                    LOG.error(errorMsg, e);
                    throw new TradingApiException(errorMsg, e);
                }
            } catch (IOException e1) {

                final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
                LOG.error(errorMsg, e1);
                throw new TradingApiException(errorMsg, e1);
            }
        } finally {
            if (exchangeConnection != null) {
                exchangeConnection.disconnect();
            }
        }
    }

    /**
     * Sets the network config for the exchange adapter. This helper method expects the network config to be present.
     *
     * @param exchangeConfig the exchange config.
     * @throws IllegalArgumentException if the network config is not set.
     */
    void setNetworkConfig(ExchangeConfig exchangeConfig) {

        final NetworkConfig networkConfig = exchangeConfig.getNetworkConfig();
        if (networkConfig == null) {
            final String errorMsg = NETWORK_CONFIG_MISSING + exchangeConfig;
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        connectionTimeout = networkConfig.getConnectionTimeout();
        if (connectionTimeout == 0) {
            final String errorMsg = CONNECTION_TIMEOUT_PROPERTY_NAME + " cannot be 0 value." + exchangeConfig;
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        LOG.info(() -> CONNECTION_TIMEOUT_PROPERTY_NAME + ": " + connectionTimeout);

        final List<Integer> nonFatalErrorCodesFromConfig = networkConfig.getNonFatalErrorCodes();
        if (nonFatalErrorCodesFromConfig != null) {
            nonFatalNetworkErrorCodes.addAll(nonFatalErrorCodesFromConfig);
        }
        LOG.info(() -> NON_FATAL_ERROR_CODES_PROPERTY_NAME + ": " + nonFatalNetworkErrorCodes);

        final List<String> nonFatalErrorMessagesFromConfig = networkConfig.getNonFatalErrorMessages();
        if (nonFatalErrorMessagesFromConfig != null) {
            nonFatalNetworkErrorMessages.addAll(nonFatalErrorMessagesFromConfig);
        }
        LOG.info(() -> NON_FATAL_ERROR_MESSAGES_PROPERTY_NAME + ": " + nonFatalNetworkErrorMessages);
    }

    /**
     * Fetches the authentication config for the exchange adapter.
     *
     * @param exchangeConfig the exchange adapter config.
     * @return the authentication config for the adapter.
     * @throws IllegalArgumentException if authentication config is not set in exchange adapter config.
     */
    AuthenticationConfig getAuthenticationConfig(ExchangeConfig exchangeConfig) {

        final AuthenticationConfig authenticationConfig = exchangeConfig.getAuthenticationConfig();
        if (authenticationConfig == null) {
            final String errorMsg = AUTHENTICATION_CONFIG_MISSING + exchangeConfig;
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return authenticationConfig;
    }

    /**
     * Fetches the optional config for the exchange adapter.
     *
     * @param exchangeConfig the exchange adapter config.
     * @return the optional config for the adapter.
     * @throws IllegalArgumentException if optional config is not set.
     */
    OptionalConfig getOptionalConfig(ExchangeConfig exchangeConfig) {

        final OptionalConfig optionalConfig = exchangeConfig.getOptionalConfig();
        if (optionalConfig == null) {
            final String errorMsg = OTHER_CONFIG_MISSING + exchangeConfig;
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return optionalConfig;
    }

    /**
     * Fetches an authentication item value from the adapter config.
     *
     * @param authenticationConfig the authentication config for the adapter.
     * @param itemName             the config item name, e.g. key. secret, client-id
     * @return the config item value.
     * @throws IllegalArgumentException if authentication item is not set.
     */
    String getAuthenticationConfigItem(AuthenticationConfig authenticationConfig, String itemName) {

        final String itemValue = authenticationConfig.getItem(itemName);
        // WARNING: careful when you log this
//        LOG.info( () -> itemName + ": " + itemValue);
        return assertItemExists(itemName, itemValue);
    }

    /**
     * Fetches an optional config item value from the adapter config.
     *
     * @param optionalConfig the optional config for the adapter.
     * @param itemName    the config item name, e.g. buy-fee, sell-fee
     * @return the config item value.
     * @throws IllegalArgumentException if authentication item is not set.
     */
    String getOptionalConfigItem(OptionalConfig optionalConfig, String itemName) {

        final String itemValue = optionalConfig.getItem(itemName);
        LOG.info(() -> itemName + ": " + itemValue);
        return assertItemExists(itemName, itemValue);
    }

    /**
     * Sorts the request params alphabetically (uses natural ordering) and returns them as a query string.
     *
     * @param params the request params to sort.
     * @return the query string containing the sorted request params.
     */
    String createAlphabeticallySortedQueryString(Map<String, String> params) {

        final List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); // use natural/alphabetical ordering of params

        final StringBuilder sortedQueryString = new StringBuilder();
        for (final String param : keys) {
            if (sortedQueryString.length() > 0) {
                sortedQueryString.append("&");
            }
            sortedQueryString.append(param);
            sortedQueryString.append("=");
            sortedQueryString.append(params.get(param));
        }
        return sortedQueryString.toString();
    }

    /**
     * Wrapper for holding Exchange HTTP response.
     */
    static class ExchangeHttpResponse {

        private final int statusCode;
        private final String reasonPhrase;
        private final String payload;

        ExchangeHttpResponse(int statusCode, String reasonPhrase, String payload) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.payload = payload;
        }

        String getReasonPhrase() {
            return reasonPhrase;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("statusCode", statusCode)
                    .add("reasonPhrase", reasonPhrase)
                    .add("payload", payload)
                    .toString();
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Util methods
    // ------------------------------------------------------------------------------------------------

    private static String assertItemExists(String itemName, String itemValue) {
        if (itemValue == null || itemValue.length() == 0) {
            final String errorMsg = itemName + CONFIG_IS_NULL_OR_ZERO_LENGTH + EXCHANGE_CONFIG_FILE + " ?";
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return itemValue;
    }
}
