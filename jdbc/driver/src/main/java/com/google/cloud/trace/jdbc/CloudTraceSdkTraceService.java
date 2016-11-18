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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.service.AutoService;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.TraceContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

/**
 * Implementation of {@link TraceService} based on the Cloud Trace SDK's {@link Tracer},
 * passed in via a {@link ThreadLocal}.
 *
 * TODO: Replace this with the upcoming mechanism in the Cloud Trace for Java SDK.
 */
@AutoService(TraceService.class)
public class CloudTraceSdkTraceService extends TraceService {
  private final Tracers tracers;

  // Visible no-arg constructor required ServiceLoader.
  public CloudTraceSdkTraceService() {
    this(new Tracers());
  }

  @VisibleForTesting
  CloudTraceSdkTraceService(Tracers tracers) {
    this.tracers = checkNotNull(tracers);
  }

  @Override
  public Span open(String name) {
    checkNotNull(name);

    return new SpanImpl(tracers.getCurrent(), name);
  }

  private static class SpanImpl implements Span {
    private final Tracer tracer;
    private final TraceContext traceContext;

    SpanImpl(Tracer tracer, String name) {
      checkNotNull(tracer);
      checkNotNull(name);

      this.tracer = tracer;
      this.traceContext = tracer.startSpan(name);
    }

    @Override
    public void annotate(Label label, Optional<String> value) {
      checkNotNull(label);
      checkNotNull(value);

      if (value.isPresent()) {
        tracer.annotateSpan(traceContext, Labels.builder().add(label.key(), value.get()).build());
      }
    }

    @Override
    public void close() {
      tracer.endSpan(traceContext);
    }
  }
}
