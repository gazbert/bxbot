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

package com.gazbert.bxbot.domain.strategy;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain object representing a Strategy config.
 *
 * @author gazbert
 */
@Schema
public class StrategyConfig {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "A unique identifier for the Strategy. Value must be an alphanumeric string. "
              + "Underscores and dashes are also permitted.")
  private String id;

  @Schema(description = "An optional friendly name for the Strategy.")
  private String name;

  @Schema(description = "An optional description of the Strategy.")
  private String description;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "The fully qualified Strategy Class name, "
              + "e.g. com.me.mybot.SuperStrat. Must be specified if beanName not set.")
  private String className;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The Strategy Spring Bean name. " + "Must be specified if className not set.")
  private String beanName;

  @Schema(description = "The optional Strategy config items.")
  private Map<String, String> configItems = new HashMap<>();

  /** Creates a new StrategyConfig. Required by ConfigurableComponentFactory */
  public StrategyConfig() {
    // noimpl
  }

  /**
   * Creates a StrategyConfig from an existing one.
   *
   * @param other the Strategy Config to copy.
   */
  public StrategyConfig(StrategyConfig other) {
    this.id = other.id;
    this.name = other.name;
    this.description = other.description;
    this.className = other.className;
    this.beanName = other.beanName;
    this.configItems = other.configItems;
  }

  /**
   * Creates a new StrategyConfig.
   *
   * @param id the strategy ID.
   * @param name the strategy name.
   * @param description the strategy description.
   * @param className the strategy class name.
   * @param beanName the strategy bean name.
   * @param configItems the strategy config.
   */
  public StrategyConfig(
      String id,
      String name,
      String description,
      String className,
      String beanName,
      Map<String, String> configItems) {

    this.id = id;
    this.name = name;
    this.description = description;
    this.className = className;
    this.beanName = beanName;
    this.configItems = configItems;
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
   * Returns the description.
   *
   * @return the description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the classname.
   *
   * @return the classname.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Sets the classname.
   *
   * @param className the classname.
   */
  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * Returns the bean name.
   *
   * @return the bean name.
   */
  public String getBeanName() {
    return beanName;
  }

  /**
   * Sets the bean name.
   *
   * @param beanName the bean name.
   */
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  /**
   * Returns the config items.
   *
   * @return the config items.
   */
  public Map<String, String> getConfigItems() {
    return configItems;
  }

  /**
   * Sets the config items.
   *
   * @param configItems the config items.
   */
  public void setConfigItems(Map<String, String> configItems) {
    this.configItems = configItems;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StrategyConfig that = (StrategyConfig) o;
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
        .add("description", description)
        .add("className", className)
        .add("beanName", beanName)
        .add("configItems", configItems)
        .toString();
  }
}
