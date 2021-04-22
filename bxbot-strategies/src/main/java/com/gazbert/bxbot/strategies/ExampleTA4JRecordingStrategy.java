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

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Component("exampleTa4jRecordingStrategy") // used to load the strategy using Spring bean injection
public class ExampleTA4JRecordingStrategy implements TradingStrategy {

    private static final Logger LOG = LogManager.getLogger();
    private TradingApi tradingApi;
    private Market market;
    private BarSeries series;

    @Override
    public void init(TradingApi tradingApi, Market market, StrategyConfig config) {
        LOG.info(() -> "Initialising TA4J Recording Strategy...");
        this.tradingApi = tradingApi;
        this.market = market;
        series = new BaseBarSeriesBuilder().withName(market.getName() + "_" + System.currentTimeMillis()).build();
        LOG.info(() -> "Trading Strategy initialised successfully!");
    }

    @Override
    public void execute() throws StrategyException {
        try {

            Ticker currentTicker = tradingApi.getTicker(market.getId());
            LOG.info(() -> market.getName() + " Updated latest market info: " + currentTicker);
            BigDecimal tickHighPrice = currentTicker.getAsk(); //save ask price as high price.
            BigDecimal tickLowPrice = currentTicker.getBid(); //save bid price as low price.
            // Store markets data as own bar per strategy execution. Hereby
            // * Close == Open --> last market price
            // * High  --> ask market price
            // * Low --> bid market price
            series.addBar(ZonedDateTime.now(), currentTicker.getLast(), tickHighPrice, tickLowPrice, currentTicker.getLast());
        } catch (TradingApiException | ExchangeNetworkException e) {
            // as soon as the server communcation fails, store the recorded series to a json file
            String filename = market.getId() + "_" + System.currentTimeMillis() + ".json";
            JsonBarsSerializer.persistSeries(series, filename);
            LOG.info(() -> market.getName() + " Stored recorded market data as json to '" + filename + "'");

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
}
