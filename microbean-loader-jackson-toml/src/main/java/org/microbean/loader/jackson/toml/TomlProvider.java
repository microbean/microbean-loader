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
package org.microbean.loader.jackson.toml;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import org.microbean.loader.jackson.InputStreamJacksonProvider;

import org.microbean.invoke.CachingSupplier;

/**
 * An {@link InputStreamJacksonProvider} that reads TOML-formatted
 * {@code application.toml} classpath resources.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see InputStreamJacksonProvider
 */
public class TomlProvider extends InputStreamJacksonProvider<Object> {


  /*
   * Static fields.
   */


  private static final Supplier<TomlMapper> supplier = new CachingSupplier<>(TomlMapper::new);


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link TomlProvider} that reads TOML-formatted
   * {@code application.toml} classpath resources.
   *
   * @see #TomlProvider(String)
   */
  public TomlProvider() {
    this("application.toml");
  }

  /**
   * Creates a new {@link TomlProvider}.
   *
   * @param resourceName the name of the classpath resource to read
   * from; must not be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   */
  public TomlProvider(final String resourceName) {
    super(TomlProvider.supplier, resourceName);
  }

}
