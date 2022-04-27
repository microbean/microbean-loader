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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import java.util.function.Supplier;

import org.microbean.invoke.FixedValueSupplier;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaTypes;
import org.microbean.type.Type.CovariantSemantics;

/**
 * An {@link AbstractProvider} that can return {@link Value}s backed
 * by System properties.
 *
 * <p>While System properties are often casually assumed to be stable
 * and {@link String}-typed, the exact opposite is true: System
 * properties may contain arbitrarily-typed {@link Object}s under
 * arbitrarily-typed keys, and the properties themselves {@linkplain
 * System#setProperties(Properties) may be replaced} at any point.
 * This means that all {@link Value}s supplied by this {@link
 * SystemPropertyProvider} are {@linkplain Value#determinism()
 * non-deterministic} and may change type and presence from one call
 * to another.</p>
 *
 * <p>It is also worth mentioning explicitly that, deliberately, no
 * type conversion of any System property value takes place in this
 * class.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see System#getProperty(String, String)
 *
 * @see System#getProperties()
 *
 * @see System#setProperties(Properties)
 *
 * @see Properties#getProperty(String, String)
 *
 * @see Value#determinism()
 */
public class SystemPropertyProvider extends AbstractProvider {


  /*
   * Instance fields.
   */


  private final boolean flatKeys;

  private final boolean onlyStrings;

  private final boolean mutable;
  

  /*
   * Constructors.
   */


  /**
   * Creates a new {@link SystemPropertyProvider} that uses flat keys,
   * does not presume that the only things stored in the System
   * properties are {@link String}s, and honors the fact that System
   * properties are mutable.
   *
   * @see #SystemPropertyProvider(boolean, boolean, boolean)
   */
  public SystemPropertyProvider() {
    this(true, false, true);
  }

  /**
   * Creates a new {@link SystemPropertyProvider}.
   *
   * @param flatKeys whether the key for a system property is derived
   * from a {@link Path}'s {@linkplain Path#lastElement() last
   * element}'s {@linkplain Element#name() name} only
   *
   * @param onlyStrings whether all System properties are expected to
   * be {@link String}-typed
   *
   * @param mutable whether System properties should be treated as
   * mutable (which they are, strictly speaking, but for many
   * applications this may not matter in practice)
   */
  public SystemPropertyProvider(final boolean flatKeys,
                                final boolean onlyStrings,
                                final boolean mutable) {
    super(onlyStrings ? String.class : null);
    this.flatKeys = flatKeys;
    this.onlyStrings = onlyStrings;
    this.mutable = mutable;
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Supplier} suitable for the System property
   * represented by the supplied {@link Path}.
   *
   * <p>This method never returns {@code null}.  Its overrides may if
   * they wish.</p>
   *
   * <p>The {@linkplain Path.Element#name() name} of the {@linkplain
   * Path#lastElement() last element} of the supplied {@link Path} is
   * taken to be the name of the System property value to retrieve.
   * If the {@linkplain JavaTypes#erase(Type) type erasure} of the
   * supplied {@link Path}'s {@link Path#qualified() qualified()}
   * method {@linkplain Class#isAssignableFrom(Class) is assignable
   * from} {@link String String.class}, then calls will be made by the
   * returned {@link Value}'s {@link Value#get() get()} method to
   * {@link System#getProperty(String)} before simple calls to {@link
   * Properties#get(String) System.getProperties().get(String)}.</p>
   *
   * <p>Any {@link Supplier} returned by this method will be
   * {@linkplain
   * org.microbean.invoke.OptionalSupplier.Determinism#NON_DETERMINISTIC
   * non-deterministic}, since system properties may change at any
   * point.  Additionally, if the supplied {@link Path}'s {@linkplain
   * Path#qualified() type} is not assignable from that borne by a
   * System property value, then the {@link Supplier} will return
   * {@code null} from its {@link Value#get() get()} method in such a
   * case, indicating that the value is present but cannot be
   * represented.  Overrides are strongly encouraged to abide by these
   * conditions.</p>
   *
   * @param requestor the {@link Loader} seeking a {@link Value}; must
   * not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a {@link Value} is being sought;
   * must not be {@code null}
   *
   * @return a {@link Supplier} suitable for the System property whose
   * name is represented by the supplied {@link Path}'s {@linkplain
   * Path#lastElement() last <code>Element</code>}'s {@linkplain
   * Path.Element#name() name}
   *
   * @exception NullPointerException if an argument for either
   * parameter is {@code null}
   *
   * @nullability This method never returns {@code null} but its
   * overrides may.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is idempotent and deterministic.
   * Overrides must be idempotent, but need not be deterministic.
   * {@link Supplier}s returned by this method or its overrides are
   * <em>not</em> guaranteed to be idempotent or deterministic.
   */
  @Override
  protected Supplier<?> find(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());

    // This is tricky.  System properties suck.
    //
    // System properties are mutable.  They can come and go at any
    // point.  They can also be of any type.  If you set them to null,
    // the "required" ones get re-initialized to newly sourced values.
    // And so on.
    //
    // Next, while the keys are "flat", they are often naively assumed
    // to represent a hierarchy.  But the naming scheme suggests that
    // even in the case of the "required" properties the designers
    // were thinking in terms of hierarchies.
    //
    // Anyway, suppose an absolute Path of "/com/foo/bar" (making up
    // the syntax; "/" is a Path.Element separator; each "word" is a
    // Path.Element name() value) has a size of 4 (the leading
    // element's name is always "") (hierarchical).  Should this
    // Provider respond by trying
    // System.getProperties().getProperty("com.foo.bar")?
    //
    // Or should this Provider only accept a Path of "/com.foo.bar"
    // whose size is 2 (flat), so that the name of only the *last*
    // element is the property key ("com.foo.bar")?
    //
    // (The end call to System.getProperty() is the same, of course.)
    //
    // If we do the hierarchy case, we might run into spurious
    // conflicts between this Provider and
    // ProxyingProvider. "java.home", for example, is not really a
    // hierarchy; "java.home" as a flat string is actually the name of
    // the property in question and just happens to have "." in it.
    //
    // On the other hand, if we do the flat case, there isn't an easy
    // way to override scalar nodes in a tree, and it seems at least
    // moderately clear that the designers wanted these keys to be
    // perceived as hierarchical in some fashion.

    final String key = key(absolutePath, this.flatKeys);
    if (key == null || key.isEmpty()) {
      // System properties never permit null or empty keys.  See
      // https://github.com/openjdk/jdk/blob/jdk-17+35/src/java.base/share/classes/java/lang/System.java#L1043-L1050.
      return null;
    }
    final Type type = absolutePath.qualified();
    if (CovariantSemantics.INSTANCE.assignable(type, String.class)) {
      if (this.mutable) {
        return () -> {
          final String returnValue = System.getProperty(key);
          if (returnValue == null) {
            // System.getProperty() is documented to purposely conflate
            // absence with null for some reason.
            throw new NoSuchElementException(key);
          }
          return returnValue;
        };
      } else {
        final String value = System.getProperty(key);
        if (value == null) {
          // System.getProperty() is documented to purposely conflate
          // absence with null for some reason.
          return null;
        }
        return FixedValueSupplier.of(value);
      }
    } else if (this.onlyStrings) {
      return null;
    } else if (this.mutable) {
      // At this point we know that the requested type is not a
      // supertype of String.  Therefore there's no point in calling
      // System.getProperty(key) because you wouldn't be able to
      // assign its (String) return value to the caller doing the
      // requesting.  Therefore defaults are not in play because only
      // System.getProperty() consults them.  Therefore we can treat
      // the return value of System.getProperties() as just an
      // ordinary Map.  We also know that is is a Properties instance,
      // so we also know it is fully synchronized on itself.  Putting
      // this all together we can tell definitively when a value has
      // been explicitly and deliberately set to null versus when it
      // is absent.
      return () -> {
        final Object returnValue;
        final Map<?, ?> map = System.getProperties();
        synchronized (map) {
          if ((returnValue = map.get(key)) == null && !map.containsKey(key)) {
            throw new NoSuchElementException(key);
          }
        }
        if (returnValue == null || CovariantSemantics.INSTANCE.assignable(type, returnValue.getClass())) {
          return returnValue;
        }
        return null;
      };
    } else {
      final Object returnValue;
      final Map<?, ?> map = System.getProperties();
      synchronized (map) {
        if ((returnValue = map.get(key)) == null && !map.containsKey(key)) {
          return null;
        }
      }
      if (returnValue == null || CovariantSemantics.INSTANCE.assignable(type, returnValue.getClass())) {
        return FixedValueSupplier.of(returnValue);
      }
      return null;
    }
  }

  /**
   * Overrides the {@link AbstractProvider#path(Loader, Path)} method
   * to return a (relative) {@link Path} consisting solely of the
   * {@linkplain Path#lastElement() last element} of the supplied
   * {@code absolutePath}.
   *
   * @param requestor the {@link Loader} seeking a {@link Value}; must
   * not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a {@link Value} is being sought;
   * must not be {@code null}
   *
   * @return a {@link Path} that will be {@linkplain
   * Value#Value(Supplier, Path) used to build a <code>Value</code>}
   * to be returned by the {@link #get(Loader, Path)} method
   *
   * @nullability This method does not, but overrides may, return
   * {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see AbstractProvider#path(Loader, Path)
   */
  @Override
  @SuppressWarnings("unchecked")
  protected <T extends Type> Path<T> path(final Loader<?> requestor, final Path<T> absolutePath) {
    return Path.of(absolutePath.lastElement());
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link String} representation of the supplied {@link Path}.
   *
   * @param path the {@link Path} in question; must not be {@code
   * null}
   *
   * @param flat whether the key is derived from the supplied {@link
   * Path}'s {@linkplain Path#lastElement() last element}'s
   * {@linkplain Element#name() name} only
   *
   * @return a {@link String} representation of the supplied {@link
   * Path}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  protected static final String key(final Path<?> path, final boolean flat) {
    if (flat) {
      return path.lastElement().name();
    } else {
      return path.stream()
        .map(Element::name)
        .filter(s1 -> !s1.isEmpty())
        .reduce((s1, s2) -> String.join(".", s1, s2))
        .orElse("");
    }
  }

}
