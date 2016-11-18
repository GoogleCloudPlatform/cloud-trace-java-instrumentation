// Copyright 2016 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.trace.jdbc;

import com.google.common.base.Optional;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link TraceService} provides a minimal interface for starting trace spans, abstracting away the
 * underlying differences in trace APIs (e.g. App Engine Trace API vs. Cloud Trace SDK).
 */
abstract class TraceService {

  private static final TraceService INSTANCE = createInstance();

  /**
   * Creates and returns the platform-specific implementation of {@link TraceService}, located at
   * runtime via {@link ServiceLoader}.
   */
  private static TraceService createInstance() {
    Iterator<TraceService> it = ServiceLoader.load(TraceService.class).iterator();

    if (!it.hasNext()) {
      Logger.getLogger(TraceService.class.getName())
          .log(
              Level.SEVERE,
              "Could not find an implementation of {0}. "
                  + "Class initialization will fail, expect a java.lang.NoClassDefFoundError.",
              TraceService.class);
    }

    return it.next();
  }

  /** Returns the platform-specific implementation of {@link TraceService}. */
  static TraceService getInstance() {
    return INSTANCE;
  }

  /** Starts a new span with the specified name. */
  abstract Span open(String name);

  /** {@link Span} allows clients to manage details of a span: set a label or end the span. */
  interface Span extends AutoCloseable {

    /** Annotates this span using the specified label and value, if present. */
    void annotate(Label label, Optional<String> value);

    /** Ends the current span. */
    @Override
    void close();
  }
}
