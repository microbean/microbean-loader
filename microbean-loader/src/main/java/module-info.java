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
import org.microbean.loader.DefaultLoader;
import org.microbean.loader.EnvironmentVariableProvider;
import org.microbean.loader.ProxyingProvider;
import org.microbean.loader.SystemPropertyProvider;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AmbiguityHandler;
import org.microbean.loader.spi.Provider;
import org.microbean.loader.spi.ServiceProviderInstantiator;

/**
 * Provides packages related to the {@linkplain DefaultLoader default
 * implementation} of the {@linkplain Loader microBean™ Loader API}
 * and its {@linkplain Provider service provider interface}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see DefaultLoader
 *
 * @see AmbiguityHandler
 *
 * @see Provider
 *
 * @see org.microbean.loader.spi.Value
 */
module org.microbean.loader {

  exports org.microbean.loader;
  exports org.microbean.loader.spi;

  provides Loader with DefaultLoader;
  provides Provider with EnvironmentVariableProvider, ProxyingProvider, SystemPropertyProvider;

  requires transitive org.microbean.loader.api;

  uses AmbiguityHandler;
  uses Provider;
  uses ServiceProviderInstantiator;


}
