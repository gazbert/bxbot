/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 maiph
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

package com.gazbert.bxbot.exchange.api;

import java.math.BigDecimal;

/**
 * Some Exchange Adapters will need custom precision configs when placing orders.
 *
 * <p>This interface allows us to have a uniform way to fetch this precision for the various
 * Exchange houses.
 *
 * @author maiph
 * @since 1.2
 */
public interface PairPrecisionConfig {

  /**
   * Gets the number of decimal places for price precision. The default value if no pair is found
   * will be -1.
   *
   * @param pair the coin pair.
   * @return the number of decimal places for the price.
   */
  int getPricePrecision(String pair);

  /**
   * Gets the number of decimal places for volume precision. The default value if no pair is found
   * will be -1.
   *
   * @param pair the coin pair.
   * @return the number of decimal places for volume.
   */
  int getVolumePrecision(String pair);

  /**
   * Gets the minimal amount of order volume for this pair. The default value if no pair is found
   * will be null.
   *
   * @param pair the coin pair.
   * @return the minimum amount of order volume.
   */
  BigDecimal getMinimalOrderVolume(String pair);
}
