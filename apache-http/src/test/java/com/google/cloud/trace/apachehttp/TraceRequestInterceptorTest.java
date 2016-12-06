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
import com.google.cloud.trace.TestTracer.StartSpanEvent;
import com.google.cloud.trace.core.Label;
import org.apache.http.HttpHeaders;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceRequestInterceptorTest {
  private HttpGet requestWithNoHeaders;
  private HttpGet requestWithHeaders;

  private TestTracer tracer;
  private TraceRequestInterceptor requestInterceptor;

  @Before
  public void setup() {
    requestWithNoHeaders = new HttpGet("http://example.com/");

    requestWithHeaders = new HttpGet("http://example.com/foo/bar");
    requestWithHeaders.setHeader(HttpHeaders.USER_AGENT, "test-user-agent");
    requestWithHeaders.setProtocolVersion(new ProtocolVersion("HTTP", 2, 0));
    requestWithHeaders.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(123456));

    tracer = new TestTracer();
    requestInterceptor = new TraceRequestInterceptor(tracer);
  }

  @Test
  public void testProcess_WithNoHeaders() throws Exception {
    HttpContext context = new BasicHttpContext();
    requestInterceptor.process(requestWithNoHeaders, context);

    assertThat(tracer.startSpanEvents).hasSize(1);
    StartSpanEvent startEvent = tracer.startSpanEvents.get(0);
    assertThat(startEvent.getName()).isEqualTo("http://example.com/");
    assertThat(context.getAttribute("TRACE-CONTEXT")).isEqualTo(startEvent.getTraceContext());

    assertThat(tracer.annotateEvents).hasSize(1);
    AnnotateEvent annotateEvent = tracer.annotateEvents.get(0);
    assertThat(annotateEvent.getLabels().getLabels()).containsAllOf(
        new Label("/http/method", "GET"),
        new Label("/http/client_protocol", "HTTP")
    );
    assertThat(annotateEvent.getTraceContext()).isEqualTo(startEvent.getTraceContext());
  }

  @Test
  public void testProcess_WithHeaders() throws Exception {
    HttpContext context = new BasicHttpContext();
    requestInterceptor.process(requestWithHeaders, context);

    assertThat(tracer.startSpanEvents).hasSize(1);
    StartSpanEvent startEvent = tracer.startSpanEvents.get(0);
    assertThat(startEvent.getName()).isEqualTo("http://example.com/foo/bar");
    assertThat(context.getAttribute("TRACE-CONTEXT")).isEqualTo(startEvent.getTraceContext());

    assertThat(tracer.annotateEvents).hasSize(1);
    AnnotateEvent annotateEvent = tracer.annotateEvents.get(0);
    assertThat(annotateEvent.getLabels().getLabels()).containsAllOf(
        new Label("/http/user_agent", "test-user-agent"),
        new Label("/request/size", "123456"),
        new Label("/http/method", "GET"),
        new Label("/http/client_protocol", "HTTP")
    );
    assertThat(annotateEvent.getTraceContext()).isEqualTo(startEvent.getTraceContext());
  }
}
