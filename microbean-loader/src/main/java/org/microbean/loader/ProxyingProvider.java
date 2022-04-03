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

import java.lang.annotation.Annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

import org.microbean.invoke.OptionalSupplier;
import org.microbean.invoke.OptionalSupplier.Determinism;

import org.microbean.loader.api.Loader;

import org.microbean.loader.spi.AbstractProvider;
import org.microbean.loader.spi.LoaderFacade;
import org.microbean.loader.spi.Value;

import org.microbean.path.Path;
import org.microbean.path.Path.Element;

import org.microbean.qualifier.Qualifiers;

import org.microbean.type.JavaTypes;

/**
 * An {@link AbstractProvider} that is capable of {@linkplain Proxy
 * proxying} {@linkplain #isProxiable(Loader, Path) certain}
 * interfaces and supplying them as environmental objects.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get(Loader, Path)
 *
 * @see #isProxiable(Loader, Path)
 */
public class ProxyingProvider extends AbstractProvider<Object> {


  /*
   * Instance fields.
   */


  private final ConcurrentMap<Path<? extends Type>, Object> proxies;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link ProxyingProvider}.
   *
   * @deprecated This constructor should be invoked by subclasses and
   * {@link java.util.ServiceLoader} instances only.
   */
  @Deprecated // intended for use by subclasses and java.util.ServiceLoader only
  public ProxyingProvider() {
    super();
    this.proxies = new ConcurrentHashMap<>();
  }


  /*
   * Instance methods.
   */


  @Override // Provider
  public final Value<?> get(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    assert absolutePath.absolute();
    assert absolutePath.startsWith(requestor.path());
    assert !absolutePath.equals(requestor.path());

    if (this.isProxiable(requestor, absolutePath)) {
      @SuppressWarnings("unchecked")
      final Value<?> returnValue =
        new Value<>(null, // no defaults
                    this.path(requestor, absolutePath),
                    OptionalSupplier.of(Determinism.PRESENT, 
                                        () -> this.proxies.computeIfAbsent(absolutePath,
                                                                           p -> this.newProxyInstance(requestor, p, JavaTypes.erase(p.qualified())))));
      return returnValue;
    } else {
      return null;
    }
  }

  /**
   * Returns {@code true} if the {@linkplain Path#qualified() type
   * identified by the supplied <code>absolutePath</code>} can be
   * proxied.
   *
   * <p>A type can be proxied by this {@link ProxyingProvider} if its
   * {@linkplain JavaTypes#erase(Type) type erasure}:</p>
   *
   * <ul>
   *
   * <li>is an {@linkplain Class#isInterface() interface}</li>
   *
   * <li>is {@linkplain Class#isHidden() not hidden}</li>
   *
   * <li>is {@linkplain Class#isSealed() not sealed}</li>
   *
   * </ul>
   *
   * <p>In addition, the default implementation of this method rules
   * out interfaces that declare or inherit {@code public} instance
   * methods with either exactly one parameter that does not pass the
   * test codified by the {@link #isIndexLike(Class)} method or more
   * than one parameter.</p>
   *
   * @param requestor the {@link Loader} seeking an environmental
   * object; must not be {@code null}; ignored by the default
   * implementation of this method
   *
   * @param absolutePath the {@link Path} {@linkplain Path#qualified()
   * identifying the interface to be proxied}; must not be {@code
   * null}; must be {@linkplain Path#absolute() absolute}
   *
   * @return {@code true} if the {@linkplain Path#qualified() type
   * identified by the supplied <code>absolutePath</code>} can be
   * proxied; {@code false} otherwise
   *
   * @exception NullPointerException if either argument is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code absolutePath} is
   * not {@linkplain Path#absolute() absolute}
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @see #isIndexLike(Class)
   */
  protected boolean isProxiable(final Loader<?> requestor, final Path<? extends Type> absolutePath) {
    final Class<?> c = JavaTypes.erase(absolutePath.qualified());
    if (c.isInterface() && !c.isHidden() && !c.isSealed()) {
      final LoaderFacade facadeAnnotation = c.getAnnotation(LoaderFacade.class);
      if (facadeAnnotation == null || facadeAnnotation.value()) {
        final Method[] methods = c.getMethods();
        switch (methods.length) {
        case 0:
          // Interface with no methods.
          return false;
        default:
          int getterCount = 0;
          int defaultCount = 0;
          for (final Method m : methods) {
            if (m.isDefault()) {
              // Found a default method.
              ++defaultCount;
            } else if (!Modifier.isStatic(m.getModifiers())) {
              // Found an abstract instance method.
              final Type returnType = m.getReturnType();
              if (returnType != void.class && returnType != Void.class) {
                // Found an abstract instance method that returns something other than void.
                switch (m.getParameterCount()) {
                case 0:
                  // It has no parameters, so it's a getter.
                  ++getterCount;
                  break;
                case 1:
                  if (!this.isIndexLike(m.getParameterTypes()[0])) {
                    // It has one parameter, and that parameter is not
                    // "index-like", so we don't know what to do with
                    // it.
                    return false;
                  }
                  // It has one parameter, and that parameter is
                  // "index-like", so we could conceivably implement
                  // it with a map or a list.  Keep going.
                  ++getterCount;
                  break;
                default:
                  // It has more than one parameter so we don't know
                  // what to do with it.
                  return false;
                }
              }
            }
          }
          return getterCount > 0 || defaultCount > 0;
        }

      }
    }
    return false;
  }

  /**
   * Returns a {@link Path} suitable for the combination of the
   * supplied {@link Loader} and requested {@link Path}.
   *
   * @param <T> the type of the requested and returned {@link Path}s
   *
   * @param requestor the {@link Loader} issuing the current request;
   * must not be {@code null}; ignored by this implementation
   *
   * @param absolutePath the {@linkplain Path#absolute() absolute}
   * {@link Path} representing the current request; must not be {@code
   * null}
   *
   * @return a non-{@code null} {@link Path} with which any {@link
   * Value} provided by this {@link ProxyingProvider} will be
   * associated
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent but not necessarily deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   */
  protected <T extends Type> Path<T> path(final Loader<?> requestor, final Path<T> absolutePath) {
    return Path.of(absolutePath.qualified());
  }

  /**
   * Returns {@code true} if the supplied {@link Class} representing a
   * method parameter is <em>index-like</em>, i.e. if it is something
   * typically used as an index into a larger collection or map.
   *
   * <p>The default implementation of this method returns {@code true}
   * if {@code parameterType} represents either an {@code int}, an
   * {@link Integer}, or a {@link CharSequence}.</p>
   *
   * <p>This method is called by the default implementation of the
   * {@link #isProxiable(Loader, Path)} method.</p>
   *
   * @param parameterType the method parameter type to test; may be
   * {@code null} in which case {@code false} will be returned
   *
   * @return {@code true} if the supplied {@link Class} representing a
   * method parameter is <em>index-like</em>, i.e. if it is something
   * typically used as an index into a larger collection or map
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   */
  protected boolean isIndexLike(final Class<?> parameterType) {
    return
      parameterType == int.class ||
      parameterType == Integer.class ||
      CharSequence.class.isAssignableFrom(parameterType);
  }

  /**
   * Invokes the {@link Proxy#newProxyInstance(ClassLoader, Class[],
   * InvocationHandler)} method with appropriate arguments and returns
   * the result.
   *
   * <p>The {@link Proxy#newProxyInstance(ClassLoader, Class[],
   * InvocationHandler)} method is invoked with the following
   * arguments:</p>
   *
   * <ol>
   *
   * <li>{@code interfaceToProxy.getClassLoader()}</li>
   *
   * <li>{@code new Class<?>[] { interfaceToProxy }}</li>
   *
   * <li>a special {@link InvocationHandler} backed by the supplied
   * {@link Loader} and {@link Path}</li>
   *
   * </ol>
   *
   * @param requestor the {@link Loader} performing the current
   * request; must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#absolute() absolute}
   * {@link Path} representing the current request; must not be {@code
   * null}
   *
   * @param interfaceToProxy the single interface the new proxy
   * instance will implement; must not be {@code null}; must be an
   * interface
   *
   * @return a new proxy instance as produced by the {@link
   * Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)}
   * method; never {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if any argument is unsuitable
   *
   * @nullability This method does not, and its overrides must not,
   * return {@code null}.
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent and deterministic.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads.
   *
   * @see Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)
   */
  protected Object newProxyInstance(final Loader<?> requestor,
                                    final Path<? extends Type> absolutePath,
                                    final Class<?> interfaceToProxy) {
    return
      Proxy.newProxyInstance(interfaceToProxy.getClassLoader(),
                             new Class<?>[] { interfaceToProxy },
                             new Handler(requestor, absolutePath, (m, args) -> path(m, args)));
  }


  /*
   * Static methods.
   */


  private static final Path<? extends Type> path(final Method m, final Object[] args) {
    final Map<String, Object> map;
    final Parameter[] parameters = m.getParameters();
    if (parameters.length > 0) {
      if (args.length != parameters.length) {
        throw new IllegalArgumentException("args: " + args);
      }
      map = new TreeMap<>();
      for (int i = 0; i < parameters.length; i++) {
        map.put(parameters[i].getName(), String.valueOf(args[i]));
      }
    } else {
      map = Collections.emptySortedMap();
    }
    final Type type = m.getGenericReturnType();
    return Path.of(Element.of(Qualifiers.of(map), type, propertyName(m.getName(), boolean.class == type)));
  }

  /**
   * Given a {@link CharSequence} normally representing the name of a
   * "getter" method, and a {@code boolean} indicating whether the
   * method in question returns a {@code boolean}, applies the rules
   * declared by the Java Beans specification to the name and yields
   * the result.
   *
   * @param cs a {@link CharSequence} naming a "getter" method; may be
   * {@code null} in which case {@code null} will be returned
   *
   * @param methodReturnsBoolean {@code true} if the method named by
   * the supplied {@link CharSequence} has {@code boolean} as its
   * return type
   *
   * @return the property name corresponding to the supplied method
   * name, according to the rules of the Java Beans specification, or
   * {@code null} (only if {@code cs} is {@code null})
   *
   * @nullability This method may return {@code null} but only when
   * {@code cs} is {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #decapitalize(CharSequence)
   */
  @Convenience
  @SuppressWarnings("fallthrough")
  public static final String propertyName(final CharSequence cs, final boolean methodReturnsBoolean) {
    if (cs == null) {
      return null;
    } else {
      final int length = cs.length();
      if (length <= 2) {
        return decapitalize(cs);
      } else if (methodReturnsBoolean) {
        switch (cs.charAt(0)) {
        case 'i':
          if (cs.charAt(1) == 's') {
            return decapitalize(cs.subSequence(2, length));
          }
        case 'g':
          if (length > 3 && cs.charAt(1) == 'e' && cs.charAt(2) == 't') {
            return decapitalize(cs.subSequence(3, length));
          }
        default:
          return decapitalize(cs);
        }
      } else if (length > 3) {
        switch (cs.charAt(0)) {
        case 'g':
          if (cs.charAt(1) == 'e' && cs.charAt(2) == 't') {
            return decapitalize(cs.subSequence(3, length));
          }
        default:
          return decapitalize(cs);
        }
      } else {
        return decapitalize(cs);
      }
    }
  }

  /**
   * Decapitalizes the supplied {@link CharSequence} according to the
   * rules of the Java Beans specification.
   *
   * @param cs the {@link CharSequence} to decapitalize; may be {@code
   * null} in which case {@code null} will be returned
   *
   * @return the decapitalized {@link String} or {@code null}
   *
   * @nullability This method may return {@code null} but only when
   * {@code cs} is {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public static final String decapitalize(final CharSequence cs) {
    if (cs == null) {
      return null;
    } else if (cs.isEmpty() || Character.isLowerCase(cs.charAt(0))) {
      return cs.toString();
    } else if (cs.length() == 1) {
      return cs.toString().toLowerCase();
    } else if (Character.isUpperCase(cs.charAt(1))) {
      return cs.toString();
    } else {
      final char[] chars = cs.toString().toCharArray();
      chars[0] = Character.toLowerCase(chars[0]);
      return String.valueOf(chars);
    }
  }


  /*
   * Inner and nested classes.
   */


  private static final class Handler implements InvocationHandler {

    /**
     * A {@link Loader} whose {@link Loader#of(Path)} method will
     * eventually be called by the {@link #invoke(Object, Method,
     * Object[])} method.
     *
     * <p>Note that this {@link Loader}'s {@link Loader#path()} method
     * will return a {@link Path} that <em>does not identify</em> the
     * actual interface being proxied, much less the {@link Method}
     * being handled by this {@link Handler}.  The {@link
     * #absolutePath} field, instead, contains the {@link Path}
     * identifying the proxied interface (and it will {@linkplain
     * Path#startsWith(Path) start with} the return value of {@link
     * #requestor requestor.path()}).  During execution of the {@link
     * #invoke(Object, Method, Object[])} method, the contents of the
     * {@link #absolutePath} field will be appended with a relative
     * {@link Path} corresponding to the {@link Method} being handled,
     * and <em>that</em> resulting absolute {@link Path} will be
     * supplied to the {@link Loader#of(Path)} method.  Note further
     * that the {@link Loader#of(Path)} method will internally adjust
     * the <em>actual</em> {@link Loader} used (see {@link
     * Loader#loaderFor(Path)}).</p>
     *
     * <p>All of this to say: this {@link Loader} is just a handle of
     * sorts to the proper {@link Loader} that will eventually be used
     * to locate the environmental object corresponding to the return
     * value of the {@link Method} being handled, and serves no other
     * purpose.</p>
     *
     * @see #absolutePath
     *
     * @see #invoke(Object, Method, Object[])
     */
    private final Loader<?> requestor;

    private final Path<? extends Type> absolutePath;

    private final BiFunction<? super Method, ? super Object[], ? extends Path<? extends Type>> pathFunction;

    private Handler(final Loader<?> requestor,
                    final Path<? extends Type> absolutePath,
                    final BiFunction<? super Method, ? super Object[], ? extends Path<? extends Type>> pathFunction) {
      super();
      if (!absolutePath.absolute()) {
        throw new IllegalArgumentException("!absolutePath.absolute(): " + absolutePath);
      } else if (!absolutePath.startsWith(requestor.path())) {
        throw new IllegalArgumentException("!absolutePath.startsWith(requestor.path()); absolutePath: " + absolutePath +
                                           "; requestor.path(): " + requestor.path());
      } else if (absolutePath.equals(requestor.path())) {
        throw new IllegalArgumentException("absolutePath.equals(requestor.path()): " + absolutePath);
      }
      this.requestor = requestor;
      this.absolutePath = absolutePath;
      this.pathFunction = Objects.requireNonNull(pathFunction, "pathFunction");

    }

    @Override // InvocationHandler
    public final Object invoke(final Object proxy, final Method m, final Object[] args) throws ReflectiveOperationException {
      if (m.getDeclaringClass() == Object.class) {
        return
          switch (m.getName()) {
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          case "toString" -> proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
          default -> throw new AssertionError("method: " + m);
          };
      } else {
        final Object returnType = m.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
          return defaultValue(proxy, m, args);
        } else {
          final Path<? extends Type> path = this.pathFunction.apply(m, args);
          assert path.qualified() == returnType : "path.qualified() != returnType: " + path.qualified() + " != " + returnType;
          assert !path.absolute() : "path.absolute(): " + path;
          final OptionalSupplier<Object> s = this.requestor.load(this.absolutePath.plus(path));
          return s.orElseGet(() -> defaultValue(proxy, m, args));
        }
      }
    }

    private static final Object defaultValue(final Object proxy, final Method m, final Object[] args) {
      if (m.isDefault()) {
        try {
          // If the current method is a default method of the proxied
          // interface, invoke it.
          return InvocationHandler.invokeDefault(proxy, m, args);
        } catch (final UnsupportedOperationException | Error e) {
          throw e;
        } catch (final Exception e) {
          throw new UnsupportedOperationException(m.getName(), e);
        } catch (final Throwable e) {
          throw new AssertionError(e.getMessage(), e);
        }
      } else {
        // We have no recourse.
        throw new UnsupportedOperationException(m.toString());
      }
    }

  }

}
