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
package org.microbean.loader;

import java.lang.reflect.Type;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.microbean.invoke.FixedValueSupplier;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifier;
import org.microbean.qualifier.Qualifiers;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.LoaderFacade;
import org.microbean.loader.spi.Value;

import org.microbean.type.JavaTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.microbean.loader.api.Loader.loader;

final class TestProxyingProvider {

  private TestProxyingProvider() {
    super();
  }

  @Test
  final void explore() {
    DefaultLoader<?> rootLoader = (DefaultLoader<?>)loader();
    rootLoader = rootLoader.plus(new LRWheelProvider());
    
    final Loader<Car> carCs = rootLoader.load(Car.class);    
    final Car car = carCs.get();
    assertNotNull(car);
    assertSame(car, carCs.get());
    final Powertrain pt = car.getPowertrain();
    assertNotNull(pt);
    assertSame(pt, car.getPowertrain());
    final Engine engine = pt.getEngine();
    assertNotNull(engine);
    assertSame(engine, pt.getEngine());
    engine.start();
    assertEquals(18, car.getWheel("LF").getDiameterInInches());
    assertEquals(24, car.getWheel("LR").getDiameterInInches());
  }

  @LoaderFacade
  public static interface Car {

    public Powertrain getPowertrain();

    public Wheel getWheel(final String wheelSpecifier);

  }

  @LoaderFacade
  public static interface Powertrain {

    public Engine getEngine();

  }

  @LoaderFacade
  public static interface Engine {

    public default void start() {

    }

  }

  @LoaderFacade
  public static interface Wheel {

    public default int getDiameterInInches() {
      return 18;
    }

  }

  public static final class LRWheelProvider extends AbstractProvider {

    public LRWheelProvider() {
      super(Wheel.class);
    }

    @Override
    protected final Supplier<?> find(final Loader<?> requestor, final Path<? extends Type> path) {
      final Element<? extends Type> last = path.lastElement();
      if ("wheel".equals(last.name())) {
        final Qualifiers<?, ?> lastQualifiers = last.qualifiers();        
        assertFalse(lastQualifiers.isEmpty());
        // This is kind of a neat trick: map nulls via
        // Optional::ofNullable, then reduce matching values to
        // nothing via Otpional.empty(). The reduction won't happen if
        // there's only one value.
        return
          lastQualifiers.stream()
          .filter(q -> "arg0".equals(q.name()) && "LR".equals(q.value())) // cut the stream down early
          .map(Optional::ofNullable) // nulls to optionals
          .reduce((o0, o1) -> Optional.empty())
          .map(o -> {
              final Class<?> wheelClass = JavaTypes.erase(path.qualified());
              assertSame(Wheel.class, wheelClass);
              return
                FixedValueSupplier.of(new Wheel() {
                    @Override
                    public final int getDiameterInInches() {
                      return 24;
                    }
                  });
            })
          .orElse(null);
      }
      return null;
    }

    @Override
    protected final <T extends Type> Path<T> path(final Loader<?> requestor, final Path<T> path) {
      return Path.of(Element.of(Qualifiers.of(Qualifier.<String, String>of("arg0", "LR")), path.qualified(), "wheel"));
    }

  }

}
