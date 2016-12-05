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
import com.google.cloud.trace.core.TraceContext;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpResponseInterceptor} that records tracing information. Should be used along with
 * {@link com.google.cloud.trace.apachehttp.TraceRequestInterceptor}
 */
public class TraceResponseInterceptor implements HttpResponseInterceptor {

  private final Tracer tracer;

  public TraceResponseInterceptor() {
    this(Trace.getTracer());
  }

  public TraceResponseInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  public void process(HttpResponse response, HttpContext context)
      throws HttpException, IOException {
    TraceContext traceContext = (TraceContext) context
        .getAttribute(TraceInterceptorUtil.TRACE_CONTEXT_KEY);
    if (traceContext == null) {
      return;
    }
    Labels.Builder labels = Labels.builder();
    TraceInterceptorUtil.annotateFromHeader(labels, HttpLabels.RESPONSE_SIZE,
        response.getFirstHeader(HttpHeaders.CONTENT_LENGTH));
    labels.add(HttpLabels.HTTP_STATUS_CODE,
        Integer.toString(response.getStatusLine().getStatusCode()));
    tracer.annotateSpan(traceContext, labels.build());
    tracer.endSpan(traceContext);
  }
}
