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
package org.microbean.loader.typesafe.config.hocon;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.microbean.loader.spi.Value;

import org.microbean.path.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TestSpike {

  private TestSpike() {
    super();
  }

  @Test
  final void testSpike() {
    final TypesafeConfigHoconProvider p = new TypesafeConfigHoconProvider();
    Value<?> v = p.get(null, Path.of(String.class, "a"));
    assertEquals("b", v.get());
    assertEquals("b", v.get());
    v = p.get(null, Path.of(Goop.class));
    assertNotNull(v);
  }

  public static class Goop {

    private Object c;
    
    public Goop() {
      super();
    }

    public Object getC() {
      return this.c;
    }

    public void setC(final Object c) {
      this.c = c;
    }
    
  }

}
