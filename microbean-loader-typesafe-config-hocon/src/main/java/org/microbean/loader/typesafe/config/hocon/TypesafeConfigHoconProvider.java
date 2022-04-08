/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.loader.typesafe.config.hocon;

import java.lang.reflect.Type;

import java.util.List;

import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import com.typesafe.config.ConfigUtil;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.microbean.invoke.CachingSupplier;
import org.microbean.invoke.FixedValueSupplier;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaTypes;

import org.microbean.type.Type.CovariantSemantics;

public class TypesafeConfigHoconProvider extends AbstractProvider<Object> {

  private final CachingSupplier<Config> configSupplier;

  public TypesafeConfigHoconProvider() {
    super();
    this.configSupplier = new CachingSupplier<>(TypesafeConfigHoconProvider::produceConfig);
  }

  @Override // AbstractProvider<Object>
  public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    final Value<?> returnValue;
    // TODO: doesn't handle qualifiers yet
    final String configPath = configPath(absolutePath);
    final Config config = this.config();
    if (configPath.isEmpty()) {
      Value<?> temp = null;
      try {
        temp = new Value<>(ConfigBeanFactory.create(config, JavaTypes.erase(absolutePath.qualified())), absolutePath);
      } catch (final ConfigException.BadBean | ConfigException.ValidationFailed e) {
        final Object unwrapped = config.root().unwrapped();
        final Class<?> unwrappedClass = unwrapped.getClass();
        if (CovariantSemantics.INSTANCE.assignable(absolutePath.qualified(), unwrappedClass)) {
          temp = new Value<>(unwrappedClass.cast(unwrapped), absolutePath);
        }
      } finally {
        returnValue = temp;
      }
    } else if (config.hasPathOrNull(configPath)) {
      final ConfigValue configValue;
      if (config.getIsNull(configPath)) {
        configValue = ConfigValueFactory.fromAnyRef(null);
      } else {
        configValue = config.getValue(configPath);
      }
      final Object unwrapped = configValue.unwrapped();
      final Class<?> unwrappedClass = unwrapped.getClass();
      switch (configValue.valueType()) {
      case BOOLEAN:
      case LIST:
      case NUMBER:
      case STRING:
        if (CovariantSemantics.INSTANCE.assignable(absolutePath.qualified(), unwrappedClass)) {
          returnValue = new Value<>(unwrappedClass.cast(unwrapped), absolutePath);
        } else {
          returnValue = null;
        }
        break;
      case NULL:
        returnValue = new Value<>(absolutePath);
        break;
      case OBJECT:
        Value<?> temp = null;
        try {
          temp =
            new Value<>(ConfigBeanFactory.create(((ConfigObject)configValue).toConfig(),
                                                 JavaTypes.erase(absolutePath.qualified())),
                        absolutePath);
        } catch (final ConfigException.BadBean | ConfigException.ValidationFailed e) {
          if (CovariantSemantics.INSTANCE.assignable(absolutePath.qualified(), unwrappedClass)) {
            temp = new Value<>(unwrappedClass.cast(unwrapped), absolutePath);
          }
        } finally {
          returnValue = temp;
        }
        break;
      default:
        throw new AssertionError();
      }
    } else {
      returnValue = null;
    }
    return returnValue;
  }

  private final Config config() {
    return this.configSupplier.get();
  }

  private static final String configPath(final Path<?> path) {
    return path.stream()
      .map(Element::name)
      .reduce(ConfigUtil::joinPath)
      .orElse(null);
  }

  private static final Config produceConfig() {
    return ConfigFactory.load(Thread.currentThread().getContextClassLoader(),
                              ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF));
  }

}
