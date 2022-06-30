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

package com.gazbert.bxbot.core.util;

import java.lang.reflect.InvocationTargetException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * //工厂，用于创建bot配置文件中定义的用户组件。这些都是目前
 交易所适配器和交易策略。
 * Factory for creating user components defined in the bot configuration files. These are currently
 * the Exchange Adapters and Trading Strategies.
 *
 * @author gazbert
 */
public abstract class ConfigurableComponentFactory {

  private static final Logger LOG = LogManager.getLogger();

  private ConfigurableComponentFactory() {
  }

  /**
   * Loads and instantiates a given class and returns it.
   * 加载并实例化给定的类并返回它。
   *
   * @param componentClassName the class to instantiate.
   *                           要实例化的类。
   *
   * @param <T> the type of the class to instantiate.
   *           要实例化的类的类型。
   *
   * @return the instantiated class.
   *    实例化的类。
   */
  @SuppressWarnings("unchecked")
  public static <T> T createComponent(String componentClassName) {
    try {
      final Class componentClass = Class.forName(componentClassName);
      final Object rawComponentObject = componentClass.getDeclaredConstructor().newInstance();
      LOG.info(() -> "Successfully created the Component class for: 成功创建了 Component 类：" + componentClassName);
      return (T) rawComponentObject;

    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException e) {
      final String errorMsg = "Failed to load and initialise Component class. 未能加载和初始化组件类。";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    }
  }
}
