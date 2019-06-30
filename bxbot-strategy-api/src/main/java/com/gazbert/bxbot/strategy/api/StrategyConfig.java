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

package com.gazbert.bxbot.strategy.api;

import java.util.Set;

/**
 * <p>
 * Encapsulates any (optional) configuration for a Trading Strategy.
 * Basically just a map of key-value pairs.
 * </p>
 *
 * <p>
 * Configuration comes from the strategies.yaml file.
 * </p>
 *
 * @author gazbert
 * @since 1.0
 */
public interface StrategyConfig {

  /**
   * Fetches a config item for a given key.
   *
   * @param key the key of the item to fetch.
   * @return value of the item if found, null otherwise.
   */
  String getConfigItem(String key);

  /**
   * Returns the number of config items.
   *
   * @return the number of config items.
   */
  int getNumberOfConfigItems();

  /**
   * Returns all of the config item keys.
   *
   * @return all of the config item keys.
   */
  Set<String> getConfigItemKeys();
}
