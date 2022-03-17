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

  private ExchangeAdapter delegateExchange;

  private OpenOrder currentOpenOrder;
  private BigDecimal baseCurrencyBalance;
  private boolean isOpenOrderCheckReentering;

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(
        () -> "About to initialise try-mode adapter with the following exchange config: " + config);
    setOtherConfig(config);
    initializeAdapterDelegation(config);
  }

  @Override
  public String getImplName() {
    return "Try-mode test adapter with public API delegation";
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    LOG.info(() -> "Delegate 'getMarketOrders' to the configured delegation exchange adapter.");
    return delegateExchange.getMarketOrders(marketId);
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    LinkedList<OpenOrder> result = new LinkedList<>();
    if (currentOpenOrder != null) {
      result.add(currentOpenOrder);
      LOG.info(() -> "getYourOpenOrders: Found an open DUMMY order: " + currentOpenOrder);
    } else {
      LOG.info(() -> "getYourOpenOrders: no open order found. Return empty order list");
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
    String newOrderID = "DUMMY_" + orderType + "_ORDER_ID_" + System.currentTimeMillis();
    Date creationDate = new Date();
    BigDecimal total = price.multiply(quantity);
    currentOpenOrder =
        new OpenOrderImpl(
            newOrderID, creationDate, marketId, orderType, price, quantity, quantity, total);
    LOG.info(() -> "Created a new dummy order: " + currentOpenOrder);
    checkOpenOrderExecution(marketId);
    return newOrderID;
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
              + " Expected: " + currentOpenOrder.getId()
              + ", actual: "
              + orderId);
    }
    LOG.info(() -> "The following order is canceled: " + currentOpenOrder);
    currentOpenOrder = null;
    return true;
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId)
      throws ExchangeNetworkException, TradingApiException {
    checkOpenOrderExecution(marketId);
    LOG.info(
        () -> "Delegate 'getLatestMarketPrice' to the configured delegation exchange adapter.");
    return delegateExchange.getLatestMarketPrice(marketId);
  }

  @Override
  public BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException {
    HashMap<String, BigDecimal> availableBalances = new HashMap<>();
    availableBalances.put(simulatedBaseCurrency, baseCurrencyBalance);
    availableBalances.put(simulatedCounterCurrency, counterCurrencyBalance);
    BalanceInfoImpl currentBalance = new BalanceInfoImpl(availableBalances, new HashMap<>());
    LOG.info(() -> "Return the following simulated balances: " + currentBalance);
    return currentBalance;
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    LOG.info(
        () ->
            "Delegate 'getPercentageOfBuyOrderTakenForExchangeFee'"
              + "to the configured delegation exchange adapter.");
    return delegateExchange.getPercentageOfBuyOrderTakenForExchangeFee(marketId);
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    LOG.info(
        () ->
            "Delegate 'getPercentageOfSellOrderTakenForExchangeFee'"
                    + "to the configured delegation exchange adapter.");
    return delegateExchange.getPercentageOfSellOrderTakenForExchangeFee(marketId);
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
    checkOpenOrderExecution(marketId);
    LOG.info(() -> "Delegate 'getTicker' to the configured delegation exchange adapter.");
    return delegateExchange.getTicker(marketId);
  }

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    LOG.info(() -> "Load try-mode adapter config...");
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    simulatedBaseCurrency = getOtherConfigItem(otherConfig, SIMULATED_BASE_CURRENCY_PROPERTY_NAME);
    LOG.info(() -> "Base currency to be simulated:" + simulatedBaseCurrency);

    final String startingBaseBalanceInConfig =
        getOtherConfigItem(otherConfig, BASE_CURRENCY_START_BALANCE_PROPERTY_NAME);
    baseCurrencyBalance = new BigDecimal(startingBaseBalanceInConfig);
    LOG.info(
        () ->
            "Base currency balance at simulation start in BigDecimal format: "
                + baseCurrencyBalance);

    simulatedCounterCurrency =
        getOtherConfigItem(otherConfig, SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME);
    LOG.info(() -> "Counter currency to be simulated:" + simulatedCounterCurrency);

    final String startingBalanceInConfig =
        getOtherConfigItem(otherConfig, COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME);
    counterCurrencyBalance = new BigDecimal(startingBalanceInConfig);
    LOG.info(
        () ->
            "Counter currency balance at simulation start in BigDecimal format: "
                + counterCurrencyBalance);

    delegateExchangeClassName =
        getOtherConfigItem(otherConfig, DELEGATE_ADAPTER_CLASS_PROPERTY_NAME);
    LOG.info(
        () ->
            "Delegate exchange adapter to be used for public API calls:"
                + delegateExchangeClassName);
    LOG.info(() -> "Try-mode adapter config successfully loaded.");
  }

  private void initializeAdapterDelegation(ExchangeConfig config) {
    LOG.info(
        () -> "Initializing the delegate exchange adapter '" + delegateExchangeClassName + "'...");
    try {
      final Class componentClass = Class.forName(delegateExchangeClassName);
      final Object rawComponentObject = componentClass.getDeclaredConstructor().newInstance();
      LOG.info(
          () ->
              "Successfully created the delegate exchange adapter class for: "
                  + delegateExchangeClassName);
      ExchangeAdapter loadedExchange = (ExchangeAdapter) rawComponentObject;
      loadedExchange.init(config);
      this.delegateExchange = loadedExchange;
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException e) {
      final String errorMsg = "Failed to load and initialise delegate exchange adapter.";
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
                "Order type not recognized: " + currentOpenOrder.getType());
        }
      }
    } finally {
      isOpenOrderCheckReentering = false;
    }
  }

  private void checkOpenSellOrderExecution(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    BigDecimal currentBidPrice = getTicker(marketId).getBid();
    if (currentBidPrice.compareTo(currentOpenOrder.getPrice()) >= 0) {
      LOG.info(
          "SELL: the market's bid price moved above the limit price "
                  + "--> record sell order execution with the current bid price");
      BigDecimal orderPrice = currentOpenOrder.getOriginalQuantity().multiply(currentBidPrice);
      BigDecimal buyFees =
          getPercentageOfSellOrderTakenForExchangeFee(marketId).multiply(orderPrice);
      BigDecimal netOrderPrice = orderPrice.subtract(buyFees);
      counterCurrencyBalance = counterCurrencyBalance.add(netOrderPrice);
      baseCurrencyBalance = baseCurrencyBalance.subtract(currentOpenOrder.getOriginalQuantity());
      currentOpenOrder = null;
    }
  }

  private void checkOpenBuyOrderExecution(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    BigDecimal currentAskPrice = getTicker(marketId).getAsk();
    if (currentAskPrice.compareTo(currentOpenOrder.getPrice()) <= 0) {
      LOG.info(
          "BUY: the market's current ask price moved below the limit price "
                  + "--> record buy order execution with the current ask price");
      BigDecimal orderPrice = currentOpenOrder.getOriginalQuantity().multiply(currentAskPrice);
      BigDecimal buyFees =
          getPercentageOfBuyOrderTakenForExchangeFee(marketId).multiply(orderPrice);
      BigDecimal netOrderPrice = orderPrice.add(buyFees);
      counterCurrencyBalance = counterCurrencyBalance.subtract(netOrderPrice);
      baseCurrencyBalance = baseCurrencyBalance.add(currentOpenOrder.getOriginalQuantity());
      currentOpenOrder = null;
    }
  }
}
