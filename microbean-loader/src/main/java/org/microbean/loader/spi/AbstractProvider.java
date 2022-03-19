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
package org.microbean.loader.spi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;

import org.microbean.qualifier.Qualifiers;

/**
 * A skeletal {@link Provider} implementation.
 *
 * @param <T> the upper bound of the types of objects produced by the
 * {@link #get(Loader, Path)} method

 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #upperBound()
 *
 * @see Provider
 */
public abstract class AbstractProvider<T> implements Provider {


  /*
   * Instance fields.
   */


  private final Type upperBound;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AbstractProvider}.
   */
  protected AbstractProvider() {
    super();
    this.upperBound = this.mostSpecializedParameterizedSuperclass().getActualTypeArguments()[0];
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Type} representing the <strong>upper bound of
   * all possible {@linkplain Value values}</strong> {@linkplain
   * #get(Loader, Path) supplied} by this {@link AbstractProvider}.
   *
   * <p>The value returned is harvested from the sole type argument
   * supplied to {@link AbstractProvider} by a concrete subclass.</p>
   *
   * @return the value of the sole type parameter of the {@link
   * AbstractProvider} class; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Provider
  public final Type upperBound() {
    return this.upperBound;
  }

  private final ParameterizedType mostSpecializedParameterizedSuperclass() {
    return mostSpecializedParameterizedSuperclass(AbstractProvider.class, this.getClass());
  }


  /*
   * Static methods.
   */


  private static final ParameterizedType mostSpecializedParameterizedSuperclass(final Class<?> stopClass, final Type type) {
    if (type == null || type == Object.class || type == stopClass) {
      return null;
    } else {
      final Class<?> erasure;
      if (type instanceof Class<?> c) {
        erasure = c;
      } else if (type instanceof ParameterizedType p) {
        erasure = (Class<?>)p.getRawType();
      } else {
        erasure = null;
      }
      if (erasure == null || erasure == Object.class || !(stopClass.isAssignableFrom(erasure))) {
        return null;
      } else {
        return type instanceof ParameterizedType p ? p : mostSpecializedParameterizedSuperclass(stopClass, erasure.getGenericSuperclass());
      }
    }
  }

}
