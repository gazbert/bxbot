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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Domain class representing a Market.
 *
 * @author gazbert
 */
public final class Market {

    /**
     * Name of the market
     */
    private String name;

    /**
     * ID of the market. This makes the Market unique.
     */
    private String id;

    /**
     * <p>
     * The base currency code, e.g. BTC, LTC, USD.
     * </p>
     *
     * <p>
     * This is the currency short code for the base currency in the currency pair.
     * When you buy or sell a currency pair, you are performing that action on the base currency.
     * The base currency is the commodity you are buying or selling.
     * E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency (BTC) is the
     * counter currency.
     * </p>
     */
    private String baseCurrency;

    /**
     * <p>
     * The counter currency code, e.g. BTC, LTC, USD.
     * </p>
     *
     * <p>
     * This is the currency short code for the counter currency in the currency pair. Also known as the quote currency.
     * E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency (BTC) is the
     * counter currency.
     * </p>
     */
    private String counterCurrency;


    /**
     * Constructor builds a Market object.
     *
     * @param name            market name, e.g. LTC/BTC, BTC/USD
     * @param id              market id, e.g. 3, btc_usd
     * @param baseCurrency    the base currency code short code, e.g. LTC. When you buy or sell a currency pair, you are
     *                        performing that action on the base currency. E.g. in a LTC/BTC market, the first currency
     *                        (LTC) is the base currency and the second currency (BTC) is the counter currency.
     * @param counterCurrency the counter currency short code, e.g. BTC. Also known as the quote currency.
     *                        E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second
     *                        currency (BTC) is the counter currency.
     */
    public Market(String name, String id, String baseCurrency, String counterCurrency) {
        this.id = id;
        this.name = name;
        this.baseCurrency = baseCurrency;
        this.counterCurrency = counterCurrency;
    }

    /**
     * Sets the market name, e.g. LTC_BTC, USD_BTC.
     *
     * @param name the name of the Market.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the market name, e.g. LTC_BTC, USD_BTC.
     * @return the market name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the market id, e.g. 3, btc_usd
     *
     * @param id the ID of the Market.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the market id, e.g. 3, btc_usd
     * @return the market id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the base currency for market currency pair.
     * @param baseCurrency the base currency short code, e.g. LTC
     */
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    /**
     * <p>
     * Returns the base currency for the market currency pair.
     * </p>
     *
     * <p>
     * When you buy or sell a currency pair, you are performing that action on the base currency.
     * E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency (BTC) is the
     * counter currency.
     * </p>
     *
     * @return the base currency short code, e.g. LTC
     */
    public String getBaseCurrency() {
        return baseCurrency;
    }

    /**
     * Sets the counter currency for market currency pair.
     * @param counterCurrency the counter currency short code, e.g. LTC
     */
    public void setCounterCurrency(String counterCurrency) {
        this.counterCurrency = counterCurrency;
    }

    /**
     * <p>
     * Returns the counter currency for the market currency pair. Also known as the quote currency.
     * E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency (BTC) is the
     * counter currency.
     * </p>
     *
     * @return the counter currency short code, e.g. LTC
     */
    public String getCounterCurrency() {
        return counterCurrency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Market market = (Market) o;
        return Objects.equal(id, market.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("id", id)
                .add("baseCurrency", baseCurrency)
                .add("counterCurrency", counterCurrency)
                .toString();
    }
}
