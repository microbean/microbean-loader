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

import java.util.List;

import java.util.function.BiPredicate;

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
   * ElementsMatchBiPredicate#INSTANCE
   * ElementsMatchBiPredicate.INSTANCE});</pre></blockquote>
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

    final int lastValuePathIndex = absoluteReferencePath.lastIndexOf(valuePath, ElementsMatchBiPredicate.INSTANCE);
    assert lastValuePathIndex >= 0 : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;
    assert lastValuePathIndex + valuePath.size() == absoluteReferencePath.size() : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;

    int score = valuePath.size();
    for (int valuePathIndex = 0; valuePathIndex < valuePath.size(); valuePathIndex++) {
      final int referencePathIndex = lastValuePathIndex + valuePathIndex;
      
      final Element<?> referenceElement = absoluteReferencePath.get(referencePathIndex);
      final Element<?> valueElement = valuePath.get(valuePathIndex);
      if (!referenceElement.name().equals(valueElement.name())) {
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
      }

      final Type referenceType = (Type)referenceObject;
      final Type valueType = (Type)valueObject;

      if (!CovariantSemantics.INSTANCE.assignable(referenceType, valueType)) {
        return Integer.MIN_VALUE;
      }

      final Qualifiers<?, ?> referenceQualifiers = referenceElement.qualifiers();
      final Qualifiers<?, ?> valueQualifiers = valueElement.qualifiers();
      if (referenceQualifiers == null) {
        if (valueQualifiers != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueQualifiers == null || referenceQualifiers.size() != valueQualifiers.size()) {
        return Integer.MIN_VALUE;
      }

      for (final Object referenceKey : referenceQualifiers.toMap().keySet()) {
        final Object valueValue = valueQualifiers.get(referenceKey);
        if (valueValue == null) {
          // The value is indifferent with respect to this particular
          // qualifier key.  It *could* be suitable, but not *as*
          // suitable as one that matched.  Don't adjust the score.
        } else if (valueQualifiers.get(referenceKey).equals(valueValue)) {
          score++;
        } else {
          return Integer.MIN_VALUE;
        }
      }

      /*
      // original below:
      
      final List<Class<?>> referenceParameters = referenceElement.parameters().orElse(null);
      final List<Class<?>> valueParameters = valueElement.parameters().orElse(null);
      if (referenceParameters == null) {
        if (valueParameters != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueParameters == null || referenceParameters.size() != valueParameters.size()) {
        return Integer.MIN_VALUE;
      }

      final List<String> referenceArguments = referenceElement.arguments().orElse(null);
      final List<String> valueArguments = valueElement.arguments().orElse(null);
      if (referenceArguments == null) {
        if (valueArguments != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueArguments == null) {
        // The value is indifferent with respect to arguments. It
        // *could* be suitable but not *as* suitable as one that
        // matched.  Don't adjust the score.
      } else {
        final int referenceArgsSize = referenceArguments.size();
        final int valueArgsSize = valueArguments.size();
        if (referenceArgsSize < valueArgsSize) {
          // The value path is unsuitable because it provided too
          // many arguments.
          return Integer.MIN_VALUE;
        } else if (referenceArguments.equals(valueArguments)) {
          score += referenceArgsSize;
        } else if (referenceArgsSize == valueArgsSize) {
          // Same sizes, but different arguments.  The value is not suitable.
          return Integer.MIN_VALUE;
        } else if (valueArgsSize == 0) {
          // The value is indifferent with respect to arguments. It
          // *could* be suitable but not *as* suitable as one that
          // matched.  Don't adjust the score.
        } else {
          // The reference element had, say, two arguments, and the
          // value had, say, one.  We treat this as a mismatch.
          return Integer.MIN_VALUE;
        }
      }
      */
      
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


  /*
   * Inner and nested classes.
   */


  /**
   * A {@link BiPredicate} that compares two {@link Path.Element}s to
   * see if they match.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see AmbiguityHandler#score(Path, Path)
   */
  @Experimental
  // public static final class ElementsMatchBiPredicate implements BiPredicate<Element<? extends Type>, Element<? extends Type>> {
  public static final class ElementsMatchBiPredicate implements BiPredicate<Element<?>, Element<?>> {


    /*
     * Static fields.
     */


    /**
     * The sole instance of this class.
     *
     * <p>This field is never {@code null}.</p>
     *
     * @nullability This field is never {@code null}.
     *
     * @threadsafety Accessing this field does not require any
     * synchronization.
     */
    public static final ElementsMatchBiPredicate INSTANCE = new ElementsMatchBiPredicate();


    /*
     * Constructors.
     */


    private ElementsMatchBiPredicate() {
      super();
    }


    /*
     * Instance methods.
     */


    /*
    @Override // BiPredicate<Element<?>, Element<?>>
    public final boolean test(final Element<? extends Type> e1, final Element<? extends Type> e2) {
      final String name1 = e1.name();
      final String name2 = e2.name();
      if (!name1.isEmpty() && !name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they "match"
        // any other name.
        return false;
      }

      final Type t1 = e1.qualified();
      final Type t2 = e2.qualified();
      if (t1 != null && t2 != null && !CovariantSemantics.INSTANCE.assignable(t1, t2)) {
        return false;
      }

      final Qualifiers<?, ?> q1 = e1.qualifiers();
      final Qualifiers<?, ?> q2 = e2.qualifiers();
      if (q1 != null && q2 != null) {
        return q1.toMap().keySet().equals(q2.toMap().keySet());
      }

      return true;
    }
    */

    @Override // BiPredicate<Element<?>, Element<?>>
    public final boolean test(final Element<?> e1, final Element<?> e2) {
      final String name1 = e1.name();
      final String name2 = e2.name();
      if (!name1.isEmpty() && !name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they "match"
        // any other name.
        return false;
      }

      final Object o1 = e1.qualified();
      if (!(o1 instanceof Type)) {
        return false;
      }
      final Type t1 = (Type)o1;
      final Object o2 = e2.qualified();
      if (!(o2 instanceof Type)) {
        return false;
      }
      final Type t2 = (Type)o2;
      
      if (!CovariantSemantics.INSTANCE.assignable(t1, t2)) {
        return false;
      }

      final Qualifiers<?, ?> q1 = e1.qualifiers();
      final Qualifiers<?, ?> q2 = e2.qualifiers();
      if (q1 != null && q2 != null) {
        if (q1.size() != q2.size()) {
          return false;
        } else {
          return q1.toMap().keySet().equals(q2.toMap().keySet());
        }
      }
      return true;
    }

  }

}
