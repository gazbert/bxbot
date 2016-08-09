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

package com.gazbert.bxbot.core.config.engine;

import com.google.common.base.MoreObjects;

import java.math.BigDecimal;

/**
 * Domain object representing the Engine config.
 * <p>
 * The configuration is loaded from the engine.xml file.
 *
 * @author gazbert
 * @since 20/07/2016
 */
public class EngineConfig {

    private String emergencyStopCurrency;
    private BigDecimal emergencyStopBalance;
    private int tradeCycleInterval;

    // required for jackson
    public EngineConfig() {
    }

    public EngineConfig(String emergencyStopCurrency, BigDecimal emergencyStopBalance, int tradeCycleInterval) {
        this.emergencyStopCurrency = emergencyStopCurrency;
        this.emergencyStopBalance = emergencyStopBalance;
        this.tradeCycleInterval = tradeCycleInterval;
    }

    public String getEmergencyStopCurrency() {
        return emergencyStopCurrency;
    }

    public void setEmergencyStopCurrency(String emergencyStopCurrency) {
        this.emergencyStopCurrency = emergencyStopCurrency;
    }

    public BigDecimal getEmergencyStopBalance() {
        return emergencyStopBalance;
    }

    public void setEmergencyStopBalance(BigDecimal emergencyStopBalance) {
        this.emergencyStopBalance = emergencyStopBalance;
    }

    public int getTradeCycleInterval() {
        return tradeCycleInterval;
    }

    public void setTradeCycleInterval(int tradeCycleInterval) {
        this.tradeCycleInterval = tradeCycleInterval;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("emergencyStopCurrency", emergencyStopCurrency)
                .add("emergencyStopBalance", emergencyStopBalance)
                .add("tradeCycleInterval", tradeCycleInterval)
                .toString();
    }
}
