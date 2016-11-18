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

package com.google.cloud.trace.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;

/** Unit tests for {@link AppEngineTraceService}. */
@RunWith(JUnit4.class)
public class AppEngineTraceServiceTest {

  private final com.google.appengine.api.labs.trace.TraceService mockTraceService =
      mock(com.google.appengine.api.labs.trace.TraceService.class);
  private final com.google.appengine.api.labs.trace.Span mockSpan =
      mock(com.google.appengine.api.labs.trace.Span.class);

  @Test
  public void getInstance() throws Exception {
    TraceService traceService = TraceService.getInstance();

    assertThat(traceService.getClass()).isEqualTo(AppEngineTraceService.class);
  }

  @Test
  public void exampleSpan() {
    when(mockTraceService.startSpan(any(String.class))).thenReturn(mockSpan);

    TraceService traceService = new AppEngineTraceService(mockTraceService);

    try (TraceService.Span span = traceService.open("JDBC.Driver#connect")) {
      span.annotate(Label.DATABASE_URL, Optional.of("jdbc:mockdriver"));
      span.annotate(Label.SQL_TEXT, Optional.<String>absent());
    }

    InOrder inOrder = inOrder(mockTraceService, mockSpan);
    inOrder.verify(mockTraceService).startSpan("JDBC.Driver#connect");
    inOrder.verify(mockSpan).setLabel("trace.cloud.google.com/jdbc/url", "jdbc:mockdriver");
    inOrder.verify(mockSpan).close();
    verifyNoMoreInteractions(mockTraceService, mockSpan); // No annotation for SQL_TEXT created.
  }
}
