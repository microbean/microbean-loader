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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import java.util.List;
import java.util.ServiceLoader;

import org.microbean.development.annotation.EntryPoint;
import org.microbean.development.annotation.OverridingDiscouraged;

import org.microbean.invoke.OptionalSupplier;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaType.Token;

/**
 * An {@link OptionalSupplier} that {@linkplain #get() supplies} an
 * environmental object, and can {@linkplain #load(Path) load} others.
 *
 * <p><strong>Note:</strong> {@link Loader} implementations are
 * expected to be immutable with respect to the methods exposed by
 * this interface.  All methods in this interface that have a {@link
 * Loader}-typed return type require their implementations to return a
 * <em>new</em> {@link Loader}.</p>
 *
 * <p>The presence of the {@link
 * OverridingDiscouraged @OverridingDiscouraged} annotation on a
 * method means that undefined behavior may result if an
 * implementation of this interface provides an alternate
 * implementation of the method in question.</p>
 *
 * @param <T> the type of environmental objects this {@link Loader}
 * {@linkplain #get() supplies}
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #loader()
 *
 * @see OptionalSupplier#get()
 *
 * @see #load(Path)
 */
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
   * Loader}; not {@code this} in almost all circumstances
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
   *
   * @see #loader()
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
   * Uses the addressing information encoded in the supplied {@link
   * Path} to load and return the {@link Loader} logically found at
   * that location, following additional contractual requirements
   * defined below.
   *
   * <p>Any {@link Loader} returned by an implementation of this
   * method:</p>
   *
   * <ul>
   *
   * <li>must not be {@code null}</li>
   *
   * <li>must implement its {@link #get() get()} method and its {@link
   * #determinism() determinism()} method to indicate the transitory
   * or permanent presence or absence of any value it might
   * {@linkplain #get() supply}</li>
   *
   * </ul>
   *
   * <p>An implementation of this method <strong>must</strong> first
   * call {@link #normalize(Path)} with the supplied {@link Path}, and
   * <strong>must</strong> use the {@link Path} returned by that
   * method in place of the supplied {@link Path}, or undefined
   * behavior will result.</p>
   *
   * <p>The default implementations of all other methods in this
   * interface named {@code load} call this method.</p>
   *
   * @param <U> the type of the environmental object the returned
   * {@link Loader} can {@linkplain #get() supply}
   *
   * @param path the {@link Path} (perhaps only partially) identifying
   * the {@link Loader} to load; must not be {@code null}; may be
   * {@linkplain Path#absolute() absolute} or relative (in which case
   * it will be {@linkplain Path#plus(Path) appended} to {@linkplain
   * #path() this <code>Loader</code>'s <code>Path</code>})
   *
   * @return a {@link Loader} for the supplied {@link Path}; must not
   * be {@code null}, but may implement its {@link #get() get()}
   * method and its {@link #determinism() determinism()} method to
   * indicate the transitory or permanent presence or absence of any
   * value it might {@linkplain #get() supply}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception ClassCastException if the implementation is
   * implemented improperly
   *
   * @see #get()
   *
   * @see #determinism()
   *
   * @see #normalize(Path)
   */
  @EntryPoint
  public <U> Loader<U> load(final Path<? extends Type> path);

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param qualifiers the path's {@link Qualifiers}; must not be
   * {@code null}
   *
   * @param type the path's {@link Type}; must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see Path#of(Qualifiers, Element)
   *
   * @see Element#of(Object, String)
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Qualifiers<? extends String, ?> qualifiers, final Type type) {
    if (type instanceof Class<?> c) {
      return this.load(qualifiers, (Class<U>)c);
    }
    return this.load(Path.of(qualifiers, Element.of(type, "")));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type the path's {@link Type}; must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see Path#of(Object)
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c);
    }
    return this.load(Path.of(type));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type the path's {@link Type}; must not be {@code null}
   *
   * @param name the {@linkplain Element#name() name} of the last
   * {@link Element} in the {@link Path}; must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type, final String name) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c, name);
    }
    return this.load(Path.of(type, name));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type the path's {@link Type}; must not be {@code null}
   *
   * @param names the sequence of names forming the {@link Path}; must
   * not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type, final String... names) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c, names);
    }
    return this.load(Path.of(type, names));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type the path's {@link Type}; must not be {@code null}
   *
   * @param names the sequence of names forming the {@link Path}; must
   * not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> Loader<U> load(final Type type, final List<? extends String> names) {
    if (type instanceof Class<?> c) {
      return this.load((Class<U>)c, names);
    }
    return this.load(Path.of(type, names));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param qualifiers the path's {@link Qualifiers}; must not be
   * {@code null}
   *
   * @param type a {@link Token} representing the path's {@link Type};
   * must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Qualifiers<? extends String, ?> qualifiers, final Token<U> type) {
    return this.load(qualifiers, type.type());
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Token} representing the path's {@link Type};
   * must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type) {
    return this.load(type.type());
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Token} representing the path's {@link Type};
   * must not be {@code null}
   *
   * @param name the {@linkplain Element#name() name} of the last
   * {@link Element} in the {@link Path}; must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type, final String name) {
    return this.load(type.type(), name);
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Token} representing the path's {@link Type};
   * must not be {@code null}
   *
   * @param names the sequence of names forming the {@link Path}; must
   * not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type, final String... names) {
    return this.load(type.type(), names);
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Token} representing the path's {@link Type};
   * must not be {@code null}
   *
   * @param names the sequence of names forming the {@link Path}; must
   * not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Token<U> type, final List<? extends String> names) {
    return this.load(type.type(), names);
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param qualifiers the path's {@link Qualifiers}; must not be
   * {@code null}
   *
   * @param type a {@link Class} serving as the path's {@link Type};
   * must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Qualifiers<? extends String, ?> qualifiers, Class<U> type) {
    return this.load(Path.of(qualifiers, Element.of(type, "")));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Class} serving as the path's {@link Type};
   * must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type) {
    return this.load(Path.of(type));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Class} serving as the path's {@link Type};
   * must not be {@code null}
   *
   * @param name the {@linkplain Element#name() name} of the last
   * {@link Element} in the {@link Path}; must not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type, final String name) {
    return this.load(Path.of(type, name));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Class} serving as the path's {@link Type};
   * must not be {@code null}
   *
   * @param names the sequence of names forming the {@link Path}; must
   * not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type, final String... names) {
    return this.load(Path.of(type, names));
  }

  /**
   * Builds a {@link Path} from the supplied arguments, calls the
   * {@link #load(Path)} method and returns its result.
   *
   * @param <U> the type of environmental objects the returned {@link Loader}
   * {@linkplain #get() will supply}
   *
   * @param type a {@link Class} serving as the path's {@link Type};
   * must not be {@code null}
   *
   * @param names the sequence of names forming the {@link Path}; must
   * not be {@code null}
   *
   * @return a {@link Loader}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency No guarantees are made about idempotency or
   * determinism with respect to this method or its (discouraged)
   * overrides.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type, final List<? extends String> names) {
    return this.load(Path.of(type, names));
  }

  /**
   * {@linkplain #transliterate(Path) Transliterates} the supplied
   * {@link Path}, if needed, after converting the supplied {@link
   * Path} into an {@linkplain Path#absolute() absolute} {@link Path},
   * and returns the result.
   *
   * <p>Overriding of this method is strongly discouraged.</p>
   *
   * <p>Most users will have no need to call this method directly.</p>
   *
   * @param <U> the {@link Type} designated by the supplied {@link
   * Path}
   *
   * @param path the {@link Path} to normalize; must not be {@code
   * null}
   *
   * @return a {@link Path} that {@linkplain Path#transliterated() is
   * transliterated} and {@linkplain Path#absolute() absolute}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency This method is, and its (discouraged) overrides must
   * be, idempotent and deterministic.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #transliterate(Path)
   *
   * @see Path#transliterated()
   *
   * @see Path#absolute()
   */
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

  /**
   * Returns a {@link Loader}, derived from this {@link Loader}, that
   * is suitable for a {@linkplain #normalize(Path) normalized
   * version} of the supplied {@code path}, particularly for cases
   * where, during the execution of the {@link #load(Path)} method, a
   * {@link Loader} must be supplied to some other class.
   *
   * <p>The returned {@link Loader} must be one whose {@link #path()}
   * method returns the {@linkplain Path#size() longest} {@link Path}
   * that is a parent of the ({@linkplain #normalize(Path)
   * normalized}) supplied {@code path}.  In many cases {@code this}
   * will be returned.</p>
   *
   * <p>Typically only classes implementing this interface will need
   * to call this method.  Most users will have no need to call this
   * method directly.</p>
   *
   * <p>Overriding of this method is <strong>strongly</strong>
   * discouraged.</p>
   *
   * @param path the {@link Path} in question; must not be {@code
   * null}
   *
   * @return a {@link Loader}, derived from this {@link Loader}, that
   * is suitable for a {@linkplain #normalize(Path) normalized
   * version} of the supplied {@code path}, particularly for cases
   * where, during the execution of the {@link #load(Path)} method, a
   * {@link Loader} must be supplied to some other class; never {@code
   * null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability The default implementation of this method does not,
   * and its (discouraged) overrides must not, return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its (discouraged) overrides must be, safe for concurrent use by
   * multiple threads.
   *
   * @idempotency The default implementation of this method is, and
   * its (discouraged) overrides must be, idempotent and
   * deterministic.
   *
   * @see #normalize(Path)
   *
   * @see #transliterate(Path)
   *
   * @see #root()
   */
  @OverridingDiscouraged
  public default Loader<?> loaderFor(Path<? extends Type> path) {
    path = this.normalize(path);
    Loader<?> requestor = this;
    final Path<? extends Type> requestorPath = requestor.path();
    assert requestorPath.absolute() : "!requestorPath.absolute(): " + requestorPath;
    if (requestorPath.startsWith(path)) {
      if (!requestorPath.equals(path)) {
        final int requestorPathSize = requestorPath.size();
        final int pathSize = path.size();
        assert requestorPathSize != pathSize;
        if (requestorPathSize > pathSize) {
          for (int i = 0; i < requestorPathSize - pathSize; i++) {
            requestor = requestor.parent();
          }
          assert requestor.path().equals(path) : "!requestor.path().equals(path); requestor.path(): " + requestor.path() + "; path: " + path;
        } else {
          throw new AssertionError("requestorPath.size() < path.size(); requestorPath: " + requestorPath + "; path: " + path);
        }
      } else {
        requestor = requestor.root();
      }
    }
    return requestor;
  }

  /**
   * <em>Transliterates</em> the supplied {@linkplain Path#absolute()
   * absolute <code>Path</code>} into some other {@link Path}, whose
   * meaning is the same, but whose representation may be different,
   * that will be used instead.
   *
   * <p>The {@link Path} that is returned may be the {@link Path} that
   * was supplied.  This may happen, for example, if {@linkplain
   * Path#transliterated() the path has already been transliterated},
   * or if the path identifies a transliteration request.</p>
   *
   * <p>Path transliteration is needed because the {@link
   * Element#name() name()} components of {@link Element}s may
   * unintentionally clash when two components are combined into a
   * single application.</p>
   *
   * <p>Path transliteration must occur during the execution of any
   * implementation of the {@link #load(Path)} method, such that the
   * {@link Path} supplied to that method, once it has been verified
   * to be {@linkplain Path#absolute() absolute}, is supplied to an
   * implementation of this method. The {@link Path} returned by an
   * implementation of this method must then be used during the rest
   * of the invocation of the {@link #load(Path)} method, as if it had
   * been supplied in the first place.</p>
   *
   * <p>Behavior resulting from any other usage of an implementation
   * of this method is undefined.</p>
   *
   * <p>The default implementation of this method uses the
   * configuration system itself to find a transliterated {@link Path}
   * corresponding to the supplied {@link Path}, returning the
   * supplied {@code path} itself if no transliteration can take
   * place.  Infinite loops are avoided except as noted below.</p>
   *
   * <p>Overrides of this method <strong>must not call {@link
   * #normalize(Path)}</strong> or an infinite loop may result.</p>
   *
   * <p>Overrides of this method <strong>must not call {@link
   * #loaderFor(Path)}</strong> or an infinite loop may
   * result.</p>
   *
   * <p>Overrides of this method <strong>must not call {@link
   * #load(Path)} with the supplied {@code path}</strong> or an infinite
   * loop may result.</p>
   *
   * <p>An implementation of {@link Loader} will find {@link
   * Path#transliterate(java.util.function.BiFunction)} particularly
   * useful in implementing this method.</p>
   *
   * <p>Most users will have no need to call this method directly.</p>
   *
   * @param <U> the type of the supplied and returned {@link Path}s
   *
   * @param path an {@linkplain Path#absolute() absolute
   * <code>Path</code>}; must not be null
   *
   * @return the transliterated {@link Path}; never {@code null};
   * possibly the supplied {@code path} itself
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception IllegalArgumentException if {@code path} returns
   * {@code false} from its {@link Path#absolute() absolute()}
   * method
   *
   * @nullability The default implementation of this method does not,
   * and its (discouraged) overrides must not, return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its (discouraged) overrides must be, safe for concurrent use by
   * multiple threads.
   *
   * @idempotency The default implementation of this method is, and
   * its (discouraged) overrides must be, idempotent and
   * deterministic.
   *
   * @see Path#absolute()
   *
   * @see Path#transliterate(java.util.function.BiFunction)
   *
   * @see #normalize(Path)
   *
   * @see #loaderFor(Path)
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U extends Type> Path<U> transliterate(final Path<U> path) {
    if (path.transliterated()) {
      return path;
    }
    final Element<U> last = path.lastElement();
    if (last.name().equals("transliterated")) {
      final Qualifiers<String, ?> lastQualifiers = last.qualifiers();
      if (lastQualifiers.size() == 1 && lastQualifiers.containsKey("path")) {
        // Are we in the middle of a transliteration request? Avoid
        // the infinite loop.
        return path;
      }
    }
    final ParameterizedType ptype = (ParameterizedType)new Token<Path<U>>() {}.type();
    final Element<ParameterizedType> e = Element.of(Qualifiers.of("path", path), ptype, "transliterated");
    final Path<ParameterizedType> p = this.path().plus(e);
    assert p.lastElement().name().equals("transliterated");
    assert ((TypeVariable<?>)p.qualified().getActualTypeArguments()[0]).getBounds()[0] == Type.class;
    return this.<Path<U>>load(p).orElse(path.transliterate());
  }

  /**
   * Returns {@code true} if and only if this {@link Loader} is the
   * root {@link Loader}, which occurs only when the return value of
   * {@link #parent() this.parent() == this}.
   *
   * <p>Overrides of this method are <strong>strongly</strong>
   * discouraged.</p>
   *
   * <p>Most users will have no need to call this method directly.</p>
   *
   * @return {@code true} if and only if this {@link Loader} is the
   * root {@link Loader}
   *
   * @idempotency This method is, and its (discouraged) overrides must
   * be, idempotent and deterministic.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #parent()
   */
  @OverridingDiscouraged
  public default boolean isRoot() {
    return this.parent() == this;
  }

  /**
   * Returns the root {@link Loader}, which is the {@link Loader}
   * whose {@link #parent()} method returns iteself.
   *
   * <p>Overrides of this method are <strong>strongly</strong>
   * discouraged.</p>
   *
   * <p>Most users will have no need to call this method directly.</p>
   *
   * @return the root {@link Loader}
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency This method is, and its (discouraged) overrides must
   * be, idempotent and deterministic.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   *
   * @see #isRoot()
   *
   * @see #parent()
   *
   * @see Path#root()
   */
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

  /**
   * Casts this {@link Loader} appropriately and returns it, usually
   * so that an implementation's implementation-specific methods can
   * be accessed.
   *
   * @param <L> the {@link Loader} subclass
   *
   * @param loaderSubclass a {@link Class} representing a subclass of
   * {@link Loader}; must not be {@code null}
   *
   * @return this {@link Loader}
   *
   * @exception NullPointerException if {@code loaderSubclass} is
   * {@code null}
   *
   * @exception ClassCastException if the cast could not be performed
   *
   * @nullability This method does not, and its (discouraged)
   * overrides must not, return {@code null}.
   *
   * @idempotency This method is, and its (discouraged) overrides must
   * be, idempotent and deterministic.
   *
   * @threadsafety This method is, and its (discouraged) overrides
   * must be, safe for concurrent use by multiple threads.
   */
  @OverridingDiscouraged
  public default <L extends Loader<?>> L as(final Class<L> loaderSubclass) {
    return loaderSubclass.cast(this);
  }

  /**
   * Bootstraps and returns a {@link Loader}.
   *
   * <p>If this method has been called before, its prior result is
   * returned.</p>
   *
   * <p>Otherwise, first, the <em>root {@link Loader}</em> is located
   * using Java's built-in {@link ServiceLoader}.  The first of the
   * {@link Loader} instances it discovers is used and all others are
   * ignored.  Note that the {@link ServiceLoader} discovery process
   * is non-deterministic.  Normally there is only one such {@link
   * Loader} provided by an implementation of this API.</p>
   *
   * <p>The root {@link Loader} that is loaded via this mechanism is
   * subject to the following restrictions:</p>
   *
   * <ul>
   *
   * <li>It must return a {@link Path} from its {@link #path()}
   * implementation that is {@linkplain Path#equals(Object) equal to}
   * {@link Path#root() Path.root()}.</li>
   *
   * <li>It must return itself ({@code this}) from its {@link
   * #parent()} implementation.</li>
   *
   * <li>It must return {@code true} from its {@link #isRoot()}
   * implementation.</li>
   *
   * <li>It must return itself ({@code this}) from its {@link #root()}
   * method.</li>
   *
   * <li>It must return {@link Determinism#PRESENT} from its {@link
   * #determinism() determinism()} method.</li>
   *
   * <li>It must return itself ({@code this}) from its {@link #get()
   * get()} method.</li>
   *
   * </ul>
   *
   * <p>Next, this root {@link Loader} is used to {@linkplain
   * #load(Path) find} the <em>{@link Loader} of record</em>, which in
   * most cases is simply itself.</p>
   *
   * <p>The {@link Loader} of record is subject to the following
   * restrictions (which are compatible with the overwhelmingly common
   * case of its also being the root {@link Loader}):</p>
   *
   * <ul>
   *
   * <li>It must return a {@link Path} from its {@link #path()}
   * implementation that is equal to {@link Path#root() Path.root()}
   * (same as above).</li>
   *
   * <li>It must return {@link Determinism#PRESENT} from its {@link
   * #determinism() determinism()} method (same as above).</li>
   *
   * <li>It must return the bootstrap {@link Loader} from its {@link
   * #parent()} implementation (which may be itself ({@code
   * this}).</li>
   *
   * <li>It must return a {@link Loader} implementation, often itself
   * ({@code this}), from its {@link #get() get()} method.</li>
   *
   * </ul>
   *
   * <p>Undefined behavior will result if an implementation of the
   * {@link Loader} interface does not honor the requirements
   * above.</p>
   *
   * <p>This method is the primary entry point for end users of this
   * framework.</p>
   *
   * @return a non-{@code null} {@link Loader} that can be used to
   * acquire environmental objects that abides by the requirements and
   * restrictions above
   *
   * @exception IllegalStateException if any of the requirements and
   * restrictions above is violated
   *
   * @exception java.util.ServiceConfigurationError if the root {@link
   * Loader} could not be loaded for any reason
   */
  @EntryPoint
  public static Loader<?> loader() {
    final class LoaderOfRecord {
      private static final Loader<?> INSTANCE;
      static {
        final Loader<?> rootLoader =
          ServiceLoader.load(Loader.class, Loader.class.getClassLoader()).findFirst().orElseThrow();
        if (rootLoader.determinism() != Determinism.PRESENT) {
          throw new IllegalStateException("rootLoader.determinism() != PRESENT: " + rootLoader.determinism());
        } else if (!Path.root().equals(rootLoader.path())) {
          throw new IllegalStateException("rootLoader.path(): " + rootLoader.path());
        } else if (rootLoader.parent() != rootLoader) {
          throw new IllegalStateException("rootLoader.parent(): " + rootLoader.parent());
        } else if (rootLoader.get() != rootLoader) {
          throw new IllegalStateException("rootLoader.get(): " + rootLoader.get());
        } else if (!rootLoader.isRoot()) {
          throw new IllegalStateException("!rootLoader.isRoot()");
        } else if (rootLoader.root() != rootLoader) {
          throw new IllegalStateException("rootLoader.root(): " + rootLoader.root());
        }
        INSTANCE = rootLoader.<Loader<?>>load(Path.of(new Token<Loader<?>>() {}.type())).orElse(rootLoader);
        if (INSTANCE.determinism() != Determinism.PRESENT) {
          throw new IllegalStateException("INSTANCE.determinism() != PRESENT: " + INSTANCE.determinism());
        } else if (!Path.root().equals(INSTANCE.path())) {
          throw new IllegalStateException("INSTANCE.path(): " + INSTANCE.path());
        } else if (INSTANCE.parent() != rootLoader) {
          throw new IllegalStateException("INSTANCE.parent(): " + INSTANCE.parent());
        } else if (!(INSTANCE.get() instanceof Loader)) {
          throw new IllegalStateException("INSTANCE.get(): " + INSTANCE.get());
        }
      }
    };
    return LoaderOfRecord.INSTANCE;
  }

}
