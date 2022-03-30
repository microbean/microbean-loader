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

import java.lang.reflect.Type;

import org.microbean.development.annotation.Experimental;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.Type.CovariantSemantics;

/**
 * An interface whose implementations handle various kinds of
 * ambiguity that arise when a {@link Loader} seeks configured
 * objects by way of various {@link Provider}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
@LoaderFacade(false)
public interface AmbiguityHandler {

  /**
   * Called to notify this {@link AmbiguityHandler} that a {@link
   * Provider} was discarded during the search for a configured
   * object.
   *
   * <p>The default implementation of this method does nothing.</p>
   *
   * @param rejector the {@link Loader} that rejected the {@link
   * Provider}; must not be {@code null}
   *
   * @param absolutePath the {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a configured object is being sought;
   * must not be {@code null}
   *
   * @param provider the rejected {@link Provider}, which may be
   * {@code null}
   *
   * @exception NullPointerException if either {@code rejector} or
   * {@code absolutePath} is {@code null}
   */
  public default void providerRejected(final Loader<?> rejector, final Path<? extends Type> absolutePath, final Provider provider) {

  }

  /**
   * Called to notify this {@link AmbiguityHandler} that a {@link
   * Value} provided by a {@link Provider} was discarded during the
   * search for a configured object.
   *
   * <p>The default implementation of this method does nothing.</p>
   *
   * @param rejector the {@link Loader} that rejected the {@link
   * Provider}; must not be {@code null}
   *
   * @param absolutePath the {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a configured object is being sought;
   * must not be {@code null}
   *
   * @param provider the {@link Provider} providing the rejected
   * value; must not be {@code null}
   *
   * @param value the rejected {@link Value}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public default void valueRejected(final Loader<?> rejector, final Path<? extends Type> absolutePath, final Provider provider, final Value<?> value) {

  }

  /**
   * Returns a score indicating the relative specificity of {@code
   * valueQualifiers} with respect to {@code referenceQualifiers}, or
   * {@link Integer#MIN_VALUE} if {@code valueQualifiers} is wholly
   * unsuitable for further consideration or processing.
   *
   * <p>This is <em>not</em> a comparison method.</p>
   *
   * @param referenceQualifiers the {@link Qualifiers} against which
   * to score the supplied {@code valueQualifiers}; must not be {@code
   * null}
   *
   * @param valueQualifiers the {@link Qualifiers} to score against
   * the supplied {@code referenceQualifiers}; must not be {@code
   * null}
   *
   * @return a relative score for {@code valueQualifiers} with respect
   * to {@code referenceQualifiers}; meaningless on its own
   * <em>unless</em> it is {@link Integer#MIN_VALUE} in which case the
   * supplied {@code valueQualifiers} will be treated as wholly
   * unsuitable for further consideration or processing
   *
   @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @see Loader#load(Path)
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent and deterministic.
   * Specifically, the same score is and must be returned whenever
   * this method is invoked with the same arguments.
   */
  public default int score(final Qualifiers<? extends String, ?> referenceQualifiers, final Qualifiers<? extends String, ?> valueQualifiers) {
    final int intersectionSize = referenceQualifiers.intersectionSize(valueQualifiers);
    if (intersectionSize > 0) {
      return
        intersectionSize == valueQualifiers.size() ?
        intersectionSize :
        intersectionSize - referenceQualifiers.symmetricDifferenceSize(valueQualifiers);
    } else {
      return -(referenceQualifiers.size() + valueQualifiers.size());
    }
  }

  /**
   * Returns a score indicating the relative specificity of {@code
   * valuePath} with respect to {@code absoluteReferencePath}, or
   * {@link Integer#MIN_VALUE} if {@code valuePath} is wholly
   * unsuitable for further consideration or processing.
   *
   * <p>This is <em>not</em> a comparison method.</p>
   *
   * <p>The following preconditions must hold or undefined behavior
   * will result:</p>
   *
   * <ul>
   *
   * <li>Neither parameter's value may be {@code null}.</li>
   *
   * <li>{@code absoluteReferencePath} must be {@linkplain
   * Path#absolute() absolute}
   *
   * <li>{@code valuePath} must be <em>selectable</em> with respect to
   * <code>absoluteReferencePath</code>, where the definition of
   * selectability is described below</li>
   *
   * </ul>
   *
   * <p>For {@code valuePath} to "be selectable" with respect to
   * {@code absoluteReferencePath} for the purposes of this method and
   * for no other purpose, {@code true} must be returned by a
   * hypothetical invocation of code whose behavior is that of the
   * following:</p>
   *
   * <blockquote><pre>absoluteReferencePath.endsWith(valuePath, {@link
   * #compatible(Element, Element)
   * AmbiguityHandler::compatible});</pre></blockquote>
   *
   * <p>Note that such an invocation is <em>not</em> made by the
   * default implementation of this method, but logically precedes it
   * when this method is called in the natural course of events by the
   * {@link Loader#load(Path)} method.</p>
   *
   * <p>If, during scoring, {@code valuePath} is found to be wholly
   * unsuitable for further consideration or processing, {@link
   * Integer#MIN_VALUE} will be returned to indicate this.  Overrides
   * must follow suit or undefined behavior elsewhere in this
   * framework will result.</p>
   *
   * <p>In the normal course of events, this method will be called
   * after a call to {@link #score(Qualifiers, Qualifiers)}, and so
   * there is normally no need for an implementation of this method to
   * consult a {@link Path}'s {@linkplain Path#qualifiers() affiliated
   * <code>Qualifiers</code>}.</p>
   *
   * @param absoluteReferencePath the {@link Path} against which to
   * score the supplied {@code valuePath}; must not be {@code null};
   * must adhere to the preconditions above
   *
   * @param valuePath the {@link Path} to score against the supplied
   * {@code absoluteReferencePath}; must not be {@code null}; must
   * adhere to the preconditions above
   *
   * @return a relative score for {@code valuePath} with respect to
   * {@code absoluteReferencePath}; meaningless on its own
   * <em>unless</em> it is {@link Integer#MIN_VALUE} in which case the
   * supplied {@code valuePath} will be treated as wholly unsuitable
   * for further consideration or processing
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if certain preconditions have
   * been violated
   *
   * @see Loader#load(Path)
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent and deterministic.
   * Specifically, the same score is and must be returned whenever
   * this method is invoked with the same {@link Path}s.
   */
  @Experimental
  public default int score(final Path<? extends Type> absoluteReferencePath, final Path<? extends Type> valuePath) {
    if (!absoluteReferencePath.absolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }

    final int lastValuePathIndex = absoluteReferencePath.lastIndexOf(valuePath, AmbiguityHandler::compatible);
    assert lastValuePathIndex >= 0 : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;
    assert lastValuePathIndex + valuePath.size() == absoluteReferencePath.size() : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;

    int score = valuePath.size();
    for (int valuePathIndex = 0; valuePathIndex < valuePath.size(); valuePathIndex++) {
      final int referencePathIndex = lastValuePathIndex + valuePathIndex;

      // TODO: wait a minute, we already did all this in
      // #compatible(Element, Element) below.
      
      final Element<?> referenceElement = absoluteReferencePath.get(referencePathIndex);
      final Element<?> valueElement = valuePath.get(valuePathIndex);
      if (!referenceElement.name().equals(valueElement.name())) {
        // TODO: empty names? Do they have special significance here?
        // See #compatible(Element, Element) below.
        return Integer.MIN_VALUE;
      }

      final Object referenceObject = referenceElement.qualified();
      final Object valueObject = valueElement.qualified();
      if (referenceObject == null) {
        if (valueObject == null) {
          continue;
        } else {
          return Integer.MIN_VALUE;
        }
      } else if (valueObject == null) {
        return Integer.MIN_VALUE;
      }

      if (!(referenceObject instanceof Type) || !(valueObject instanceof Type)) {
        return Integer.MIN_VALUE;
      } else if (!CovariantSemantics.INSTANCE.assignable((Type)referenceObject, (Type)valueObject)) {
        return Integer.MIN_VALUE;
      }

    }
    return score;
  }

  /**
   * Given two {@link Value}s and some contextual objects, chooses one
   * over the other and returns it, or synthesizes a new {@link Value}
   * and returns that, or indicates that disambiguation is impossible
   * by returning {@code null}.
   *
   * @param <U> the type of objects the {@link Value}s in question can
   * supply
   *
   * @param requestor the {@link Loader} currently seeking a
   * {@link Value}; must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute
   * <code>Path</code>} for which a value is being sought; must not be
   * {@code null}
   *
   * @param p0 the {@link Provider} that supplied the first {@link
   * Value}; must not be {@code null}
   *
   * @param v0 the first {@link Value}; must not be {@code null}
   *
   * @param p1 the {@link Provider} that supplied the second {@link
   * Value}; must not be {@code null}
   *
   * @param v1 the second {@link Value}; must not be {@code null}
   *
   * @return the {@link Value} to use instead; ordinarily one of the
   * two supplied {@link Value}s but may be {@code null} to indicate
   * that disambiguation was impossible, or an entirely different
   * {@link Value} altogether
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability The default implementation of this method and its
   * overrides may return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent. The default implementation of
   * this method is deterministic, but its overrides need not be.
   */
  public default <U> Value<U> disambiguate(final Loader<?> requestor,
                                           final Path<? extends Type> absolutePath,
                                           final Provider p0,
                                           final Value<U> v0,
                                           final Provider p1,
                                           final Value<U> v1) {
    return null;
  }

  /**
   * Returns {@code true} if and only if the second {@link Element} is
   * <em>compatible</em> with respect to the first {@link Element}.
   *
   * <p>This method returns {@code true} <strong>unless</strong> one
   * of the following conditions holds:</p>
   *
   * <ul>
   *
   * <li>Both {@linkplain Element element}s' {@linkplain
   * Element#name() names} are non-{@linkplain String#isEmpty() empty}
   * and are not {@linkplain String#equals(Object) equal}</li>
   *
   * <li>One {@linkplain Element element}'s {@linkplain
   * Element#qualified() qualified} is {@code null} but the other is
   * not</li>
   *
   * <li>An {@link Element element}'s {@linkplain Element#qualified()
   * qualified} is non-{@code null} but not a {@link Type}</li>
   *
   * <li>Both {@linkplain Element element}'s {@linkplain
   * Element#qualified() qualified}s are {@link Type}s, but the second
   * {@linkplain Element element}'s {@link Type} is not {@linkplain
   * CovariantSemantics#assignable(Type, Type) assignable} to the
   * first {@linkplain Element element}'s {@link Type}</li>
   *
   * <li>If the first {@linkplain Element element}'s {@linkplain
   * Element#qualifiers() qualifiers} are not {@linkplain
   * Qualifiers#isEmpty() empty} and the second {@linkplain Element
   * element}'s {@linkplain Element#qualifiers() qualifiers} are not
   * {@linkplain Qualifiers#isEmpty() empty} and the first {@linkplain
   * Element element}'s {@linkplain Element#qualifiers() qualifiers}
   * {@linkplain Qualifiers#intersectionSize(Qualifiers) do not
   * intersect} with the second {@linkplain Element element}'s
   * {@linkplain Element#qualifiers() qualifiers}</li>
   *
   * </ul>
   *
   * @param e1 the first {@link Element}; must not be {@code null}
   *
   * @param e2 the second {@link Element}; must not be {@code null}
   *
   * @return {@code true} if and only if the second {@link Element} is
   * compatible with the first
   *
   * @exception NullPointerException if either argument is {@code
   * null}
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public static boolean compatible(Element<?> e1, Element<?> e2) {
    final String name1 = e1.name();
    if (!name1.isEmpty()) {
      final String name2 = e2.name();
      if (!name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they
        // "match" any other name.
        return false;
      }
    }

    final Object o1 = e1.qualified();
    if (o1 == null) {
      return e2.qualified() == null;
    } else if (!(o1 instanceof Type) ||
               !(e2.qualified() instanceof Type t2) ||
               !CovariantSemantics.INSTANCE.assignable((Type)o1, t2)) {
      return false;
    }

    final Qualifiers<?, ?> q1 = e1.qualifiers();
    if (!q1.isEmpty()) {
      final Qualifiers<?, ?> q2 = e2.qualifiers();
      if (!q2.isEmpty() && q1.intersectionSize(q2) <= 0) {
        return false;
      }
    }

    return true;
  }

}
