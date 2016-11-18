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
import com.google.cloud.trace.core.EndSpanOptions;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.StackTrace;
import com.google.cloud.trace.core.StartSpanOptions;
import com.google.cloud.trace.core.SpanContext;
import com.google.cloud.trace.core.TraceContext;

/**
 * Helpers for working with {@link ThreadLocalTracerStore}.
 *
 * <p>This helps to avoid the direct dependency on thread-local storage (no static {@link
 * ThreadLocal} here) and reduces the API surface of {@link ThreadLocalTracerStore}.
 *
 * TODO: Replace this with the upcoming mechanism in the Cloud Trace for Java SDK.
 */
class Tracers {
  /**
   * A no-op {@link Tracer} which does nothing and has tracing disabled in its {@link
   * SpanContext}.
   */
  private static final Tracer NOOP_TRACER = new Tracer() {
    @Override
    public TraceContext startSpan(String name, StartSpanOptions options) {
      return null;
    }

    @Override
    public TraceContext startSpan(String name) {
      return null;
    }

    @Override
    public void setStackTrace(TraceContext traceContext, StackTrace stackTrace) {}

    @Override
    public void endSpan(TraceContext traceContext, EndSpanOptions options) {}

    @Override
    public void endSpan(TraceContext traceContext) {}

    @Override
    public void annotateSpan(TraceContext traceContext, Labels labels) {}
  };

  /**
   * Returns the current thread's {@link Tracer} or a no-op implementation if there is no
   * {@code Tracer} set for the current thread, thus no specialization at call sites needed.
   */
  Tracer getCurrent() {
    return ThreadLocalTracerStore.getCurrent().or(NOOP_TRACER);
  }
}
