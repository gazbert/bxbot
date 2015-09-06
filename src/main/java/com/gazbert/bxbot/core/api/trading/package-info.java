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

/**
 * <h2>Trading API</h2>
 *
 * <p>
 * It contains trading operations for Trading Strategies to use and for Exchange Adapters to implement.
 * </p>
 *
 * <p>
 * It uses a common data model that all exchange data must adapt to. The model is based off the Cryptsy data model;
 * it will most likely be normalised in the near future.
 * </p>
 *
 * <p>
 * Every Exchange Adapter must implement the {@link com.gazbert.bxbot.core.api.trading.TradingApi} interface.
 * </p>
 *
 * <p>
 * This current version of the Trading API only supports <a href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a>
 * traded at the <a href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>.
 * It does not support futures trading or margin trading... yet.
 * </p>
 *
 * <p>
 * The Trading Engine and Trading Strategies have a compile-time dependency on this API.
 * </p>
 *
 * TODO Consider moving this into a separate project - BX-bot, Trading Strategies and Exchange Adapters projects would depend on it.
 *
 * @author gazbert
 */
package com.gazbert.bxbot.core.api.trading;