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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

/**
 * Implementation of {@link TraceService} based on Google App Engine's Trace Service API.
 *
 * TODO: Replace this with the upcoming mechanism in the Cloud Trace for Java SDK.
 */
@AutoService(TraceService.class)
public class AppEngineTraceService extends TraceService {
  private final com.google.appengine.api.labs.trace.TraceService traceService;

  // Visible no-arg constructor required by {@link java.util.ServiceLoader}.
  public AppEngineTraceService() {
    this(com.google.appengine.api.labs.trace.TraceServiceFactory.getTraceService());
  }

  @VisibleForTesting
  AppEngineTraceService(com.google.appengine.api.labs.trace.TraceService traceService) {
    this.traceService = checkNotNull(traceService);
  }

  @Override
  public Span open(String name) {
    checkNotNull(name);

    return new SpanImpl(traceService.startSpan(name));
  }

  private static class SpanImpl implements Span {
    private final com.google.appengine.api.labs.trace.Span span;

    SpanImpl(com.google.appengine.api.labs.trace.Span span) {
      this.span = checkNotNull(span);
    }

    @Override
    public void annotate(Label label, Optional<String> value) {
      checkNotNull(label);
      checkNotNull(value);

      if (value.isPresent()) {
        span.setLabel(label.key(), value.get());
      }
    }

    @Override
    public void close() {
      span.close();
    }
  }
}
