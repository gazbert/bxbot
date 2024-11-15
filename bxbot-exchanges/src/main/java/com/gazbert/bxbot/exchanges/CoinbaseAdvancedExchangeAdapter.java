/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 gazbert, davidhuertas
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
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.util.List;

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
public class CoinbaseAdvancedExchangeAdapter extends AbstractExchangeAdapter
    implements ExchangeAdapter {

  private static final String EXCHANGE_ADAPTER_NAME = "Coinbase Advanced Trade REST API v3";

  @Override
  public void init(ExchangeConfig config) {
    // no impl yet
  }

  @Override
  public String getImplName() {
    return EXCHANGE_ADAPTER_NAME;
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

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

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

  @Override
  public BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException {
    throw new UnsupportedOperationException("TODO: This method not developed yet!");
  }

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
}
