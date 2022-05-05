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

import java.util.function.Supplier;

import org.microbean.invoke.FixedValueSupplier;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

/**
 * An {@link AbstractProvider} that provides access to {@linkplain
 * System#getenv(String) environment variables}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public class EnvironmentVariableProvider extends AbstractProvider {



  /*
   * Instance fields.
   */


  private final boolean flatKeys;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EnvironmentVariableProvider} that uses flat
   * keys.
   *
   * @see #EnvironmentVariableProvider(boolean)
   */
  public EnvironmentVariableProvider() {
    this(true);
  }

  /**
   * Creates a new {@link EnvironmentVariableProvider}.
   *
   * @param flatKeys whether the key for an environment variable is
   * derived from a {@link Path}'s {@linkplain Path#lastElement() last
   * element}'s {@linkplain Element#name() name} only
   */
  public EnvironmentVariableProvider(final boolean flatKeys) {
    super(String.class);
    this.flatKeys = flatKeys;
  }


  /*
   * Instance methods.
   */


  /**
   * If the supplied {@code absolutePath} has a {@linkplain
   * Path#size() size} of {@code 2} (the {@linkplain Path#root() root}
   * plus a single name), returns a {@linkplain FixedValueSupplier
   * deterministic <code>Supplier</code>} whose {@link Supplier#get()}
   * method returns the {@linkplain System#getenv(String) environment
   * variable} with that name, or {@code null} in all other cases.
   *
   * @param requestor the {@link Loader} requesting a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute}
   * {@link Path}; must not be {@code null}
   *
   * @return a {@linkplain FixedValueSupplier deterministic} {@link
   * Supplier} whose {@link Supplier#get()} method returns the
   * appropriate {@linkplain System#getenv(String) environment
   * variable} if the supplied {@code absolutePath} has a {@linkplain
   * Path#size() size} of {@code 2} (the {@linkplain Path#root() root}
   * plus a single name) and there actually is a {@linkplain
   * System#getenv(String) corresponding environment variable}; {@code
   * null} in all other cases
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic during
   * the lifetime of a Java virtual machine instance.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @exception NullPointerException if {@code requestor} or {@code
   * absolutePath} is {@code null}
   *
   * @see AbstractProvider#find(Loader, Path)
   *
   * @see System#getenv(String)
   */
  @Override // AbstractProvider
  protected Supplier<?> find(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());

    // On Unix systems, there is absolutely no question that the
    // environment is entirely immutable, even when probed via
    // System#getenv(String).  See
    // https://github.com/openjdk/jdk/blob/dfacda488bfbe2e11e8d607a6d08527710286982/src/java.base/unix/classes/java/lang/ProcessEnvironment.java#L67-L91.
    //
    // Things are ever so slightly more murky in Windows land.  As of
    // JDK 17, the environment there is also entirely immutable:
    // https://github.com/openjdk/jdk/blob/dfacda488bfbe2e11e8d607a6d08527710286982/src/java.base/windows/classes/java/lang/ProcessEnvironment.java#L257-L258
    // but the class is not as "immutable looking" as the Unix one and
    // it seems to be designed for updating in some cases.
    // Nevertheless, for the System#getenv(String) case, the
    // environment is immutable.
    //
    // TL;DR: System.getenv("foo") will always return a value for
    // "foo" if ever there was one, and will always return null if
    // there wasn't.

    final String value = System.getenv(key(absolutePath, this.flatKeys));
    if (value == null) {
      // Environment variables conflate null with absence.
      return null;
    }
    return FixedValueSupplier.of(value);
  }

  /**
   * Overrides the {@link AbstractProvider#path(Loader, Path)} method
   * to return a (relative) {@link Path} consisting solely of the
   * {@linkplain Path#lastElement() last element} of the supplied
   * {@code absolutePath}.
   *
   * @param requestor the {@link Loader} seeking a {@link Value}; must
   * not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a {@link Value} is being sought;
   * must not be {@code null}
   *
   * @return a {@link Path} that will be {@linkplain
   * Value#Value(Supplier, Path) used to build a <code>Value</code>}
   * to be returned by the {@link #get(Loader, Path)} method
   *
   * @nullability This method does not, but overrides may, return
   * {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see AbstractProvider#path(Loader, Path)
   */
  @Override // AbstractProvider
  protected <T extends Type> Path<T> path(final Loader<?> requestor, final Path<T> absolutePath) {
    return Path.of(absolutePath.lastElement());
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link String} representation of the supplied {@link Path}.
   *
   * @param path the {@link Path} in question; must not be {@code
   * null}
   *
   * @param flat whether the key is derived from the supplied {@link
   * Path}'s {@linkplain Path#lastElement() last element}'s
   * {@linkplain Element#name() name} only
   *
   * @return a {@link String} representation of the supplied {@link
   * Path}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  protected static final String key(final Path<?> path, final boolean flat) {
    if (flat) {
      return path.lastElement().name();
    } else {
      return path.stream()
        .map(Element::name)
        .filter(s1 -> !s1.isEmpty())
        .reduce((s1, s2) -> String.join(".", s1, s2))
        .orElse("");
    }
  }

}
