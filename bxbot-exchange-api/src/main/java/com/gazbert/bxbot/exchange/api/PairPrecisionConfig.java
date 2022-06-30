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

/**
 * <p>Some Exchange Adapters will need custom precision configs when placing orders.</p>
 * 一些 Exchange 适配器在下订单时需要自定义精度配置。
 *
 * <p>This interface allows us to have a uniform way to fetch this precision for thevarious Exchange houses.</p>
 * 这个接口允许我们有一个统一的方法来为不同的交易所获取这个精度。
 *
 * @author maiph
 * @since 1.2
 */
public interface PairPrecisionConfig {

  /**
   * Gets the number of decimal places for price precision. The default value id no pair is found
   will be -1.
   获取价格调整的小数位数。默认值 id 没有找到对
   将为-1。
   *
   * @param pair the coin pair.
   *             * @param 硬币对。
   *
   * @return the number of decimal places for the price.
   * @return 价格的小数位数。
   */
  int getPricePrecision(String pair);

  /**
   * Gets the number of decimal places for volume precision. The default value id no pair is found
   will be -1.
   获取体积精度的小数位数。默认值 id 没有找到对
   将为-1。

   * @param pair the coin pair.
   *             硬币对。
   *
   * @return the number of decimal places for volume.
   * 体积的小数位数。
   */
  int getVolumePrecision(String pair);

}
