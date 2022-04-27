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
package org.microbean.loader.spi;

import java.util.ServiceLoader;

/**
 * An instantiator of {@link ServiceLoader.Provider} instances.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public interface ServiceProviderInstantiator {

  /**
   * Instantiates the supplied {@link ServiceLoader.Provider} and
   * returns the resulting service provider.
   *
   * @param <T> the type of service provider to instantiate
   *
   * @param serviceLoaderProvider a {@link ServiceLoader.Provider};
   * must not be {@code null}
   *
   * @return a service provider; never {@code null}
   *
   * @exception NullPointerException if {@code
   * serviceLoaderProvider} is {@code null}
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency The idempotency and determinism of any
   * implementation of this method is left undefined.
   *
   * @threadsafety This method and any override is not guaranteed to
   * be safe for concurrent use by multiple threads.
   */
  public default <T> T instantiate(final ServiceLoader.Provider<? extends T> serviceLoaderProvider) {
    return serviceLoaderProvider.get();
  }
    
}
