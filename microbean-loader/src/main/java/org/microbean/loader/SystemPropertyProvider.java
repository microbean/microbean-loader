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
package org.microbean.loader;

import java.lang.reflect.Type;

import java.util.NoSuchElementException;
import java.util.Properties;

import java.util.function.Supplier;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaTypes;

/**
 * An {@link AbstractProvider} that can return {@link Value}s backed
 * by System properties.
 *
 * <p>While System properties are often casually assumed to be stable
 * and {@link String}-typed, the exact opposite is true: System
 * properties may contain arbitrarily-typed {@link Object}s under
 * arbitrarily-typed keys, and the properties themselves {@linkplain
 * System#setProperties(Properties) may be replaced} at any point.
 * This means that all {@link Value}s supplied by this {@link
 * SystemPropertyProvider} are {@linkplain Value#deterministic()
 * non-deterministic} and may change type and presence from one call
 * to another.</p>
 *
 * <p>It is also worth mentioning explicitly that, deliberately, no
 * type conversion of any System property value takes place in this
 * class.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see System#getProperty(String, String)
 *
 * @see System#getProperties()
 *
 * @see System#setProperties(Properties)
 *
 * @see Properties#getProperty(String, String)
 *
 * @see Value#deterministic()
 */
public final class SystemPropertyProvider extends AbstractProvider<Object> {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SystemPropertyProvider}.
   */
  public SystemPropertyProvider() {
    super();
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Value} suitable for the System property
   * represented by the supplied {@link Path}.
   *
   * <p>This method never returns {@code null}.  Its overrides may if
   * they wish.</p>
   *
   * <p>The {@linkplain Path.Element#name() name} of the {@linkplain
   * Path#lastElement() last element} of the supplied {@link Path} is
   * taken to be the name of the System property value to retrieve.
   * If the {@linkplain JavaTypes#erase(Type) type erasure} of the
   * supplied {@link Path}'s {@link Path#qualified() qualified()}
   * method {@linkplain Class#isAssignableFrom(Class) is assignable
   * from} {@link String String.class}, then calls will be made by the
   * returned {@link Value}'s {@link Value#get() get()} method to
   * {@link System#getProperty(String)} before simple calls to {@link
   * Properties#get(String) System.getProperties().get(String)}.</p>
   *
   * <p>Any {@link Value} returned by this method will have no
   * {@linkplain Value#Value(Supplier, Qualifiers, Path, Supplier,
   * boolean) defaults}, a {@link Qualifiers} equal to the return
   * value of {@link Qualifiers#of()}, a ({@linkplain Path#absolute()
   * relative}) {@link Path} equal to the supplied {@code
   * absolutePath}'s {@linkplain Path#lastElement() last element}, a
   * {@link Supplier} that is backed by System property access
   * machinery, and a value of {@code false} for its {@linkplain
   * Value#deterministic() deterministic property}.  If the supplied
   * {@link Path}'s {@linkplain Path#qualified() type} is not
   * assignable from that borne by a System property value, then the
   * {@link Value} will return {@code null} from its {@link
   * Value#get() get()} method in such a case, indicating that the
   * value is present but cannot be represented.  Overrides are
   * strongly encouraged to abide by these conditions.</p>
   *
   * @param requestor the {@link Loader} seeking a {@link Value}; must
   * not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a {@link Value} is being sought;
   * must not be {@code null}
   *
   * @return a {@link Value} suitable for the System property whose
   * name is represented by the supplied {@link Path}'s {@linkplain
   * Path#lastElement() last <code>Element</code>}'s {@linkplain
   * Path.Element#name() name}
   *
   * @exception NullPointerException if an argument for either
   * parameter is {@code null}
   *
   * @nullability This method never returns {@code null} but its
   * overrides may.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is idempotent and deterministic.
   * Overrides must be idempotent, but need not be deterministic.
   * {@link Value}s returned by this method or its overrides are
   * <em>not</em> guaranteed to be idempotent or deterministic.
   */
  @Override // AbstractProvider<Object>
  public <T> Value<T> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());

    if (absolutePath.size() == 2) { // root + name
      final Element<? extends Type> last = absolutePath.lastElement();
      final String name = last.name();
      if (!name.isEmpty()) {
        @SuppressWarnings("unchecked")
        final Class<T> pathTypeErasure = (Class<T>)JavaTypes.erase(absolutePath.qualified());
        final Supplier<T> s;
        if (CharSequence.class.isAssignableFrom(pathTypeErasure)) {
          s = () -> getCharSequenceAssignableSystemProperty(name, pathTypeErasure);
        } else {
          s = () -> getSystemProperty(name, pathTypeErasure);
        }
        return
          new Value<>(null, // no defaults
                      Qualifiers.of(),
                      Path.of(last),
                      s,
                      false); // not deterministic
      }
    }
    return null;
  }

  private static final <T> T getCharSequenceAssignableSystemProperty(final String propertyName, final Class<T> typeErasure) {
    assert CharSequence.class.isAssignableFrom(typeErasure) : "typeErasure: " + typeErasure.getName();
    Object value = System.getProperty(propertyName);
    if (value == null) {
      final Properties systemProperties = System.getProperties();
      value = systemProperties.getProperty(propertyName);
      if (value == null) {
        value = systemProperties.get(propertyName);
      }
    }
    if (typeErasure.isInstance(value)) {
      return typeErasure.cast(value);
    }
    throw new NoSuchElementException(propertyName);
  }

  private static final <T> T getSystemProperty(final String propertyName, final Class<T> typeErasure) {
    assert !CharSequence.class.isAssignableFrom(typeErasure) : "typeErasure: " + typeErasure.getName();
    final Object value = System.getProperties().get(propertyName);
    if (typeErasure.isInstance(value)) {
      return typeErasure.cast(value);
    }
    throw new NoSuchElementException(propertyName);
  }

}
