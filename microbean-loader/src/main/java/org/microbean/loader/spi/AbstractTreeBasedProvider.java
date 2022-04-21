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

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.microbean.invoke.FixedValueSupplier;

import org.microbean.qualifier.Qualifiers;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

/**
 * An abstract {@link AbstractProvider} whose implementations are
 * built around tree structures of various kinds.
 *
 * @param <N> the type of a node in the tree
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public abstract class AbstractTreeBasedProvider<N> extends AbstractProvider {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AbstractTreeBasedProvider} with no
   * {@linkplain #lowerBound() lower type bound}.
   */
  protected AbstractTreeBasedProvider() {
    this(null);
  }

  /**
   * Creates a new {@link AbstractTreeBasedProvider} with the supplied
   * {@linkplain Provider#lowerBound() lower type bound}.
   *
   * @param lowerTypeBound the {@linkplain #lowerBound() lower type
   * bound}; may be {@code null}
   */
  protected AbstractTreeBasedProvider(final Type lowerTypeBound) {
    super(lowerTypeBound);
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the number of child nodes the supplied {@code node} has,
   * which may be (and often is) {@code 0}.
   *
   * <p>If the supplied {@code null} is {@code null}, {@linkplain
   * #absent(Object) absent} or a scalar, an override of this method
   * must return {@code 0}.</p>
   *
   * @param node the parent node; may be {@code null} in which case
   * {@code 0} must be returned
   *
   * @return the number of child nodes the supplied {@code node} has,
   * which may be (and often is) {@code 0}
   *
   * @nullability Overrides of this method may return {@code null}.
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  // returns 0 if the node is null, missing, scalar
  protected abstract int size(final N node);

  /**
   * Returns an {@link Iterator} over the names of the supplied {@code
   * node}'s child nodes, or an {@linkplain
   * java.util.Collections#emptyIterator() empty
   * <code>Iterator</code>} if the supplied {@code node} is
   * {@linkplain #map(node) not a map node}.
   *
   * @param node the parent node; may be {@code null} in which case an
   * {@linkplain java.util.Collections#emptyIterator() empty
   * <code>Iterator</code>} must be returned
   *
   * @return an {@link Iterator}; never {@code null}
   *
   * @nullability Overrides of this method must not return {@code
   * null}.
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract Iterator<String> names(final N node);

  /**
   * Returns the child node of the supplied parent {@code node}
   * identified by the supplied {@code name}, or {@code null} if
   * either the child does not exist or {@code node} is not a
   * {@linkplain #map(Object) map node}.
   *
   * @param node the parent node; may be {@code null} in which case
   * {@code null} must be returned
   *
   * @param name the name of the child; must not be {@code null}
   *
   * @return the child node of the supplied parent {@code node}
   * identified by the supplied {@code name}, or {@code null} if
   * either the child does not exist or {@code node} is not a
   * {@linkplain #map(Object) map node}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @nullability Overrides of this method may (and often do) return
   * {@code null}.
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract N get(final N node, final String name);

  /**
   * Returns the child node of the supplied parent {@code node}
   * identified by the supplied {@code index}, or {@code null} if
   * either the child does not exist or {@code node} is not a
   * {@linkplain #list(Object) list node}.
   *
   * @param node the parent node; may be {@code null} in which case
   * {@code null} must be returned
   *
   * @param index the zero-based index of the child
   *
   * @return the child node of the supplied parent {@code node}
   * identified by the supplied {@code name}, or {@code null} if
   * either the child does not exist or {@code node} is not a
   * {@linkplain #map(Object) map node}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception IndexOutOfBoundsException if {@code index} is invalid
   *
   * @nullability Overrides of this method may (and often do) return
   * {@code null}.
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract N get(final N node, final int index);

  /**
   * Returns {@code true} if the supplied {@code node} is {@code
   * null}, absent or synthetic.
   *
   * @param node the node to test; may be {@code null} in which case
   * {@code true} must be returned
   *
   * @return {@code true} if the supplied {@code node} is {@code
   * null}, absent or synthetic
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract boolean absent(final N node);

  /**
   * Returns {@code true} if the supplied {@code node} is {@code null}
   * or represents an explicitly set {@code null} value.
   *
   * @param node the node to test; may be {@code null} in which case
   * {@code true} must be returned
   *
   * @return {@code true} if the supplied {@code node} is {@code null}
   * or represents an explicitly set {@code null} value
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract boolean nil(final N node);

  /**
   * Returns {@code true} if the supplied {@code node} represents a
   * map, and therefore the {@link #get(Object, String)} method is
   * likely to be relevant.
   *
   * @param node the node to test; may be {@code null} in which case
   * {@code false} must be returned
   *
   * @return {@code true} if the supplied {@code node} represents a
   * map, and therefore the {@link #get(Object, String)} method is
   * likely to be relevant
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract boolean map(final N node);

  /**
   * Returns {@code true} if the supplied {@code node} represents a
   * list or an array, and therefore the {@link #get(Object, int)}
   * method is likely to be relevant.
   *
   * @param node the node to test; may be {@code null} in which case
   * {@code false} must be returned
   *
   * @return {@code true} if the supplied {@code node} represents a
   * list or an array, and therefore the {@link #get(Object, int)}
   * method is likely to be relevant
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract boolean list(final N node);

  /**
   * Returns {@code true} if {@code node} is either a {@linkplain
   * #map(Object) map} or a {@linkplain #list(Object) list or array}.
   *
   * @param node the node to test; may be {@code null} in which case
   * {@code false} must be returned
   *
   * @return {@code true} if {@code node} is either a {@linkplain
   * #map(Object) map} or a {@linkplain #list(Object) list or array}
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected final boolean container(final N node) {
    return !nil(node) && (map(node) || list(node));
  }

  /**
   * Returns a {@link BiFunction} accepting a node and a {@link Type}
   * and returning the result of reading an object of that type, or
   * {@code null} if no such {@link BiFunction} could be sourced.
   *
   * @param requestor the {@link Loader} currently executing a
   * request; must not be {@code null}
   *
   * @param absolutePath the path being requested; must not be {@code
   * null} and must be {@linkplain Path#absolute() absolute}
   *
   * @return a {@link BiFunction} accepting a node and a {@link Type}
   * and returning the result of reading an object of that type, or
   * {@code null} if no such {@link BiFunction} could be sourced
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability Overrides of this method may return {@code null}.
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract BiFunction<? super N, ? super Type, ?> reader(final Loader<?> requestor,
                                                                   final Path<? extends Type> absolutePath);

  /**
   * Returns the root node of the tree that is suitable for the
   * supplied {@link Loader} and {@link Path}, or {@code null} if no
   * tree is suitable.
   *
   * @param requestor the {@link Loader} currently executing a
   * request; must not be {@code null}
   *
   * @param absolutePath the path being requested; must not be {@code
   * null} and must be {@linkplain Path#absolute() absolute}
   *
   * @return the root node, or {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability Overrides of this method may return {@code null}.
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract N rootNode(final Loader<?> requestor, final Path<? extends Type> absolutePath);

  /**
   * Returns a node possibly containing qualifiers applicable to the
   * supplied node, or {@code null}.
   *
   * @param node the node for which a corresponding qualifiers node
   * should be returned; may be {@code null} in which case {@code
   * null} must be returned
   *
   * @return a node possibly containing qualifiers applicable to the
   * supplied node, or {@code null}
   *
   * @nullability Overrides of this method may (and often do) return
   * {@code null}
   *
   * @idempotency Overrides of this method must be idempotent and
   * deterministic.
   *
   * @threadsafety Overrides of this method must be safe for
   * concurrent use by multiple threads.
   */
  protected abstract N qualifiers(final N node);

  /**
   * Returns a {@linkplain FixedValueSupplier#of(Supplier)
   * deterministic <code>Supplier</code>} suitable for the current
   * request, represented by the supplied {@code absolutePath}, as
   * being executed by the supplied {@link Loader}, or {@code null} if
   * no such {@link Supplier} is or ever will be suitable.
   *
   * @param requestor the {@link Loader} currently executing a
   * request; must not be {@code null}
   *
   * @param absolutePath the path being requested; must not be {@code
   * null} and must be {@linkplain Path#absolute() absolute}
   *
   * @return a {@link Supplier}, or {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @idempotency No guarantees are made of either idempotency or
   * determinism, either of this implementation or of its overrides.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see Provider#get(Loader, Path)
   */
  @Override // Provider
  protected Supplier<?> find(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());

    final int size = absolutePath.size();
    assert size > 1; // follows from the above

    N node = this.rootNode(requestor, absolutePath);
    if (node != null) {

      final BiFunction<? super N, ? super Type, ?> reader = this.reader(requestor, absolutePath);
      if (reader != null) {

        boolean containerNode = container(node);

        for (int i = 1; i < size; i++) { // note that i is 1 on purpose; we skip the first root-designating element
          final Element<?> element = absolutePath.get(i);
          final String name = element.name();

          if (name.isEmpty()) {
            // Empty name means "the current node". The node remains
            // what it was.
            continue;
          }

          if (!containerNode) {
            // The name was non-empty, and the prior node was not a
            // container, so we would be trying to dereference the
            // name against a value node or a missing node, either of
            // which is impossible.
            node = null;
            break;
          }

          if (!container(node)) {
            // The next path element may have an empty name, in which
            // case it will refer to this node, so the fact that it is
            // not a container is still as of this moment OK.  Record
            // this fact so we can make sure on the next pass.
            // valuePathElements.add(element);
            containerNode = false;
            continue;
          }

          if (list(node)) {
            node = handleListNode(element, node);
          } else if (map(node)) {
            node = get(node, name);
          } else {
            throw new AssertionError();
          }
          if (node == null) {
            break;
          }

        }

        if (node != null) {
          return FixedValueSupplier.of(reader.apply(node, absolutePath.qualified()));
        }
      }
    }
    return null;
  }

  /**
   * Calls the {@link #path(Loader, Path, BiFunction)} method with the
   * supplied arguments and the return value of an invocation of the
   * {@link #reader(Loader, Path)} method and returns its result.
   *
   * @param requestor the {@link Loader} currently executing a
   * request; must not be {@code null}
   *
   * @param absolutePath the path being requested; must not be {@code
   * null} and must be {@linkplain Path#absolute() absolute}
   *
   * @exception NullPointerException if {@code absolutePath} or {@code
   * reader} is {@code null}
   *
   * @nullability This method does not return {@code null}.
   *
   * @idempotency No guarantees about idempotency or determinism are
   * made of this method.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #path(Loader, Path, BiFunction)
   *
   * @see #reader(Loader, Path)
   */
  @Override
  protected final <T extends Type> Path<T> path(final Loader<?> requestor, final Path<T> absolutePath) {
    return this.path(requestor, absolutePath, this.reader(requestor, absolutePath));
  }

  /**
   * Returns a {@link Path} for a new {@link Value} that will be
   * returned by the {@link #get(Loader, Path)} method.
   *
   * <p>The default implementation of this method attempts to build a
   * {@link Path} with the same {@linkplain Path#size() sizze} as the
   * supplied {@link Path} and differing, perhaps, only in the {@link
   * Qualifiers} assigned to each {@link Element}.</p>
   *
   * <p>Overrides of this method must not call the {@link
   * #path(Loader, Path)} or the {@link #get(Loader, Path)} methods or
   * undefined behavior, such as an infinite loop, may result.</p>
   *
   * @param <T> the type of the supplied {@link Path} and the returned
   * {@link Path}
   *
   * @param requestor the {@link Loader} currently executing a
   * request; must not be {@code null}
   *
   * @param absolutePath the path being requested; must not be {@code
   * null} and must be {@linkplain Path#absolute() absolute}
   *
   * @param reader a {@link BiFunction} accepting a node and a {@link
   * Type} and returning the result of reading an object of that type;
   * may be {@code null}
   *
   * @return a {@link Path}; never {@code null}; sometimes simply the
   * supplied {@code absolutePath}
   *
   * @exception NullPointerException if {@code absolutePath} or {@code
   * reader} is {@code null}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency No guarantees about idempotency or determinism are
   * made of this method or its overrides.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   */
  protected <T extends Type> Path<T> path(final Loader<?> requestor,
                                          final Path<T> absolutePath,
                                          final BiFunction<? super N, ? super Type, ?> reader) {
    final int size = absolutePath.size();
    if (size <= 1) {
      return absolutePath;
    }

    N node = this.rootNode(requestor, absolutePath);
    if (node == null) {
      return absolutePath;
    }

    Element<?> element = absolutePath.get(0);
    assert element.isRoot();

    String nextName = absolutePath.get(1).name();

    final List<Element<?>> valuePathElements = new ArrayList<>(size);
    valuePathElements.add(element);

    N qualifiersNode = qualifiers(node); // often gets a child of this node named "@qualifiers"
    final Qualifiers<? extends String, ?> valuePathQualifiers = this.qualifiers(reader, qualifiersNode, nextName);
    qualifiersNode = this.get(qualifiersNode, nextName);

    boolean containerNode = this.container(node);

    for (int i = 1; i < size; i++) {
      element = absolutePath.get(i);
      final String name = element.name();

      if (name.isEmpty()) {
        // Empty name means "the current node". The node remains what
        // it was.
        valuePathElements.add(element);
        continue;
      }

      if (!containerNode) {
        // The name was non-empty, and the prior node was not a
        // container, so we would be trying to dereference the name
        // against a value node or a missing node, either of which is
        // impossible.
        return absolutePath;
      }

      if (!this.container(node)) {
        // The next path element may have an empty name, in which case
        // it will refer to this node, so the fact that it is not a
        // container is still as of this moment OK.  Record this fact
        // so we can make sure on the next pass.
        valuePathElements.add(element);
        containerNode = false;
        continue;
      }

      final boolean hasNext = i + 1 < size;
      nextName = hasNext ? absolutePath.get(i + 1).name() : null;
      valuePathElements.add(Element.of(qualifiers(reader, qualifiersNode, nextName),
                                       element.qualified(),
                                       name));
      qualifiersNode = this.get(qualifiersNode, nextName);
    }
    @SuppressWarnings("unchecked")
    final Element<T> last = (Element<T>)valuePathElements.remove(valuePathElements.size() - 1);
    return Path.of(valuePathQualifiers, valuePathElements, last);
  }

  /**
   * Returns a {@link Qualifiers} derived from the supplied {@code
   * qualifiersNode}.
   *
   * @param reader a {@link BiFunction} accepting a node and a {@link
   * Type} and returning the result of reading an object of that type;
   * may be {@code null}
   *
   * @param qualifiersNode a node {@linkplain #names(Object)
   * containing} qualifier entries; may be {@code null}
   *
   * @param nextPathElementName the name of the next path element; may
   * be {@code null}
   *
   * @return a {@link Qualifiers}; never {@code null}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   */
  protected Qualifiers<? extends String, ?> qualifiers(final BiFunction<? super N, ? super Type, ?> reader,
                                                       final N qualifiersNode,
                                                       final String nextPathElementName) {
    if (qualifiersNode == null || reader == null || size(qualifiersNode) <= 0) {
      return Qualifiers.of();
    } else {
      final Map<String, Object> qualifiersMap;
      final Iterator<? extends String> fieldNamesIterator = names(qualifiersNode);
      if (fieldNamesIterator.hasNext()) {
        qualifiersMap = new HashMap<>();
        while (fieldNamesIterator.hasNext()) {
          final String qualifierKey = fieldNamesIterator.next();
          final N qualifierValueNode = get(qualifiersNode, qualifierKey);
          assert qualifierValueNode != null; // ...or it shouldn't have been iterated
          assert !absent(qualifierValueNode); // never returned by get()
          if (!map(qualifierValueNode) || !qualifierKey.equals(nextPathElementName)) {
            final Object qualifierValue = reader.apply(qualifierValueNode, Object.class);
            if (qualifierValue != null) {
              qualifiersMap.put(qualifierKey, qualifierValue);
            }
          }
        }
      } else {
        qualifiersMap = Map.of();
      }
      return qualifiersMap.isEmpty() ? Qualifiers.of() : Qualifiers.of(qualifiersMap);
    }
  }

  private final N handleListNode(final Element<?> element, N node) {
    final String key;
    final Qualifiers<String, Object> q = element.qualifiers();
    if (q.containsKey("index")) {
      key = "index";
    } else if (q.containsKey("arg0")) {
      key = "arg0";
    } else {
      return null;
    }
    final Object o = q.get(key);
    assert o != null; // qualifiers can't contain null values
    int index;
    if (o instanceof Number n) {
      index = n.intValue();
    } else {
      try {
        index = Integer.parseInt(o.toString());
      } catch (final NumberFormatException numberFormatException) {
        return null;
      }
    }
    return get(node, index);
  }

}
