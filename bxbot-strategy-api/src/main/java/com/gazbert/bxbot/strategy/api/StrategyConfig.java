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
 * 封装交易策略的任何（可选）配置。
 *
 * Basically just a map of key-value pairs.
 * * 基本上只是一个键值对的映射。
 * </p>
 *
 * <p>
 * Configuration comes from the strategies.yaml file.
 * 配置来自于 strategy.yaml 文件。
 * </p>
 *
 * @author gazbert
 * @since 1.0
 */
public interface StrategyConfig {

  /**
   * Fetches a config item for a given key.
   * 获取给定键的配置项。
   *
   * @param key the key of the item to fetch.
   *            要获取的项目的键。
   *
   * @return value of the item if found, null otherwise.
   *      如果找到该项目，否则 null。
   */
  String getConfigItem(String key);

  /**
   * Returns the number of config items.
   * 返回配置项的数量。
   *
   * @return the number of config items.
   * @return 配置项的数量。
   */
  int getNumberOfConfigItems();

  /**
   * Returns all of the config item keys.
   * 返回所有配置项键。
   *
   * @return all of the config item keys.
   * @return 所有的配置项键。
   */
  Set<String> getConfigItemKeys();
}
