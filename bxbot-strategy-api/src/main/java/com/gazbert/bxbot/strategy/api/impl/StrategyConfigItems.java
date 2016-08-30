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

package com.gazbert.bxbot.strategy.api.impl;

import com.google.common.base.MoreObjects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain object representing (optional) Strategy Config Items.
 *
 * @author gazbert
 */
public final class StrategyConfigItems implements com.gazbert.bxbot.strategy.api.StrategyConfig {

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

        // return HashSet else Jackson barfs with: handleHttpMessageNotReadable() - Failed to read HTTP message:
        // (was java.lang.UnsupportedOperationException) java.util.LinkedKeySet[2])
        return new HashSet<>(items.keySet());
    }

    public void addConfigItem(String key, String value) {
        items.put(key, value);
    }

    public Map<String, String> getItems() {
        return items;
    }

    public void setItems(Map<String, String> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("items", items)
                .toString();
    }
}