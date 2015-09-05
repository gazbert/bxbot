/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.core.api.trading;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Domain class representing your wallet balance info on the exchange.
 *
 * @author gazbert
 */
public final class BalanceInfo {

    /**
     * <p>
     * Map of wallet balances <em>available</em> to trade.
     * </p>
     *
     * <p>
     * Key is currency id, e.g. LTC, BTC, USD
     * </p>
     */
    private Map<String, BigDecimal> balancesAvailable;

    /**
     * <p>
     * Map of wallet balances currently <em>on hold</em> for open orders.
     * </p>
     *
     * <p>
     * Key is currency id, e.g. LTC, BTC, USD
     * </p>
     */
    private Map<String, BigDecimal> balancesOnHold;


    /**
     * Constructor creates balance info.
     *
     * @param balancesAvailable map of available balances.
     * @param balancesOnHold    map of balances on hold.
     */
    public BalanceInfo(Map<String, BigDecimal> balancesAvailable, Map<String, BigDecimal> balancesOnHold) {
        this.balancesAvailable = balancesAvailable;
        this.balancesOnHold = balancesOnHold;
    }

    /**
     * Returns map of available balances.
     * @return map of available balances.
     */
    public Map<String, BigDecimal> getBalancesAvailable() {
        return balancesAvailable;
    }

    /**
     * Sets map of available balances.
     *
     * @param balancesAvailable the map of available balances.
     */
    public void setBalancesAvailable(Map<String, BigDecimal> balancesAvailable) {
        this.balancesAvailable = balancesAvailable;
    }

    /**
     * Returns map of balances on hold.
     * @return map of balances on hold.
     */
    public Map<String, BigDecimal> getBalancesOnHold() {
        return balancesOnHold;
    }

    /**
     * Sets map of balances on hold.
     *
     * @param balancesOnHold a map of balances on hold.
     */
    public void setBalancesOnHold(Map<String, BigDecimal> balancesOnHold) {
        this.balancesOnHold = balancesOnHold;
    }

    @Override
    public String toString() {
        return BalanceInfo.class.getSimpleName()
                + " ["
                + "balancesAvailable=" + balancesAvailable
                + ", balancesOnHold=" + balancesOnHold
                + "]";
    }
}

