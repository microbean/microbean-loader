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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigResolver;
import com.typesafe.config.ConfigSyntax;
import com.typesafe.config.ConfigUtil;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.junit.jupiter.api.Test;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestHocon {

  private TestHocon() {
    super();
  }

  @Test
  final void testHocon() {
    final Config config =
      ConfigFactory.load(Thread.currentThread().getContextClassLoader(),
                         "test",
                         ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF),
                         ConfigResolveOptions.noSystem().setAllowUnresolved(true).appendResolver(new Resolver()));
    assertFalse(config.hasPathOrNull("x"));
    assertTrue(config.hasPathOrNull("a"));
    assertTrue(config.hasPathOrNull("c.d"));
    assertTrue(config.hasPathOrNull("f"));
    assertThrows(ConfigException.Null.class, () -> config.getValue("f"));
    // This is a bizarre design decision.
    assertThrows(ConfigException.Null.class, () -> config.getObject("f"));

    // Apparently you're supposed to do this:
    assertTrue(config.getIsNull("f"));

    assertEquals("b", config.getString("g"));
  }

  private static final class Resolver implements ConfigResolver {

    private final ConfigResolver fallback;

    private Resolver() {
      this(null);
    }
    
    private Resolver(final ConfigResolver fallback) {
      super();
      this.fallback = fallback;
    }

    @Override // ConfigResolver
    public final ConfigValue lookup(final String path) {
      // Called only for substitutions that are not internal to the Config.
      return
        ConfigValueFactory.fromAnyRef(Loader.loader()
                                      .load(Path.of(Object.class, ConfigUtil.splitPath(path)))
                                      .orElseGet(() -> this.fallback == null ? null : this.fallback.lookup(path)));
    }

    @Override // ConfigResolver
    public final ConfigResolver withFallback(final ConfigResolver fallback) {
      if (fallback == null || fallback.equals(this.fallback)) {
        return this;
      }
      return new Resolver(fallback);
    }
    
  }
  
}
