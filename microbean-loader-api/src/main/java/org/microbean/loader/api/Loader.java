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
   * <li>may implement its {@link #get()} method and its {@link
   * #deterministic()} method to indicate the permanent absence of any
   * value</li>
   *
   * <li>must implement its {@link #deterministic()} method
   * properly</li>
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
   * @param path the {@link Path} (perhaps only partially) identifying
   * the {@link Loader} to load; must not be {@code null}; may be
   * {@linkplain Path#absolute() absolute} or relative (in which case
   * it will be {@linkplain Path#plus(Path) appended} to {@linkplain
   * #path() this <code>Loader</code>'s <code>Path</code>}
   *
   * @return a {@link Loader} for the supplied {@link Path}; must not
   * be {@code null}, but may implement its {@link #get()} method and
   * its {@link #deterministic()} method to indicate the permanent
   * absence of any value
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception ClassCastException if the implementation is
   * implemented improperly
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

  /**
   * {@linkplain #transliterate(Path) Transliterates} the supplied
   * {@link Path}, if needed, after converting the supplied {@link
   * Path} into an {@linkplain Path#absolute() absolute} {@link Path},
   * and returns the result.
   *
   * <p>Overriding of this method is strongly discouraged.</p>
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
   * to call this method.</p>
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
  public default <U extends Type> Loader<?> loaderFor(Path<U> path) {
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
   * Bootstraps and returns the root {@link Loader}.
   *
   * <p>First, a <em>bootstrap {@link Loader}</em> is located using
   * Java's built-in {@link ServiceLoader}.  The first of the {@link
   * Loader} instances it discovers is used and all others are
   * ignored.  Note that the {@link ServiceLoader} discovery process
   * is non-deterministic.  Normally there is only one such {@link
   * Loader} provided by an implementation of this API.</p>
   *
   * <p>The bootstrap {@link Loader} that is loaded via this mechanism
   * is subject to the following restrictions:</p>
   *
   * <ul>
   *
   * <li>It must return a {@link Path} from its {@link #path()}
   * implementation that is {@link Path#equals(Object) equal to}
   * {@link Path#root() Path.root()}.</li>
   *
   * <li>It must return itself ({@code this}) from its {@link
   * #parent()} implementation.</li>
   *
   * <li>It must return itself ({@code this}) from its {@link #get()
   * get()} method.</li>
   *
   * <li>It must return {@code true} from its {@link #isRoot()}
   * implementation.</li>
   *
   * <li>It must return itself ({@code this}) from its {@link #root()}
   * method.</li>
   *
   * </ul>
   *
   * <p>This bootstrap {@link Loader} is then used to {@linkplain
   * #load(Path) find} the <em>{@link Loader} of record</em>,
   * which in most cases is simply itself.</p>
   *
   * <p>The {@link Loader} of record is subject to the following
   * restrictions (which are compatible with the overwhelmingly common
   * case of its also being the bootstrap {@link Loader}):</p>
   *
   * <ul>
   *
   * <li>It must return a {@link Path} from its {@link #path()}
   * implementation that is equal to {@link Path#root() Path.root()}
   * (same as above).</li>
   *
   * <li>It must return the bootstrap {@link Loader} from its {@link
   * #parent()} implementation.</li>
   *
   * <li>It must return a {@link Loader} implementation, normally
   * itself ({@code this}), from its {@link #get() get()} method.</li>
   *
   * </ul>
   *
   * <p>Undefined behavior will result if an implementation of the
   * {@link Loader} interface does not honor the requirements
   * above.</p>
   *
   * <p>This is the primary entry point for end users of this
   * framework.</p>
   *
   * @return a non-{@code null} {@linkplain #isRoot() root} {@link
   * Loader} that can be used to acquire environmental objects
   *
   * @exception IllegalStateException if any of the restrictions above
   * is violated
   *
   * @exception java.util.ServiceConfigurationError if the bootstrap
   * {@link Loader} could not be loaded for any reason
   */
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
        INSTANCE = bootstrapLoader.<Loader<?>>load(Path.of(new Token<Loader<?>>() {}.type())).orElse(bootstrapLoader);
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
