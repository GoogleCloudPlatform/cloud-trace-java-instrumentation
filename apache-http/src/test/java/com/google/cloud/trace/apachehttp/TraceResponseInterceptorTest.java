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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.trace.TestTracer;
import com.google.cloud.trace.TestTracer.AnnotateEvent;
import com.google.cloud.trace.TestTracer.EndSpanEvent;
import com.google.cloud.trace.core.Label;
import com.google.cloud.trace.core.TraceContext;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceResponseInterceptorTest {
  private HttpResponse response;

  private TestTracer tracer;
  private TraceResponseInterceptor responseInterceptor;

  @Before
  public void setup() {
    response = new BasicHttpResponse(new ProtocolVersion("HTTP", 2, 0), 200, "OK");
    response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(123));

    tracer = new TestTracer();
    responseInterceptor = new TraceResponseInterceptor(tracer);
  }

  @Test
  public void testProcess_WithContext() throws IOException, HttpException {
    TraceContext traceContext = tracer.startSpan("test");
    tracer.reset();
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("TRACE-CONTEXT", traceContext);
    responseInterceptor.process(response, httpContext);
    assertThat(tracer.endSpanEvents).hasSize(1);
    EndSpanEvent endEvent = tracer.endSpanEvents.get(0);
    assertThat(endEvent.getTraceContext()).isEqualTo(traceContext);

    assertThat(tracer.annotateEvents).hasSize(1);
    AnnotateEvent annotateEvent = tracer.annotateEvents.get(0);
    assertThat(annotateEvent.getLabels().getLabels()).containsAllOf(
        new Label("/response/size", "123"),
        new Label("/http/status_code", "200")
    );
    assertThat(annotateEvent.getTraceContext()).isEqualTo(traceContext);
  }

  @Test
  public void testProcess_WithoutContext() throws IOException, HttpException {
    HttpContext httpContext = new BasicHttpContext();
    responseInterceptor.process(response, httpContext);
    assertThat(tracer.startSpanEvents).hasSize(0);
    assertThat(tracer.endSpanEvents).hasSize(0);
    assertThat(tracer.annotateEvents).hasSize(0);
  }
}
