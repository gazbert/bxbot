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

package com.gazbert.bxbot.repository;

import com.gazbert.bxbot.domain.market.MarketConfig;
import java.util.List;

/**
 * The Market configuration repository.
 *
 * @author gazbert
 */
public interface MarketConfigRepository {

  /**
   * Fetches all market config.
   *
   * @return all market config.
   */
  List<MarketConfig> findAll();

  /**
   * Fetches a market config by id.
   *
   * @param id the id of the market config.
   * @return the market config.
   */
  MarketConfig findById(String id);

  /**
   * Saves the market config.
   *
   * @param config the market config.
   * @return the saved market config.
   */
  MarketConfig save(MarketConfig config);

  /**
   * Deletes a market config.
   *
   * @param id the id of market config.
   * @return the deleted market config.
   */
  MarketConfig delete(String id);
}
