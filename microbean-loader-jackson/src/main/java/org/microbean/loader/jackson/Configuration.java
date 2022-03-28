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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.qualifier.Qualifiers;

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

  public Configuration() {
    super();
    this.qualifiersMap = Map.of();
    this.any = new LinkedHashMap<>();
  }

  public final Qualifiers<? extends String, ?> qualifiers() {
    return this.qualifiers(List.of());
  }
  
  @SuppressWarnings("unchecked")
  public final Qualifiers<? extends String, ?> qualifiers(final Iterable<? extends String> names) {
    Map<? extends String, ?> map = this.qualifiersMap;
    if (map == null || map.isEmpty()) {
      return Qualifiers.of();
    }
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
    map.entrySet().removeIf(e -> e.getValue() instanceof Map);
    return Qualifiers.of(map); // TODO: check
  }

  protected final Object any(final String name) {
    return this.any.get(name);
  }
  
  @JsonAnySetter
  @SuppressWarnings("unchecked")
  private final void any(final String name, final Object value) {
    if (name.equals("@qualifiers")) {
      if (value == null) {
        this.qualifiersMap = Map.of();
      } else if (value instanceof Qualifiers) {
        this.qualifiersMap = ((Qualifiers<? extends String, ?>)value).toMap();
      } else if (value instanceof Map) {
        this.qualifiersMap = (Map<? extends String, ?>)value;
      } else {
        this.any.put(name, value);
      }
    } else {
      this.any.put(name, value);
    }
  }

  @Override // Object
  public int hashCode() {
    return Objects.hash(this.qualifiersMap, this.any);
  }

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

  @Override // Object
  public String toString() {
    return this.qualifiersMap + " " + this.any;
  }
  
}
