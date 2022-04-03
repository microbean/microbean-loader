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

import org.microbean.invoke.OptionalSupplier;

import org.microbean.path.Path;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaTypes;

/**
 * An {@link OptionalSupplier} of a value that is additionally
 * qualified by a {@link Qualifiers} and a {@link Path} partially
 * identifying the kinds of {@link Qualifiers} and {@link Path}s for
 * which it might be suitable.
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
 * @see Supplier
 *
 * @see Provider
 */
public final class Value<T> implements OptionalSupplier<T> {


  /*
   * Static fields.
   */


  /**
   * An {@link Object} representing the presence of a qualifier key.
   *
   * @nullability This field is never {@code null}.
   */
  // TODO: yuck
  public static final Object ANY = new Object[0];

  

  /*
   * Instance fields.
   */


  private final Path<? extends Type> path;

  private final OptionalSupplier<? extends T> supplier;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Path, Supplier,
   * Determinism)} constructor with the supplied arguments, {@code
   * null} for the value of the {@code defaults} parameter, and {@code
   * true} for the value of the {@code deterministic} parameter.</p>
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @param value the value that will be returned by the {@link
   * #get()} method; may be {@code null}
   *
   * @see #Value(Supplier, Path, Supplier, Determinism)
   */
  public Value(final Path<? extends Type> path, final T value) {
    this(path, OptionalSupplier.of(value));
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Path, Supplier,
   * Determinism)} constructor with the supplied arguments, {@code null}
   * for the value of the {@code defaults} parameter, and {@code
   * false} for the value of the {@code deterministic} parameter.</p>
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @see #Value(Supplier, Path, Supplier, Determinism)
   */
  public Value(final Path<? extends Type> path, final Supplier<? extends T> supplier) {
    this(null, path, supplier, supplier instanceof OptionalSupplier<? extends T> os ? os.determinism() : Determinism.NON_DETERMINISTIC);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Path, Supplier,
   * Determinism)} constructor with the supplied arguments and {@code
   * null} for the value of the {@code defaults} parameter.</p>
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @param deterministic a {@code boolean} indicating whether the
   * supplied {@code supplier} returns a singleton from its {@link
   * Supplier#get()} method
   *
   * @see #Value(Supplier, Path, Supplier, Determinism)
   */
  public Value(final Path<? extends Type> path, final Supplier<? extends T> supplier, final boolean deterministic) {
    this(null, path, supplier, deterministic ? Determinism.DETERMINISTIC : Determinism.NON_DETERMINISTIC);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Path, Supplier,
   * Determinism)} constructor with the supplied arguments and and {@code
   * false} for the value of the {@code deterministic} parameter.</p>
   *
   * @param defaults a {@link Supplier} to be used in case this {@link
   * Value}'s {@link #get()} method throws either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}; may be {@code null}
   *
   * @param path the {@link Path}, possibly relative, for which this
   * {@link Value} is suitable; must not be {@code null}
   *
   * @param supplier the actual {@link Supplier} that will return
   * values; must not be {@code null}
   *
   * @see #Value(Supplier, Path, Supplier, Determinism)
   */
  public Value(final Supplier<? extends T> defaults, final Path<? extends Type> path, final Supplier<? extends T> supplier) {
    this(defaults,
         path,
         supplier,
         defaults == null || defaults == supplier ? supplier instanceof OptionalSupplier<? extends T> os ? os.determinism() : Determinism.NON_DETERMINISTIC : Determinism.NON_DETERMINISTIC);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Path, Supplier,
   * Determinism)} constructor with the value of the {@code
   * defaults} parameter and arguments derived from the supplied {@code
   * source}.</p>
   *
   * @param defaults a {@link Supplier} to be used in case this {@link
   * Value}'s {@link #get()} method throws either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}; may be {@code null}
   *
   * @param source the {@link Value} from which other arguments will
   * be derived; must not be {@code null}
   *
   * @exception NullPointerException if {@code source} is {@code null}
   *
   * @see #Value(Supplier, Path, Supplier,
   * Determinism)
   */
  public Value(final Supplier<? extends T> defaults, final Value<T> source) {
    this(defaults, source.path(), source, defaults == null || defaults == source ? source.determinism() : Determinism.NON_DETERMINISTIC);
  }

  /**
   * Creates a new {@link Value}.
   *
   * <p>Calls the {@link #Value(Supplier, Path, Supplier,
   * Determinism)} constructor with no defaults and arguments
   * derived from the supplied {@code source}.</p>
   *
   * @param source the {@link Value} from which other arguments will
   * be derived; must not be {@code null}
   *
   * @exception NullPointerException if {@code source} is {@code null}
   *
   * @see #Value(Supplier, Path, Supplier,
   * Determinism)
   */
  public Value(final Value<T> source) {
    this(null, source.path(), source, source.determinism());
  }

  private Value(final Supplier<? extends T> defaults,
                final Path<? extends Type> path,
                final Supplier<? extends T> supplier,
                final Determinism determinism) {
    super();
    Objects.requireNonNull(determinism, "determinism");
    this.path = Objects.requireNonNull(path, "path");
    // this.deterministic = deterministic;
    if (supplier == null) {
      if (defaults == null) {
        if (determinism == Determinism.ABSENT) {
          this.supplier = OptionalSupplier.of();
        } else {
          throw new IllegalArgumentException("determinism: " + determinism + "; supplier: null; defaults: null");
        }
      } else if (determinism == Determinism.ABSENT) {
        throw new IllegalArgumentException("determinism: " + determinism + "; supplier: null; defaults: " + defaults);
      } else {
        this.supplier = OptionalSupplier.of(determinism, defaults);
      }
    } else if (determinism == Determinism.ABSENT) {
      throw new IllegalArgumentException("determinism: " + determinism + "; supplier: " + supplier + "; defaults: " + defaults);
    } else if (defaults == null || supplier == defaults) {
      this.supplier = OptionalSupplier.of(determinism, supplier);
    } else if (determinism != Determinism.NON_DETERMINISTIC) {
      throw new IllegalArgumentException("determinism: " + determinism + "; supplier: " + supplier + "; defaults: " + defaults);
    } else {
      this.supplier = new OptionalSupplier<>() {
          @Override
          public final Determinism determinism() {
            return Determinism.NON_DETERMINISTIC;
          }
          @Override
          public final T get() {
            try {
              return supplier.get();
            } catch (final NoSuchElementException | UnsupportedOperationException e) {
              return defaults.get();
            }
          }
        };
    }
  }


  /*
   * Instance methods.
   */


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
   * @see #Value(Supplier, Path, Supplier, Determinism)
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
   *
   * @see #Value(Supplier, Path, Supplier, Determinism)
   */
  public final Path<? extends Type> path() {
    return this.path;
  }

  /**
   * Invokes the {@link Supplier#get() get()} method of the {@link
   * Supplier} supplied at {@linkplain #Value(Supplier, 
   * Path, Supplier, Determinism) construction time} and returns its
   * value, which may be {@code null}.
   *
   * @return tbe return value of an invocation of the {@link
   * Supplier#get() get()} method of the {@link Supplier} supplied at
   * {@linkplain #Value(Supplier, Path, Supplier, Determinism)
   * construction time}, which may be {@code null}
   *
   * @exception NoSuchElementException if this method should no longer
   * be invoked because there is no chance it will ever produce a
   * suitable value again
   *
   * @exception UnsupportedOperationException if this method should no longer
   * be invoked because there is no chance it will ever produce a
   * suitable value again
   *
   * @see #Value(Supplier, Path, Supplier, Determinism)
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, provided that the {@link Supplier} supplied at
   * {@linkplain #Value(Supplier, Path, Supplier, Determinism)
   * construction time} is also safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is as idempotent and deterministic as
   * the {@link Supplier} supplied at {@linkplain #Value(Supplier,
   * Path, Supplier, Determinism) construction time}.
   */
  @Override // Supplier<T>
  public final T get() {
    return this.supplier.get();
  }

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
   * @return the result of invoking {@link JavaTypes#erase(Type)} on the
   * return value of this {@link Value}'s {@link #type()} method;
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
  @SuppressWarnings("unchecked")
  public final Class<T> typeErasure() {
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
   * Objects.equals(this.determinism(), otherValue.determinism())} returns
   * {@code true}</li>
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
