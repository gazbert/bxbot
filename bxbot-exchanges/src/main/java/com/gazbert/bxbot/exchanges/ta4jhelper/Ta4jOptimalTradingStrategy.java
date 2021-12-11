package com.gazbert.bxbot.exchanges.ta4jhelper;

import com.gazbert.bxbot.trading.api.TradingApiException;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;

public class Ta4jOptimalTradingStrategy extends BaseStrategy {
    private static final TA4JRecordingRule buyRule = new TA4JRecordingRule();
    private static final TA4JRecordingRule sellRule = new TA4JRecordingRule();

    public Ta4jOptimalTradingStrategy(BarSeries series, BigDecimal buyFee, BigDecimal sellFee) throws TradingApiException {
        super("Optimal trading rule", buyRule, sellRule);
        this.calculateOptimalTrades(series, series.numOf(buyFee), series.numOf(sellFee));
    }

    private void calculateOptimalTrades(BarSeries series, Num buyFee, Num sellFee) throws TradingApiException {
        int lastSeenMinimumIndex = -1;
        Num lastSeenMinimum = null;
        int lastSeenMaximumIndex = -1;
        Num lastSeenMaximum = null;

        for(int index = series.getBeginIndex(); index <= series.getEndIndex(); index++) {
            Bar bar = series.getBar(index);
            Num askPrice = bar.getHighPrice();
            Num bidPrice = bar.getLowPrice();
            if (lastSeenMinimum == null) {
                lastSeenMinimum = askPrice;
                lastSeenMinimumIndex = index;
            } else {
                if (lastSeenMinimum.isGreaterThan(askPrice)) {
                    createTrade(lastSeenMinimumIndex, lastSeenMinimum, lastSeenMaximumIndex, lastSeenMaximum);
                    lastSeenMaximum = null;
                    lastSeenMaximumIndex = -1;
                    lastSeenMinimum = askPrice;
                    lastSeenMinimumIndex = index;
                } else {
                    Num buyFees = lastSeenMinimum.multipliedBy(buyFee);
                    Num minimumPlusFees = lastSeenMinimum.plus(buyFees);
                    Num currentPriceSellFees = bidPrice.multipliedBy(sellFee);
                    Num currentPriceMinusFees = bidPrice.minus(currentPriceSellFees);
                    if(lastSeenMaximum == null) {
                        if(currentPriceMinusFees.isGreaterThan(minimumPlusFees)) {
                            lastSeenMaximum = bidPrice;
                            lastSeenMaximumIndex = index;
                        }
                    } else {
                        if(bidPrice.isGreaterThanOrEqual(lastSeenMaximum)) {
                            lastSeenMaximum = bidPrice;
                            lastSeenMaximumIndex = index;
                        } else {
                            Num lastMaxPriceSellFees = lastSeenMaximum.multipliedBy(sellFee);
                            Num lastMaxPriceMinusFees = lastSeenMaximum.minus(lastMaxPriceSellFees);
                            Num currentPricePlusBuyFees = bidPrice.plus(bidPrice.multipliedBy(buyFee));
                            if (currentPricePlusBuyFees.isLessThan(lastMaxPriceMinusFees)) {
                                createTrade(lastSeenMinimumIndex, lastSeenMinimum, lastSeenMaximumIndex, lastSeenMaximum);
                                lastSeenMaximum = null;
                                lastSeenMaximumIndex = -1;
                                lastSeenMinimum = askPrice;
                                lastSeenMinimumIndex = index;
                            }
                        }
                    }
                }
            }
        }
    }

    private void createTrade(int lastSeenMinimumIndex, Num lastSeenMinimum, int lastSeenMaximumIndex, Num lastSeenMaximum) throws TradingApiException {
        if (lastSeenMinimum != null && lastSeenMaximum != null) {
            buyRule.addTrigger(lastSeenMinimumIndex);
            sellRule.addTrigger(lastSeenMaximumIndex);
        }
    }
}
