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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This Exchange Adapter decorates a 'real' Exchange Adapter and delegates operations to it.
 * 这个交换适配器装饰了一个“真正的”交换适配器并将操作委托给它。
 *
 * <p>It's purpose is to provide a paper trading/dry run simulation capability against a configured exchange.
 * * <p>其目的是针对已配置的交易所提供纸面交易/试运行模拟功能。
 *
 * <p>The Exchange Adapter to use is configured using the ./config/exchange.yaml otherConfig section.
 * * <p>要使用的 Exchange 适配器是使用 ./config/exchange.yaml otherConfig 部分配置的。
 *
 * <p>Public API calls are delegated to the configured Exchange Adapter.
 * <p>公共 API 调用被委派给配置的 Exchange 适配器。
 *
 * <p>Authenticated API calls to create orders, cancel orders, and fetch open orders are simulated based on actual ticker data from the exchange.
 * <p>用于创建订单、取消订单和获取未结订单的经过验证的 API 调用是根据来自交易所的实际代码数据进行模拟的。
 *
 * <p>Only 1 open order is simulated at any time.
 * <p>任何时候都只模拟 1 个未结订单。
 *
 * @author MarcDahlem
 * @since 1.0
 */
public class TryModeExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME = "simulatedCounterCurrency";
  private static final String COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME =
      "counterCurrencyStartingBalance";
  private static final String SIMULATED_BASE_CURRENCY_PROPERTY_NAME = "simulatedBaseCurrency";
  private static final String BASE_CURRENCY_START_BALANCE_PROPERTY_NAME =
      "baseCurrencyStartingBalance";
  private static final String DELEGATE_ADAPTER_CLASS_PROPERTY_NAME = "delegateAdapter";

  private String simulatedBaseCurrency;
  private String simulatedCounterCurrency;
  private BigDecimal counterCurrencyBalance;
  private String delegateExchangeClassName;

  private ExchangeAdapter delegateExchangeAdapter;

  private OpenOrder currentOpenOrder;
  private BigDecimal baseCurrencyBalance;
  private boolean isOpenOrderCheckReentering;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(
        () -> "About to initialise try-mode adapter with the following exchange config: 即将使用以下交换配置初始化 try-mode 适配器：" + config);
    setOtherConfig(config);
    initializeAdapterDelegation(config);
  }

  @Override
  public String getImplName() {
    return "Try-Mode Test Adapter: configurable exchange public API delegation & simulated orders. Try-Mode 测试适配器：可配置的交换公共 API 委托和模拟订单.";
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    LOG.info(() -> "Delegate 'getMarketOrders' to the configured delegation exchange adapter. 将“getMarketOrders”委托给配置的委托交换适配器。");
    return delegateExchangeAdapter.getMarketOrders(marketId);
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    final List<OpenOrder> result = new LinkedList<>();
    if (currentOpenOrder != null) {
      result.add(currentOpenOrder);
      LOG.info(() -> "getYourOpenOrders: Found an open DUMMY order: getYourOpenOrders: 找到一个打开的 DUMMY 订单：" + currentOpenOrder);
    } else {
      LOG.info(() -> "getYourOpenOrders: no open order found. Return empty order list. getYourOpenOrders：未找到未结订单。返回空订单列表.");
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
          "Can only record/execute one order at a time. Wait for the open order to fulfill. 一次只能记录/执行一个订单。等待未结订单完成。");
    }
    final String newOrderId = "DUMMY_" + orderType + "_ORDER_ID_" + System.currentTimeMillis();
    final Date creationDate = new Date();
    final BigDecimal total = price.multiply(quantity);
    currentOpenOrder =
        new OpenOrderImpl(
            newOrderId, creationDate, marketId, orderType, price, quantity, quantity, total);
    LOG.info(() -> "Created a new dummy order: 创建了一个新的虚拟订单：" + currentOpenOrder);
    checkOpenOrderExecution(marketId);
    return newOrderId;
  }

  @Override
  public boolean cancelOrder(String orderId, String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    if (currentOpenOrder == null) {
      throw new TradingApiException("Tried to cancel a order, but no open order found. 尝试取消订单，但未找到未结订单");
    }
    if (!currentOpenOrder.getId().equals(orderId)) {
      throw new TradingApiException(
          "Tried to cancel a order, but the order id does not match the current open order. 尝试取消订单，但订单 ID 与当前未结订单不匹配。"
              + " Expected: 预期的："
              + currentOpenOrder.getId()
              + ", actual: 实际的："
              + orderId);
    }
    LOG.info(() -> "The following order is canceled: 以下订单被取消：" + currentOpenOrder);
    currentOpenOrder = null;
    return true;
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    LOG.info(
        () -> "Delegate 'getLatestMarketPrice' to the configured delegation exchange adapter. 将“getLatestMarketPrice”委托给配置的委托交换适配器。");
    return delegateExchangeAdapter.getLatestMarketPrice(marketId);
  }

  @Override
  public BalanceInfo getBalanceInfo() {
    final HashMap<String, BigDecimal> availableBalances = new HashMap<>();
    availableBalances.put(simulatedBaseCurrency, baseCurrencyBalance);
    availableBalances.put(simulatedCounterCurrency, counterCurrencyBalance);
    final BalanceInfo currentBalance = new BalanceInfoImpl(availableBalances, new HashMap<>());
    LOG.info(() -> "Return the following simulated balances: 返回以下模拟余额：" + currentBalance);
    return currentBalance;
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    LOG.info(
        () ->
            "Delegate 'getPercentageOfBuyOrderTakenForExchangeFee'to the configured delegation exchange adapter. " +
                    "将“getPercentageOfBuyOrderTakenForExchangeFee”委托给配置的委托交换适配器。");
    return delegateExchangeAdapter.getPercentageOfBuyOrderTakenForExchangeFee(marketId);
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    LOG.info(
        () ->
            "Delegate 'getPercentageOfSellOrderTakenForExchangeFee' to the configured delegation exchange adapter." +
                    "将“getPercentageOfSellOrderTakenForExchangeFee”委托给配置的委托交换适配器。");
    return delegateExchangeAdapter.getPercentageOfSellOrderTakenForExchangeFee(marketId);
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
    checkOpenOrderExecution(marketId);
    LOG.info(() -> "Delegate 'getTicker' to the configured delegation exchange adapter. 将“getTicker”委托给配置的委托交换适配器。");
    return delegateExchangeAdapter.getTicker(marketId);
  }

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    LOG.info(() -> "Load try-mode adapter config... 加载尝试模式适配器配置...");
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    simulatedBaseCurrency = getOtherConfigItem(otherConfig, SIMULATED_BASE_CURRENCY_PROPERTY_NAME);
    LOG.info(() -> "Base currency to be simulated: 要模拟的基础货币：" + simulatedBaseCurrency);

    final String startingBaseBalanceInConfig =
        getOtherConfigItem(otherConfig, BASE_CURRENCY_START_BALANCE_PROPERTY_NAME);
    baseCurrencyBalance = new BigDecimal(startingBaseBalanceInConfig);
    LOG.info(
        () ->
            "Base currency balance at simulation start in BigDecimal format: BigDecimal 格式的模拟开始时的基础货币余额："
                + baseCurrencyBalance);

    simulatedCounterCurrency =
        getOtherConfigItem(otherConfig, SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME);
    LOG.info(() -> "Counter currency to be simulated: 要模拟的柜台货币：" + simulatedCounterCurrency);

    final String startingBalanceInConfig =
        getOtherConfigItem(otherConfig, COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME);
    counterCurrencyBalance = new BigDecimal(startingBalanceInConfig);
    LOG.info(
        () ->
            "Counter currency balance at simulation start in BigDecimal format: BigDecimal 格式的模拟开始时的计数器货币余额："
                + counterCurrencyBalance);

    delegateExchangeClassName =
        getOtherConfigItem(otherConfig, DELEGATE_ADAPTER_CLASS_PROPERTY_NAME);
    LOG.info(
        () ->
            "Delegate exchange adapter to be used for public API calls: 用于公共 API 调用的委托交换适配器："
                + delegateExchangeClassName);
    LOG.info(() -> "Try-mode adapter config successfully loaded. Try-mode 适配器配置已成功加载。");
  }

  private void initializeAdapterDelegation(ExchangeConfig config) {
    LOG.info(
        () -> "Initializing the delegate exchange adapter 初始化委托交换适配器 '" + delegateExchangeClassName + "'...");
    delegateExchangeAdapter = createDelegateExchangeAdapter();
    delegateExchangeAdapter.init(config);
  }

  private ExchangeAdapter createDelegateExchangeAdapter() {
    LOG.info(() -> "Creating the delegate exchange adapter 创建委托交换适配器 '" + delegateExchangeClassName + "'...");
    try {
      final Class componentClass = Class.forName(delegateExchangeClassName);
      final Object rawComponentObject = componentClass.getDeclaredConstructor().newInstance();
      LOG.info(
          () ->
              "Successfully created the delegate exchange adapter class for: 成功地为以下对象创建了委托交换适配器类："
                  + delegateExchangeClassName);
      return (ExchangeAdapter) rawComponentObject;
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException e) {
      final String errorMsg = "Failed to load and create delegate exchange adapter. 未能加载和创建委托交换适配器。";
      LOG.error(errorMsg, e);
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
                "Order type not recognized: 订单类型无法识别：" + currentOpenOrder.getType());
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
      LOG.info(
          "SELL: the market's bid price moved above the limit price --> record sell order execution with the current bid price。 " +
                  "SELL: 市场买入价高于限价 --> 以当前买入价记录卖出订单执行。");
      final BigDecimal orderPrice =
          currentOpenOrder.getOriginalQuantity().multiply(currentBidPrice);
      final BigDecimal buyFees =
          getPercentageOfSellOrderTakenForExchangeFee(marketId).multiply(orderPrice);
      final BigDecimal netOrderPrice = orderPrice.subtract(buyFees);
      counterCurrencyBalance = counterCurrencyBalance.add(netOrderPrice);
      baseCurrencyBalance = baseCurrencyBalance.subtract(currentOpenOrder.getOriginalQuantity());
      currentOpenOrder = null;
    }
  }

  private void checkOpenBuyOrderExecution(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    final BigDecimal currentAskPrice = getTicker(marketId).getAsk();
    if (currentAskPrice.compareTo(currentOpenOrder.getPrice()) <= 0) {
      LOG.info(
          "BUY: the market's current ask price moved below the limit price --> record buy order execution with the current ask price。 " +
                  "BUY: 市场当前的卖价低于限价--> 以当前卖价记录买单执行。");
      final BigDecimal orderPrice =
          currentOpenOrder.getOriginalQuantity().multiply(currentAskPrice);
      final BigDecimal buyFees =
          getPercentageOfBuyOrderTakenForExchangeFee(marketId).multiply(orderPrice);
      final BigDecimal netOrderPrice = orderPrice.add(buyFees);
      counterCurrencyBalance = counterCurrencyBalance.subtract(netOrderPrice);
      baseCurrencyBalance = baseCurrencyBalance.add(currentOpenOrder.getOriginalQuantity());
      currentOpenOrder = null;
    }
  }
}
