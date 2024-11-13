/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Marc Dahlem, gazbert
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
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.Ticker;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;

/**
 * This Exchange Adapter decorates a 'real' Exchange Adapter and delegates operations to it.
 *
 * <p>It's purpose is to provide a paper trading/dry run simulation capability against a configured
 * exchange.
 *
 * <p>The Exchange Adapter to use is configured using the ./config/exchange.yaml otherConfig
 * section.
 *
 * <p>Public API calls are delegated to the configured Exchange Adapter.
 *
 * <p>Authenticated API calls to create orders, cancel orders, and fetch open orders are simulated
 * based on actual ticker data from the exchange.
 *
 * <p>Only 1 open order is simulated at any time.
 *
 * @author MarcDahlem
 * @since 1.0
 */
@Log4j2
public class TryModeExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

  private static final String SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME = "simulatedCounterCurrency";
  private static final String SIMULATED_COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME =
      "simulatedCounterCurrencyStartingBalance";

  private static final String SIMULATED_BASE_CURRENCY_PROPERTY_NAME = "simulatedBaseCurrency";
  private static final String SIMULATED_BASE_CURRENCY_START_BALANCE_PROPERTY_NAME =
      "simulatedBaseCurrencyStartingBalance";

  private static final String SIMULATED_SELL_FEE_PROPERTY_NAME = "simulatedSellFee";
  private static final String SIMULATED_BUY_FEE_PROPERTY_NAME = "simulatedBuyFee";

  private static final String DELEGATE_ADAPTER_CLASS_PROPERTY_NAME = "delegateAdapter";

  private String simulatedBaseCurrency;
  private BigDecimal simulatedBaseCurrencyBalance;

  private String simulatedCounterCurrency;
  private BigDecimal simulatedCounterCurrencyBalance;

  private BigDecimal simulatedSellFee;
  private BigDecimal simulatedBuyFee;

  private String delegateExchangeClassName;

  private ExchangeAdapter delegateExchangeAdapter;

  private OpenOrder currentOpenOrder;
  private boolean isOpenOrderCheckReentering;

  /** Constructs the Exchange Adapter. */
  public TryModeExchangeAdapter() {
    // No extra init.
  }

  @Override
  public void init(ExchangeConfig config) {
    log.info("About to initialise try-mode adapter with the following exchange config: {}", config);
    setOtherConfig(config);
    initializeAdapterDelegation(config);
  }

  @Override
  public String getImplName() {
    return "Try-Mode Test Adapter (configurable exchange public API delegation & simulated orders)";
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    log.info("Delegate 'getMarketOrders' to the configured delegation exchange adapter.");
    return delegateExchangeAdapter.getMarketOrders(marketId);
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    final List<OpenOrder> result = new LinkedList<>();
    if (currentOpenOrder != null) {
      result.add(currentOpenOrder);
      log.info("getYourOpenOrders: Found an open DUMMY order: {}", currentOpenOrder);
    } else {
      log.info("getYourOpenOrders: no open order found. Return empty order list");
    }
    return result;
  }

  @Override
  public String createOrder(
      String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    if (currentOpenOrder != null) {
      throw new TradingApiException(
          "Can only record/execute one order at a time. Wait for the open order to fulfill");
    }
    final String newOrderId = "DUMMY_" + orderType + "_ORDER_ID_" + System.currentTimeMillis();
    final Date creationDate = new Date();
    final BigDecimal total = price.multiply(quantity);
    currentOpenOrder =
        new OpenOrderImpl(
            newOrderId, creationDate, marketId, orderType, price, quantity, quantity, total);
    log.info("Created a new dummy order: {}", currentOpenOrder);
    checkOpenOrderExecution(marketId);
    return newOrderId;
  }

  @Override
  public boolean cancelOrder(String orderId, String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    if (currentOpenOrder == null) {
      throw new TradingApiException("Tried to cancel a order, but no open order found");
    }
    if (!currentOpenOrder.getId().equals(orderId)) {
      throw new TradingApiException(
          "Tried to cancel a order, but the order id does not match the current open order."
              + " Expected: "
              + currentOpenOrder.getId()
              + ", actual: "
              + orderId);
    }
    log.info("The following order is canceled: {}", currentOpenOrder);
    currentOpenOrder = null;
    return true;
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    log.info("Delegate 'getLatestMarketPrice' to the configured delegation exchange adapter.");
    return delegateExchangeAdapter.getLatestMarketPrice(marketId);
  }

  @Override
  public BalanceInfo getBalanceInfo() {
    final HashMap<String, BigDecimal> availableBalances = new HashMap<>();
    availableBalances.put(simulatedBaseCurrency, simulatedBaseCurrencyBalance);
    availableBalances.put(simulatedCounterCurrency, simulatedCounterCurrencyBalance);
    final BalanceInfo currentBalance = new BalanceInfoImpl(availableBalances, new HashMap<>());
    log.info("Return the following simulated balances: {}", currentBalance);
    return currentBalance;
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return simulatedBuyFee;
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return simulatedSellFee;
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
    checkOpenOrderExecution(marketId);
    log.info("Delegate 'getTicker' to the configured delegation exchange adapter.");
    return delegateExchangeAdapter.getTicker(marketId);
  }

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    log.info("Loading try-mode adapter config...");
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    simulatedBaseCurrency = getOtherConfigItem(otherConfig, SIMULATED_BASE_CURRENCY_PROPERTY_NAME);
    log.info("Base currency to be simulated: {}", simulatedBaseCurrency);

    final String startingBaseBalanceInConfig =
        getOtherConfigItem(otherConfig, SIMULATED_BASE_CURRENCY_START_BALANCE_PROPERTY_NAME);
    simulatedBaseCurrencyBalance = new BigDecimal(startingBaseBalanceInConfig);
    log.info(
        "Base currency balance at simulation start in BigDecimal format: {}",
        simulatedBaseCurrencyBalance);

    simulatedCounterCurrency =
        getOtherConfigItem(otherConfig, SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME);
    log.info("Counter currency to be simulated: {}", simulatedCounterCurrency);

    final String startingBalanceInConfig =
        getOtherConfigItem(otherConfig, SIMULATED_COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME);
    simulatedCounterCurrencyBalance = new BigDecimal(startingBalanceInConfig);
    log.info(
        "Counter currency balance at simulation start in BigDecimal format: {}",
        simulatedCounterCurrencyBalance);

    final String sellFeeInConfig =
        getOtherConfigItem(otherConfig, SIMULATED_SELL_FEE_PROPERTY_NAME);
    simulatedSellFee = new BigDecimal(sellFeeInConfig);
    log.info("Sell Fee at simulation start in BigDecimal format: {}", simulatedSellFee);

    final String buyFeeInConfig = getOtherConfigItem(otherConfig, SIMULATED_BUY_FEE_PROPERTY_NAME);
    simulatedBuyFee = new BigDecimal(buyFeeInConfig);
    log.info("Buy Fee at simulation start in BigDecimal format: {}", simulatedBuyFee);

    delegateExchangeClassName =
        getOtherConfigItem(otherConfig, DELEGATE_ADAPTER_CLASS_PROPERTY_NAME);
    log.info(
        "Delegate exchange adapter to be used for public API calls: {}", delegateExchangeClassName);
    log.info("Try-mode adapter config successfully loaded.");
  }

  private void initializeAdapterDelegation(ExchangeConfig config) {
    delegateExchangeAdapter = createDelegateExchangeAdapter();
    delegateExchangeAdapter.init(config);
  }

  private ExchangeAdapter createDelegateExchangeAdapter() {
    log.info("Creating the delegate exchange adapter: {}...", delegateExchangeClassName);
    try {
      final Class<?> componentClass = Class.forName(delegateExchangeClassName);
      final Object rawComponentObject = componentClass.getDeclaredConstructor().newInstance();
      log.info(
          "Successfully created the delegate exchange adapter class for: {}",
          delegateExchangeClassName);
      return (ExchangeAdapter) rawComponentObject;
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException e) {
      final String errorMsg = "Failed to load and create delegate exchange adapter.";
      log.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    }
  }

  private void checkOpenOrderExecution(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    if (isOpenOrderCheckReentering) {
      return;
    }
    isOpenOrderCheckReentering = true;
    try {
      if (currentOpenOrder != null) {
        switch (currentOpenOrder.getType()) {
          case BUY:
            checkOpenBuyOrderExecution(marketId);
            break;
          case SELL:
            checkOpenSellOrderExecution(marketId);
            break;
          default:
            throw new TradingApiException(
                "Order type not recognized: " + currentOpenOrder.getType());
        }
      }
    } finally {
      isOpenOrderCheckReentering = false;
    }
  }

  private void checkOpenSellOrderExecution(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    final BigDecimal currentBidPrice = getTicker(marketId).getBid();
    if (currentBidPrice.compareTo(currentOpenOrder.getPrice()) >= 0) {
      log.info(
          "SELL: the market's bid price moved above the limit price "
              + "--> record sell order execution with the current bid price");
      final BigDecimal orderPrice =
          currentOpenOrder.getOriginalQuantity().multiply(currentBidPrice);
      final BigDecimal buyFees =
          getPercentageOfSellOrderTakenForExchangeFee(marketId).multiply(orderPrice);
      final BigDecimal netOrderPrice = orderPrice.subtract(buyFees);
      simulatedCounterCurrencyBalance = simulatedCounterCurrencyBalance.add(netOrderPrice);
      simulatedBaseCurrencyBalance =
          simulatedBaseCurrencyBalance.subtract(currentOpenOrder.getOriginalQuantity());
      currentOpenOrder = null;
    }
  }

  private void checkOpenBuyOrderExecution(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    final BigDecimal currentAskPrice = getTicker(marketId).getAsk();
    if (currentAskPrice.compareTo(currentOpenOrder.getPrice()) <= 0) {
      log.info(
          "BUY: the market's current ask price moved below the limit price "
              + "--> record buy order execution with the current ask price");
      final BigDecimal orderPrice =
          currentOpenOrder.getOriginalQuantity().multiply(currentAskPrice);
      final BigDecimal buyFees =
          getPercentageOfBuyOrderTakenForExchangeFee(marketId).multiply(orderPrice);
      final BigDecimal netOrderPrice = orderPrice.add(buyFees);
      simulatedCounterCurrencyBalance = simulatedCounterCurrencyBalance.subtract(netOrderPrice);
      simulatedBaseCurrencyBalance =
          simulatedBaseCurrencyBalance.add(currentOpenOrder.getOriginalQuantity());
      currentOpenOrder = null;
    }
  }
}
