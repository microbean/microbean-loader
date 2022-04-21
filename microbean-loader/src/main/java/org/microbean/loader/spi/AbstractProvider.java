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

import java.util.function.Supplier;

import org.microbean.development.annotation.OverridingEncouraged;

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

  /**
   * Implements the {@link Provider#get(Loader, Path)} method to first
   * call the {@link #find(Loader, Path)} method, and, if necessary,
   * the {@link #path(Loader, Path)} method, and to then return a
   * {@link Value} made up of the relevant return values.
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @return a {@link Value} more or less suitable for the combination
   * of the supplied {@link Loader} and {@link Path}, or {@code
   * null} if there is no such {@link Value} now <strong>and if there
   * never will be such a {@link Value}</strong> for the supplied
   * arguments
   *
   * @exception NullPointerException if either {@code requestor} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#absolute() is not absolute}, or if {@link
   * Path#startsWith(Path)
   * !absolutePath.startsWith(requestor.absolutePath())}, or if {@link
   * Path#equals(Object)
   * absolutePath.equals(requestor.absolutePath())}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent, but not necessarily
   * deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see Provider#get(Loader, Path)
   *
   * @see #find(Loader, Path)
   *
   * @see #path(Loader, Path)
   */
  @Override // Provider
  public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    final Supplier<?> s = this.find(requestor, absolutePath);
    if (s != null) {
      if (s instanceof Value<?> v) {
        return v;
      }
      final Path<? extends Type> p = this.path(requestor, absolutePath);
      if (p != null) {
        return new Value<>(s, p);
      }
    }
    return null;
  }

  /**
   * Returns a {@link Supplier} suitable for the supplied {@link
   * Loader} and {@link Path}, or {@code null} if there is no such
   * {@link Supplier} now <strong>and if there never will be such a
   * {@link Supplier}</strong> for the supplied arguments.
   *
   * <p>Overrides of this method must not call the {@link #get(Loader,
   * Path)} method or undefined behavior (such as an infinite loop)
   * may result.</p>
   *
   * @param requestor the {@link Loader} seeking a {@link Value} (as
   * originally passed to the {@link #get(Loader, Path)} method); must
   * not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value (as originally passed to the {@link #get(Loader,
   * Path)} method); must not be {@code null}
   *
   * @return a {@link Supplier} more or less suitable for the
   * combination of the supplied {@link Loader} and {@link Path}, or
   * {@code null} if there is no such {@link Supplier} now <strong>and
   * if there never will be such a {@link Supplier}</strong> for the
   * supplied arguments
   *
   * @exception NullPointerException if either {@code requestor} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#absolute() is not absolute}, or if {@link
   * Path#startsWith(Path)
   * !absolutePath.startsWith(requestor.absolutePath())}, or if {@link
   * Path#equals(Object)
   * absolutePath.equals(requestor.absolutePath())}
   *
   * @nullability Overrides of this method may return {@code null}, in
   * which case the {@link #get(Loader, Path)} method will return
   * {@code null} as well.
   *
   * @idempotency Overrides of this method must be idempotent but are
   * not assumed to be deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @see #get(Loader, Path)
   */
  protected abstract Supplier<?> find(final Loader<?> requestor, final Path<? extends Type> absolutePath);

  /**
   * Returns a {@link Path} that will be {@linkplain
   * Value#Value(Supplier, Path) used to build a <code>Value</code>}
   * to be returned by the {@link #get(Loader, Path)} method.
   *
   * <p>This method does not, and its overrides must not, call the
   * {@link #get(Loader, Path)} method or undefined behavior (such as
   * an infinite loop) may result.</p>
   *
   * <p>The default implementation of this method returns the supplied
   * {@code absolutePath}.</p>
   *
   * <p>This method is called by the {@link #get(Loader, Path)} method
   * only when a call to the {@link #find(Loader, Path)} method
   * returns a non-{@code null} value.</p>
   *
   * @param <T> the type of both the supplied and returned {@link
   * Path}s
   *
   * @param requestor requestor the {@link Loader} seeking a {@link
   * Value} (as originally passed to the {@link #get(Loader, Path)}
   * method); must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value (as originally passed to the {@link #get(Loader,
   * Path)} method); must not be {@code null}
   *
   * @return a {@link Path} that will be {@linkplain
   * Value#Value(Supplier, Path) used to build a <code>Value</code>}
   * to be returned by the {@link #get(Loader, Path)} method; if
   * {@code null}, then the {@link #get(Loader, Path)} method will
   * return {@code null} as well
   *
   * @exception NullPointerException if either {@code requestor} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#absolute() is not absolute}, or if {@link
   * Path#startsWith(Path)
   * !absolutePath.startsWith(requestor.absolutePath())}, or if {@link
   * Path#equals(Object)
   * absolutePath.equals(requestor.absolutePath())}
   *
   * @nullability This method does not, but its overrides may, return
   * {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see #get(Loader, Path)
   */
  @OverridingEncouraged
  protected <T extends Type> Path<T> path(final Loader<?> requestor, final Path<T> absolutePath) {
    return absolutePath;
  }

}
