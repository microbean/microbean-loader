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
package org.microbean.loader.jackson.yaml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.junit.jupiter.api.Test;

import org.microbean.loader.DefaultLoader;

import org.microbean.loader.api.Loader;

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
    final Loader<?> loader = loader().as(DefaultLoader.class).plus(new YamlProvider());

    // This treats the whole application.yaml as a Frobnicator,
    // ignoring unknown properties.  I'm not sure this is a great use
    // case but it's at least possible.
    final Frobnicator f = loader.load(Frobnicator.class).orElse(null);
    assertNotNull(f);
    assertEquals(37, f.getFrobnicationInterval());

    // This extracts a Blatz object out of application.yaml named
    // "gorp", also ignoring unknown properties.
    final Blatz b = loader.load(Blatz.class, "gorp").orElse(null);
    assertNotNull(b);
    assertEquals("foo", b.getBlatz());

    // This goes after a single string.
    final String blatz = loader.load(String.class, List.of("gorp", "blatz")).orElse(null);
  }

  @JsonAutoDetect(creatorVisibility = Visibility.NONE,
                  fieldVisibility = Visibility.NONE,
                  getterVisibility = Visibility.NONE,
                  isGetterVisibility = Visibility.NONE,
                  setterVisibility = Visibility.NONE)
  public static final class Blatz {

    private final String blatz;

    @JsonCreator
    public Blatz(@JsonProperty(value = "blatz", required = true) final String blatz) {
      super();
      this.blatz = blatz;
    }

    @JsonProperty(value = "blatz")
    public final String getBlatz() {
      return this.blatz;
    }
    
  }

  @JsonAutoDetect(creatorVisibility = Visibility.NONE,
                  fieldVisibility = Visibility.NONE,
                  getterVisibility = Visibility.NONE,
                  isGetterVisibility = Visibility.NONE,
                  setterVisibility = Visibility.NONE)
  public static final class Frobnicator {

    private final int frobnicationInterval;

    @JsonCreator
    public Frobnicator(@JsonProperty(value = "frobnicationInterval", required = true) final int frobnicationInterval) {
      super();
      this.frobnicationInterval = frobnicationInterval;
    }

    @JsonProperty(value = "frobnicationInterval")
    public final int getFrobnicationInterval() {
      return this.frobnicationInterval;
    }

    @JsonAnySetter
    private final void gorp(final String name, final Object value) {

    }
    
  }
  
}
