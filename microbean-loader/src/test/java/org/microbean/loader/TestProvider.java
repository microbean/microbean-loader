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
package org.microbean.loader;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.Provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import static org.microbean.loader.api.Loader.loader;

final class TestProvider {

  private TestProvider() {
    super();
  }

  @Test
  final void testJavaHome() {
    assertEquals(System.getProperty("java.home"), loader().load(String.class, "java.home").get());
  }

  @Test
  final void testLoadedProviders() {
    final Collection<Provider> ps = DefaultLoader.loadedProviders();
    assertFalse(ps.isEmpty());
    assertSame(ps, DefaultLoader.loadedProviders());
    assertSame(ps, DefaultLoader.loadedProviders());
  }

}
