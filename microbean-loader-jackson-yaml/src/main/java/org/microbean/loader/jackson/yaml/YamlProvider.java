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
package org.microbean.loader.jackson.yaml;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.microbean.loader.jackson.InputStreamJacksonProvider;

import org.microbean.invoke.CachingSupplier;

/**
 * An {@link InputStreamJacksonProvider} that reads YAML-formatted
 * {@code application.yaml} classpath resources.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see InputStreamJacksonProvider
 */
public class YamlProvider extends InputStreamJacksonProvider<Object> {


  /*
   * Static fields.
   */


  private static final Supplier<YAMLMapper> supplier = new CachingSupplier<>(YAMLMapper::new);


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link YamlProvider} that reads YAML-formatted
   * {@code application.yaml} classpath resources.
   *
   * @see #YamlProvider(String)
   */
  public YamlProvider() {
    this("application.yaml");
  }

  /**
   * Creates a new {@link YamlProvider}.
   *
   * @param resourceName the name of the classpath resource to read
   * from; must not be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   */
  public YamlProvider(final String resourceName) {
    super(YamlProvider.supplier, resourceName);
  }

}
