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
 * 保存交易所市场的信息。
 *
 * @author gazbert
 * @since 1.0
 */
public interface Market {

  /**
   * Returns the market name, e.g. LTC_BTC, USD_BTC.
   * * 返回市场名称 ,e.g. LTC_BTC, USD_BTC.
   *
   * @return the market name. 市场名称。
   */
  String getName();

  /**
   * Sets the market id, e.g. 3, btc_usd
   * 设置市场 id，例如3、btc_usd
   *
   * @param id the ID of the Market.
   *           @param id 市场的 ID。
   */
  void setId(String id);

  /**
   * Returns the market id, e.g. 3, btc_usd
   * * 返回市场 id，例如3、btc_usd
   *
   * @return the market id.
   * 市场编号。
   */
  String getId();

  /**
   * Returns the base currency for the market currency pair.
   * 返回市场货币对的基础货币。
   *
   * <p>When you buy or sell a currency pair, you are performing that action on the base currency.
    E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency
    (BTC) is the counter currency.
   <p>当您买入或卖出货币对时，您是在对基础货币执行该操作。
   例如。在 LTC/BTC 市场中，第一货币 (LTC) 是基础货币，第二货币
   (BTC) 是相对货币。
   *
   * @return the base currency short code, e.g. LTC
   *  基础货币短代码 e.g. LTC
   */
  String getBaseCurrency();

  /**
   * Returns the counter currency for the market currency pair. Also known as the quote currency.
    E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency
    (BTC) is the counter currency.
   返回市场货币对的对应货币。也称为报价货币。
   例如。在 LTC/BTC 市场中，第一货币 (LTC) 是基础货币，第二货币
   (BTC) 是相对货币。
   *
   * @return the counter currency short code, e.g. LTC
   *  柜台货币短代码 e.g. LTC
   */
  String getCounterCurrency();
}
