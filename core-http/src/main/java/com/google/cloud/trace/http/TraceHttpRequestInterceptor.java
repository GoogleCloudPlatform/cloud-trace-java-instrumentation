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
 * An interceptor that records tracing information for HTTP requests. Should be used along with
 * {@link TraceHttpResponseInterceptor}.
 */
public class TraceHttpRequestInterceptor {

  private final Tracer tracer;

  public TraceHttpRequestInterceptor() {
    this(Trace.getTracer());
  }

  public TraceHttpRequestInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Starts a span for an HTTP request and record relevant labels.
   * @param request The HTTP request.
   * @return The new TraceContext
   */
  public TraceContext process(HttpRequest request) {
    Labels.Builder labels = Labels.builder();
    TraceInterceptorUtil
        .annotateIfNotEmpty(labels, HttpLabels.HTTP_METHOD, request.getURI().toString());
    labels.add(HttpLabels.HTTP_METHOD, request.getMethod());
    TraceInterceptorUtil
        .annotateIfNotEmpty(labels, HttpLabels.HTTP_URL, request.getURI().toString());
    TraceInterceptorUtil.annotateIfNotEmpty(labels, HttpLabels.HTTP_CLIENT_PROTOCOL,
        request.getProtocol());
    TraceInterceptorUtil.annotateIfNotEmpty(labels, HttpLabels.HTTP_USER_AGENT,
        request.getHeader(HttpHeaders.USER_AGENT));
    TraceInterceptorUtil.annotateIfNotEmpty(labels, HttpLabels.REQUEST_SIZE,
        request.getHeader(HttpHeaders.CONTENT_LENGTH));
    TraceContext traceContext = tracer.startSpan(request.getURI().getPath());
    tracer.annotateSpan(traceContext, labels.build());
    return traceContext;
  }
}
