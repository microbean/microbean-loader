/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2021–2022 microBean™.
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

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.microbean.loader.jackson.InputStreamJacksonProvider;

import org.microbean.invoke.CachingSupplier;

/**
 * An {@link InputStreamJacksonProvider} that reads JSON-formatted
 * {@code application.json} classpath resources.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see InputStreamJacksonProvider
 */
public class JsonProvider extends InputStreamJacksonProvider {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link JsonProvider} that reads JSON-formatted
   * {@code application.json} classpath resources.
   *
   * @see #JsonProvider(Type, String)
   */
  public JsonProvider() {
    this(null, "application.json");
  }

  /**
   * Creates a new {@link JsonProvider}.
   *
   * @param resourceName the name of the classpath resource to read
   * from; must not be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   *
   * @see #JsonProvider(Type, String)
   */
  public JsonProvider(final String resourceName) {
    this(null, resourceName);
  }

  /**
   * Creates a new {@link JsonProvider}.
   *
   * @param lowerBound the {@linkplain #lowerBound() lower type bound}
   * of this {@link JsonProvider}; may be {@code null}
   *
   * @param resourceName the name of the classpath resource to read
   * from; must not be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   */
  public JsonProvider(final Type lowerBound, final String resourceName) {
    super(lowerBound, new CachingSupplier<>(ObjectMapper::new), resourceName);
  }

}
