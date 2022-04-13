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
package org.microbean.loader.microprofile.config.configsource;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.Provider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * A {@link Provider} that uses a <a
 * href="https://github.com/eclipse/microprofile-config"
 * target="_parent">MicroProfile Config</a> {@link ConfigSource} and
 * <a href="https://microprofile.io/microprofile-config/"
 * target="_parent">MicroProfile Config</a> {@link Converter}s to
 * {@linkplain #get(Loader, Path) produce} {@link Value}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get(Loader, Path)
 *
 * @see ConfigSource#getValue(String)
 *
 * @see Converter#convert(String)
 */
public class ConfigSourceProvider implements Provider {

  private final ConfigSource configSource;

  private final Map<Type, Converter<?>> converters;

  /**
   * Creates a new {@link ConfigSourceProvider}.
   *
   * @param configSource the {@link ConfigSource} whose {@linkplain
   * ConfigSource#getValue(String) getValue(String)} method will be
   * used to back {@link Value}s produced by the {@link #get(Loader,
   * Path)} method; must not be {@code null}
   *
   * @param converters a {@link Map} of {@link Converter}s indexed by
   * their conversion {@link Type}; may be {@code null}
   *
   * @exception NullPointerException if {@code configSource} is {@code
   * null}
   */
  public ConfigSourceProvider(final ConfigSource configSource,
                              final Map<? extends Type, ? extends Converter<?>> converters) {
    super();
    this.configSource = Objects.requireNonNull(configSource, "configSource");
    if (converters == null || converters.isEmpty()) {
      this.converters = Map.of();
    } else {
      this.converters = Map.copyOf(converters);
    }
  }

  /**
   * Creates a new {@link ConfigSourceProvider}.
   *
   * @param configSource the {@link ConfigSource} whose {@linkplain
   * ConfigSource#getValue(String) getValue(String)} method will be
   * used to back {@link Value}s produced by the {@link #get(Loader,
   * Path)} method; must not be {@code null}
   *
   * @param converterRegistrations a {@link Collection} of {@link
   * ConverterRegistration}s; may be {@code null}
   *
   * @exception NullPointerException if {@code configSource} is {@code
   * null}
   *
   * @see ConverterRegistration
   */
  public ConfigSourceProvider(final ConfigSource configSource,
                              final Collection<? extends ConverterRegistration<?>> converterRegistrations) {
    super();
    this.configSource = Objects.requireNonNull(configSource, "configSource");
    if (converterRegistrations == null || converterRegistrations.isEmpty()) {
      this.converters = Map.of();
    } else {
      final Map<Type, ConverterRegistration<?>> map = new HashMap<>();
      for (final ConverterRegistration<?> cr : converterRegistrations) {
        final Type type = cr.type();
        ConverterRegistration<?> existing = map.get(type);
        if (existing == null || existing.priority() < cr.priority()) {
          map.put(type, cr);
        }
      }
      this.converters = Collections.unmodifiableMap(map);
    }
  }

  @Override // Provider
  public final Type lowerBound() {
    return this.converters.isEmpty() ? String.class : Object.class;
  }

  @Override // Provider
  public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    final Type pathType = absolutePath.qualified();
    // We could do fancy assignability checks here for converters, but
    // the MicroProfile Config API makes users expect an exact match.
    // That is, in the native MicroProfile Config API, users asking
    // for a CharSequence conversion expect that if there is not a
    // Converter registered for CharSequence explicitly, even if there
    // is one registered for, say, String, the call will fail.
    //
    // See https://github.com/eclipse/microprofile-config/issues/740
    // and https://github.com/eclipse/microprofile-config/issues/448.
    final Converter<?> converter = this.converters.get(pathType);
    if (converter == null) {
      if (pathType instanceof Class<?> c && c.isAssignableFrom(String.class)) {
        final Element<?> absolutePathLastElement = absolutePath.lastElement();   
        final Element<? extends Type> lastElement =
          Element.of(absolutePathLastElement.qualifiers(), String.class, absolutePathLastElement.name());
        final List<Element<?>> elements;
        if (absolutePath.size() == 1) {
          elements = List.of();
        } else {
          elements = new ArrayList<>(absolutePath.size() - 1);
          for (int i = 0; i < elements.size(); i++) {
            elements.add(absolutePath.get(i));
          }
        }
        final String configPath = configPath(absolutePath);
        return
          new Value<>(() -> {
              final String s = this.configSource.getValue(configPath);
              if (s == null) {
                // Sadly, among its many flaws is the fact that
                // MicroProfile Config conflates null with absence.
                throw new NoSuchElementException(configPath);
              }
              return s;
          }, Path.of(absolutePath.qualifiers(), elements, lastElement));
      }
    } else {
      final String configPath = configPath(absolutePath);
      return
        new Value<>(() -> {
            final String s = this.configSource.getValue(configPath);
            if (s == null) {
              // Sadly, among its many flaws is the fact that
              // MicroProfile Config conflates null with absence.
              throw new NoSuchElementException(configPath);
            }
            return converter.convert(s);
        }, absolutePath);
    }
    return null;
  }

  private static final String configPath(final Path<?> path) {
    return path.stream()
      .map(Element::name)
      .reduce((s1, s2) -> String.join(".", s1, s2))
      .orElse(null);
  }

  /**
   * A coupling of a {@link Type}, a priority (as defined in the <a
   * href="https://javadoc.io/static/org.eclipse.microprofile.config/microprofile-config-api/3.0.1/org/eclipse/microprofile/config/spi/Converter.html#priority">MicroProfile
   * Config</a> specification), and a {@link Converter}.
   *
   * <p>For convenience, this class also implements the {@link
   * Converter} interface itself.</p>
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see ConfigSourceProvider#ConfigSourceProvider(ConfigSource, Collection)
   */
  public static final class ConverterRegistration<T> implements Converter<T> {

    /**
     * The version of this class for {@link java.io.Serializable
     * serialization} purposes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The conversion type of this registration.
     *
     * @nullability This field is never {@code null}.
     */
    private final Type type;

    /**
     * The priority of this registration.
     */
    private final int priority;

    /**
     * A {@link Converter} to which all work is delegated.
     *
     * @nullability This field is never {@code null}.
     */
    private final Converter<? extends T> converter;

    /**
     * Creates a new {@link ConverterRegistration} with a priority of
     * {@code 100}, following the conventions of the <a
     * href="https://javadoc.io/static/org.eclipse.microprofile.config/microprofile-config-api/3.0.1/org/eclipse/microprofile/config/spi/Converter.html#priority">MicroProfile
     * Config</a> specification
     *
     * @param type the conversion type; must not be {@code null}
     *
     * @param converter the {@link Converter}; must not be {@code
     * null}
     *
     * @see #ConverterRegistration(Type, int, Converter)
     */
    public ConverterRegistration(final Type type, final Converter<? extends T> converter) {
      this(type, 100, converter);
    }

    /**
     * Creates a new {@link ConverterRegistration}.
     *
     * @param type the conversion type; must not be {@code null}
     *
     * @param priority the priority (as defined in the <a
     * href="https://javadoc.io/static/org.eclipse.microprofile.config/microprofile-config-api/3.0.1/org/eclipse/microprofile/config/spi/Converter.html#priority">MicroProfile
     * Config</a> specification)
     *
     * @param converter the {@link Converter}; must not be {@code
     * null}
     */
    public ConverterRegistration(final Type type, final int priority, final Converter<? extends T> converter) {
      super();
      this.type = Objects.requireNonNull(type, "type");
      this.priority = priority;
      this.converter = Objects.requireNonNull(converter, "converter");
    }

    /**
     * Returns the conversion type of this {@link
     * ConverterRegistration}.
     *
     * @return the conversion type
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final Type type() {
      return this.type;
    }

    /**
     * Returns the priority of this {@link ConverterRegistration}.
     *
     * @return the priority
     *
     * @idempotency This method is idempotent and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     */
    public final int priority() {
      return this.priority;
    }

    /**
     * Converts the supplied {@link String}-typed configuration value
     * to the conversion type {@linkplain #ConverterRegistration(Type,
     * int, Converter) supplied at construction time} using the {@link
     * Converter} {@linkplain #ConverterRegistration(Type, int,
     * Converter) supplied at construction time} and returns the
     * result of the conversion.
     *
     * @param configurationValue the value to convert; must not be
     * {@code null}
     *
     * @return the conversion
     *
     * @exception NullPointerException if {@code configurationValue}
     * is {@code null}
     *
     * @exception IllegalArgumentException if conversion could not
     * occur for any reason
     *
     * @nullability This method may return {@code null}.
     *
     * @idempotency No guarantees are made about idempotency or
     * determinism.
     *
     * @threadsafety No guarantees are made about thread safety.
     *
     * @see Converter#convert(String)
     */
    @Override // Converter<T>
    public final T convert(final String configurationValue) {
      return this.converter.convert(configurationValue);
    }
    
  }
  
}
