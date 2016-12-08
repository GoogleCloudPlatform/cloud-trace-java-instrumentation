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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.trace.TestSpanContextHandle;
import com.google.cloud.trace.core.SpanContext;
import com.google.cloud.trace.core.SpanId;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.core.TraceId;
import com.google.cloud.trace.core.TraceOptions;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import com.google.cloud.trace.http.HttpResponse;
import java.io.IOException;
import java.math.BigInteger;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

@RunWith(JUnit4.class)
public class TraceResponseInterceptorTest {
  private BasicHttpResponse response;

  private TraceResponseInterceptor responseInterceptor;
  private TraceHttpResponseInterceptor mockDelegate;
  private TraceContext testContext = new TraceContext(
      new TestSpanContextHandle(new SpanContext(new TraceId(
          BigInteger.TEN), new SpanId(22), TraceOptions.forTraceEnabled())));

  @Before
  public void setup() {
    response = new BasicHttpResponse(new ProtocolVersion("HTTP", 2, 0), 200, "OK");
    response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(123));

    mockDelegate = mock(TraceHttpResponseInterceptor.class);
    responseInterceptor = new TraceResponseInterceptor(mockDelegate);
  }

  @Test
  public void testProcess_WithContext() throws IOException, HttpException {
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("TRACE-CONTEXT", testContext);
    responseInterceptor.process(response, httpContext);

    ArgumentCaptor<HttpResponse> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    ArgumentCaptor<TraceContext> contextCaptor = ArgumentCaptor.forClass(TraceContext.class);
    verify(mockDelegate).process(responseCaptor.capture(), contextCaptor.capture());

    HttpResponse response = responseCaptor.getValue();
    TraceContext traceContext = contextCaptor.getValue();
    assertThat(traceContext).isEqualTo(testContext);
    assertThat(response.getHeader("Content-Length")).isEqualTo("123");
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void testProcess_WithoutContext() throws IOException, HttpException {
    HttpContext httpContext = new BasicHttpContext();
    responseInterceptor.process(response, httpContext);
    verify(mockDelegate).process(any(HttpResponse.class), (TraceContext) isNull());
  }
}
