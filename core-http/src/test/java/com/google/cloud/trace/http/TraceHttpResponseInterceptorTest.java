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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.trace.TestTracer;
import com.google.cloud.trace.TestTracer.AnnotateEvent;
import com.google.cloud.trace.TestTracer.EndSpanEvent;
import com.google.cloud.trace.core.Label;
import com.google.cloud.trace.core.TraceContext;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceHttpResponseInterceptorTest {
  private HttpResponse response;

  private TestTracer tracer;
  private TraceHttpResponseInterceptor responseInterceptor;

  @Before
  public void setup() {
    response = new TestResponse(200, ImmutableMap.of("Content-Length", "123"));

    tracer = new TestTracer();
    responseInterceptor = new TraceHttpResponseInterceptor(tracer);
  }

  @Test
  public void testProcess_WithContext() throws IOException {
    TraceContext traceContext = tracer.startSpan("test");
    tracer.reset();
    responseInterceptor.process(response, traceContext);
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
  public void testProcess_WithoutContext() throws IOException {
    responseInterceptor.process(response, null);
    assertThat(tracer.startSpanEvents).hasSize(0);
    assertThat(tracer.endSpanEvents).hasSize(0);
    assertThat(tracer.annotateEvents).hasSize(0);
  }

  private static class TestResponse implements HttpResponse {
    private final int status;
    private final Map<String, String> headers;

    private TestResponse(int status, Map<String, String> headers) {
      this.status = status;
      this.headers = headers;
    }

    public String getHeader(String name) {
      return headers.get(name);
    }

    public int getStatus() {
      return status;
    }
  }
}
