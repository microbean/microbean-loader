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

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

/**
 * An {@link AbstractProvider} that provides access to {@linkplain
 * System#getenv(String) environment variables}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public final class EnvironmentVariableProvider extends AbstractProvider<String> {


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link EnvironmentVariableProvider}.
   */
  public EnvironmentVariableProvider() {
    super();
  }


  /*
   * Instance methods.
   */
  

  /**
   * If the supplied {@code absolutePath} has a {@linkplain
   * Path#size() size} of {@code 2} (the {@linkplain Path#root() root}
   * plus a single name), returns a {@linkplain Value#deterministic()
   * deterministic <code>Value</code>} whose {@link Value#get()}
   * method returns the {@linkplain System#getenv(String) environment
   * variable} with that name, or {@code null} in all other cases.
   *
   * @param requestor the {@link Loader} requesting a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute}
   * {@link Path}; must not be {@code null}
   *
   * @return a {@linkplain Value#deterministic() deterministic} {@link
   * Value} whose {@link Value#get()} method returns the appropriate
   * {@linkplain System#getenv(String) environment variable} if the
   * supplied {@code absolutePath} has a {@linkplain Path#size() size}
   * of {@code 2} (the {@linkplain Path#root() root} plus a single
   * name) and there actually is a {@linkplain System#getenv(String)
   * corresponding environment variable}; {@code null} in all other
   * cases
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
   */
  @Override // AbstractProvider<String>
  public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
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

    if (absolutePath.size() == 2) { // 2: root plus a single name
      final String name = absolutePath.lastElement().name();
      if (!name.isEmpty()) {
        @SuppressWarnings("unchecked")
        final String value = System.getenv(name);
        if (value != null) {
          return
            new Value<>(null, // no defaults
                        absolutePath,
                        () -> value,
                        true); // deterministic
        }
      }
    }
    return null;
  }

}
