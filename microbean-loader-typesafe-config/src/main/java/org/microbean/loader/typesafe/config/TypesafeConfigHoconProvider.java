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
package org.microbean.loader.typesafe.config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import java.lang.reflect.Type;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import java.util.List;

import java.util.function.Supplier;

import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigResolver;
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

  private final Supplier<? extends Config> configSupplier;

  public TypesafeConfigHoconProvider() {
    this(Thread.currentThread().getContextClassLoader(), "application.conf");
  }

  public TypesafeConfigHoconProvider(final String resourceName) {
    this(Thread.currentThread().getContextClassLoader(), resourceName);
  }

  public TypesafeConfigHoconProvider(final ClassLoader cl, final String resourceName) {
    super();
    this.configSupplier = new CachingSupplier<>(() -> produceConfig(cl, resourceName));
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

  private static final Config produceConfig(final ClassLoader cl, final String resourceName) {
    final InputStream inputStream =
      cl == null ? ClassLoader.getSystemResourceAsStream(resourceName) : cl.getResourceAsStream(resourceName);
    Reader reader;
    if (inputStream == null) {
      try {
        reader = Files.newBufferedReader(Paths.get(System.getProperty("user.dir", "."), resourceName));
      } catch (final FileNotFoundException /* this probably isn't thrown */ | NoSuchFileException e) {
        return ConfigFactory.empty();
      } catch (final IOException ioException) {
        throw new UncheckedIOException(ioException.getMessage(), ioException);
      }
    } else if (inputStream instanceof BufferedInputStream) {
      reader = new InputStreamReader(inputStream);
    } else {
      reader = new BufferedReader(new InputStreamReader(inputStream));
    }
    return ConfigFactory.parseReader(reader,
                                     ConfigParseOptions.defaults().setSyntaxFromFilename(resourceName))
      .resolve(ConfigResolveOptions.noSystem().appendResolver(new Resolver()));
  }

  private static final class Resolver implements ConfigResolver {

    private final ConfigResolver fallback;

    private Resolver() {
      this(null);
    }

    private Resolver(final ConfigResolver fallback) {
      super();
      this.fallback = fallback;
    }

    @Override // ConfigResolver
    public final ConfigValue lookup(final String path) {
      // Called only for substitutions that are not internal to the Config.
      return
        ConfigValueFactory.fromAnyRef(Loader.loader()
                                      .load(Path.of(Object.class, ConfigUtil.splitPath(path)))
                                      .orElseGet(() -> this.fallback == null ? null : this.fallback.lookup(path)));
    }

    @Override // ConfigResolver
    public final ConfigResolver withFallback(final ConfigResolver fallback) {
      if (fallback == null || fallback.equals(this.fallback)) {
        return this;
      }
      return new Resolver(fallback);
    }

  }

}
