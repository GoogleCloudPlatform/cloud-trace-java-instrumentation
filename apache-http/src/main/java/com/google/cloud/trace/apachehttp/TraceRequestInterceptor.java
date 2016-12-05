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

package com.google.cloud.trace.apachehttp;

import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpRequestInterceptor} that records tracing information. Should be used along with
 * {@link com.google.cloud.trace.apachehttp.TraceResponseInterceptor}
 */
public class TraceRequestInterceptor implements HttpRequestInterceptor {

  private final Tracer tracer;

  public TraceRequestInterceptor() {
    this(Trace.getTracer());
  }

  public TraceRequestInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
    Labels.Builder labels = Labels.builder();
    String uri = request.getRequestLine().getUri();
    TraceInterceptorUtil
        .annotateIfNotEmpty(labels, HttpLabels.HTTP_METHOD, request.getRequestLine().getMethod());
    TraceInterceptorUtil.annotateIfNotEmpty(labels, HttpLabels.HTTP_URL, uri);

    TraceInterceptorUtil.annotateIfNotEmpty(labels, HttpLabels.HTTP_CLIENT_PROTOCOL,
        request.getProtocolVersion().getProtocol());
    TraceInterceptorUtil
        .annotateFromHeader(labels, HttpLabels.HTTP_USER_AGENT, request.getFirstHeader(
            HttpHeaders.USER_AGENT));
    TraceInterceptorUtil.annotateFromHeader(labels, HttpLabels.REQUEST_SIZE,
        request.getFirstHeader(HttpHeaders.CONTENT_LENGTH));
    TraceContext traceContext = tracer.startSpan(uri);
    tracer.annotateSpan(traceContext, labels.build());
    request.setHeader(SpanContextFactory.headerKey(),
        SpanContextFactory.toHeader(traceContext.getHandle().getCurrentSpanContext()));
    context.setAttribute(TraceInterceptorUtil.TRACE_CONTEXT_KEY, traceContext);
  }
}
