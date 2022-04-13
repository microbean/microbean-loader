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
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #lowerBound()
 *
 * @see Provider
 */
public abstract class AbstractProvider implements Provider {


  /*
   * Instance fields.
   */


  private final Type lowerBound;


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link AbstractProvider} with {@code null} as its
   * {@linkplain #lowerBound() lower type bound}.
   *
   * @see #AbstractProvider(Type)
   *
   * @see #lowerBound()
   */
  protected AbstractProvider() {
    this(null);
  }
  
  /**
   * Creates a new {@link AbstractProvider}.
   *
   * @param lowerBound the {@linkplain #lowerBound() lower type bound}
   * of this {@link AbstractProvider}; may be, and often is, {@code
   * null}
   *
   * @see #lowerBound()
   */
  protected AbstractProvider(final Type lowerBound) {
    super();
    this.lowerBound = lowerBound;
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Type} representing the <strong>lower bound of
   * all possible {@linkplain Value values}</strong> {@linkplain
   * #get(Loader, Path) supplied} by this {@link AbstractProvider}.
   *
   * @return the lower type bound of this {@link AbstractProvider}
   * implementation, or {@code null} if its lower bound is the lowest
   * possible bound
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Provider
  public final Type lowerBound() {
    return this.lowerBound;
  }

}
