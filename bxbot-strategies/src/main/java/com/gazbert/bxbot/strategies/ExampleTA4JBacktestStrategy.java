package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategies.ta4jhelper.JsonBarsSerializer;
import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component("exampleTa4jBacktestStrategy") // used to load the strategy using Spring bean injection
public class ExampleTA4JBacktestStrategy implements TradingStrategy {

    private static final Logger LOG = LogManager.getLogger();
    /**
     * The decimal format for the logs.
     */
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.########");

    private TradingApi tradingApi;
    private Market market;
    private BarSeries series;
    private Ticker currentTicker;
    private BaseStrategy ta4jStrategy;

    @Override
    public void init(TradingApi tradingApi, Market market, StrategyConfig config) {
        LOG.info(() -> "Initialising TA4J Backtest Strategy...");
        this.tradingApi = tradingApi;
        this.market = market;
        series = new BaseBarSeriesBuilder().withName(market.getName() + "_" + System.currentTimeMillis()).build();
        initYourStrategy();

        LOG.info(() -> "Trading Strategy initialised successfully!");
    }

    private void initYourStrategy() {
        // In this example, we use a simple sma strategy with the help of ta4j. You can of course implement your own logic instead
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        SMAIndicator shortTimeSma = new SMAIndicator(closePriceIndicator, 30);
        SMAIndicator longTimeSma = new SMAIndicator(closePriceIndicator, 200);
        Rule entryRule = new CrossedUpIndicatorRule(shortTimeSma, longTimeSma);
        Rule exitRule = new CrossedDownIndicatorRule(shortTimeSma, longTimeSma);
        ta4jStrategy = new BaseStrategy(entryRule, exitRule);
    }

    @Override
    public void execute() throws StrategyException {
        try {
            // first get the current market info. This will update the ta4j backtest exchange to the next tick/timeslot
            currentTicker = tradingApi.getTicker(market.getId());
            LOG.info(() -> market.getName() + " Updated latest market info: " + currentTicker);

            BigDecimal tickHighPrice = currentTicker.getAsk(); //save ask price as high price.
            BigDecimal tickLowPrice = currentTicker.getBid(); //save bid price as low price.
            // Store markets data as own bar per strategy execution. Hereby
            // * Close == Open --> last market price
            // * High  --> ask market price
            // * Low --> bid market price
            ZonedDateTime tickTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTicker.getTimestamp()), ZoneId.systemDefault());
            series.addBar(tickTime, currentTicker.getLast(), tickHighPrice, tickLowPrice, currentTicker.getLast());

            executeStrategy();
        } catch (TradingApiException | ExchangeNetworkException e) {
            // We are just going to re-throw as StrategyException for engine to deal with - it will
            // shutdown the bot.
            LOG.error(
                    market.getName()
                            + " Failed to perform the strategy because Exchange threw TradingApiException, ExchangeNetworkexception or StrategyException. "
                            + "Telling Trading Engine to shutdown bot!",
                    e);
            throw new StrategyException(e);
        }
    }

    private void executeStrategy() throws ExchangeNetworkException, TradingApiException, StrategyException {
        // Ask the ta4j strategy how we want to proceed
        int endIndex = series.getEndIndex();
        if (ta4jStrategy.shouldEnter(endIndex)) {
            // we should enter the market
            shouldEnter();
        } else if (ta4jStrategy.shouldExit(endIndex)) {
            // we should leave the market
            shouldExit();
        }
    }

    private void shouldExit() throws ExchangeNetworkException, TradingApiException {
        //place a sell order with the available base currency units with the current bid price to get the order filled directly
        BigDecimal availableBaseCurrency = getAvailableCurrencyBalance(market.getBaseCurrency());
        String orderId = tradingApi.createOrder(market.getId(), OrderType.SELL, availableBaseCurrency, currentTicker.getBid());
        LOG.info(() -> market.getName() + " SELL Order sent successfully to exchange. ID: " + orderId);
    }

    private void shouldEnter() throws ExchangeNetworkException, TradingApiException {
        //place a buy order of 25% of the available counterCurrency units with the current ask price to get the order filled directly
        BigDecimal availableCounterCurrency = getAvailableCurrencyBalance(market.getCounterCurrency());
        BigDecimal balanceToUse = availableCounterCurrency.multiply(new BigDecimal("0.25"));
        final BigDecimal piecesToBuy = balanceToUse.divide(currentTicker.getAsk(), 8, RoundingMode.HALF_DOWN);
        String orderID = tradingApi.createOrder(market.getId(), OrderType.BUY, piecesToBuy, currentTicker.getAsk());
        LOG.info(() -> market.getName() + " BUY Order sent successfully to exchange. ID: " + orderID);
    }

    private BigDecimal getAvailableCurrencyBalance(String currency) throws ExchangeNetworkException, TradingApiException {
        LOG.info(() -> market.getName() + " Fetching the available balance for the currency '" + currency + "'.");
        BalanceInfo balanceInfo = tradingApi.getBalanceInfo();
        final BigDecimal currentBalance = balanceInfo.getBalancesAvailable().get(currency);
        if (currentBalance == null) {
            final String errorMsg = "Failed to get current currency balance as '" + currency + "' key is not available in the balances map. Balances returned: " + balanceInfo.getBalancesAvailable();
            LOG.warn(() -> errorMsg);
            return BigDecimal.ZERO;
        } else {
            LOG.info(() -> market.getName() + "Currency balance available on exchange is ["
                    + decimalFormat.format(currentBalance)
                    + "] "
                    + currency);
        }
        return currentBalance;
    }
}
