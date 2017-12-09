/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.google.common.base.MoreObjects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates (optional) Strategy Config Items.
 *
 * @author gazbert
 */
public final class StrategyConfigItems implements StrategyConfig {

    private Map<String, String> items = new HashMap<>();

    @Override
    public String getConfigItem(String key) {
        return items.get(key);
    }

    @Override
    public int getNumberOfConfigItems() {
        return items.size();
    }

    @Override
    public Set<String> getConfigItemKeys() {
        return Collections.unmodifiableSet(items.keySet());
    }

    public void setItems(Map<String, String> items) {
        this.items = items;
    }

    public Map<String, String> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("items", items)
                .toString();
    }
}
