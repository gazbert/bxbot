package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.ta4jhelper.*;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.TickerImpl;
import com.gazbert.bxbot.trading.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ta4j.core.*;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.tradereport.PerformanceReport;
import org.ta4j.core.tradereport.TradeStatsReport;
import org.ta4j.core.tradereport.TradingStatement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class TA4JRecordingAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {
    private static final Logger LOG = LogManager.getLogger();
    private static final String BUY_FEE_PROPERTY_NAME = "buy-fee";
    private static final String SELL_FEE_PROPERTY_NAME = "sell-fee";


    private BigDecimal buyFeePercentage;
    private BigDecimal sellFeePercentage;
    private BigDecimal sellLimitDistancePercentage;
    private String tradingSeriesTradingPath;


    private BarSeries tradingSeries;

    private static final String counterCurrency = "ZEUR";
    private static final String baseCurrency = "XXRP";

    private BigDecimal baseCurrencyBalance = BigDecimal.ZERO;
    private BigDecimal counterCurrencyBalance = new BigDecimal(100); // simulated starting balance
    private OpenOrder currentOpenOrder;
    private int currentTick;
    private final TA4JRecordingRule sellOrderRule = new TA4JRecordingRule();
    private final TA4JRecordingRule buyOrderRule = new TA4JRecordingRule();

    @Override
    public void init(ExchangeConfig config) {
        LOG.info(() -> "About to initialise ta4j recording ExchangeConfig: " + config);
        setOtherConfig(config);
        loadRecodingSeriesFromJson();
        currentTick = tradingSeries.getBeginIndex() - 1;
    }

    private void loadRecodingSeriesFromJson() {
        tradingSeries = JsonBarsSerializer.loadSeries(tradingSeriesTradingPath);
        if(tradingSeries == null || tradingSeries.isEmpty()) {
            throw new IllegalArgumentException("Could not load ta4j series from json '" + tradingSeriesTradingPath + "'");
        }
    }

    private void setOtherConfig(ExchangeConfig exchangeConfig) {
        final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

        final String buyFeeInConfig = getOtherConfigItem(otherConfig, BUY_FEE_PROPERTY_NAME);
        buyFeePercentage =
                new BigDecimal(buyFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        LOG.info(() -> "Buy fee % in BigDecimal format: " + buyFeePercentage);

        final String sellFeeInConfig = getOtherConfigItem(otherConfig, SELL_FEE_PROPERTY_NAME);
        sellFeePercentage =
                new BigDecimal(sellFeeInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        LOG.info(() -> "Sell fee % in BigDecimal format: " + sellFeePercentage);

        final String sellLimitDistanceInConfig = getOtherConfigItem(otherConfig, "sell-stop-limit-percentage-distance");
        sellLimitDistancePercentage =
                new BigDecimal(sellLimitDistanceInConfig).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        LOG.info(() -> "Sell (stop-limit order) limit distance % in BigDecimal format: " + sellLimitDistancePercentage);

        tradingSeriesTradingPath = getOtherConfigItem(otherConfig, "trading-series-json-path");
        LOG.info(() -> "path to load series json from for recording:" + tradingSeriesTradingPath);
    }

    @Override
    public String getImplName() {
        return "ta4j recording and analyzing adapter";
    }

    @Override
    public MarketOrderBook getMarketOrders(String marketId) throws ExchangeNetworkException, TradingApiException {
        throw new TradingApiException("get market orders is not implemented", new UnsupportedOperationException());
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
        Date creationDate = Date.from(tradingSeries.getBar(currentTick).getEndTime().toInstant());
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
        return (BigDecimal) tradingSeries.getBar(currentTick).getClosePrice().getDelegate();
    }

    @Override
    public BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException {
        HashMap<String, BigDecimal> availableBalances = new HashMap<>();
        availableBalances.put(baseCurrency, baseCurrencyBalance);
        availableBalances.put(counterCurrency, counterCurrencyBalance);
        return new BalanceInfoImpl(availableBalances, new HashMap<>());
    }

    @Override
    public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
        currentTick++;
        LOG.info("Tick increased to '" + currentTick + "'");
        if (currentTick > tradingSeries.getEndIndex()) {
            finishRecording(marketId);
            return null;
        }

        checkOpenOrderExecution(marketId);

        Bar currentBar = tradingSeries.getBar(currentTick);
        BigDecimal last = (BigDecimal) currentBar.getClosePrice().getDelegate();
        BigDecimal bid = (BigDecimal) currentBar.getLowPrice().getDelegate();
        BigDecimal ask = (BigDecimal) currentBar.getHighPrice().getDelegate();
        BigDecimal low = (BigDecimal) currentBar.getLowPrice().getDelegate();
        BigDecimal high = (BigDecimal) currentBar.getHighPrice().getDelegate();
        BigDecimal open = (BigDecimal) currentBar.getOpenPrice().getDelegate();
        BigDecimal volume = (BigDecimal) currentBar.getVolume().getDelegate();
        BigDecimal vwap = BigDecimal.ZERO;
        Long timestamp = currentBar.getEndTime().toEpochSecond();
        return new TickerImpl(last, bid, ask, low, high, open, volume, vwap, timestamp);
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
                    throw new TradingApiException("Order type not recognized: " +currentOpenOrder.getType());
            }
        }
    }

    private void checkOpenSellOrderExecution(String marketId) throws TradingApiException, ExchangeNetworkException {
        BigDecimal currentBidPrice = (BigDecimal)tradingSeries.getBar(currentTick).getLowPrice().getDelegate();
        if (currentBidPrice.compareTo(currentOpenOrder.getPrice()) <= 0) {
            LOG.info("SELL: the bid price is below or equal to the stop price --> record sell order execution with the bid price");
            sellOrderRule.addTrigger(currentTick);
            BigDecimal orderPrice = currentOpenOrder.getOriginalQuantity().multiply(currentBidPrice);
            BigDecimal buyFees = getPercentageOfSellOrderTakenForExchangeFee(marketId).multiply(orderPrice);
            BigDecimal netOrderPrice = orderPrice.subtract(buyFees);
            counterCurrencyBalance = counterCurrencyBalance.add(netOrderPrice);
            baseCurrencyBalance = baseCurrencyBalance.subtract(currentOpenOrder.getOriginalQuantity());
            currentOpenOrder = null;
        }
    }

    private void checkOpenBuyOrderExecution(String marketId) throws TradingApiException, ExchangeNetworkException {
        BigDecimal currentAskPrice = (BigDecimal)tradingSeries.getBar(currentTick).getHighPrice().getDelegate();
        if (currentAskPrice.compareTo(currentOpenOrder.getPrice()) <=0) {
            LOG.info("BUY: the current ask price is below or queal to the limit price --> record buy order execution with the current ask price");
            buyOrderRule.addTrigger(currentTick);
            BigDecimal orderPrice = currentOpenOrder.getOriginalQuantity().multiply(currentAskPrice);
            BigDecimal buyFees = getPercentageOfBuyOrderTakenForExchangeFee(marketId).multiply(orderPrice);
            BigDecimal netOrderPrice = orderPrice.add(buyFees);
            counterCurrencyBalance = counterCurrencyBalance.subtract(netOrderPrice);
            baseCurrencyBalance = baseCurrencyBalance.add(currentOpenOrder.getOriginalQuantity());
            currentOpenOrder = null;
        }
    }


    private void finishRecording(String marketId) throws TradingApiException, ExchangeNetworkException {
        final List<Strategy> strategies = new ArrayList<>();
        Strategy strategy = new BaseStrategy("Recorded ta4j trades", buyOrderRule, sellOrderRule);
        strategies.add(strategy);
        Ta4jOptimalTradingStrategy optimalTradingStrategy = new Ta4jOptimalTradingStrategy(tradingSeries, getPercentageOfBuyOrderTakenForExchangeFee(marketId), getPercentageOfSellOrderTakenForExchangeFee(marketId));
        strategies.add(optimalTradingStrategy);

        TradePriceRespectingBacktestExecutor backtestExecutor = new TradePriceRespectingBacktestExecutor(tradingSeries, new LinearTransactionCostModel(getPercentageOfBuyOrderTakenForExchangeFee(marketId).doubleValue()));
        List<TradingStatement> statements = backtestExecutor.execute(strategies, tradingSeries.numOf(25), Order.OrderType.BUY);
        logReports(statements);
        BuyAndSellSignalsToChart.printSeries(tradingSeries, strategy);
        BuyAndSellSignalsToChart.printSeries(tradingSeries, optimalTradingStrategy);
        throw new TradingApiException("Simulation end finished. Ending balance: " + getBalanceInfo());
    }

    private void logReports(List<TradingStatement> statements) {
        for(TradingStatement statement:statements) {
            LOG.info( () ->
            "\n######### "+statement.getStrategy().getName()+" #########\n" +
            createPerformanceReport(statement) + "\n" +
            createTradesReport(statement)+ "\n"+
                    "###########################"
            );
        }
    }

    private String createTradesReport(TradingStatement statement) {
        TradeStatsReport tradeStatsReport = statement.getTradeStatsReport();
        return "--------- trade statistics report ---------\n" +
                "loss trade count: " + tradeStatsReport.getLossTradeCount() + "\n" +
                "profit trade count: " + tradeStatsReport.getProfitTradeCount() + "\n" +
                "break even trade count: " + tradeStatsReport.getBreakEvenTradeCount() + "\n" +
                "---------------------------";
    }

    private String createPerformanceReport(TradingStatement statement) {
        PerformanceReport performanceReport = statement.getPerformanceReport();
        return "--------- performance report ---------\n" +
                "total loss: " + performanceReport.getTotalLoss() + "\n" +
                "total profit: " + performanceReport.getTotalProfit() + "\n" +
                "total profit loss: " + performanceReport.getTotalProfitLoss() + "\n" +
                "total profit loss percentage: " + performanceReport.getTotalProfitLossPercentage() + "\n" +
                "---------------------------";
    }

    @Override
    public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) throws TradingApiException, ExchangeNetworkException {
        return buyFeePercentage;
    }

    @Override
    public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) throws TradingApiException, ExchangeNetworkException {
        return sellFeePercentage;
    }
}
