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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.qualifier.Qualifier;
import org.microbean.qualifier.Qualifiers;

/**
 * A Java object that represents a tree-like configuration structure,
 * using Jackson mapping constructs to make things easier.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
@Experimental
@Incomplete
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Configuration {

  private Map<? extends String, ?> qualifiersMap;

  private final Map<String, Object> any;

  /**
   * Creates a new {@link Configuration}.
   */
  public Configuration() {
    super();
    this.qualifiersMap = Map.of();
    this.any = new LinkedHashMap<>();
  }

  /**
   * Returns a {@link Qualifiers} representing all qualifiers in the
   * {@link Configuration}.
   *
   * @return a {@link Qualifiers} representing all qualifiers in the
   * {@link Configuration}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is <strong>not</strong> safe for
   * concurrent use by multiple threads.
   */
  public final Qualifiers<? extends String, ?> qualifiers() {
    return this.qualifiers(List.of());
  }

  /**
   * Returns a {@link Qualifiers} representing all qualifiers in the
   * {@link Configuration} associated with the supplied sequence of
   * names.
   *
   * @param names a sequence of names identifying a path to the root
   * of a tree of qualifiers; may be {@code null}
   *
   * @return a {@link Qualifiers} representing all qualifiers in the
   * {@link Configuration} associated with the supplied sequence of
   * names
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is <strong>not</strong> safe for
   * concurrent use by multiple threads.
   */
  @SuppressWarnings("unchecked")
  public final Qualifiers<? extends String, ?> qualifiers(final Iterable<? extends String> names) {
    Map<? extends String, ?> map = this.qualifiersMap;
    if (map == null || map.isEmpty()) {
      return Qualifiers.of();
    }
    map = new LinkedHashMap<>(map);
    if (names != null) {
      for (final String name : names) {
        Object value = map.get(name);
        if (value == null) {
          return Qualifiers.of();
        } else if (value instanceof Map) {
          map = (Map<? extends String, ?>)value;
        }
      }
    }
    final Collection<Qualifier<String, Object>> c = new TreeSet<>();
    for (final Entry<? extends String, ?> e : map.entrySet()) {
      final Object value = e.getValue();
      if (!(value instanceof Map)) {
        c.add(Qualifier.of(e.getKey(), value));
      }
    }
    return Qualifiers.of(c);
  }

  /**
   * Returns an {@link Object} in this {@link Configuration} indexed
   * under the supplied {@code name}.
   *
   * @param name the name; must not be {@code null}
   *
   * @return the associated {@link Object}, or {@code null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is <strong>not</strong> safe for
   * concurrent use by multiple threads.
   */
  protected final Object any(final String name) {
    return this.any.get(name);
  }

  @JsonAnySetter
  @SuppressWarnings("unchecked")
  private final void any(final String name, final Object value) {
    if (name.equals("@qualifiers")) {
      if (value == null) {
        this.qualifiersMap = Map.of();
      } else if (value instanceof Map) {
        this.qualifiersMap = (Map<? extends String, ?>)value;
      } else {
        this.any.put(name, value);
      }
    } else {
      this.any.put(name, value);
    }
  }

  /**
   * Returns a hashcode for this {@link Configuration}.
   *
   * @return a hashcode for this {@link Configuration}
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is <strong>not</strong> safe for
   * concurrent use by multiple threads.
   */
  @Override // Object
  public int hashCode() {
    return Objects.hash(this.qualifiersMap, this.any);
  }

  /**
   * Returns {@code true} if the supplied {@link Object} is equal to
   * this {@link Configuration}.
   *
   * @param other the {@link Object} to test; may be {@code null}
   *
   * @return {@code true} if the supplied {@link Object} is equal to
   * this {@link Configuration}
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is <strong>not</strong> safe for
   * concurrent use by multiple threads.
   */
  @Override // Object
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      final Configuration her = (Configuration)other;
      return
        Objects.equals(this.qualifiersMap, her.qualifiersMap) &&
        Objects.equals(this.any, her.any);
    } else {
      return false;
    }
  }

  /**
   * Returns a {@link String} representation of this {@link
   * Configuration}.
   *
   * <p>The format of the returned {@link String} is deliberately
   * undefined and subject to change between revisions of this class
   * without notice.</p>
   *
   * @return a {@link String} representation of this {@link
   * Configuration}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is <strong>not</strong> safe for
   * concurrent use by multiple threads.
   */
  @Override // Object
  public String toString() {
    return this.qualifiersMap + " " + this.any;
  }

}
