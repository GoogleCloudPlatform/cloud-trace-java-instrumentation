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
import com.google.cloud.trace.TestTracer.StartSpanEvent;
import com.google.cloud.trace.core.Label;
import com.google.cloud.trace.core.TraceContext;
import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceHttpRequestInterceptorTest {

  private HttpRequest requestWithNoHeaders;
  private HttpRequest requestWithHeaders;

  private TestTracer tracer;
  private TraceHttpRequestInterceptor requestInterceptor;

  @Before
  public void setup() {
    requestWithNoHeaders = new TestRequest("GET", "HTTP",
      URI.create("http://example.com/"),
      Collections.<String, String>emptyMap());
    requestWithHeaders = new TestRequest(
      "GET", "HTTP", URI.create("http://example.com/foo/bar"),
      ImmutableMap.of(
          "User-Agent", "test-user-agent",
          "Content-Length", "123456"
      ));
    tracer = new TestTracer();
    requestInterceptor = new TraceHttpRequestInterceptor(tracer);
  }

  @Test
  public void testProcess_WithNoHeaders() throws Exception {
    TraceContext traceContext = requestInterceptor.process(requestWithNoHeaders);

    assertThat(tracer.startSpanEvents).hasSize(1);
    StartSpanEvent startEvent = tracer.startSpanEvents.get(0);
    assertThat(startEvent.getName()).isEqualTo("/");
    assertThat(traceContext).isEqualTo(startEvent.getTraceContext());

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
    TraceContext traceContext = requestInterceptor.process(requestWithHeaders);

    assertThat(tracer.startSpanEvents).hasSize(1);
    StartSpanEvent startEvent = tracer.startSpanEvents.get(0);
    assertThat(startEvent.getName()).isEqualTo("/foo/bar");
    assertThat(traceContext).isEqualTo(startEvent.getTraceContext());

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

  private static class TestRequest implements HttpRequest {
    private final String method, protocol;
    private final URI uri;
    private final Map<String, String> headers;

    public TestRequest(String method, String protocol, URI uri, Map<String, String> headers) {
      this.method = method;
      this.protocol = protocol;
      this.uri = uri;
      this.headers = headers;
    }

    public String getMethod() {
      return method;
    }

    public URI getURI() {
      return uri;
    }

    public String getHeader(String name) {
      return headers.get(name);
    }

    public String getProtocol() {
      return protocol;
    }
  }
}
