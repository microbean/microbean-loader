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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestJsonPlayground {

  private URL url;
  
  private ObjectMapper jsonMapper;
  
  private TestJsonPlayground() {
    super();
  }

  @BeforeEach
  final void beforeEach() throws IOException {
    this.url = Thread.currentThread().getContextClassLoader().getResource("playground.json");
    assertNotNull(this.url);
    this.jsonMapper = new ObjectMapper();
  }

  @Test
  final void testJsonPlayground() throws IOException {
    Map<?, ?> map = this.jsonMapper.readValue(this.url, Map.class);
    System.out.println(map);
  }

  @Test
  final void testNodeStuff() throws IOException {
    TreeNode node = this.jsonMapper.readTree(this.url);
    assertNotNull(node);
    // Jackson does not have any built-in handling of an empty field
    // name.  So get("") will yield null.
    assertNull(node.get(""));
    node = node.get("array");
    assertTrue(node.isArray());
    final List<?> o = node.traverse(this.jsonMapper).readValueAs(List.class);
    System.out.println("*** o: " + o);
    System.out.println("    type: " + o.getClass());
    for (final Object element : o) {
      System.out.println("    element: " + element);
      System.out.println("    element type: " + element.getClass());
    }
  }

}
