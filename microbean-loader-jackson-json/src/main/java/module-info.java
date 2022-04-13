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
import org.microbean.loader.spi.Provider;

import org.microbean.loader.jackson.json.JsonProvider;

/**
 * Provides packages related to implementing {@link Provider}s using
 * <a href="https://github.com/FasterXML/jackson"
 * target="_top">Jackson</a> constructs.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
module org.microbean.loader.jackson.json {

  exports org.microbean.loader.jackson.json;

  requires com.fasterxml.jackson.databind;
  requires transitive org.microbean.loader.jackson;
  
}
