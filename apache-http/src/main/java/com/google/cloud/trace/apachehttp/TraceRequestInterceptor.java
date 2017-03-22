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

import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.http.HttpRequest;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import java.io.IOException;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpRequestInterceptor} that records tracing information. Should be used along with
 * {@link com.google.cloud.trace.apachehttp.TraceResponseInterceptor}
 */
public class TraceRequestInterceptor implements HttpRequestInterceptor {

  private final TraceHttpRequestInterceptor interceptor;

  public TraceRequestInterceptor() {
    this(new TraceHttpRequestInterceptor());
  }

  public TraceRequestInterceptor(TraceHttpRequestInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  public void process(org.apache.http.HttpRequest request, HttpContext context) throws HttpException, IOException {
    TraceContext traceContext = interceptor.process(new RequestAdapter(request));
    request.addHeader(SpanContextFactory.headerKey(),
        SpanContextFactory.toHeader(traceContext.getHandle().getCurrentSpanContext()));
    context.setAttribute(TraceInterceptorUtil.TRACE_CONTEXT_KEY, traceContext);
  }

  private static class RequestAdapter implements HttpRequest {

    private final org.apache.http.HttpRequest request;

    public RequestAdapter(org.apache.http.HttpRequest request) {
      this.request = request;
    }

    public String getMethod() {
      return request.getRequestLine().getMethod();
    }

    public URI getURI() {
      return URI.create(request.getRequestLine().getUri());
    }

    public String getHeader(String name) {
      Header header = request.getFirstHeader(name);
      if (header == null) {
        return null;
      } else {
        return header.getValue();
      }
    }

    public String getProtocol() {
      return request.getRequestLine().getProtocolVersion().getProtocol();
    }
  }
}
