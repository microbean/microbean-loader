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
import java.util.Map;
import java.util.Objects;

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

/**
 * An {@link AbstractProvider} that uses a {@link Config} to
 * {@linkplain #get(Loader, Path) produce} {@link Value}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get(Loader, Path)
 */
public class TypesafeConfigHoconProvider extends AbstractProvider {


  /*
   * Instance fields.
   */


  private final Supplier<? extends Config> configSupplier;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link TypesafeConfigHoconProvider} that reads
   * solely from a classpath resource named {@code application.conf},
   * using the {@link Thread#getContextClassLoader() context
   * classloader}.
   *
   * <p>No stacking of other configurations is performed.</p>
   *
   * @see #TypesafeConfigHoconProvider(ClassLoader, String)
   */
  public TypesafeConfigHoconProvider() {
    this(null, Thread.currentThread().getContextClassLoader(), "application.conf");
  }

  /**
   * Creates a new {@link TypesafeConfigHoconProvider} that reads
   * solely from a classpath resource bearing the supplied name using
   * the {@linkplain Thread#getContextClassLoader() context
   * classloader}.
   *
   * <p>No stacking of other configurations is performed.</p>
   *
   * @param resourceName the name of the classpath resource; must not
   * be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   *
   * @see #TypesafeConfigHoconProvider(ClassLoader, String)
   */
  public TypesafeConfigHoconProvider(final String resourceName) {
    this(null, Thread.currentThread().getContextClassLoader(), resourceName);
  }

  /**
   * Creates a new {@link TypesafeConfigHoconProvider} that reads
   * solely from a classpath resource bearing the supplied name using
   * the supplied {@link ClassLoader}.
   *
   * <p>No stacking of other configurations is performed.</p>
   *
   * @param cl the {@link ClassLoader}; may be {@code null} in which
   * case the system classloader will be used instead, normally via
   * {@link ClassLoader#getSystemResourceAsStream(String)}
   *
   * @param resourceName the name of the classpath resource; must not
   * be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   */
  public TypesafeConfigHoconProvider(final ClassLoader cl, final String resourceName) {
    this(null, cl, resourceName);
  }

  /**
   * Creates a new {@link TypesafeConfigHoconProvider} that reads
   * solely from a classpath resource bearing the supplied name using
   * the supplied {@link ClassLoader}.
   *
   * <p>No stacking of other configurations is performed.</p>
   *
   * @param lowerBound the {@linkplain #lowerBound() lower type bound}
   * of this {@link TypesafeConfigHoconProvider} implementation; may
   * be {@code null}
   *
   * @param cl the {@link ClassLoader}; may be {@code null} in which
   * case the system classloader will be used instead, normally via
   * {@link ClassLoader#getSystemResourceAsStream(String)}
   *
   * @param resourceName the name of the classpath resource; must not
   * be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   */
  public TypesafeConfigHoconProvider(final Type lowerBound, final ClassLoader cl, final String resourceName) {
    super(lowerBound);
    Objects.requireNonNull(resourceName, "resourceName");
    this.configSupplier = new CachingSupplier<>(() -> produceConfig(cl, resourceName));    
  }


  /*
   * Instance methods.
   */


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
        final Map<? extends String, ?> unwrapped = config.root().unwrapped();
        if (CovariantSemantics.INSTANCE.assignable(absolutePath.qualified(), unwrapped.getClass())) {
          temp = new Value<>(unwrapped, absolutePath);
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
      final Class<?> unwrappedClass = unwrapped == null ? null : unwrapped.getClass();
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
        // See
        // https://javadoc.io/static/com.typesafe/config/1.4.2/com/typesafe/config/ConfigValue.html#unwrapped--
        assert unwrapped instanceof Map;
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


  /*
   * Static methods.
   */


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


  /*
   * Inner and nested classes.
   */


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
