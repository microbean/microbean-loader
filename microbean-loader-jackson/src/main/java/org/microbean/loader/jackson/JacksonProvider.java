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
package org.microbean.loader.jackson;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.lang.reflect.Type;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.core.exc.InputCoercionException;

import com.fasterxml.jackson.core.type.TypeReference;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

/**
 * A partial {@link AbstractProvider} implementation backed by <a
 * href="https://github.com/FasterXML/jackson"
 * target="_top">Jackson</a>.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public abstract class JacksonProvider extends AbstractProvider {


  /*
   * Static fields.
   */


  private static final Logger logger = Logger.getLogger(JacksonProvider.class.getName());


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link JacksonProvider}.
   *
   * @see #JacksonProvider(Type)
   */
  protected JacksonProvider() {
    this(null);
  }

  /**
   * Creates a new {@link JacksonProvider}.
   *
   * @param lowerBound the {@linkplain #lowerBound() lower type bound}
   * of this {@link JacksonProvider} implementation; may be {@code
   * null}
   */
  protected JacksonProvider(final Type lowerBound) {
    super(lowerBound);
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Value} suitable for the supplied {@link Loader}
   * and {@link Path}, <strong>or {@code null} if there is no such
   * {@link Value} now and if there never will be such a {@link
   * Value}</strong> for the supplied arguments.
   *
   * <p>The following assertions will be true when this method is
   * called in the normal course of events:</p>
   *
   * <ul>
   *
   * <li>{@code assert absolutePath.absolute();}</li>
   *
   * <li>{@code assert
   * absolutePath.startsWith(requestor.absolutePath());}</li>
   *
   * <li>{@code assert
   * !absolutePath.equals(requestor.absolutePath());}</li>
   *
   * </ul>
   *
   * <p>This implementation first {@linkplain #objectCodec(Loader,
   * Path) acquires} an {@link ObjectCodec} and then {@linkplain
   * #rootNode(Loader, Path, ObjectCodec) uses it to acquire the
   * root <code>TreeNode</code> of a document}.  With this {@link
   * TreeNode} in hand, it treats the supplied {@code absolutePath} as
   * a series of names terminating in a type, much like, in principle,
   * <a href="https://datatracker.ietf.org/doc/html/rfc6901"
   * target="_parent">JSON Pointer</a>.</p>
   *
   * <p>TODO: FINISH</p>
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @return a {@link Value} more or less suitable for the combination
   * of the supplied {@link Loader} and {@link Path}, or {@code
   * null} if there is no such {@link Value} now <strong>and if there
   * never will be such a {@link Value}</strong> for the supplied
   * arguments
   *
   * @exception NullPointerException if either {@code requestor} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#absolute() is not absolute}
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
  @Override // AbstractProvider<Object>
  public Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());

    final int size = absolutePath.size();
    assert size > 1; // follows from the above

    final ObjectCodec reader = this.objectCodec(requestor, absolutePath);
    if (reader != null) {

      TreeNode node = this.rootNode(requestor, absolutePath, reader);
      if (node != null) {

        Element<?> element = absolutePath.get(0);
        assert element.isRoot();

        String nextName = absolutePath.get(1).name();

        final List<Element<?>> valuePathElements = new ArrayList<>(size);
        valuePathElements.add(element);

        TreeNode qualifiersNode = node.path("@qualifiers");
        final Qualifiers<? extends String, ?> valuePathQualifiers = qualifiers(reader, qualifiersNode, nextName);
        qualifiersNode = qualifiersNode.path(nextName);

        boolean containerNode = node.isContainerNode();

        for (int i = 1; i < size; i++) {
          element = absolutePath.get(i);
          final String name = element.name();

          if (name.isEmpty()) {
            // Empty name means "the current node". The node remains
            // what it was.
            valuePathElements.add(element);
            continue;
          }

          if (!containerNode) {
            // The name was non-empty, and the prior node was not a
            // container, so we would be trying to dereference the
            // name against a value node or a missing node, either
            // of which is impossible.
            node = null;
            break;
          }

          if (!node.isContainerNode()) {
            // The next path element may have an empty name, in which
            // case it will refer to this node, so the fact that it is
            // not a container is still as of this moment OK.  Record
            // this fact so we can make sure on the next pass.
            valuePathElements.add(element);
            containerNode = false;
            continue;
          }

          if (node.isArray()) {
            node = handleArrayNode(element, node);
          } else if (node.isObject()) {
            node = node.get(name);
          } else {
            throw new AssertionError();
          }
          if (node == null) {
            break;
          }

          nextName = i + 1 < size ? absolutePath.get(i + 1).name() : null;
          valuePathElements.add(Element.of(qualifiers(reader, qualifiersNode, nextName),
                                           element.qualified(),
                                           name));
          qualifiersNode = qualifiersNode.path(nextName);
        }

        if (node != null) {
          @SuppressWarnings("unchecked")
          final Element<? extends Type> lastElement =
            (Element<? extends Type>)valuePathElements.remove(valuePathElements.size() - 1);
          final Path<? extends Type> valuePath = Path.of(valuePathQualifiers, valuePathElements, lastElement);
          try {
            return
              value(node.traverse(reader).readValueAs(new TypeReference<>() {
                  // This is a slight abuse of the
                  // TypeReference class, but the getType()
                  // method is not final and it is public,
                  // so this seems to be at least possible
                  // using a public API. It also avoids
                  // type loss we'd otherwise incur (if we
                  // used readValueAs(Class), for example).
                  @Override
                  public final Type getType() {
                    return absolutePath.qualified();
                  }
                }),
                valuePath);
          } catch (final JsonProcessingException jpe) {
            if (logger.isLoggable(Level.FINE)) {
              logger.logp(Level.FINE, this.getClass().getName(), "get", jpe.getMessage(), jpe);
            }
          } catch (final IOException ioException) {
            throw new UncheckedIOException(ioException.getMessage(), ioException);
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns an {@link ObjectCodec} suitable for the combination of
   * the supplied {@link Loader} and {@link Path}, or {@code null}
   * if there is no such {@link ObjectCodec}.
   *
   * <p>This method is called by the {@link #get(Loader, Path)}
   * method in the normal course of events.</p>
   *
   * @param <T> the type of the supplied {@link Path}
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @return an {@link ObjectCodec} suitable for the combination of
   * the supplied {@link Loader} and {@link Path}, or {@code null}
   *
   * @nullability Implementations of this method may return {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent,
   * but not necessarily deterministic.
   */
  protected abstract <T> ObjectCodec objectCodec(final Loader<?> requestor, final Path<? extends Type> absolutePath);

  /**
   * Returns a {@link TreeNode} representing the root of an abstract
   * document suitable for the combination of the supplied {@link
   * Loader} and {@link Path}, or {@code null} if there is no such
   * {@link TreeNode}.
   *
   * <p>This method will not be called by the {@link #get(Loader,
   * Path)} method if the {@link #objectCodec(Loader, Path)}
   * method returns {@code null}.  Otherwise, it will be called
   * immediately afterwards on the same thread.</p>
   *
   * @param <T> the type of the supplied {@link Path}
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @param reader for convenience, the {@link ObjectCodec} returned
   * by this {@link JacksonProvider}'s {@link #objectCodec(Loader,
   * Path)} method; must not be {@code null}
   *
   * @return a {@link TreeNode} representing the root of an abstract
   * document suitable for the combination of the supplied {@link
   * Loader} and {@link Path}, or {@code null}
   *
   * @nullability Implementations of this method may return {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent,
   * but not necessarily deterministic.
   */
  protected abstract <T> TreeNode rootNode(final Loader<?> requestor, final Path<? extends Type> absolutePath, final ObjectCodec reader);


  /*
   * Static methods.
   */


  private static final Qualifiers<? extends String, ?> qualifiers(final ObjectCodec reader,
                                                                  final TreeNode qualifiersNode,
                                                                  final String nextPathElementName) {
    if (qualifiersNode == null || qualifiersNode.size() <= 0) {
      return Qualifiers.of();
    } else {
      final Map<String, Object> qualifiersMap = new HashMap<>();
      final Iterator<? extends String> fieldNamesIterator = qualifiersNode.fieldNames();
      while (fieldNamesIterator.hasNext()) {
        final String qualifierKey = fieldNamesIterator.next();
        final TreeNode qualifierValueNode = qualifiersNode.get(qualifierKey);
        assert qualifierValueNode != null; // ...or it shouldn't have been iterated
        assert !qualifierValueNode.isMissingNode(); // never returned by get()
        if (!qualifierValueNode.isObject() || !qualifierKey.equals(nextPathElementName)) {
          try {
            final Object qualifierValue = qualifierValueNode.traverse(reader).readValueAs(Object.class);
            if (qualifierValue != null) {
              qualifiersMap.put(qualifierKey, qualifierValue);
            }
          } catch (final IOException ioException) {
            throw new UncheckedIOException(ioException.getMessage(), ioException);
          }
        }
      }
      return qualifiersMap.isEmpty() ? Qualifiers.of() : Qualifiers.of(qualifiersMap);
    }
  }

  private static final TreeNode handleArrayNode(final Element<?> element, TreeNode node) {
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
    return node.get(index);
  }

  private static final <T> Value<T> value(final T value, final Path<? extends Type> valuePath) {
    return new Value<>(value, valuePath);
  }

}
