package com.gazbert.bxbot.exchanges;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Abstract base class for shared adapter tests.
 *
 * @author gazbert
 */
abstract class AbstractExchangeAdapterTest {

  private DecimalFormatSymbols decimalFormatSymbols;

  AbstractExchangeAdapterTest() {
    // Some locales (e.g. France, Germany, Czech Republic) default to ',' instead of '.' for decimal
    // point. Exchanges always require a '.'
    decimalFormatSymbols = new DecimalFormatSymbols(Locale.getDefault());
    decimalFormatSymbols.setDecimalSeparator('.');
  }

  /**
   * Returns the decimal format symbols for using with BigDecimals with the exchanges. Specifically,
   * the decimal point symbol is set to a '.'
   *
   * @return the decimal format symbols.
   */
  DecimalFormatSymbols getDecimalFormatSymbols() {
    return decimalFormatSymbols;
  }
}
