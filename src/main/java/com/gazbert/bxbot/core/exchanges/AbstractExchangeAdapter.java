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
package com.gazbert.bxbot.core.exchanges;

import com.gazbert.bxbot.core.api.trading.ExchangeTimeoutException;
import com.gazbert.bxbot.core.api.trading.TradingApiException;
import com.gazbert.bxbot.core.util.LogUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Base class for shared Exchange Adapter functionality.
 *
 * @author gazbert
 * @since 01/07/16
 */
abstract class AbstractExchangeAdapter {

    private static final Logger LOG = Logger.getLogger(AbstractExchangeAdapter.class);

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
     * Makes a public request to the Exchange. It uses HTTP GET.
     *
     * @param url               the URL to invoke.
     * @param connectionTimeout timeout value before a 'stuck' connection is terminated. Value must be in seconds.
     * @return the response from the Exchange.
     * @throws ExchangeTimeoutException if a timeout occurred trying to connect to the exchange. The timeout limit is
     *                                  implementation specific for each Exchange Adapter. This allows for recovery from
     *                                  temporary network issues.
     * @throws TradingApiException      if the API call failed for any reason other than a timeout. This means something
     *                                  really bad as happened.
     */
    ExchangeHttpResponse sendPublicNetworkRequest(URL url, int connectionTimeout) throws TradingApiException, ExchangeTimeoutException {

        HttpURLConnection exchangeConnection = null;
        final StringBuilder exchangeResponse = new StringBuilder();

        try {

            LogUtils.log(LOG, Level.DEBUG, () -> "Using following URL for API call: " + url);

            exchangeConnection = (HttpURLConnection) url.openConnection();
            exchangeConnection.setUseCaches(false);
            exchangeConnection.setDoOutput(true);

            exchangeConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Er, perhaps, I need to be a bit more stealth here...
            exchangeConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");

            // Add a timeout so we don't get blocked indefinitley; timeout on URLConnection is in millis.
            final int timeoutInMillis = connectionTimeout * 1000;
            exchangeConnection.setConnectTimeout(timeoutInMillis);
            exchangeConnection.setReadTimeout(timeoutInMillis);

            // Grab the response - we just block here as per Java Connection API
            final BufferedReader responseInputStream = new BufferedReader(new InputStreamReader(
                    exchangeConnection.getInputStream()));

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
            throw new ExchangeTimeoutException(errorMsg, e);

        } catch (IOException e) {


            // TODO rework this stuff to read network retry codes from exchange adapter config
            try {

                /*
                 * Occasionally get this on Bitfinex, Huobi,
                 */
                if (e.getMessage() != null &&
                        (e.getMessage().contains("Connection reset") ||
                         e.getMessage().contains("Remote host closed connection during handshake") ||
                         e.getMessage().contains("Unexpected end of file from server") ||
                         e.getMessage().contains("Connection refused"))) {

                    final String errorMsg = "Failed to connect to Exchange. SSL Connection was refused or reset by the server.";
                    LOG.error(errorMsg, e);
                    throw new ExchangeTimeoutException(errorMsg, e);

                /*
                 * Exchange sometimes fails with these codes, but recovers by next request...
                 */
                } else if (exchangeConnection != null && (exchangeConnection.getResponseCode() == 502
                        || exchangeConnection.getResponseCode() == 503
                        || exchangeConnection.getResponseCode() == 504

                        // Cloudflare related
                        || exchangeConnection.getResponseCode() == 520
                        || exchangeConnection.getResponseCode() == 522
                        || exchangeConnection.getResponseCode() == 525)) {

                    final String errorMsg = IO_5XX_TIMEOUT_ERROR_MSG;
                    LOG.error(errorMsg, e);
                    throw new ExchangeTimeoutException(errorMsg, e);

                } else {
                    final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
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
     * Wrapper for holding Exchange HTTP response.
     */
    static class ExchangeHttpResponse {

        private int statusCode;
        private String reasonPhrase;
        private String payload;

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
            return AbstractExchangeAdapter.ExchangeHttpResponse.class.getSimpleName()
                    + " ["
                    + "statusCode=" + statusCode
                    + ", reasonPhrase=" + reasonPhrase
                    + ", payload=" + payload
                    + "]";
        }
    }
}
