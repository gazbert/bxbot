/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.core.config.market;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Domain object representing a Market config.
 * <p>
 * The configuration is loaded from the markets.xml file.
 *
 * @author gazbert
 * @since 20/07/2016
 */
public class MarketConfig {

    private String label;
    private String id;
    private String baseCurrency;
    private String counterCurrency;
    private boolean enabled;
    private String tradingStrategy; // TODO might change this to ref to StrategyConfig ...


    // required for Jackson
    public MarketConfig() {
    }

    public MarketConfig(String label, String id, String baseCurrency, String counterCurrency, boolean enabled, String tradingStrategy) {
        this.label = label;
        this.id = id;
        this.baseCurrency = baseCurrency;
        this.counterCurrency = counterCurrency;
        this.enabled = enabled;
        this.tradingStrategy = tradingStrategy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getCounterCurrency() {
        return counterCurrency;
    }

    public void setCounterCurrency(String counterCurrency) {
        this.counterCurrency = counterCurrency;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTradingStrategy() {
        return tradingStrategy;
    }

    public void setTradingStrategy(String tradingStrategy) {
        this.tradingStrategy = tradingStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketConfig that = (MarketConfig) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("label", label)
                .add("id", id)
                .add("baseCurrency", baseCurrency)
                .add("counterCurrency", counterCurrency)
                .add("enabled", enabled)
                .add("tradingStrategy", tradingStrategy)
                .toString();
    }
}
