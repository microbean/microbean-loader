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

import java.util.Iterator;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;

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
   * @param lowerBound the {@linkplain #lowerBound() lower type bound}
   * of this {@link JacksonProvider} implementation; may be {@code
   * null}
   */
  protected JacksonProvider(final Type lowerBound) {
    super();
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
  public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());
    assert absolutePath.size() > 1; // follows from the above
    final ObjectCodec reader = this.objectCodec(requestor, absolutePath);
    if (reader != null) {
      TreeNode node = this.rootNode(requestor, absolutePath, reader);
      if (node != null) {
        TreeNode qualifiersNode = null;
        final Iterator<Element<?>> elementIterator = absolutePath.iterator();
        elementIterator.next(); // skip the root element; we know size() > 1
        node = node(elementIterator, node);
        if (node != null) {
          try {
            return this.value(requestor, absolutePath, node.traverse(reader).readValueAs(new TypeReference<>() {
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
              }));
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

  private static final TreeNode node(final Iterator<? extends Element<?>> elementIterator, TreeNode node) {
    boolean containerNode = node.isContainerNode();
    while (elementIterator.hasNext()) {
      final Element<?> element = elementIterator.next();
      final String name = element.name();
      if (name.isEmpty()) {
        // Empty name means "the current node". Do nothing. The
        // node remains what it was.
        continue;
      } else if (!containerNode) {
        // The name was non-empty, and the prior node was not a
        // container, so we would be trying to dereference the
        // name against a value node or a missing node, either
        // of which is impossible.
        node = null;
      } else if (!node.isContainerNode()) {
        containerNode = false;
      } else if (elementIterator.hasNext()) {
        if (node.isArray()) {
          node = handleArrayNode(element, node);
        } else if (node.isObject()) {
          node = node.get(name);
        } else {
          throw new AssertionError();
        }
      } else if (node.isArray()) {
        node = handleArrayNode(element, node);
      } else if (node.isObject()) {
        node = node.get(name);
      } else {
        throw new AssertionError();
      }
      if (node == null) {
        break;
      }
    }
    return node;
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
    return index < 0 ? null : node.get(index);
  }

  /**
   * Returns a {@linkplain Value#determinism() deterministic} {@link
   * Value} suitable for the combination of the supplied {@link
   * Loader}, {@link Path} and object.
   *
   * @param <T> the type of the supplied {@link Path} and hence also
   * of the returned {@link Value}
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @param value the object the returned {@link Value} will
   * {@linkplain Value#get() supply}; may be {@code null}
   *
   * @return a new {@link Value}; never {@code null}
   *
   * @exception NullPointerException if {@code requestor} or {@code
   * absolutePath} is {@code null}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   */
  @SuppressWarnings("unchecked")
  protected <T> Value<T> value(final Loader<?> requestor,
                               final Path<? extends Type> absolutePath,
                               final T value) {
    return new Value<>(value, Path.of(absolutePath.qualified()));
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

}
