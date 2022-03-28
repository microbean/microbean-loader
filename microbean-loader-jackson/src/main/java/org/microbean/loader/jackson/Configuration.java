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
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.microbean.qualifier.Qualifiers;

@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Configuration {

  private Qualifiers<? extends String, ?> qualifiers;

  private final Map<String, Object> any;

  public Configuration() {
    super();
    this.qualifiers = Qualifiers.of();
    this.any = new LinkedHashMap<>();
  }

  public Qualifiers<? extends String, ?> qualifiers() {
    return this.qualifiers;
  }

  protected final Object any(final String name) {
    return this.any.get(name);
  }
  
  @JsonAnySetter
  @SuppressWarnings("unchecked")
  private final void any(final String name, final Object value) {
    if (name.equals("@qualifiers")) {
      if (value == null) {
        this.qualifiers = Qualifiers.of();
      } else if (value instanceof Qualifiers) {
        this.qualifiers = (Qualifiers<? extends String, ?>)value;
      } else if (value instanceof Map) {
        this.qualifiers = Qualifiers.of((Map<? extends String, ?>)value);
      } else {
        this.any.put(name, value);
      }
    } else {
      this.any.put(name, value);
    }
  }

  @Override // Object
  public int hashCode() {
    return Objects.hash(this.qualifiers, this.any);
  }

  @Override // Object
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      final Configuration her = (Configuration)other;
      return
        Objects.equals(this.qualifiers, her.qualifiers) &&
        Objects.equals(this.any, her.any);
    } else {
      return false;
    }
  }

  @Override // Object
  public String toString() {
    final Qualifiers<?, ?> q = this.qualifiers();
    if (q == null || q.isEmpty()) {
      return this.any.toString();      
    } else if (this.any.isEmpty()) {
      return q.toString();
    } else {
      return q.toString() + " " + this.any.toString();
    }
  }
  
}
