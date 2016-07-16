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

package com.gazbert.bxbot.core.exchanges;

import com.gazbert.bxbot.core.api.exchange.AuthenticationConfig;
import com.gazbert.bxbot.core.api.exchange.ExchangeAdapter;
import com.gazbert.bxbot.core.api.exchange.ExchangeConfig;
import com.gazbert.bxbot.core.api.exchange.OtherConfig;
import com.gazbert.bxbot.core.api.trading.*;
import com.gazbert.bxbot.core.util.LogUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * TODO Work in Progress...
 *
 * <p>
 * Exchange Adapter for integrating with the GDAX (formerly Coinbase) exchange using the
 * <a href="https://en.wikipedia.org/wiki/Financial_Information_eXchange">FIX</a> protocol.
 * The GDAX FIX API is documented <a href="https://docs.gdax.com/#fix-api">here</a>.
 * </p>
 *
 * <p>
 * <strong>
 * DISCLAIMER:
 * This Exchange Adapter is provided as-is; it might have bugs in it and you could lose money. Despite running live
 * on GDAX, it has only been unit tested up until the point of calling the
 * {@link #sendPublicRequestToExchange(String, Map)} and
 * {@link #sendAuthenticatedRequestToExchange(String, String, Map)} methods. Use it at our own risk!
 * </strong>
 * </p>
 *
 * <p>
 * This adapter only supports the GDAX <a href="https://docs.gdax.com/#fix-api">FIX API</a>. The design
 * of the API and documentation is excellent.
 * </p>
 *
 * <p>
 * TODO check this
 * Exchange fees are loaded from the exchange.xml file on startup; they are not fetched from the exchange
 * at runtime as the GDAX FIX API does not support this. The fees are used across all markets. Make sure you keep
 * an eye on the <a href="https://docs.gdax.com/#fees">exchange fees</a> and update the config accordingly.
 * This adapter will use the <em>Taker</em> fees to keep things simple for now.
 * </p>
 *
 * <p>
 * TODO check this
 * NOTE: GDAX requires all price values to be limited to 2 decimal places when creating orders.
 * This adapter truncates any prices with more than 2 decimal places and rounds using
 * {@link java.math.RoundingMode#HALF_EVEN}, E.g. 250.176 would be sent to the exchange as 250.18.
 * </p>
 *
 * <p>
 * The Exchange Adapter is <em>not</em> thread safe. It expects to be called using a single thread in order to
 * preserve trade execution order. The {@link URLConnection} achieves this by blocking/waiting on the input stream
 * (response) for each API call.
 * </p>
 *
 * <p>
 * The {@link TradingApi} calls will throw a {@link ExchangeNetworkException} if a network error occurs trying to
 * connect to the exchange. A {@link TradingApiException} is thrown for <em>all</em> other failures.
 * </p>
 *
 * @author gazbert
 */
public final class GdaxFixExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

    private static final Logger LOG = Logger.getLogger(GdaxFixExchangeAdapter.class);

    /**
     * The public API URI.
     */
    private static final String PUBLIC_API_BASE_URL = "https://api.gdax.com/";

    /**
     * The Authenticated API URI - it is the same as the Authenticated URL as of 12 Jul 2016.
     */
    private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

    /**
     * Used for reporting unexpected errors.
     */
    private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in GDAX Exchange Adapter. ";

    /**
     * Unexpected IO error message for logging.
     */
    private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

    /**
     * Name of passphrase prop in config file.
     */
    private static final String PASSPHRASE_PROPERTY_NAME = "passphrase";

    /**
     * Name of PUBLIC key prop in config file.
     */
    private static final String KEY_PROPERTY_NAME = "key";

    /**
     * Name of secret prop in config file.
     */
    private static final String SECRET_PROPERTY_NAME = "secret";

    /**
     * Name of buy fee property in config file.
     */
    private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";

    /**
     * Name of sell fee property in config file.
     */
    private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";

    /**
     * Exchange buy fees in % in {@link BigDecimal} format.
     */
    private BigDecimal buyFeePercentage;

    /**
     * Exchange sell fees in % in {@link BigDecimal} format.
     */
    private BigDecimal sellFeePercentage;

    /**
     * Used to indicate if we have initialised the MAC authentication protocol.
     */
    private boolean initializedMACAuthentication = false;

    /**
     * The passphrase used in the MAC signature.
     */
    private String passphrase = "";

    /**
     * The key used in the MAC message.
     */
    private String key = "";

    /**
     * The secret used for signing MAC message.
     */
    private String secret = "";

    /**
     * Provides the "Message Authentication Code" (MAC) algorithm used for the secure messaging layer.
     * Used to encrypt the hash of the entire message with the private key to ensure message integrity.
     */
    private Mac mac;


    @Override
    public void init(ExchangeConfig config) {

        LogUtils.log(LOG, Level.INFO, () -> "About to initialise GDAX FIX ExchangeConfig: " + config);
        setAuthenticationConfig(config);
        setNetworkConfig(config);
        setOtherConfig(config);

        initSecureMessageLayer();
    }

    // ------------------------------------------------------------------------------------------------
    // GDAX FIX API Calls adapted to the Trading API.
    // See https://docs.gdax.com/#fix-api
    // ------------------------------------------------------------------------------------------------

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws
            TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    /*
     * marketId is not needed for cancelling orders on this exchange.
     */
    @Override
    public boolean cancelOrder(String orderId, String marketIdNotNeeded) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BalanceInfo getBalanceInfo() throws TradingApiException, ExchangeNetworkException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws ExchangeNetworkException, TradingApiException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        // GDAX does not provide API call for fetching % buy fee; it only provides the fee monetary value for a
        // given order via e.g. /orders/<order-id> API call. We load the % fee statically from exchange.xml
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException,
            ExchangeNetworkException {

        // GDAX does not provide API call for fetching % sell fee; it only provides the fee monetary value for a
        // given order via e.g. /orders/<order-id> API call. We load the % fee statically from exchange.xml
        return sellFeePercentage;
    }

    @Override
    public String getImplName() {
        return "GDAX FIX API v1";
    }

    // ------------------------------------------------------------------------------------------------
    //  Transport layer methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Makes a public API call to the GDAX exchange.
     *
     * @param apiMethod the API method to call.
     * @param params any (optional) query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod, Map<String, String> params)
            throws ExchangeNetworkException, TradingApiException {
        throw new UnsupportedOperationException("Not developed yet!");
    }

    /**
     * <p>
     * Makes an authenticated API call to the GDAX exchange.
     * </p>
     *
     * @param httpMethod the HTTP method to use, e.g. GET, POST, DELETE
     * @param apiMethod the API method to call.
     * @param params the query param args to use in the API call.
     * @return the response from the exchange.
     * @throws ExchangeNetworkException if there is a network issue connecting to exchange.
     * @throws TradingApiException if anything unexpected happens.
     */
    private ExchangeHttpResponse sendAuthenticatedRequestToExchange(
            String httpMethod, String apiMethod, Map<String, String> params) throws
            ExchangeNetworkException, TradingApiException {

        if (!initializedMACAuthentication) {
            final String errorMsg = "MAC Message security layer has not been initialized.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        throw new UnsupportedOperationException("Not developed yet!");
    }

    /**
     * Initialises the secure messaging layer
     * Sets up the MAC to safeguard the data we send to the exchange.
     * We fail hard n fast if any of this stuff blows.
     */
    private void initSecureMessageLayer() {

        // Setup the MAC
        try {

            // GDAX secret is in Base64 so we must decode it first.
            final byte[] decodedBase64Secret = DatatypeConverter.parseBase64Binary(secret);

            final SecretKeySpec keyspec = new SecretKeySpec(decodedBase64Secret, "HmacSHA256");
            mac = Mac.getInstance("HmacSHA256");
            mac.init(keyspec);
            initializedMACAuthentication = true;
        } catch (NoSuchAlgorithmException e) {
            final String errorMsg = "Failed to setup MAC security. HINT: Is HMAC-SHA256 installed?";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        } catch (InvalidKeyException e) {
            final String errorMsg = "Failed to setup MAC security. Secret key seems invalid!";
            LOG.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }

    // ------------------------------------------------------------------------------------------------
    //  Config methods
    // ------------------------------------------------------------------------------------------------

    private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {

        final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
        passphrase = getAuthenticationConfigItem(authenticationConfig, PASSPHRASE_PROPERTY_NAME);
        key = getAuthenticationConfigItem(authenticationConfig, KEY_PROPERTY_NAME);
        secret = getAuthenticationConfigItem(authenticationConfig, SECRET_PROPERTY_NAME);
    }

    private void setOtherConfig(ExchangeConfig exchangeConfig) {

        final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

        final String buyFeeInConfig = getOtherConfigItem(otherConfig, BUY_FEE_PROPERTY_NAME);
        buyFeePercentage = new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
        LogUtils.log(LOG, Level.INFO, () -> "Buy fee % in BigDecimal format: " + buyFeePercentage);

        final String sellFeeInConfig = getOtherConfigItem(otherConfig, SELL_FEE_PROPERTY_NAME);
        sellFeePercentage = new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
        LogUtils.log(LOG, Level.INFO, () -> "Sell fee % in BigDecimal format: " + sellFeePercentage);
    }

    // ------------------------------------------------------------------------------------------------
    //  Util methods
    // ------------------------------------------------------------------------------------------------

    /*
     * Hack for unit-testing map params passed to transport layer.
     */
    private Map<String, String> getRequestParamMap() {
        return new HashMap<>();
    }
}