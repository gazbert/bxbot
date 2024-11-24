/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 gazbert, David Huertas
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

import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * Exchange Adapter for integrating with the Coinbase Advanced Trade exchange. The Coinbase API is
 * documented <a href="https://docs.cdp.coinbase.com/advanced-trade/docs/welcome">here</a>.
 *
 * <p>This adapter only supports the Coinbase Advanced Trade <a
 * href="https://docs.cdp.coinbase.com/advanced-trade/docs/api-overview">REST API</a>. The design of
 * the API and documentation is excellent.
 *
 * @author gazbert, davidhuertas
 * @since 1.0
 */
@Log4j2
public class CoinbaseExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final String EXCHANGE_ADAPTER_NAME = "Coinbase Advanced Trade REST API v3";

  private static final String PUBLIC_API_BASE_URL = "https://api.coinbase.com/api/v3/brokerage";

  private static final String PRODUCT_BOOK = "/market/product_book";
  private static final String PRODUCT_ID_PARAM = "product_id";
  private static final String PRODUCT_BOOK_LIMIT_PARAM = "limit";
  private static final int PRODUCT_BOOK_LIMIT_VALUE = 100;

  private static final String UNEXPECTED_ERROR_MSG =
      "Unexpected error has occurred in Coinbase Advanced Trade Exchange Adapter. ";
  private static final String UNEXPECTED_IO_ERROR_MSG =
      "Failed to connect to Exchange due to unexpected IO error.";

  private static final String PASSPHRASE_PROPERTY_NAME = "passphrase";
  private static final String KEY_PROPERTY_NAME = "key";
  private static final String SECRET_PROPERTY_NAME = "secret";

  private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
  private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";
  private static final String SERVER_TIME_BIAS_PROPERTY_NAME = "time-server-bias";

  private BigDecimal buyFeePercentage;
  private BigDecimal sellFeePercentage;
  private Long timeServerBias;

  private Gson gson;

  /** Constructs the Exchange Adapter. */
  public CoinbaseExchangeAdapter() {
    // No extra init.
  }

  @Override
  public void init(ExchangeConfig config) {
    log.info("About to initialise Coinbase Advanced Trade ExchangeConfig: {}", config);
    setNetworkConfig(config);
    setOtherConfig(config);
    initGson();
  }

  @Override
  public String getImplName() {
    return EXCHANGE_ADAPTER_NAME;
  }

  // --------------------------------------------------------------------------
  // Public API calls
  // --------------------------------------------------------------------------

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws TradingApiException, ExchangeNetworkException {

    try {
      final Map<String, String> params = createRequestParamMap();
      params.put(PRODUCT_ID_PARAM, marketId);
      params.put(PRODUCT_BOOK_LIMIT_PARAM, String.valueOf(PRODUCT_BOOK_LIMIT_VALUE));

      final ExchangeHttpResponse response = sendPublicRequestToExchange(PRODUCT_BOOK, params);

      log.info("Market Orders response: {}", response);

      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
        final CoinbaseAdvancedTradeProductBookWrapper productBookWrapper =
            gson.fromJson(response.getPayload(), CoinbaseAdvancedTradeProductBookWrapper.class);

        final List<MarketOrder> buyOrders = new ArrayList<>();
        for (CoinbaseAdvancedTradePriceBookOrder coinbaseProBuyOrder :
            productBookWrapper.pricebook.bids) {
          final MarketOrder buyOrder =
              new MarketOrderImpl(
                  OrderType.BUY,
                  coinbaseProBuyOrder.price,
                  coinbaseProBuyOrder.size,
                  coinbaseProBuyOrder.price.multiply(coinbaseProBuyOrder.size));
          buyOrders.add(buyOrder);
        }

        final List<MarketOrder> sellOrders = new ArrayList<>();
        for (CoinbaseAdvancedTradePriceBookOrder coinbaseProSellOrder :
            productBookWrapper.pricebook.asks) {
          final MarketOrder sellOrder =
              new MarketOrderImpl(
                  OrderType.SELL,
                  coinbaseProSellOrder.price,
                  coinbaseProSellOrder.size,
                  coinbaseProSellOrder.price.multiply(coinbaseProSellOrder.size));
          sellOrders.add(sellOrder);
        }
        return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);

      } else {
        final String errorMsg =
            "Failed to get market order book from exchange. Details: " + response;
        log.error(errorMsg);
        throw new TradingApiException(errorMsg);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;

    } catch (Exception e) {
      log.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  @Override
  public BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  // --------------------------------------------------------------------------
  // Private API calls
  // --------------------------------------------------------------------------

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  @Override
  public boolean cancelOrder(String orderId, String marketId)
      throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  // --------------------------------------------------------------------------
  // Non exchange visiting calls
  // --------------------------------------------------------------------------

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  // --------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  // --------------------------------------------------------------------------

  /**
   * GSON class for Coinbase Advanced Trade '/market/product_book?product_id={trading-pair}' Product
   * Book API response.
   */
  @ToString
  private static class CoinbaseAdvancedTradeProductBookWrapper {
    CoinbaseAdvancedTradePriceBook pricebook;
    BigDecimal last;

    @SerializedName("mid_market")
    BigDecimal midMarket;

    @SerializedName("spread_bps")
    BigDecimal spreadBps;

    @SerializedName("spread_absolute")
    BigDecimal spreadAbsolute;
  }

  /** GSON class for Coinbase Advanced Trade Price Book. */
  @ToString
  private static class CoinbaseAdvancedTradePriceBook {
    @SerializedName("product_id")
    String productId;

    List<CoinbaseAdvancedTradePriceBookOrder> bids;
    List<CoinbaseAdvancedTradePriceBookOrder> asks;
    String time;
  }

  /** GSON class for Coinbase Advanced Trade Price Book Order. */
  @ToString
  private static class CoinbaseAdvancedTradePriceBookOrder {
    BigDecimal price;
    BigDecimal size;
  }

  // --------------------------------------------------------------------------
  //  Transport layer methods
  // --------------------------------------------------------------------------

  private ExchangeHttpResponse sendPublicRequestToExchange(
      String apiMethod, Map<String, String> params)
      throws ExchangeNetworkException, TradingApiException {

    if (params == null) {
      params = createRequestParamMap(); // no params, so empty query string
    }

    // Request headers required by Exchange
    final Map<String, String> requestHeaders = new HashMap<>();

    try {

      final StringBuilder queryString = new StringBuilder();
      if (!params.isEmpty()) {

        queryString.append("?");

        for (final Map.Entry<String, String> param : params.entrySet()) {
          if (queryString.length() > 1) {
            queryString.append("&");
          }
          queryString.append(param.getKey());
          queryString.append("=");
          queryString.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }

        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
      }

      final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod + queryString);
      return makeNetworkRequest(url, "GET", null, requestHeaders);

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      log.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  // --------------------------------------------------------------------------
  //  Config methods
  // --------------------------------------------------------------------------

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    final String buyFeeInConfig = getOtherConfigItem(otherConfig, BUY_FEE_PROPERTY_NAME);
    buyFeePercentage =
        new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    log.info("Buy fee % in BigDecimal format: {}", buyFeePercentage);

    final String sellFeeInConfig = getOtherConfigItem(otherConfig, SELL_FEE_PROPERTY_NAME);
    sellFeePercentage =
        new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    log.info("Sell fee % in BigDecimal format: {}", sellFeePercentage);

    final String serverTimeBiasInConfig =
        getOtherConfigItem(otherConfig, SERVER_TIME_BIAS_PROPERTY_NAME);
    timeServerBias = Long.parseLong(serverTimeBiasInConfig);
    log.info("Time server bias in long format: {}", timeServerBias);
  }

  // --------------------------------------------------------------------------
  //  Util methods
  // --------------------------------------------------------------------------

  private void initGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gson = gsonBuilder.create();
  }

  /*
   * Hack for unit-testing request params passed to transport layer.
   */
  private Map<String, String> createRequestParamMap() {
    return new HashMap<>();
  }

  /*
   * Hack for unit-testing transport layer.
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders)
      throws TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
