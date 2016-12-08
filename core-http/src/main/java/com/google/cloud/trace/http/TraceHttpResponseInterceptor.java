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

package com.google.cloud.trace.http;

import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.TraceContext;

/**
 * An interceptor that records tracing information for HTTP responses. Should be used along with
 * {@link TraceHttpRequestInterceptor}.
 */
public class TraceHttpResponseInterceptor {
  private final Tracer tracer;

  public TraceHttpResponseInterceptor() {
    this(Trace.getTracer());
  }

  public TraceHttpResponseInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Ends a span for an HTTP request and records relevant labels.
   * @param response The HTTP response.
   * @param traceContext The TraceContext for the request.
   */
  public void process(HttpResponse response, TraceContext traceContext) {
    if (traceContext == null) {
      return;
    }
    Labels.Builder labels = Labels.builder();
    TraceInterceptorUtil.annotateIfNotEmpty(labels, HttpLabels.RESPONSE_SIZE,
        response.getHeader(HttpHeaders.CONTENT_LENGTH));
    labels.add(HttpLabels.HTTP_STATUS_CODE, Integer.toString(response.getStatus()));
    tracer.annotateSpan(traceContext, labels.build());
    tracer.endSpan(traceContext);
  }
}
