package com.gazbert.bxbot.exchanges.ta4jhelper;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeries;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class GsonBarData {
    private long endTime;
    private Number openPrice;
    private Number highPrice;
    private Number lowPrice;
    private Number closePrice;
    private Number volume;
    private Number amount;

    public static GsonBarData from(Bar bar) {
        GsonBarData result = new GsonBarData();
        result.endTime = bar.getEndTime().toInstant().toEpochMilli();
        result.openPrice = bar.getOpenPrice().getDelegate();
        result.highPrice = bar.getHighPrice().getDelegate();
        result.lowPrice = bar.getLowPrice().getDelegate();
        result.closePrice = bar.getClosePrice().getDelegate();
        result.volume = bar.getVolume().getDelegate();
        result.amount = bar.getAmount().getDelegate();
        return result;
    }

    public void addTo(BaseBarSeries barSeries) {
        Instant endTimeInstant = Instant.ofEpochMilli(endTime);
        ZonedDateTime endBarTime = ZonedDateTime.ofInstant(endTimeInstant, ZoneId.systemDefault());
        Number volumeToAdd = volume == null ? BigDecimal.ZERO : volume;
        Number amountToAdd = amount == null ? BigDecimal.ZERO : amount;
        barSeries.addBar(endBarTime, openPrice, highPrice, lowPrice, closePrice, volumeToAdd, amountToAdd);
    }
}
