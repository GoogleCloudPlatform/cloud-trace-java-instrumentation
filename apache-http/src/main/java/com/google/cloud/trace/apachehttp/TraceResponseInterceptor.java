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

import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import com.google.cloud.trace.http.HttpResponse;
import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpResponseInterceptor} that records tracing information. Should be used along with
 * {@link com.google.cloud.trace.apachehttp.TraceRequestInterceptor}
 */
public class TraceResponseInterceptor implements HttpResponseInterceptor {

  private final TraceHttpResponseInterceptor interceptor;

  public TraceResponseInterceptor() {
    this(new TraceHttpResponseInterceptor());
  }

  public TraceResponseInterceptor(TraceHttpResponseInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  public void process(org.apache.http.HttpResponse response, HttpContext context)
      throws HttpException, IOException {
    TraceContext traceContext = (TraceContext) context
        .getAttribute(TraceInterceptorUtil.TRACE_CONTEXT_KEY);
    interceptor.process(new ResponseAdapter(response), traceContext);
  }

  private static class ResponseAdapter implements HttpResponse {
    private final org.apache.http.HttpResponse response;

    private ResponseAdapter(org.apache.http.HttpResponse response) {
      this.response = response;
    }

    public String getHeader(String name) {
      Header header = response.getFirstHeader(name);
      if (header == null) {
        return null;
      }
      return header.getValue();
    }

    public int getStatus() {
      return response.getStatusLine().getStatusCode();
    }
  }
}
