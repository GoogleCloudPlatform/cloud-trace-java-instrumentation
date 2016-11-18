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

import com.google.cloud.trace.Tracer;
import com.google.common.base.Optional;

/**
 * Thread-local context holding the current thread's {@link Tracer} (if any).
 *
 * <p>This context is used for passing the {@code Tracer} to other methods in cases where
 * there is no way to pass the {@code Tracer} as a parameter.
 *
 * TODO: Replace this with the upcoming mechanism in the Cloud Trace for Java SDK.
 */
public final class ThreadLocalTracerStore {
  private ThreadLocalTracerStore() {}

  /** Holds the current thread's {@link Tracer} (if any). */
  private static final ThreadLocal<Tracer> perThreadTracer = new ThreadLocal<>();

  /** Returns the current thread's {@link Tracer}. */
  public static Optional<Tracer> getCurrent() {
    return Optional.fromNullable(perThreadTracer.get());
  }

  /** Sets the {@link Tracer} associated with the current thread. */
  public static void setCurrent(Tracer tracer) {
    perThreadTracer.set(tracer);
  }

  /** Removes the current thread's {@link Tracer}. */
  public static void remove() {
    perThreadTracer.remove();
  }
}
