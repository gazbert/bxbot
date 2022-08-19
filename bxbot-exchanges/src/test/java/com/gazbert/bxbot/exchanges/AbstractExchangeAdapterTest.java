package com.gazbert.bxbot.exchanges;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Abstract base class for shared adapter tests.
 * 共享适配器测试的抽象基类。
 *
 * @author gazbert
 */
abstract class AbstractExchangeAdapterTest {

  private DecimalFormatSymbols decimalFormatSymbols;

  AbstractExchangeAdapterTest() {
    // Some locales (e.g. France, Germany, Czech Republic) default to ',' instead of '.' for decimal point. Exchanges always require a '.'
    // 某些语言环境（例如法国、德国、捷克共和国）默认为 ',' 而不是 '.'为小数点。交易所总是需要一个“.”
    decimalFormatSymbols = new DecimalFormatSymbols(Locale.getDefault());
    decimalFormatSymbols.setDecimalSeparator('.');
  }

  /**
   * Returns the decimal format symbols for using with BigDecimals with the exchanges. Specifically,
    the decimal point symbol is set to a '.'
   返回与 BigDecimals 和交换一起使用的十进制格式符号。具体来说，
   小数点符号设置为“.”
   *
   * @return the decimal format symbols.
   * @return 十进制格式符号。
   */
  DecimalFormatSymbols getDecimalFormatSymbols() {
    return decimalFormatSymbols;
  }
}
