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

package com.gazbert.bxbot.trading.api;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * BX-bot's Trading API.
 *
 * <p>This is what Trading Strategies use to trade.
 *
 * <p>Exchange Adapters provide their own implementation of the API for the exchange they wish to
 * integrate with.
 *
 * <p>This version of the Trading API only supports <a
 * href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> traded at the <a
 * href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>. It does not support
 * futures or margin trading.
 *
 * @author gazbert
 * @since 1.0
 */
public interface TradingApi {

  /**
   * Returns the current version of the API.
   *
   * @return the API version.
   * @since 1.0
   */
  default String getVersion() {
    return "1.2";
  }

  /**
   * Returns the API implementation name.
   *
   * @return the API implementation name.
   * @since 1.0
   */
  String getImplName();

  /**
   * Fetches latest <em>market</em> orders for a given market.
   *
   * @param marketId the id of the market.
   * @return the market order book.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  MarketOrderBook getMarketOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Fetches <em>your</em> current open orders, i.e. the orders placed by the bot.
   *
   * @param marketId the id of the market.
   * @return your current open orders.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  List<OpenOrder> getYourOpenOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Places an order on the exchange.
   *
   * @param marketId the id of the market.
   * @param orderType Value must be {@link OrderType#BUY} or {@link OrderType#SELL}.
   * @param quantity amount of units you are buying/selling in this order.
   * @param price the price per unit you are buying/selling at.
   * @return the id of the order.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Cancels your existing order on the exchange.
   *
   * @param orderId your order Id.
   * @param marketId the id of the market the order was placed on, e.g. btc_usd
   * @return true if order cancelled ok, false otherwise.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  boolean cancelOrder(String orderId, String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Fetches the latest price for a given market. This is usually in BTC for altcoin markets and USD
   * for BTC/USD markets - see the Exchange Adapter documentation.
   *
   * @param marketId the id of the market.
   * @return the latest market price.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException;

  /**
   * Fetches the balance of your wallets on the exchange.
   *
   * @return your wallet balance info.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException;

  /**
   * Returns the exchange BUY order fee for a given market id. The returned value is the % of the
   * BUY order that the exchange uses to calculate its fee as a {@link BigDecimal}. If the fee is
   * 0.33%, then the {@link BigDecimal} value returned is 0.0033.
   *
   * @param marketId the id of the market.
   * @return the % of the BUY order that the exchange uses to calculate its fee as a {@link
   *     BigDecimal}.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException;

  /**
   * Returns the exchange SELL order fee for a given market id. The returned value is the % of the
   * SELL order that the exchange uses to calculate its fee as a {@link BigDecimal}. If the fee is
   * 0.33%, then the {@link BigDecimal} value returned is 0.0033.
   *
   * @param marketId the id of the market.
   * @return the % of the SELL order that the exchange uses to calculate its fee as a {@link
   *     BigDecimal}.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.0
   */
  BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException;

  /**
   * Returns the exchange Ticker a given market id.
   *
   * <p>Not all exchanges provide the information returned in the Ticker methods - you'll need to
   * check the relevant Exchange Adapter code/Javadoc and online Exchange API documentation.
   *
   * <p>If the exchange does not provide the information, a null value is returned.
   *
   * @param marketId the id of the market.
   * @return the exchange Ticker for a given market.
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.1
   */
  default Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {

    return new Ticker() {
      @Override
      public BigDecimal getLast() {
        return null;
      }

      @Override
      public BigDecimal getBid() {
        return null;
      }

      @Override
      public BigDecimal getAsk() {
        return null;
      }

      @Override
      public BigDecimal getLow() {
        return null;
      }

      @Override
      public BigDecimal getHigh() {
        return null;
      }

      @Override
      public BigDecimal getOpen() {
        return null;
      }

      @Override
      public BigDecimal getVolume() {
        return null;
      }

      @Override
      public BigDecimal getVwap() {
        return null;
      }

      @Override
      public Long getTimestamp() {
        return null;
      }
    };
  }

  /**
   * Returns the exchange OHLC data for a given market id and the prefredred interval.
   *
   * <p>Not all exchanges provide the information returned in the OHLC method or requested interval
   * - you'll need to check the relevant Exchange Adapter code/Javadoc and online Exchange API
   * documentation.
   *
   * <p>If the exchange does not provide the information, a enmoty result value is returned.
   *
   * @param marketId the id of the market.
   * @param interval the interval in which the OHLC data should be requested
   * @return the OHLC answer as a sorted list of the exchange OHLC data for a given market,
   *     seperated into the requested interval. And an resume id, if subseqeutn data is to be
   *     requested. Not all exchanges support this
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.2
   */
  default Ohlc getOhlc(String marketId, OhlcInterval interval)
      throws TradingApiException, ExchangeNetworkException {

    return new Ohlc() {
      @Override
      public Integer getResumeID() {
        return null;
      }

      @Override
      public List<OhlcFrame> getFrames() {
        return Collections.emptyList();
      }
    };
  }

  /**
   * Returns the exchange OHLC data for a given market id in the prefredred interval. The market
   * data queried is limitited by the resumeID, which must be catched first with an initial OHLC
   * call.
   *
   * <p>Not all exchanges provide the information returned in the OHLC method or requested interval
   * - you'll need to check the relevant Exchange Adapter code/Javadoc and online Exchange API
   * documentation.
   *
   * <p>If the exchange does not provide the information, a enmoty result value is returned.
   *
   * @param marketId the id of the market.
   * @param interval the interval in which the OHLC data should be requested
   * @param resumeID the ID from which the OHLC data retrieval should be resumed
   * @return the OHLC answer as a sorted list of the exchange OHLC data for a given market,
   *     seperated into the requested interval. And an resume id, if subseqeutn data is to be
   *     requested. Not all exchanges support this
   * @throws ExchangeNetworkException if a network error occurred trying to connect to the exchange.
   *     This is implementation specific for each Exchange Adapter - see the documentation for the
   *     adapter you are using. You could retry the API call, or exit from your Trading Strategy and
   *     let the Trading Engine execute your Trading Strategy at the next trade cycle.
   * @throws TradingApiException if the API call failed for any reason other than a network error.
   *     This means something bad as happened; you would probably want to wrap this exception in a
   *     StrategyException and let the Trading Engine shutdown the bot immediately to prevent
   *     unexpected losses.
   * @since 1.2
   */
  default Ohlc getOhlc(String marketId, OhlcInterval interval, Integer resumeID)
      throws TradingApiException, ExchangeNetworkException {

    return new Ohlc() {
      @Override
      public Integer getResumeID() {
        return null;
      }

      @Override
      public List<OhlcFrame> getFrames() {
        return Collections.emptyList();
      }
    };
  }
}
