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

package com.gazbert.bxbot.core;

import com.gazbert.bxbot.core.engine.TradingEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * BX-bot is a simple algo trading bot for running on Bitcoin exchanges.
 *
 * It is made up of 3 components:
 *
 * 1. The data stream unit (the part of the systems that receives data (e.g. order book, news wire) from
 *    external sources) - the Exchange Adapters.
 * 2. The decision or strategy unit - the Trading Strategies.
 * 3. The execution unit - the Trading Engine.
 *
 */
public final class BXBot {

    private static final Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        LOG.info(() -> "Starting BX-bot...");
        TradingEngine.newInstance().start();
    }
}
