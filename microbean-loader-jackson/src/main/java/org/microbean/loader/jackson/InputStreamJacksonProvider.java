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
package org.microbean.loader.jackson;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import java.lang.reflect.Type;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import java.util.Objects;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.microbean.development.annotation.Convenience;

import org.microbean.loader.api.Loader;

import org.microbean.path.Path;

import org.microbean.type.JavaTypes;

/**
 * A {@link JacksonProvider} built around an {@link
 * InputStream}-providing {@linkplain BiFunction bifunction} and an
 * {@link ObjectCodec}-providing {@linkplain BiFunction bifunction}.
 *
 * @param <T> the upper bound of types of objects that this {@link
 * InputStreamJacksonProvider} provides
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public class InputStreamJacksonProvider<T> extends JacksonProvider<T> {


  /*
   * Instance fields.
   */


  private final BiFunction<? super Loader<?>, ? super Path<? extends Type>, ? extends ObjectCodec> objectCodecFunction;

  private final BiFunction<? super Loader<?>, ? super Path<? extends Type>, ? extends InputStream> inputStreamFunction;

  private final Consumer<? super InputStream> inputStreamReadConsumer;


  /*
   * Constructors.
   */


  private InputStreamJacksonProvider() {
    super();
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new {@link InputStreamJacksonProvider}.
   *
   * @param mapperSupplier a {@link Supplier}, deterministic or not,
   * of {@link ObjectMapper} instances; ordinarily callers should
   * supply a {@link Supplier} that caches; may be {@code null}
   *
   * @param resourceName a resource name that is treated first as a
   * classpath resource and finally as the name of a file relative to
   * the directory identified by the {@link System#getProperty(String,
   * String) user.dir} system property
   *
   * @see #InputStreamJacksonProvider(BiFunction, BiFunction, Consumer)
   *
   * @see #inputStream(ClassLoader, String)
   */
  public InputStreamJacksonProvider(final Supplier<? extends ObjectMapper> mapperSupplier, final String resourceName) {
    this(objectCodecFunction(mapperSupplier),
         (l, p) -> inputStream(classLoader(p), resourceName),
         InputStreamJacksonProvider::closeInputStream);
  }

  /**
   * Creates a new {@link InputStreamJacksonProvider}.
   *
   * @param objectCodecFunction a {@link BiFunction} that returns an
   * {@link ObjectCodec} when supplied with a {@link Loader} and a
   * {@link Path}; may be {@code null}
   *
   * @param inputStreamFunction a {@link BiFunction} that returns an
   * open {@link InputStream} when supplied with a {@link Loader} and
   * a {@link Path}; may be {@code null}
   *
   * @param inputStreamReadConsumer a {@link Consumer} that is called
   * with an {@link InputStream} after the {@link InputStream} has
   * been fully read; may be {@code null}; normally should
   * {@linkplain InputStream#close() close} the {@link InputStream}
   */
  public InputStreamJacksonProvider(final BiFunction<? super Loader<?>, ? super Path<? extends Type>, ? extends ObjectCodec> objectCodecFunction,
                                    final BiFunction<? super Loader<?>, ? super Path<? extends Type>, ? extends InputStream> inputStreamFunction,
                                    final Consumer<? super InputStream> inputStreamReadConsumer) {
    super();
    this.objectCodecFunction = objectCodecFunction == null ? InputStreamJacksonProvider::returnNull : objectCodecFunction;
    this.inputStreamFunction = inputStreamFunction == null ? InputStreamJacksonProvider::returnNull : inputStreamFunction;
    this.inputStreamReadConsumer = inputStreamReadConsumer == null ? InputStreamJacksonProvider::sink : inputStreamReadConsumer;
  }


  /*
   * Instance methods.
   */


  /**
   * Invokes the {@link BiFunction#apply(Object, Object)} method of
   * the {@code objectCodecFunction} {@linkplain
   * #InputStreamJacksonProvider(BiFunction, BiFunction, Consumer)
   * supplied at construction time} and returns the result.
   *
   * @param <T> the type of the value ultimately being provided
   *
   * @param requestingLoader the {@link Loader} requesting a value;
   * must not be {@code null}
   *
   * @param absolutePath the {@linkplain Path#absolute() absolute}
   * {@link Path} the {@code requestingLoader} is currently
   * requesting; must not be {@code null}
   *
   * @return an {@link ObjectCodec} suitable for the supplied
   * arguments, or {@code null} if this {@link
   * InputStreamJacksonProvider} should not handle the current request
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, but the {@code objectCodecFunction} {@linkplain
   * #InputStreamJacksonProvider(BiFunction, BiFunction, Consumer)
   * supplied at construction time} may not be.
   *
   * @idempotency This method is idempotent and deterministic if the
   * {@code objectCodecFunction} {@linkplain
   * #InputStreamJacksonProvider(BiFunction, BiFunction, Consumer)
   * supplied at construction time} is.
   */
  @Override // JacksonProvider<T>
  protected final <T> ObjectCodec objectCodec(final Loader<?> requestingLoader, final Path<? extends Type> absolutePath) {
    return this.objectCodecFunction.apply(requestingLoader, absolutePath);
  }

  /**
   * Overrides {@link JacksonProvider#rootNode(Loader, Path,
   * ObjectCodec)} to produce a {@link TreeNode} using the {@code
   * inputStreamFunction} {@linkplain
   * #InputStreamJacksonProvider(BiFunction, BiFunction, Consumer)
   * supplied at construction time}.
   *
   * <p>This method will return {@code null} to indicate that this
   * {@link InputStreamJacksonProvider} will not handle the current
   * request.</p>
   *
   * @param requestingLoader the {@link Loader} requesting a value;
   * must not be {@code null}
   *
   * @param absolutePath the {@linkplain Path#absolute() absolute}
   * {@link Path} the {@code requestingLoader} is currently
   * requesting; must not be {@code null}
   *
   * @param objectCodec the {@link ObjectCodec} returned by the {@link
   * #objectCodec(Loader, Path)} method; may be {@code null} in which
   * case {@code null} will be returned
   *
   * @return a {@link TreeNode}, or {@code null}
   *
   * @nullability This method and its overrides may return {@code null}.
   *
   * @threadsafety This method is, and its overrides must be, safe for
   * concurrent use by multiple threads, but the {@code
   * inputStreamFunction} {@linkplain
   * #InputStreamJacksonProvider(BiFunction, BiFunction, Consumer)
   * supplied at construction time} used by this method may not be
   *
   * @idempotency This method is, and its overrides must be,
   * idempotent, but not necessarily deterministic.
   */
  @Override // JacksonProvider<T>
  protected <T> TreeNode rootNode(final Loader<?> requestingLoader, final Path<? extends Type> absolutePath, final ObjectCodec objectCodec) {
    TreeNode returnValue = null;
    if (objectCodec != null) {
      InputStream is = null;
      RuntimeException runtimeException = null;
      JsonParser parser = null;
      try {
        is = this.inputStreamFunction.apply(requestingLoader, absolutePath);
        if (is != null) {
          parser = objectCodec.getFactory().createParser(is);
          parser.setCodec(objectCodec);
          returnValue = parser.readValueAsTree();
        }
      } catch (final IOException ioException) {
        runtimeException = new UncheckedIOException(ioException.getMessage(), ioException);
      } catch (final RuntimeException e) {
        runtimeException = e;
      } finally {
        try {
          if (parser != null) {
            parser.setCodec(null);
            parser.close();
          }
        } catch (final IOException ioException) {
          if (runtimeException == null) {
            runtimeException = new UncheckedIOException(ioException.getMessage(), ioException);
          } else {
            runtimeException.addSuppressed(ioException);
          }
        } catch (final RuntimeException e) {
          if (runtimeException == null) {
            runtimeException = e;
          } else {
            runtimeException.addSuppressed(e);
          }
        } finally {
          try {
            if (is != null) {
              this.inputStreamReadConsumer.accept(is);
            }
          } catch (final RuntimeException e) {
            if (runtimeException == null) {
              runtimeException = e;
            } else {
              runtimeException.addSuppressed(e);
            }
          } finally {
            if (runtimeException != null) {
              throw runtimeException;
            }
          }
        }
      }
    }
    return returnValue;
  }


  /*
   * Static methods.
   */


  /**
   * Returns an open {@link InputStream} loaded using the supplied
   * {@link ClassLoader} and a name of a classpath resource.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param cl the {@link ClassLoader} that will actually cause the
   * {@link InputStream} to be created and opened; may be {@code null}
   * in which case the system classloader will be used instead
   *
   * @param resourceName the name of the classpath resource for which
   * an {@link InputStream} will be created and opened; must not be
   * {@code null}
   *
   * @return a non-{@code null}, open {@link InputStream}
   *
   * @exception NullPointerException if {@code resourceName} is {@code
   * null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Convenience
  protected static final InputStream inputStream(final ClassLoader cl, final String resourceName) {
    final InputStream returnValue;
    InputStream temp = cl == null ? ClassLoader.getSystemResourceAsStream(resourceName) : cl.getResourceAsStream(resourceName);
    if (temp == null) {
      try {
        temp = new BufferedInputStream(Files.newInputStream(Paths.get(System.getProperty("user.dir", "."), resourceName)));
      } catch (final FileNotFoundException /* this probably isn't thrown */ | NoSuchFileException e) {

      } catch (final IOException ioException) {
        throw new UncheckedIOException(ioException.getMessage(), ioException);
      } finally {
        returnValue = temp;
      }
    } else if (temp instanceof BufferedInputStream) {
      returnValue = (BufferedInputStream)temp;
    } else {
      returnValue = new BufferedInputStream(temp);
    }
    return returnValue;
  }

  /**
   * Calls {@link InputStream#close()} on the supplied {@link
   * InputStream} if it is non-{@code null}.
   *
   * @param is the {@link InputStream}; may be {@code null} in which
   * case no action will be taken
   *
   * @exception UncheckedIOException if an {@link IOException} is
   * thrown by the {@link InputStream#close()} method
   */
  @Convenience
  protected static final void closeInputStream(final InputStream is) {
    if (is != null) {
      try {
        is.close();
      } catch (final IOException ioException) {
        throw new UncheckedIOException(ioException.getMessage(), ioException);
      }
    }
  }

  private static final BiFunction<? super Loader<?>, ? super Path<? extends Type>, ? extends ObjectCodec> objectCodecFunction(final Supplier<? extends ObjectMapper> mapperSupplier) {
    if (mapperSupplier == null) {
      return InputStreamJacksonProvider::returnNull;
    } else {
      return (l, p) -> {
        // Note that otherwise potential infinite loops are handled in
        // the DefaultLoader class.
        final ObjectMapper mapper = l.load(ObjectMapper.class).orElseGet(mapperSupplier);
        if (mapper == null) {
          return null;
        }
        final JavaType javaType = mapper.constructType(p.qualified());
        return mapper.canDeserialize(javaType) ? mapper.readerFor(javaType) : null;
      };
    }
  }

  private static final ClassLoader classLoader(final Path<? extends Type> path) {
    final Class<?> c = JavaTypes.erase(path.qualified());
    ClassLoader cl = null;
    if (c == null) {
      cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) {
        cl = ClassLoader.getSystemClassLoader();
      }
    } else {
      cl = c.getClassLoader();
    }
    return cl;
  }
  
  private static final void sink(final Object ignored) {

  }
  
  private static final <T> T returnNull(final Object ignored, final Object alsoIgnored) {
    return null;
  }

}
