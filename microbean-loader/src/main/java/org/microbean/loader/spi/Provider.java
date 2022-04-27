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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ServiceLoader;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;

/**
 * A service provider of {@link Value}s that might be suitable for a
 * {@link Loader} implementation to return.
 *
 * <p>{@link Provider} instances are subordinate to {@link
 * org.microbean.loader.DefaultLoader}.</p>
 *
 * <p>Any {@link Provider} implementation must have a {@code public}
 * constructor that has no arguments.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get(Loader, Path)
 *
 * @see AbstractProvider
 *
 * @see org.microbean.loader.DefaultLoader
 */
@FunctionalInterface
public interface Provider {


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Type} representing the <em>lower type bound</em>
   * of all possible {@linkplain Value values} {@linkplain
   * #get(Loader, Path) supplied} by this {@link Provider}.
   *
   * <p>Often the value returned by implementations of this method is
   * no more specific than the lowest possible type, which is {@code
   * null}, meaning that the {@link Provider} has a chance of
   * {@linkplain #get(Loader, Path) producing} a {@link Value} for any
   * requested type.</p>
   *
   * <p>A return value of, for example, {@link String String.class}
   * indicates that the {@link Provider} may satisfy requests for
   * {@link String String.class}, or any of its supertypes (such as
   * {@link CharSequence} or {@link Object}), but cannot satisfy
   * requests for {@link Integer Integer.class}, for example.</p>
   *
   * <p>A return value of {@link Object Object.class} would be
   * extremely unusual and would indicate a maximally opaque type,
   * i.e. only requests for <em>exactly</em> {@link Object
   * Object.class} have the possibility of being satisfied by this
   * {@link Provider}.  Such a return value is possible, but rarely
   * used, and {@link Provider} implementations are urged to consider
   * returning a different {@link Type}.</p>
   *
   * <p>Note that this method is used solely to help eliminate types
   * from consideration, not permit them.  That is, although {@link
   * Provider} may indicate via an implementation of this method that
   * a given {@link Type} is suitable, it is not thereby obliged to
   * return a {@link Value} corresponding to it from its {@link
   * #get(Loader, Path) get(Loader, Path)} method implementation.</p>
   *
   * <p>The default implementation of this method returns {@code
   * null}.  Many {@link Provider} implementations will choose not to
   * override this method.</p>
   *
   * @return a {@link Type} representing the lower type bound of all
   * possible {@linkplain Value values} {@linkplain #get(Loader, Path)
   * supplied} by this {@link Provider}, or {@code null} to indicate
   * the lowest possible type bound
   *
   * @nullability This method always does, and overrides often may,
   * return {@code null}.
   *
   * @idempotency This method is, and overrides of this method must
   * be, idempotent and deterministic.
   *
   * @threadsafety This method is, and overrides of this method must
   * be, safe for concurrent use by multiple threads.
   */
  public default Type lowerBound() {
    return null; // the lowest possible type, assignable to all others
  }

  /**
   * Returns a {@link Value} suitable for the supplied {@link Loader}
   * and {@link Path}, <strong>or {@code null} if there is no such
   * {@link Value} now and if there never will be such a {@link
   * Value}</strong> for the supplied arguments.
   *
   * <p>In addition to the other requirements described here, the
   * following assertions will be (and must be) true when this method
   * is called in the normal course of events:</p>
   *
   * <ul>
   *
   * <li>{@code assert absolutePath.isAbsolute();}</li>
   *
   * <li>{@code assert
   * absolutePath.startsWith(requestor.absolutePath());}</li>
   *
   * <li>{@code assert
   * !absolutePath.equals(requestor.absolutePath());}</li>
   *
   * </ul>
   *
   * <p>If any caller does not honor these requirements, undefined
   * behavior may result.</p>
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @return a {@link Value} more or less suitable for the combination
   * of the supplied {@link Loader} and {@link Path}, <strong>or
   * {@code null} if there is no such {@link Value} now and if there
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
   * @nullability Implementations of this method may return {@code
   * null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * but are not assumed to be deterministic.
   */
  public Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath);

}
