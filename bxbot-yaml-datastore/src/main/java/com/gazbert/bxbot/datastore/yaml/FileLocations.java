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

package com.gazbert.bxbot.datastore.yaml;

/**
 * Locations of YAML files for the entities.
 *
 * @author gazbert
 */
public final class FileLocations {

  /** Location of email alerts YML file. */
  public static final String EMAIL_ALERTS_CONFIG_YAML_FILENAME = "config/email-alerts.yaml";

  /** Location of engine YML file. */
  public static final String ENGINE_CONFIG_YAML_FILENAME = "config/engine.yaml";

  /** Location of exchange YML file. */
  public static final String EXCHANGE_CONFIG_YAML_FILENAME = "config/exchange.yaml";

  /** Location of markets YML file. */
  public static final String MARKETS_CONFIG_YAML_FILENAME = "config/markets.yaml";

  /** Location of strategies YML file. */
  public static final String STRATEGIES_CONFIG_YAML_FILENAME = "config/strategies.yaml";

  private FileLocations() {
    // noimpl
  }
}
