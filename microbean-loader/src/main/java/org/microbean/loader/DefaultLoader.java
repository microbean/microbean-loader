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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;

import org.microbean.invoke.Absence;
import org.microbean.invoke.FixedValueSupplier;
import org.microbean.invoke.OptionalSupplier;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualified;
import org.microbean.qualifier.Qualifiers;

import org.microbean.loader.spi.AmbiguityHandler;
import org.microbean.loader.spi.Provider;
import org.microbean.loader.spi.Value;

import org.microbean.type.Type.CovariantSemantics;

/**
 * A subclassable default {@link Loader} implementation that delegates
 * its work to {@link Provider}s and an {@link #ambiguityHandler()
 * AmbiguityHandler}.
 *
 * @param <T> the type of configured objects this {@link
 * DefaultLoader} supplies
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Loader
 *
 * @see Provider
 */
public class DefaultLoader<T> implements AutoCloseable, Loader<T> {


  private static final ThreadLocal<Map<Path<? extends Type>, Deque<Provider>>> currentProviderStacks = ThreadLocal.withInitial(() -> new HashMap<>(7));


  /*
   * Instance fields.
   */


  // Package-private for testing only.
  final ConcurrentMap<Path<? extends Type>, DefaultLoader<?>> loaderCache;

  private final Path<? extends Type> absolutePath;

  private final DefaultLoader<?> parent;

  private final OptionalSupplier<? extends T> supplier;

  private final Collection<Provider> providers;

  private final AmbiguityHandler ambiguityHandler;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link DefaultLoader}.
   *
   * @see org.microbean.loader.api.Loader#loader()
   *
   * @deprecated This constructor should be invoked by subclasses and
   * {@link ServiceLoader java.util.ServiceLoader} instances only.
   */
  @Deprecated // intended for use by subclasses and java.util.ServiceLoader only
  public DefaultLoader() {
    this(new ConcurrentHashMap<Path<? extends Type>, DefaultLoader<?>>(),
         null, // providers
         null, // parent,
         null, // absolutePath
         null, // Supplier
         null); // AmbiguityHandler
  }

  private DefaultLoader(final DefaultLoader<T> loader) {
    this(loader.loaderCache,
         loader.providers(),
         loader.parent(),
         loader.path(),
         loader.supplier,
         loader.ambiguityHandler());
  }

  private DefaultLoader(final DefaultLoader<T> loader, final AmbiguityHandler ambiguityHandler) {
    this(loader.loaderCache,
         loader.providers(),
         loader.parent(),
         loader.path(),
         loader.supplier,
         ambiguityHandler);
  }
  
  private DefaultLoader(final DefaultLoader<T> loader, final Provider provider) {
    this(loader.loaderCache,
         add(loader.providers(), provider),
         loader.parent(),
         loader.path(),
         loader.supplier,
         loader.ambiguityHandler());
  }

  @SuppressWarnings("unchecked")
  private DefaultLoader(final ConcurrentMap<Path<? extends Type>, DefaultLoader<?>> loaderCache,
                        final Collection<? extends Provider> providers,
                        final DefaultLoader<?> parent, // if null, will end up being "this" if absolutePath is null or Path.root()
                        final Path<? extends Type> absolutePath,
                        final OptionalSupplier<? extends T> supplier, // if null, will end up being () -> this if absolutePath is null or Path.root()
                        final AmbiguityHandler ambiguityHandler) {
    super();
    this.loaderCache = Objects.requireNonNull(loaderCache, "loaderCache");
    if (parent == null) {
      // Bootstrap case, i.e. the zero-argument constructor called us.
      // Pay attention.
      if (absolutePath == null || absolutePath.equals(Path.root())) {
        final Path<? extends Type> rootPath = (Path<? extends Type>)Path.root();
        this.absolutePath = rootPath;
        this.parent = this; // NOTE
        this.supplier = supplier == null ? FixedValueSupplier.of((T)this) : supplier;
        this.providers = providers == null ? loadedProviders() : List.copyOf(providers);
        this.loaderCache.put(rootPath, this); // NOTE
        // While the following call is in effect, our
        // final-but-as-yet-uninitialized ambiguityHandler field will
        // be null.  Note that the ambiguityHandler() instance method
        // accounts for this.
        try {
          this.ambiguityHandler = this.load(AmbiguityHandler.class).orElseGet(DefaultLoader::loadedAmbiguityHandler);
        } finally {
          this.loaderCache.remove(rootPath);
        }
      } else {
        throw new IllegalArgumentException("!absolutePath.equals(Path.root()): " + absolutePath);
      }
    } else if (!absolutePath.absolute()) {
      throw new IllegalArgumentException("!absolutePath.absolute(): " + absolutePath);
    } else if (!parent.path().absolute()) {
      throw new IllegalArgumentException("!parent.path().absolute(): " + parent.path());
    } else {
      this.absolutePath = absolutePath;
      this.parent = parent;
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      this.providers = List.copyOf(providers);
      this.ambiguityHandler = Objects.requireNonNull(ambiguityHandler, "ambiguityHandler");
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Clears any caches used by this {@link DefaultLoader}.
   *
   * <p>This {@link DefaultLoader} remains valid to use.</p>
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is deterministic but not idempotent
   * unless the caches are already cleared.
   */
  @Experimental
  @Override // AutoCloseable
  public final void close() {
    this.loaderCache.clear();
  }

  /**
   * Returns a {@link DefaultLoader} that {@linkplain #providers()
   * uses} the additional {@link Provider}.
   *
   * @param provider the additional {@link Provider}; may be {@code
   * null} in which case {@code this} will be returned
   *
   * @return a {@link DefaultLoader} that {@linkplain #providers()
   * uses} the additional {@link Provider}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is not idempotent (it usually creates a
   * new {@link DefaultLoader} to return) but is deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final DefaultLoader<T> plus(final Provider provider) {
    return provider == null ? this : new DefaultLoader<>(this, provider);
  }

  /**
   * Returns a {@link DefaultLoader} that {@linkplain
   * #ambiguityHandler() uses the supplied
   * <code>AmbiguityHandler</code>}.
   *
   * @param ambiguityHandler the {@link AmbiguityHandler}; must not be
   * {@code null}
   *
   * @return a {@link DefaultLoader} that {@linkplain
   * #ambiguityHandler() uses the supplied
   * <code>AmbiguityHandler</code>}
   *
   * @exception NullPointerException if {@code ambiguityHandler} is
   * {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is not idempotent (it usually creates a
   * new {@link DefaultLoader} to return) but is deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final DefaultLoader<T> with(final AmbiguityHandler ambiguityHandler) {
    if (ambiguityHandler == this.ambiguityHandler()) {
      return this;
    } else {
      return new DefaultLoader<>(this, ambiguityHandler);
    }
  }

  /**
   * Returns an {@linkplain
   * java.util.Collections#unmodifiableCollection(Collection)
   * unmodifiable} {@link Collection} of {@link Provider}s that this
   * {@link DefaultLoader} will use to supply objects.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return an {@linkplain
   * java.util.Collections#unmodifiableCollection(Collection)
   * unmodifiable} {@link Collection} of {@link Provider}s that this
   * {@link DefaultLoader} will use to supply objects; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Collection<Provider> providers() {
    return this.providers;
  }

  /**
   * Returns the {@link AmbiguityHandler} associated with this {@link
   * DefaultLoader}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the {@link AmbiguityHandler} associated with this {@link
   * DefaultLoader}; never {@code null}
   *
   * @nullability This method never returns {@code null}
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see AmbiguityHandler
   */
  public final AmbiguityHandler ambiguityHandler() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the AmbiguityHandler will not have
    // been loaded yet, and yet the bootstrapping mechanism may still
    // end up calling this.ambiguityHandler().  The alternative would
    // be to make the ambiguityHandler field non-final and I don't
    // want to do that.
    final AmbiguityHandler ambiguityHandler = this.ambiguityHandler;
    return ambiguityHandler == null ? NoOpAmbiguityHandler.INSTANCE : ambiguityHandler;
  }

  /**
   * Returns the {@link DefaultLoader} serving as the parent of this
   * {@link DefaultLoader}.
   *
   * <p>The "root" {@link DefaultLoader} returns itself from its
   * {@link #parent()} implementation.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the non-{@code null} {@link DefaultLoader} serving as the
   * parent of this {@link DefaultLoader}; may be this {@link
   * DefaultLoader} itself
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  // Note that the root will have itself as its parent.
  @Override // Loader<T>
  public final DefaultLoader<?> parent() {
    return this.parent;
  }

  /**
   * Returns the {@linkplain Path#absolute() absolute} {@link Path}
   * with which this {@link DefaultLoader} is associated.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the non-{@code null} {@linkplain Path#absolute()
   * absolute} {@link Path} with which this {@link DefaultLoader} is
   * associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  @Override // Loader<T>
  public final Path<? extends Type> path() {
    return this.absolutePath;
  }

  /**
   * Returns a {@link Determinism} suitable for this {@link
   * DefaultLoader}.
   *
   * @return a {@link Determinism} suitable for this {@link
   * DefaultLoader}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Determinism determinism() {
    final OptionalSupplier<?> s = this.supplier;
    return s == null ? Determinism.NON_DETERMINISTIC : s.determinism();
  }

  @Override // Loader<T>
  public final T get() {
    return this.supplier.get();
  }

  @Override // Loader<T>
  public final DefaultLoader<?> loaderFor(Path<? extends Type> path) {
    return (DefaultLoader<?>)Loader.super.loaderFor(path);
  }

  /**
   * Returns a {@link DefaultLoader} that can {@linkplain #get()
   * supply} environmental objects that are suitable for the supplied
   * {@code path}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The {@link DefaultLoader} that is returned may return {@code
   * null} from its {@link Loader#get() get()} method.  Additionally,
   * the {@link DefaultLoader} that is returned may throw {@link
   * java.util.NoSuchElementException} or {@link
   * UnsupportedOperationException} from its {@link #get() get()}
   * method.</p>
   *
   * @param <U> the type of the supplied {@link Path} and the type of
   * the returned {@link DefaultLoader}
   *
   * @param path the {@link Path} for which a {@link DefaultLoader}
   * should be returned; must not be {@code null}
   *
   * @return a {@link DefaultLoader} capable of {@linkplain #get()
   * supplying} environmental objects suitable for the supplied {@code
   * path}; never {@code null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception IllegalArgumentException if the {@code path}, after
   * {@linkplain #normalize(Path) normalization}, {@linkplain
   * Path#isRoot() is the root <code>Path</code>}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @threadsafety This method is idempotent and deterministic.
   */
  @Override // Loader<T>
  public final <U> DefaultLoader<U> load(final Path<? extends Type> path) {
    final Path<? extends Type> absolutePath = this.normalize(path);
    if (!absolutePath.absolute()) {
      throw new IllegalArgumentException("!normalize(path).absolute(): " + absolutePath);
    } else if (absolutePath.isRoot()) {
      throw new IllegalArgumentException("normalize(path).isRoot(): " + absolutePath);
    }
    final DefaultLoader<?> requestor = this.loaderFor(absolutePath);
    // We deliberately do not use computeIfAbsent() because load()
    // operations can kick off other load() operations, and then you'd
    // have a cache mutating operation occuring within a cache
    // mutating operation, which is forbidden.  Sometimes you get an
    // IllegalStateException as you are supposed to; other times you
    // do not, which is a JDK bug.  See
    // https://blog.jooq.org/avoid-recursion-in-concurrenthashmap-computeifabsent/.
    //
    // This obviously can result in unnecessary work, but most
    // configuration use cases will cause this work to happen anyway.
    DefaultLoader<?> defaultLoader = this.loaderCache.get(absolutePath);
    if (defaultLoader == null) {
      defaultLoader = this.loaderCache.putIfAbsent(absolutePath, this.computeLoader(requestor, absolutePath));
      if (defaultLoader == null) {
        defaultLoader = this.loaderCache.get(absolutePath);
      }
    }
    @SuppressWarnings("unchecked")
    final DefaultLoader<U> returnValue = (DefaultLoader<U>)defaultLoader;
    return returnValue;
  }

  @SuppressWarnings("unchecked")
  private final <U> DefaultLoader<U> computeLoader(final DefaultLoader<?> requestor, final Path<? extends Type> absolutePath) {
    final Qualifiers<? extends String, ?> qualifiers = absolutePath.qualifiers();
    final AmbiguityHandler ambiguityHandler = requestor.ambiguityHandler();
    Value<U> candidate = null;
    final Collection<? extends Provider> providers = this.providers();
    if (!providers.isEmpty()) {
      final Map<Path<? extends Type>, Deque<Provider>> map = currentProviderStacks.get();
      Provider candidateProvider = null;
      if (providers.size() == 1) {
        candidateProvider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
        if (candidateProvider == null) {
          ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
        } else if (candidateProvider == peek(map, absolutePath)) {
          // Behave the same as the null case immediately prior, but
          // there's no need to notify the ambiguityHandler.
        } else if (!isSelectable(candidateProvider, absolutePath)) {
          ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
        } else {
          push(map, absolutePath, candidateProvider);
          try {
            candidate = (Value<U>)candidateProvider.get(requestor, absolutePath);
          } finally {
            pop(map, absolutePath);
          }
          if (candidate == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
          } else if (!isSelectable(absolutePath, candidate.path())) {
            ambiguityHandler.valueRejected(requestor, absolutePath, candidateProvider, candidate);
          }
        }
      } else {
        int candidateQualifiersScore = Integer.MIN_VALUE;
        int candidatePathScore = Integer.MIN_VALUE;
        PROVIDER_LOOP:
        for (final Provider provider : providers) {

          if (provider == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          if (provider == peek(map, absolutePath)) {
            // Behave the same as the null case immediately prior, but
            // there's no need to notify the ambiguityHandler.
            continue PROVIDER_LOOP;
          }

          if (!isSelectable(provider, absolutePath)) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          Value<U> value;

          push(map, absolutePath, provider);
          try {
            value = (Value<U>)provider.get(requestor, absolutePath);
          } finally {
            pop(map, absolutePath);
          }

          if (value == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          // NOTE: INFINITE LOOP POSSIBILITY; read carefully!
          VALUE_EVALUATION_LOOP:
          while (true) {

            if (!isSelectable(absolutePath, value.path())) {
              ambiguityHandler.valueRejected(requestor, absolutePath, provider, value);
              break VALUE_EVALUATION_LOOP;
            }

            if (candidate == null) {
              candidate = value;
              candidateProvider = provider;
              candidateQualifiersScore = ambiguityHandler.score(qualifiers, candidate.qualifiers());
              candidatePathScore = ambiguityHandler.score(absolutePath, candidate.path());
              break VALUE_EVALUATION_LOOP;
            }

            // Let's score Qualifiers first, not paths.  This is an
            // arbitrary decision, but one grounded in reality: most
            // often qualifiers are both empty and so this is very
            // quick, and when they are not empty, in real-world
            // situations they're normally completely disjoint.  Get
            // all this out of the way early.
            //
            // We score qualifiers in a method devoted to them rather
            // than just folding the qualifier scoring into the path
            // scoring method because the scoring systems may produce
            // wildly different numbers, and if the path- and
            // qualifier-scoring systems use, for example, size() in
            // their algorithms, you don't want a huge pile of
            // qualifiers accidentally affecting a path score.
            final int valueQualifiersScore = ambiguityHandler.score(qualifiers, value.qualifiers());
            if (valueQualifiersScore < candidateQualifiersScore) {
              candidate = new Value<>(candidate, value);
              break VALUE_EVALUATION_LOOP;
            }

            if (valueQualifiersScore > candidateQualifiersScore) {
              candidate = new Value<>(value, candidate);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              // (No need to update candidatePathScore.)
              break VALUE_EVALUATION_LOOP;
            }

            // The Qualifiers scores were equal (extremely common).
            // Let's do paths.
            final int valuePathScore = ambiguityHandler.score(absolutePath, value.path());

            if (valuePathScore < candidatePathScore) {
              candidate = new Value<>(candidate, value);
              break VALUE_EVALUATION_LOOP;
            }

            if (valuePathScore > candidatePathScore) {
              candidate = new Value<>(value, candidate);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              candidatePathScore = valuePathScore;
              break VALUE_EVALUATION_LOOP;
            }

            final Value<U> disambiguatedValue =
              ambiguityHandler.disambiguate(requestor, absolutePath, candidateProvider, candidate, provider, value);

            if (disambiguatedValue == null) {
              // Couldn't disambiguate.  Drop both values.
              break VALUE_EVALUATION_LOOP;
            }

            if (disambiguatedValue.equals(candidate)) {
              candidate = new Value<>(candidate, value);
              break VALUE_EVALUATION_LOOP;
            }

            if (disambiguatedValue.equals(value)) {
              candidate = new Value<>(disambiguatedValue, candidate);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              candidatePathScore = valuePathScore;
              break VALUE_EVALUATION_LOOP;
            }

            // Disambiguation came up with an entirely different value, so
            // run it back through the while loop.
            value = disambiguatedValue;
            continue VALUE_EVALUATION_LOOP;

          }
        }
      }
    }
    return
      new DefaultLoader<>(this.loaderCache,
                          providers,
                          requestor, // parent
                          absolutePath,
                          candidate == null ? Absence.instance() : candidate,
                          ambiguityHandler);
  }

  @SuppressWarnings("unchecked")
  private final <X> X returnThis() {
    return (X)this;
  }


  /*
   * Static methods.
   */


  private static final <T> Collection<T> add(final Collection<? extends T> c, final T e) {
    if (c == null || c.isEmpty()) {
      return e == null ? List.of() : List.of(e);
    } else if (e == null) {
      return Collections.unmodifiableCollection(c);
    } else {
      final Collection<T> newC = new ArrayList<>(c.size() + 1);
      newC.addAll(c);
      newC.add(e);
      return Collections.unmodifiableCollection(newC);
    }
  }

  private static final Provider peek(final Map<?, ? extends Deque<Provider>> map,
                                     final Path<? extends Type> absolutePath) {
    final Queue<? extends Provider> q = map.get(absolutePath);
    return q == null ? null : q.peek();
  }

  private static final void push(final Map<Path<? extends Type>, Deque<Provider>> map,
                                 final Path<? extends Type> absolutePath,
                                 final Provider provider) {
    map.computeIfAbsent(absolutePath, ap -> new ArrayDeque<>(5)).push(provider);
  }

  private static final Provider pop(final Map<?, ? extends Deque<Provider>> map,
                                    final Path<? extends Type> absolutePath) {
    final Deque<Provider> dq = map.get(absolutePath);
    return dq == null ? null : dq.pop();
  }

  private static final boolean isSelectable(final Provider provider, final Path<? extends Type> absolutePath) {
    return CovariantSemantics.INSTANCE.assignable(provider.upperBound(), absolutePath.qualified());
  }

  static final Collection<Provider> loadedProviders() {
    return Loaded.providers;
  }

  private static final AmbiguityHandler loadedAmbiguityHandler() {
    return Loaded.ambiguityHandler;
  }

  /**
   * Returns {@code true} if the supplied {@code valuePath} is
   * <em>selectable</em> (for further consideration and scoring) with
   * respect to the supplied {@code absoluteReferencePath}.
   *
   * <p>This method calls {@link Path#endsWith(Path, BiPredicate)} on
   * the supplied {@code absoluteReferencePath} with {@code valuePath}
   * as its {@link Path}-typed first argument, and a {@link
   * BiPredicate} that returns {@code true} if and only if all of the
   * following conditions are true:</p>
   *
   * <ul>
   *
   * <li>Each {@link Element} has a {@linkplain Element#name()
   * name} that is either {@linkplain String#isEmpty() empty} or equal
   * to the other's.</li>
   *
   * <li>Either {@link Element} has a {@link Element#type() Type}
   * that is {@code null}, or the first {@link Element}'s {@link
   * Element#type() Type} {@link AssignableType#of(Type) is
   * assignable from} the second's.</li>
   *
   * <li>Either {@link Element} has {@code null} {@linkplain
   * Element#parameters() parameters} or each of the first {@link
   * Element}'s {@linkplain Element#parameters() parameters}
   * {@linkplain Class#isAssignableFrom(Class) is assignable from} the
   * second's corresponding parameter.</li>
   *
   * </ul>
   *
   * <p>In all other cases this method returns {@code false} or throws
   * an exception.</p>
   *
   * @param absoluteReferencePath the reference path; must not be
   * {@code null}; must be {@linkplain Path#absolute() absolute}
   *
   * @param valuePath the {@link Path} to test; must not be {@code
   * null}
   *
   * @return {@code true} if {@code valuePath} is selectable (for
   * further consideration and scoring) with respect to {@code
   * absoluteReferencePath}; {@code false} in all other cases
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code
   * absoluteReferencePath} {@linkplain Path#absolute() is not
   * absolute}
   */
  private static final boolean isSelectable(final Path<? extends Type> absoluteReferencePath, final Path<? extends Type> valuePath) {
    if (!absoluteReferencePath.absolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }
    final Qualifiers<?, ?> q1 = absoluteReferencePath.qualifiers();
    // Remember that a Path's Qualifiers include the Qualifiers of
    // every Path.Element in the Path.
    if (q1 != null && !q1.isEmpty()) {
      final Qualifiers<?, ?> q2 = valuePath.qualifiers();
      if (q2 != null && !q2.isEmpty() && q1.intersectionSize(q2) <= 0) {
        return false;
      }
    }
    return absoluteReferencePath.endsWith(valuePath, NamesAndTypesAreCompatibleBiPredicate.INSTANCE);
  }

  private static final <T> T throwNoSuchElementException() {
    throw new NoSuchElementException();
  }


  /*
   * Inner and nested classes.
   */


  private static final class Loaded {

    private static final List<Provider> providers =
      ServiceLoader.load(Provider.class, Provider.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .toList();

    private static final AmbiguityHandler ambiguityHandler =
      ServiceLoader.load(AmbiguityHandler.class, AmbiguityHandler.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .findFirst()
      .orElse(NoOpAmbiguityHandler.INSTANCE);

  }

  private static final class NoOpAmbiguityHandler implements AmbiguityHandler {

    private static final NoOpAmbiguityHandler INSTANCE = new NoOpAmbiguityHandler();

    private NoOpAmbiguityHandler() {
      super();
    }

  }

  private static final class NamesAndTypesAreCompatibleBiPredicate implements BiPredicate<Element<?>, Element<?>> {

    private static final NamesAndTypesAreCompatibleBiPredicate INSTANCE = new NamesAndTypesAreCompatibleBiPredicate();

    private NamesAndTypesAreCompatibleBiPredicate() {
      super();
    }

    @Override // BiPredicate<Element<?>, Element<?>>
    public final boolean test(final Element<?> e1, final Element<?> e2) {
      final String name1 = e1.name();
      if (!name1.isEmpty()) {
        final String name2 = e2.name();
        if (!name2.isEmpty() && !name1.equals(name2)) {
          // Empty names have special significance in that they
          // "match" any other name.
          return false;
        }
      }

      final Object o1 = e1.qualified();
      if (o1 == null) {
        return e2.qualified() == null;
      } else if (!(o1 instanceof Type) ||
                 !(e2.qualified() instanceof Type t2) ||
                 !CovariantSemantics.INSTANCE.assignable((Type)o1, t2)) {
        return false;
      }

      return true;
    }

  }

}
