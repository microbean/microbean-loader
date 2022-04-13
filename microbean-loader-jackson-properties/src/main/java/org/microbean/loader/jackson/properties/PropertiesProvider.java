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
package org.microbean.loader.jackson.properties;

import java.lang.reflect.Type;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

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
public class PropertiesProvider extends InputStreamJacksonProvider {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link PropertiesProvider} that reads Java
   * properties-formatted {@code application.properties} classpath
   * resources.
   *
   * @see #PropertiesProvider(Type, String)
   */
  public PropertiesProvider() {
    this(null, "application.properties");
  }

  /**
   * Creates a new {@link PropertiesProvider}.
   *
   * @param lowerBound the {@linkplain #lowerBound() lower type bound}
   * of this {@link InputStreamJacksonProvider} implementation; may be
   * {@code null}
   *
   * @param resourceName the name of the classpath resource to read
   * from; must not be {@code null}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   */
  public PropertiesProvider(final Type lowerBound, final String resourceName) {
    super(lowerBound, new CachingSupplier<>(JavaPropsMapper::new), resourceName);
  }

}
