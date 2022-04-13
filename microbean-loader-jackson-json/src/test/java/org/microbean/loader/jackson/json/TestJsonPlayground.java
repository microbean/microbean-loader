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
package org.microbean.loader.jackson.json;

import java.io.IOException;

import java.net.URL;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TestJsonPlayground {

  private TestJsonPlayground() {
    super();
  }

  @Test
  final void testJsonPlayground() throws IOException {
    final URL url = Thread.currentThread().getContextClassLoader().getResource("playground.json");
    assertNotNull(url);
    final ObjectMapper jsonMapper = new ObjectMapper();
    Map<?, ?> map = jsonMapper.readValue(url, Map.class);
    System.out.println(map);
  }

}
