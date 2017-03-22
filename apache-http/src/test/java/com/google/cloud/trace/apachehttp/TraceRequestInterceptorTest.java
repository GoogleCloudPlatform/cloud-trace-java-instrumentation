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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.trace.TestSpanContextHandle;
import com.google.cloud.trace.core.SpanContext;
import com.google.cloud.trace.core.SpanId;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.core.TraceId;
import com.google.cloud.trace.core.TraceOptions;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.HttpRequest;
import java.math.BigInteger;
import java.net.URI;
import org.apache.http.HttpHeaders;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

@RunWith(JUnit4.class)
public class TraceRequestInterceptorTest {
  private HttpGet requestWithHeaders;

  private TraceRequestInterceptor requestInterceptor;
  private TraceHttpRequestInterceptor mockDelegate;
  private TraceContext testContext = new TraceContext(
      new TestSpanContextHandle(new SpanContext(new TraceId(
          BigInteger.TEN), new SpanId(22), TraceOptions.forTraceEnabled())));

  @Before
  public void setup() {
    requestWithHeaders = new HttpGet("http://example.com/foo/bar");
    requestWithHeaders.setHeader(HttpHeaders.USER_AGENT, "test-user-agent");
    requestWithHeaders.setProtocolVersion(new ProtocolVersion("HTTP", 2, 0));
    requestWithHeaders.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(123456));

    mockDelegate = mock(TraceHttpRequestInterceptor.class);
    when(mockDelegate.process(any(HttpRequest.class))).thenReturn(testContext);
    requestInterceptor = new TraceRequestInterceptor(mockDelegate);
  }

  @Test
  public void testProcess() throws Exception {
    HttpContext context = new BasicHttpContext();
    requestInterceptor.process(requestWithHeaders, context);
    assertThat(context.getAttribute("TRACE-CONTEXT")).isEqualTo(testContext);

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockDelegate).process(captor.capture());
    HttpRequest request = captor.getValue();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getProtocol()).isEqualTo("HTTP");
    assertThat(request.getURI()).isEqualTo(URI.create("http://example.com/foo/bar"));
    assertThat(request.getHeader("User-Agent")).isEqualTo("test-user-agent");
  }
}
