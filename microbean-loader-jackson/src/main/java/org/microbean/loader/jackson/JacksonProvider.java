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

import java.util.function.BiFunction;

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

import org.microbean.loader.spi.AbstractTreeBasedProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

/**
 * A partial {@link AbstractTreeBasedProvider} implementation backed
 * by <a href="https://github.com/FasterXML/jackson"
 * target="_top">Jackson</a>.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public abstract class JacksonProvider extends AbstractTreeBasedProvider<TreeNode> {


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


  @Override // AbstractTreeBasedProvider<TreeNode>
  public final int size(final TreeNode node) {
    return node == null ? 0 : node.size();
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final Iterator<String> names(final TreeNode node) {
    return node == null ? Collections.emptyIterator() : node.fieldNames();
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final TreeNode get(final TreeNode node, final String name) {
    return node == null ? null : node.get(name); // ...or path(name)
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final TreeNode get(final TreeNode node, final int index) {
    return node == null ? null : node.get(index);
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final boolean absent(final TreeNode node) {
    return node == null || node.isMissingNode();
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final boolean nil(final TreeNode node) {
    return node == null || node.asToken() == JsonToken.VALUE_NULL;
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final boolean map(final TreeNode node) {
    return node != null && node.isObject();
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  public final boolean list(final TreeNode node) {
    return node != null && node.isArray();
  }

  @Override // AbstractTreeBasedProvider<TreeNode>
  protected TreeNode qualifiers(final TreeNode node) {
    return get(node, "@qualifiers");
  }

  /**
   * Returns an {@link ObjectCodec} suitable for the combination of
   * the supplied {@link Loader} and {@link Path}, or {@code null}
   * if there is no such {@link ObjectCodec}.
   *
   * <p>This method is called by the {@link #get(Loader, Path)}
   * method in the normal course of events.</p>
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
  protected abstract ObjectCodec objectCodec(final Loader<?> requestor, final Path<? extends Type> absolutePath);

  @Override // AbstractTreeBasedProvider<TreeNode>
  protected BiFunction<? super TreeNode, ? super Type, ?> reader(final Loader<?> requestor,
                                                                 final Path<? extends Type> absolutePath) {
    final ObjectCodec reader = this.objectCodec(requestor, absolutePath);
    if (reader == null) {
      return null;
    }
    return (treeNode, type) -> {
      try {
        if (type instanceof Class<?> c) {
          return treeNode.traverse(reader).readValueAs(c);
        } else {
          return treeNode.traverse(reader).readValueAs(new TypeReference<>() {
              // This is a slight abuse of the TypeReference class, but
              // the getType() method is not final and it is public, so
              // this seems to be at least possible using a public
              // API. It also avoids type loss we'd otherwise incur (if
              // we used readValueAs(Class), for example).
              @Override // TypeReference<T>
              public final Type getType() {
                return type;
              }
            });
        }
      } catch (final IOException ioException) {
        throw new UncheckedIOException(ioException.getMessage(), ioException);
      }
    };
  }

}
