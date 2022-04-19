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
package org.microbean.loader.jackson.properties;

import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import org.microbean.loader.DefaultLoader;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.microbean.loader.api.Loader.loader;

final class TestMoreRealWorld {

  private TestMoreRealWorld() {
    super();
  }

  @Test
  final void testMoreRealWorld() {
    final PropertiesProvider pp = new PropertiesProvider("realworld.properties") {
        @Override
        public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
          final Value<?> v = super.get(requestor, absolutePath);
          if (v != null) {
            final Path<?> p = v.path();
            if ("hostname".equals(p.lastElement().name())) {
              assertEquals("test", p.qualifiers().get("env"));
              assertEquals("bar", p.qualifiers().get("foo"));
            }
          }
          return v;
        }
      };
    final Loader<?> loader = loader().as(DefaultLoader.class).plus(pp);
    assertEquals("localhost", loader.load(String.class, "hostname").orElse(null));
  }

}
