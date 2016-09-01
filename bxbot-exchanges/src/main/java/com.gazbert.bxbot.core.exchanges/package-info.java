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
 * <h2>Exchange Adapters</h2>
 *
 * <p>
 * Trading Strategies will use Exchange Adapters to execute trades on the exchange via the
 * {@link com.gazbert.bxbot.trading.api.TradingApi}.
 * </p>
 *
 * <p>
 * The Trading Engine will initialise Exchange Adapters via the
 * {@link com.gazbert.bxbot.exchange.api.ExchangeAdapter}.
 * </p>
 *
 * <p>
 * You can write your own Exchange Adapters and keep them here. Alternatively, you can package them up in a
 * separate jar and place it on BX-bot's runtime classpath.
 * </p>
 *
 * Your Exchange Adapter must:
 * <ol>
 * <li>implement the {@link com.gazbert.bxbot.exchange.api.ExchangeAdapter} and
 *     {@link com.gazbert.bxbot.trading.api.TradingApi} interfaces.
 * <li>be placed on the Trading Engine's runtime classpath: keep it here, or in a separate jar file.</li>
 * <li>include a configuration entry in the ./config/exchanges.xml file.</li>
 * </ol>
 *
 * </p>
 * See the project README "How do I write my own Exchange Adapter?" section.
 * </p>
 * 
 * @author gazbert
 * @since 1.0
 */
package com.gazbert.bxbot.core.exchanges;