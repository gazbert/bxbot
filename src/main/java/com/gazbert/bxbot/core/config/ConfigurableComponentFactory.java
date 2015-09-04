/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015. Gareth Jon Lynch
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

package com.gazbert.bxbot.core.config;

import org.apache.log4j.Logger;

/*
 * Factory for creating user components defined in the bot configuration files. These are the
 * Exchange Adapters and Trading Strategies.
 */
public abstract class ConfigurableComponentFactory {
    private static final Logger LOG = Logger.getLogger(ConfigurableComponentFactory.class);

    // lockdown
    private ConfigurableComponentFactory() {
    }

    /*
     * Creates the given class using reflection and returns it.
     */
    public static <T> T createComponent(String componentClassName) {
        try {
            final Class componentClass = Class.forName(componentClassName);
            final Object rawComponentObject = componentClass.newInstance();

            if (LOG.isInfoEnabled()) {
                LOG.info("Created successfully the Component class for: " + componentClassName);
            }

            // should be one of ours
            return (T) rawComponentObject;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            final String errorMsg = "Failed to load and initialise Component class.";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }
}