package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.trading.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TryModeExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {
    private static final Logger LOG = LogManager.getLogger();

    private static final String SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME = "simulatedCounterCurrency";
    private static final String COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME = "counterCurrencyStartingBalance";
    private static final String SIMULATED_BASE_CURRENCY_PROPERTY_NAME = "simulatedBaseCurrency";

    private String simulatedBaseCurrency;
    private String simulatedCounterCurrency;
    private BigDecimal counterCurrencyBalance;

    private OpenOrder currentOpenOrder;
    private BigDecimal baseCurrencyBalance = BigDecimal.ZERO;

    @Override
    public void init(ExchangeConfig config) {
        LOG.info(() -> "About to initialise try-mode adapter with the following exchange config: " + config);
        setOtherConfig(config);
    }

    @Override
    public String getImplName() {
        return "Try-mode test adapter: does not place orders, but keeps track of them in Dummys";
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws ExchangeNetworkException, TradingApiException {
        // TODO
        return null;
    }

    @Override
    public List<OpenOrder> getYourOpenOrders(String marketId) throws ExchangeNetworkException, TradingApiException {
        LinkedList<OpenOrder> result = new LinkedList<>();
        if (currentOpenOrder != null) {
            result.add(currentOpenOrder);
        }
        return result;
    }

    @Override
    public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price) throws ExchangeNetworkException, TradingApiException {
        if (currentOpenOrder != null) {
            throw new TradingApiException("Can only record/execute one order at a time. Wait for the open order to fulfill");
        }
        String newOrderID = "DUMMY_" + orderType + "_ORDER_ID_" + System.currentTimeMillis();
        Date creationDate = new Date();
        BigDecimal total = price.multiply(quantity);
        currentOpenOrder = new OpenOrderImpl(newOrderID, creationDate, marketId, orderType, price, quantity, quantity, total);
        checkOpenOrderExecution(marketId);
        return newOrderID;
    }

    @Override
    public boolean cancelOrder(String orderId, String marketId) throws ExchangeNetworkException, TradingApiException {
        if (currentOpenOrder == null) {
            throw new TradingApiException("Tried to cancel a order, but no open order found");
        }
        if (!currentOpenOrder.getId().equals(orderId)) {
            throw new TradingApiException("Tried to cancel a order, but the order id does not match the current open order. Expected: " + currentOpenOrder.getId() + ", actual: " + orderId);
        }
        currentOpenOrder = null;
        return true;
    }

    @Override
    public BigDecimal getLatestMarketPrice(String marketId) throws ExchangeNetworkException, TradingApiException {
        // TODO
        return null;
    }

    @Override
    public BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException {
        HashMap<String, BigDecimal> availableBalances = new HashMap<>();
        availableBalances.put(simulatedBaseCurrency, baseCurrencyBalance);
        availableBalances.put(simulatedCounterCurrency, counterCurrencyBalance);
        return new BalanceInfoImpl(availableBalances, new HashMap<>());
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException, ExchangeNetworkException {
        return null;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException, ExchangeNetworkException {
        return null;
    }

    @Override
    public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
        // TODO where to put this check? checkOpenOrderExecution(marketId);
        // TODO
        return null;
    }

    private void setOtherConfig(ExchangeConfig exchangeConfig) {
        final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

        simulatedBaseCurrency = getOtherConfigItem(otherConfig, SIMULATED_BASE_CURRENCY_PROPERTY_NAME);
        LOG.info(() -> "Base currency to be simulated:" + simulatedBaseCurrency);

        simulatedCounterCurrency = getOtherConfigItem(otherConfig, SIMULATED_COUNTER_CURRENCY_PROPERTY_NAME);
        LOG.info(() -> "Counter currency to be simulated:" + simulatedCounterCurrency);

        final String startingBalanceInConfig = getOtherConfigItem(otherConfig, COUNTER_CURRENCY_START_BALANCE_PROPERTY_NAME);
        counterCurrencyBalance = new BigDecimal(startingBalanceInConfig);
        LOG.info(() -> "Counter currency balance at simulation start in BigDecimal format: " + counterCurrencyBalance);

    }

    private void checkOpenOrderExecution(String marketId) throws TradingApiException, ExchangeNetworkException {
        if (currentOpenOrder != null) {
            switch (currentOpenOrder.getType()) {
                case BUY:
                    checkOpenBuyOrderExecution(marketId);
                    break;
                case SELL:
                    checkOpenSellOrderExecution(marketId);
                    break;
                default:
                    throw new TradingApiException("Order type not recognized: " + currentOpenOrder.getType());
            }
        }
    }

    private void checkOpenSellOrderExecution(String marketId) throws TradingApiException, ExchangeNetworkException {
        BigDecimal currentBidPrice =getTicker(marketId).getBid();
        if (currentBidPrice.compareTo(currentOpenOrder.getPrice()) >= 0) {
            LOG.info("SELL: the market's bid price moved above the limit price --> record sell order execution with the current bid price");
            BigDecimal orderPrice = currentOpenOrder.getOriginalQuantity().multiply(currentBidPrice);
            BigDecimal buyFees = getPercentageOfSellOrderTakenForExchangeFee(marketId).multiply(orderPrice);
            BigDecimal netOrderPrice = orderPrice.subtract(buyFees);
            counterCurrencyBalance = counterCurrencyBalance.add(netOrderPrice);
            baseCurrencyBalance = baseCurrencyBalance.subtract(currentOpenOrder.getOriginalQuantity());
            currentOpenOrder = null;
        }
    }

    private void checkOpenBuyOrderExecution(String marketId) throws TradingApiException, ExchangeNetworkException {
        BigDecimal currentAskPrice = getTicker(marketId).getAsk();
        if (currentAskPrice.compareTo(currentOpenOrder.getPrice()) <= 0) {
            LOG.info("BUY: the market's current ask price moved below the limit price --> record buy order execution with the current ask price");
            BigDecimal orderPrice = currentOpenOrder.getOriginalQuantity().multiply(currentAskPrice);
            BigDecimal buyFees = getPercentageOfBuyOrderTakenForExchangeFee(marketId).multiply(orderPrice);
            BigDecimal netOrderPrice = orderPrice.add(buyFees);
            counterCurrencyBalance = counterCurrencyBalance.subtract(netOrderPrice);
            baseCurrencyBalance = baseCurrencyBalance.add(currentOpenOrder.getOriginalQuantity());
            currentOpenOrder = null;
        }
    }
}
