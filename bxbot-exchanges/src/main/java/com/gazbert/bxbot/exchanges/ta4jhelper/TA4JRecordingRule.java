package com.gazbert.bxbot.exchanges.ta4jhelper;

import com.gazbert.bxbot.trading.api.TradingApiException;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.trading.rules.AbstractRule;

import java.util.HashSet;
import java.util.Set;

public class TA4JRecordingRule extends AbstractRule {
    private Set<Integer> recordedIndeces = new HashSet<>();

    public void addTrigger(int index) throws TradingApiException {
        if(recordedIndeces.contains(index)) {
            throw new TradingApiException("Recorded two trades at the same time.");
        }
        recordedIndeces.add(index);
    }



    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        final boolean satisfied = recordedIndeces.contains(index);
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
