/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.datastore;

/**
 * Locations of XML and XSD files for the entities.
 *
 * @author gazbert
 */
public final class FileLocations {

    /*
     * Location of the XML config files relative to project/installation root.
     */
    public static final String EMAIL_ALERTS_CONFIG_XML_FILENAME = "config/email-alerts.xml";
    public static final String ENGINE_CONFIG_XML_FILENAME = "config/engine.xml";
    public static final String EXCHANGE_CONFIG_XML_FILENAME = "config/exchange.xml";
    public static final String MARKETS_CONFIG_XML_FILENAME = "config/markets.xml";
    public static final String STRATEGIES_CONFIG_XML_FILENAME = "config/strategies.xml";

    /*
     * XSD schema files for validating the XML config - their location in the main/resources folder.
     */
    public static final String EMAIL_ALERTS_CONFIG_XSD_FILENAME = "com/gazbert/bxbot/datastore/config/email-alerts.xsd";
    public static final String ENGINE_CONFIG_XSD_FILENAME = "com/gazbert/bxbot/datastore/config/engine.xsd";
    public static final String EXCHANGE_CONFIG_XSD_FILENAME = "com/gazbert/bxbot/datastore/config/exchange.xsd";
    public static final String MARKETS_CONFIG_XSD_FILENAME = "com/gazbert/bxbot/datastore/config/markets.xsd";
    public static final String STRATEGIES_CONFIG_XSD_FILENAME = "com/gazbert/bxbot/datastore/config/strategies.xsd";

    private FileLocations() {
    }
}
