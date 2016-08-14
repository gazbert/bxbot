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

package com.gazbert.bxbot.core.config.strategy;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Domain object representing a Strategy config.
 * <p>
 * The configuration is loaded from the strategies.xml file.
 *
 * @author gazbert
 * @since 20/07/2016
 */
public class StrategyConfig {

    /*
     * Location of the XML config files relative to project/installation root.
     */
    public static final String STRATEGIES_CONFIG_XML_FILENAME = "config/strategies.xml";

    /*
     * XSD schema files for validating the XML config - their location in the main/resources folder.
     */
    public static final String STRATEGIES_CONFIG_XSD_FILENAME = "com/gazbert/bxbot/core/config/strategy/strategies.xsd";

    private String id;
    private String label;
    private String description;
    private String className;
    private StrategyConfigItems configItems;


    // required for Jackson
    public StrategyConfig() {
    }

    public StrategyConfig(String id, String label, String description, String className, StrategyConfigItems configItems) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.className = className;
        this.configItems = configItems;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public StrategyConfigItems getConfigItems() {
        return configItems;
    }

    public void setConfigItems(StrategyConfigItems configItems) {
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
                .add("label", label)
                .add("description", description)
                .add("className", className)
                .add("configItems", configItems)
                .toString();
    }
}
