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
package org.microbean.loader.typesafe.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.microbean.loader.DefaultLoader;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.Value;

import org.microbean.path.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.microbean.loader.api.Loader.loader;

final class TestSpike {

  private TestSpike() {
    super();
  }

  @Test
  final void testSpike() {
    final Loader<?> loader = loader().as(DefaultLoader.class).plus(new TypesafeConfigHoconProvider());
    final String s = loader.load(String.class, "a").get();
    assertEquals("b", s);
    final Goop goop = loader.load(Goop.class).get();
    assertNotNull(goop);
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
