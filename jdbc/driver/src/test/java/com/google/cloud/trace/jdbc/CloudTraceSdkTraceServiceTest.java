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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Labels;
import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Unit tests for {@link CloudTraceSdkTraceService}. */
@RunWith(JUnit4.class)
public class CloudTraceSdkTraceServiceTest {
  private final Tracers mockTracers = mock(Tracers.class);
  private final Tracer mockTracer = mock(Tracer.class);

  @Test
  public void getInstance() throws Exception {
    TraceService traceService = TraceService.getInstance();

    assertThat(traceService.getClass()).isEqualTo(CloudTraceSdkTraceService.class);
  }

  @Test
  public void exampleSpan() {
    when(mockTracers.getCurrent()).thenReturn(mockTracer);
    TraceService traceService = new CloudTraceSdkTraceService(mockTracers);

    try (TraceService.Span span = traceService.open("JDBC.Driver#connect")) {
      span.annotate(Label.DATABASE_URL, Optional.of("jdbc:mockdriver"));
      span.annotate(Label.SQL_TEXT, Optional.absent());
    }

    ArgumentCaptor<Labels> labels = ArgumentCaptor.forClass(Labels.class);

    InOrder inOrder = inOrder(mockTracer);
    inOrder.verify(mockTracer).startSpan("JDBC.Driver#connect");
    inOrder.verify(mockTracer).annotateSpan(eq(null), labels.capture());
    inOrder.verify(mockTracer).endSpan(null);
    verifyNoMoreInteractions(mockTracer); // No annotation for SQL_TEXT created.

    assertThat(labels.getValue().getLabels())
        .containsExactly(new com.google.cloud.trace.core.Label(
            "g.co/jdbc/url", "jdbc:mockdriver"));
  }
}
