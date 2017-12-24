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

import java.util.HashMap;
import java.util.Map;

/**
 * Domain object representing a Strategy config.
 *
 * @author gazbert
 */
public class StrategyConfig {

    private String id;
    private String name;
    private String description;
    private String className;
    private String beanName;
    private Map<String, String> configItems = new HashMap<>();


    // required for Jackson
    public StrategyConfig() {
    }

    public StrategyConfig(StrategyConfig other) {
        this.id = other.id;
        this.name = other.name;
        this.description = other.description;
        this.className = other.className;
        this.beanName = other.beanName;
        this.configItems = other.configItems;
    }

    public StrategyConfig(String id, String name, String description, String className, String beanName, Map<String, String> configItems) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
