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
package org.microbean.loader.api;

import java.lang.reflect.Type;

import java.util.List;
import java.util.ServiceLoader;

import org.microbean.development.annotation.EntryPoint;
import org.microbean.development.annotation.OverridingDiscouraged;

import org.microbean.invoke.OptionalSupplier;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaType.Token;

public interface Loader<T> extends OptionalSupplier<T> {

  /**
   * Returns the {@link Loader} that is the parent of this {@link
   * Loader}.
   *
   * <p>The {@linkplain #root() root <code>Loader</code>} is defined
   * to be the only one whose {@link #parent()} method returns itself.
   * It follows that in general a {@link Loader} implementation should
   * not return {@code this}.  See {@link #isRoot()} for details.</p>
   *
   * @return the {@link Loader} that is the parent of this {@link
   * Loader}
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @see #root()
   *
   * @see #isRoot()
   */
  public Loader<?> parent();

  /**
   * Returns the <strong>{@linkplain Path#absolute()
   * absolute}</strong> representation of the {@link Path} that was
   * supplied to an invocation of this {@link Loader}'s {@linkplain
   * #parent() parent <code>Loader</code>}'s {@link #load(Path)}
   * method and that resulted in this {@link Loader} being {@linkplain
   * #load(Path) loaded} as a result.
   *
   * <p>Implementations of this method must return an {@linkplain
   * Path#absolute() absolute} {@link Path} that adheres to these
   * requirements or undefined behavior will result.</p>
   *
   * @return the <strong>{@linkplain Path#absolute()
   * absolute}</strong> representation of the {@link Path} that was
   * supplied to an invocation of this {@link Loader}'s {@linkplain
   * #parent() parent <code>Loader</code>}'s {@link #load(Path)}
   * method and that resulted in this {@link Loader} being {@linkplain
   * #load(Path) loaded} as a result
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @see #load(path)
   *
   * @see Path#absolute()
   */
  public Path<? extends Type> path(); // must be absolute

  /**
   * Uses the information encoded in the supplied {@link Path} to load
   * and return the {@link Loader} logically found at that location,
   * following additional contractual requirements defined below.
   *
   * <p>Any {@link Loader} returned by an implementation of this method:</p>
   *
   * <ul>
   *
   * <li>must not be {@code null}</li>
   *
   * <li>may implement its {@link #get()} method and its
   * {@link #deterministic()} method to indicate the permanent absence
   * of any value</li>
   *
   * <li>must implement its {@link #deterministic()} method
   * properly</li>
   *
   * </ul>
   *
   * <p>An implementation of this method must first call {@link
   * #normalize(Path)} with the supplied {@link Path}, and must use
   * the {@link Path} returned by that method in place of the supplied
   * {@link Path}, or undefined behavior will result.</p>
   *
   * @param path the {@link Path} (perhaps only partially) identifying
   * the {@link Loader} to load; must not be {@code null}; may be
   * {@linkplain Path#absolute() absolute} or relative (in which case
   * it will be {@linkplain Path#plus(Path) appended} to {@linkplain
   * #path() this <code>Loader</code>'s <code>Path</code>}
   *
   * @return a {@link Loader} for the supplied {@link Path}; must not
   * be {@code null}, but may implement its {@link #get()} method and its
   * {@link #deterministic()} method to indicate the permanent absence
   * of any value
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @see #get()
   *
   * @see #deterministic()
   *
   * @see #normalize(Path)
   */
  @EntryPoint
  public <U> Loader<U> load(final Path<? extends Type> path);

  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Qualifiers<? extends String, ?> qualifiers, final Type type) {
    if (type instanceof Class<?> c) {
      return this.load(qualifiers, (Class<U>)c);
    }
    return this.load(Path.of(qualifiers, Element.of(type, "")));
  }

  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c);
    }
    return this.load(Path.of(type));
  }

  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type, final String name) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c, name);
    }
    return this.load(Path.of(type, name));
  }

  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type, final String... names) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c, names);
    }
    return this.load(Path.of(type, names));
  }

  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type, final List<? extends String> names) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c, names);
    }
    return this.load(Path.of(type, names));
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Qualifiers<? extends String, ?> qualifiers, final Token<U> type) {
    return this.load(qualifiers, type.type());
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type) {
    return this.load(type.type());
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type, final String name) {
    return this.load(type.type(), name);
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type, final String... names) {
    return this.load(type.type(), names);
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type, final List<? extends String> names) {
    return this.load(type.type(), names);
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Qualifiers<? extends String, ?> qualifiers, Class<U> type) {
    return this.load(Path.of(qualifiers, Element.of(type, "")));
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type) {
    return this.load(Path.of(type));
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type, final String name) {
    return this.load(Path.of(type, name));
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type, final String... names) {
    return this.load(Path.of(type, names));
  }

  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type, final List<? extends String> names) {
    return this.load(Path.of(type, names));
  }

  @OverridingDiscouraged
  public default <U extends Type> Path<U> normalize(final Path<U> path) {
    if (path.absolute()) {
      if (path.transliterated() || path.size() == 1) {
        return path;
      } else {
        return this.transliterate(path);
      }
    } else {
      return this.transliterate(this.path().plus(path));
    }
  }

  @OverridingDiscouraged
  public default <U extends Type> Path<U> transliterate(final Path<U> path) {
    if (path.transliterated()) {
      return path;
    }
    final Element<?> last = path.lastElement();
    if (last.name().equals("transliterate")) {
      final Qualifiers<String, ?> lastQualifiers = last.qualifiers();
      if (lastQualifiers.size() == 1 && lastQualifiers.toMap().containsKey("path")) {
        // Are we in the middle of a transliteration request? Avoid
        // the infinite loop.
        return path;
      }
    }
    return
      this.<Path<U>>load(this.path().plus(Path.of(Element.of(Qualifiers.of("path", path.toString()),
                                                             new Token<>() {}.type(), // type
                                                             "transliterate"))))
      .orElse(path);
  }

  @OverridingDiscouraged
  public default boolean isRoot() {
    return this.parent() == this;
  }

  @OverridingDiscouraged
  public default Loader<?> root() {
    Loader<?> root = this;
    Loader<?> parent = root.parent();
    while (parent != null && parent != root) {
      // (Strictly speaking, Loader::parent should NEVER be null.)
      root = parent;
      parent = root.parent();
    }
    assert root.path().equals(Path.root());
    assert this != root ? !this.path().equals(Path.root()) : true;
    return root;
  }

  public static Loader<?> loader() {
    final class RootLoader {
      private static final Loader<?> INSTANCE;
      static {
        final Loader<?> bootstrapLoader =
          ServiceLoader.load(Loader.class, Loader.class.getClassLoader()).findFirst().orElseThrow();
        if (!Path.root().equals(bootstrapLoader.path())) {
          throw new IllegalStateException("bootstrapLoader.path(): " + bootstrapLoader.path());
        } else if (bootstrapLoader.parent() != bootstrapLoader) {
          throw new IllegalStateException("bootstrapLoader.parent(): " + bootstrapLoader.parent());
        } else if (bootstrapLoader.get() != bootstrapLoader) {
          throw new IllegalStateException("bootstrapLoader.get(): " + bootstrapLoader.get());
        } else if (!bootstrapLoader.isRoot()) {
          throw new IllegalStateException("!bootstrapLoader.isRoot()");
        } else if (bootstrapLoader.root() != bootstrapLoader) {
          throw new IllegalStateException("bootstrapLoader.root(): " + bootstrapLoader.root());
        }
        INSTANCE = bootstrapLoader.<Loader<?>>load(Path.of(new Token<>() {}.type())).orElse(bootstrapLoader);
        if (!Path.root().equals(INSTANCE.path())) {
          throw new IllegalStateException("INSTANCE.path(): " + INSTANCE.path());
        } else if (INSTANCE.parent() != bootstrapLoader) {
          throw new IllegalStateException("INSTANCE.parent(): " + INSTANCE.parent());
        } else if (!(INSTANCE.get() instanceof Loader)) {
          throw new IllegalStateException("INSTANCE.get(): " + INSTANCE.get());
        }
      }
    };
    return RootLoader.INSTANCE;
  }

}
