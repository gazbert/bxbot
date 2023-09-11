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

package com.gazbert.bxbot.domain.market;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Domain object representing a Market config.
 *
 * @author gazbert
 */
@Schema
public class MarketConfig {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "A unique identifier for the Market. Value must be an alphanumeric string. "
              + "Underscores and dashes are also permitted.")
  private String id;

  @Schema(description = "An optional friendly name for the Market.")
  private String name;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The Market Base Currency.")
  private String baseCurrency;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The Market Counter Currency.")
  private String counterCurrency;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Enable trading on this Market?")
  private boolean enabled;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The Strategy ID to use for the Market.")
  private String tradingStrategyId;

  /** Creates a MarketConfig. Required by ConfigurableComponentFactory */
  public MarketConfig() {
    // noimpl
  }

  /**
   * Creates a MarketConfig from an existing one.
   *
   * @param other the MarketConfig to copy.
   */
  public MarketConfig(MarketConfig other) {
    this.id = other.id;
    this.name = other.name;
    this.baseCurrency = other.baseCurrency;
    this.counterCurrency = other.counterCurrency;
    this.enabled = other.enabled;
    this.tradingStrategyId = other.tradingStrategyId;
  }

  /**
   * Creates a new MarketConfig.
   *
   * @param id the market ID.
   * @param name the market name.
   * @param baseCurrency the market base currency.
   * @param counterCurrency the market counter currency.
   * @param enabled is market enabled?
   * @param tradingStrategyId the trading strategy id to use for the market.
   */
  public MarketConfig(
      String id,
      String name,
      String baseCurrency,
      String counterCurrency,
      boolean enabled,
      String tradingStrategyId) {

    this.id = id;
    this.name = name;
    this.baseCurrency = baseCurrency;
    this.counterCurrency = counterCurrency;
    this.enabled = enabled;
    this.tradingStrategyId = tradingStrategyId;
  }

  /**
   * Returns the id.
   *
   * @return the id.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the id.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the name.
   *
   * @return the name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the base currency.
   *
   * @return the base currency.
   */
  public String getBaseCurrency() {
    return baseCurrency;
  }

  /**
   * Sets the base currency.
   *
   * @param baseCurrency the base currency.
   */
  public void setBaseCurrency(String baseCurrency) {
    this.baseCurrency = baseCurrency;
  }

  /**
   * Returns the counter currency.
   *
   * @return the counter currency.
   */
  public String getCounterCurrency() {
    return counterCurrency;
  }

  /**
   * Sets the counter currency.
   *
   * @param counterCurrency the counter currency.
   */
  public void setCounterCurrency(String counterCurrency) {
    this.counterCurrency = counterCurrency;
  }

  /**
   * Returns is market enabled.
   *
   * @return is market enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets market enabled.
   *
   * @param enabled market enabled.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns trading strategy id.
   *
   * @return trading strategy id.
   */
  public String getTradingStrategyId() {
    return tradingStrategyId;
  }

  /**
   * Sets trading strategy id.
   *
   * @param tradingStrategyId trading strategy id.
   */
  public void setTradingStrategyId(String tradingStrategyId) {
    this.tradingStrategyId = tradingStrategyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarketConfig that = (MarketConfig) o;
    return Objects.equal(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("name", name)
        .add("baseCurrency", baseCurrency)
        .add("counterCurrency", counterCurrency)
        .add("enabled", enabled)
        .add("tradingStrategyId", tradingStrategyId)
        .toString();
  }
}
