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

import java.util.NoSuchElementException;
import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

import org.microbean.invoke.FixedValueSupplier;
import org.microbean.invoke.OptionalSupplier;

import org.microbean.path.Path;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaTypes;

/**
 * An {@link OptionalSupplier} of a value that is additionally
 * qualified by a {@link Path} partially identifying the kinds of
 * {@link Qualifiers} and {@link Path}s for which it might be
 * suitable.
 *
 * <p>{@link Value}s are typically returned by {@link Provider}
 * implementations.</p>
 *
 * <p>A {@link Value} once received retains no reference to whatever
 * produced it and can be regarded as an authoritative source for
 * (possibly ever-changing) values going forward.  Notably, it can be
 * cached.</p>
 *
 * @param <T> the type of value this {@link Value} returns
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see OptionalSupplier
 *
 * @see Provider
 */
public final class Value<T> implements OptionalSupplier<T> {


  /*
   * Instance fields.
   */


  private final Path<? extends Type> path;

  private final OptionalSupplier<? extends T> supplier;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Value} <strong>that returns a valid {@code
   * null} value from its {@link #get() get()} method</strong>.
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @see #get()
   *
   * @see FixedValueSupplier#of(Object)
   */
  public Value(final Path<? extends Type> path) {
    this(FixedValueSupplier.of(null), path);
  }

  /**
   * Creates a new {@link Value} that will forever and always return
   * the supplied {@code value} from its {@link #get() get()} method.
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @param value a fixed value to be returned by the {@link #get()}
   * method whenever it is invoked; may be {@code null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @see #get()
   *
   * @see FixedValueSupplier#of(Object)
   */
  public Value(final T value, final Path<? extends Type> path) {
    this(FixedValueSupplier.of(value), path);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param source the {@link Value} to use as the primary supplier;
   * must not be {@code null}
   *
   * @param defaults the {@link Supplier} to use as the fallback; may
   * be {@code null}
   *
   * @exception NullPointerException if {@code source} is {@code null}
   */
  public Value(final Value<? extends T> source, final Supplier<? extends T> defaults) {
    this(OptionalSupplier.of(source, defaults), source.path());
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; may be {@code null}
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   */
  public Value(final Supplier<? extends T> supplier, final Path<? extends Type> path) {
    super();
    this.supplier = OptionalSupplier.of(supplier);
    this.path = Objects.requireNonNull(path, "path");
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Value} with this {@link Value}'s supplier and
   * the supplied {@link Path}.
   *
   * @param path the new {@link Path}; must not be {@code null}
   *
   * @return a {@link Value} with this {@link Value}'s supplier and
   * the supplied {@link Path}
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
  public final Value<T> with(final Path<? extends Type> path) {
    if (path.equals(this.path())) {
      return this;
    } else {
      return new Value<>(this.supplier, path);
    }
  }

  /**
   * Returns the {@link Qualifiers} with which this {@link Value} is
   * associated.
   *
   * @return the {@link Qualifiers} with which this {@link Value} is
   * associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see Path#qualifiers()
   */
  @Convenience
  public final Qualifiers<? extends String, ?> qualifiers() {
    return this.path().qualifiers();
  }

  /**
   * Returns the {@link Path} with which this {@link Value} is
   * associated.
   *
   * @return the {@link Path} with which this {@link Value} is
   * associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Path<? extends Type> path() {
    return this.path;
  }

  /**
   * Invokes the {@link Supplier#get() get()} method of the {@link
   * Supplier} supplied at {@linkplain #Value(Supplier, Path)
   * construction time} and returns its value, which may be {@code
   * null}.
   *
   * @return tbe return value of an invocation of the {@link
   * OptionalSupplier#get() get()} method of the {@link Supplier}
   * supplied at {@linkplain #Value(Supplier, Path) construction
   * time}, which may be {@code null}
   *
   * @exception NoSuchElementException if this method should no longer
   * be invoked because there is no chance it will ever produce a
   * suitable value again
   *
   * @exception UnsupportedOperationException if this method should no
   * longer be invoked because there is no chance it will ever produce
   * a suitable value again
   *
   * @see #Value(Supplier, Path)
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, provided that the {@link Supplier} supplied at
   * {@linkplain #Value(Supplier, Path) construction time} is also
   * safe for concurrent use by multiple threads.
   *
   * @idempotency This method is as idempotent and deterministic as
   * the {@link Supplier} supplied at {@linkplain #Value(Supplier,
   * Path) construction time}.
   */
  @Override // Supplier<T>
  public final T get() {
    return this.supplier.get();
  }

  /**
   * Returns an appropriate {@link Determinism} for this {@link
   * Value}.
   *
   * @return an appropriate {@link Determinism} for this {@link Value}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // OptionalSupplier<T>
  public final Determinism determinism() {
    return this.supplier.determinism();
  }

  /**
   * Returns the result of invoking {@link Path#qualified()} on the
   * return value of this {@link Value}'s {@link #path()} method.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the result of invoking {@link Path#qualified()} on the
   * return value of this {@link Value}'s {@link #path()} method;
   * never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #path()
   *
   * @see Path#qualified()
   */
  @Convenience
  public final Type type() {
    return this.path().qualified();
  }

  /**
   * Returns the type erasure of the return value of the {@link
   * #type()} method.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the result of invoking {@link JavaTypes#erase(Type)} on
   * the return value of this {@link Value}'s {@link #type()} method;
   * never {@code null}
   *
   * @exception IllegalStateException if somehow the return value of
   * {@link #type()} could not be erased
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #path()
   *
   * @see JavaTypes#erase(Type)
   */
  public final Class<T> typeErasure() {
    @SuppressWarnings("unchecked")
    final Class<T> returnValue = (Class<T>)JavaTypes.erase(this.type());
    if (returnValue == null) {
      throw new IllegalStateException();
    }
    return returnValue;
  }

  /**
   * Returns a hashcode for this {@link Value} calculated from its
   * {@link #path() Path}, and {@linkplain #determinism() whether it
   * is deterministic}.
   *
   * @return a hashcode for this {@link Value}
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #equals(Object)
   *
   * @see #path()
   *
   * @see #determinism()
   */
  @Override // Object
  public final int hashCode() {
    int hashCode = 17;

    Object v = this.path();
    int c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;

    v = this.determinism();
    c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;

    return hashCode;
  }

  /**
   * Returns {@code true} if this {@link Value} is equal to the
   * supplied {@link Object}.
   *
   * <p>This method will return {@code true} if and only if the
   * following conditions hold:</p>
   *
   * <ul>
   *
   * <li>The supplied {@link Object} is not {@code null}</li>
   *
   * <li>The supplied {@link Object}'s {@link Object#getClass()}
   * method returns {@link Value Value.class}</li>
   *
   * <li>{@link Objects#equals(Object, Object)
   * Objects.equals(this.path(), otherValue.path())} returns {@code
   * true}</li>
   *
   * <li>{@link Objects#equals(Object, Object)
   * Objects.equals(this.determinism(), otherValue.determinism())}
   * returns {@code true}</li>
   *
   * </ul>
   *
   * @param other the {@link Object} to test; may be {@code null} in
   * which case {@code false} will be returned
   *
   * @return {@code true} if this {@link Value} is equal to the
   * supplied {@link Object}; {@code false} otherwise
   *
   * @idempotency This method is idempotent and and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by
   * multiple threads.
   *
   * @see #path()
   *
   * @see #determinism()
   */
  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      final Value<?> her = (Value<?>)other;
      return
        Objects.equals(this.path(), her.path()) &&
        Objects.equals(this.determinism(), her.determinism());
    } else {
      return false;
    }
  }

}
