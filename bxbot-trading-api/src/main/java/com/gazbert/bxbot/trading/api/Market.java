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

package com.gazbert.bxbot.trading.api;

/**
 * Holds information for an Exchange market.
 *
 * @author gazbert
 * @since 1.0
 */
public interface Market {

  /**
   * Returns the market name, e.g. LTC_BTC, USD_BTC.
   *
   * @return the market name.
   */
  String getName();

  /**
   * Sets the market id, e.g. 3, btc_usd
   *
   * @param id the ID of the Market.
   */
  void setId(String id);

  /**
   * Returns the market id, e.g. 3, btc_usd
   *
   * @return the market id.
   */
  String getId();

  /**
   * Returns the base currency for the market currency pair.
   *
   * <p>When you buy or sell a currency pair, you are performing that action on the base currency.
   * E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency
   * (BTC) is the counter currency.
   *
   * @return the base currency short code, e.g. LTC
   */
  String getBaseCurrency();

  /**
   * Returns the counter currency for the market currency pair. Also known as the quote currency.
   * E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency
   * (BTC) is the counter currency.
   *
   * @return the counter currency short code, e.g. LTC
   */
  String getCounterCurrency();
}
