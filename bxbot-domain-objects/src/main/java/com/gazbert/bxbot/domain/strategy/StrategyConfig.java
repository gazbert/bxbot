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
 * 表示策略配置的域对象。
 *
 * @author gazbert
 */
@Schema
public class StrategyConfig {

    @Schema(
            required = true,
            description =
                    "A unique identifier for the Strategy. Value must be an alphanumeric string. 策略的唯一标识符。值必须是字母数字字符串。"
                            + "Underscores and dashes are also permitted. 也允许使用下划线和破折号。")
    private String id;

    @Schema(description = "An optional friendly name for the Strategy. 策略的可选友好名称。")
    private String name;

    @Schema(description = "An optional description of the Strategy. 策略的可选描述。")
    private String description;

    @Schema(required = true, description = "The fully qualified Strategy Class name, e.g. com.me.mybot.SuperStrat. Must be specified if beanName not set." +
            "完全限定的策略类名称，例如com.me.mybot.SuperStrat。如果未设置 beanName，则必须指定。")
    private String className;

    @Schema(required = true, description = "The Strategy Spring Bean name. 策略 Spring Bean 名称。"
            + "Must be specified if className not set. 如果未设置 className，则必须指定。")
    private String beanName;

    @Schema(description = "The optional Strategy config items. 可选的策略配置项。")
    private Map<String, String> configItems = new HashMap<>();

    // Required by ConfigurableComponentFactory // ConfigurableComponentFactory 需要
    public StrategyConfig() {
    }

    /**
     * Creates a StrategyConfig from an existing one.
     * 从现有配置创建策略配置。
     *
     * @param other     the Strategy Config to copy.
     * @param 要复制的策略配置。
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
     * 创建一个新的策略配置。
     *
     * @param id          the strategy ID.
     *                    策略 ID。
     * @param name        the strategy name.
     *                    策略名称。
     * @param description the strategy description.
     *                    策略描述。
     * @param className   the strategy class name.
     *                    策略类名称。
     * @param beanName    the strategy bean name.
     *                    策略 bean 名称。
     * @param configItems the strategy config.
     *                    策略配置。
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Map<String, String> getConfigItems() {
        return configItems;
    }

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
