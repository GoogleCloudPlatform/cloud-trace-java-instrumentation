// Copyright 2017 Google Inc. All rights reserved.
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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import static org.mockito.Mockito.mock;

/** Unit tests for {@link StatementInvocationHandler}. */
@RunWith(JUnit4.class)
public class StatementInvocationHandlerTest {

  private final TraceService mockTraceService = mock(TraceService.class);
  private final TraceService.Span mockTraceSpan =mock(TraceService.Span.class);
  private final Statement mockRealStatement=mock(Statement.class);
  private final PreparedStatement mockRealPreparedStatement=mock(PreparedStatement.class);
  private final CallableStatement mockRealCallableStatement=mock(CallableStatement.class);
  private final Proxy mockProxy=mock(Proxy.class);

  private static final TraceOptions TRACE_OPTIONS =
      TraceOptions.builder()
          .setEnabled(true)
          .setSqlScrubber(
              new Scrubbers.Scrubber() {
                @Override
                public Optional<String> apply(String input) {
                  return Optional.of("#scrubbed SQL#");
                }
              })
          .setUrlScrubber(Scrubbers.KEEP)
          .build();

  @Before
  public void before() {
    when(mockTraceService.open(any(String.class))).thenReturn(mockTraceSpan);
  }

  @Test
  public void invoke_Statement_close() throws Throwable {
    StatementInvocationHandler statementInvocationHandler =
        new StatementInvocationHandler(
            mockRealStatement, TRACE_OPTIONS, mockTraceService, Optional.<String>absent());

    statementInvocationHandler.invoke(
        mockProxy, Statement.class.getDeclaredMethod("close"), new Object[0]);

    verify(mockRealStatement).close();
    verifyNoMoreInteractions(mockTraceSpan); // No trace span created.
  }

  @Test
  public void invoke_Statement_close_fails() throws Throwable {
    StatementInvocationHandler statementInvocationHandler =
        new StatementInvocationHandler(
            mockRealStatement, TRACE_OPTIONS, mockTraceService, Optional.<String>absent());
    Exception expectedException = new SQLException();
    doThrow(expectedException).when(mockRealStatement).close();

    try {
      statementInvocationHandler.invoke(
              mockProxy, Statement.class.getDeclaredMethod("close"), new Object[0]);
      fail("expected SQLException");
    } catch (Exception actualException) {
      assertThat(actualException).isSameAs(expectedException);
    }

    verify(mockRealStatement).close();
    verifyNoMoreInteractions(mockTraceSpan); // No trace span created.
  }

  @Test
  public void invoke_Statement_execute() throws Throwable {
    StatementInvocationHandler statementInvocationHandler =
        new StatementInvocationHandler(
            mockRealStatement, TRACE_OPTIONS, mockTraceService, Optional.<String>absent());

    statementInvocationHandler.invoke(
        mockProxy,
        Statement.class.getDeclaredMethod("execute", new Class<?>[] {String.class}),
        new Object[] {"insert some sql text"});

    InOrder inOrder = inOrder(mockTraceService, mockTraceSpan, mockRealStatement);
    inOrder.verify(mockTraceService).open("JDBC.Statement#execute");
    inOrder.verify(mockTraceSpan).annotate(Label.SQL_TEXT, Optional.of("#scrubbed SQL#"));
    inOrder.verify(mockRealStatement).execute("insert some sql text");
    inOrder.verify(mockTraceSpan).close();
  }

  @Test
  public void invoke_PreparedStatement_execute() throws Throwable {
    StatementInvocationHandler statementInvocationHandler =
        new StatementInvocationHandler(
            mockRealPreparedStatement,
            TRACE_OPTIONS,
            mockTraceService,
            Optional.of("select some sql text"));
    statementInvocationHandler.invoke(
        mockProxy, PreparedStatement.class.getDeclaredMethod("execute"), new Object[0]);

    InOrder inOrder =
        inOrder(mockTraceService, mockTraceSpan, mockRealPreparedStatement);
    inOrder.verify(mockTraceService).open("JDBC.Statement#execute");
    inOrder.verify(mockTraceSpan).annotate(Label.SQL_TEXT, Optional.of("#scrubbed SQL#"));
    inOrder.verify(mockRealPreparedStatement).execute();
    inOrder.verify(mockTraceSpan).close();
  }

  @Test
  public void invoke_CallableStatement_execute() throws Throwable {
    StatementInvocationHandler statementInvocationHandler =
        new StatementInvocationHandler(
            mockRealCallableStatement,
            TRACE_OPTIONS,
            mockTraceService,
            Optional.of("call myproc()"));
    statementInvocationHandler.invoke(
        mockProxy, PreparedStatement.class.getDeclaredMethod("execute"), new Object[0]);

    InOrder inOrder =
        inOrder(mockTraceService, mockTraceSpan, mockRealCallableStatement);
    inOrder.verify(mockTraceService).open("JDBC.Statement#execute");
    inOrder.verify(mockTraceSpan).annotate(Label.SQL_TEXT, Optional.of("#scrubbed SQL#"));
    inOrder.verify(mockRealCallableStatement).execute();
    inOrder.verify(mockTraceSpan).close();
  }

  @Test
  public void invoke_fails() throws Throwable {
    StatementInvocationHandler statementInvocationHandler =
        new StatementInvocationHandler(
                    mockRealStatement, TRACE_OPTIONS, mockTraceService, Optional.<String>absent());
    Exception expectedException = new SQLException();
    when(mockRealStatement.execute("some sql text")).thenThrow(expectedException);

    try {
      statementInvocationHandler.invoke(
              mockProxy,
              Statement.class.getDeclaredMethod("execute", String.class),
              new Object[]{"some sql text"});
    } catch (Exception actualException) {
      assertThat(actualException).isSameAs(expectedException);
    }

    InOrder inOrder = inOrder(mockTraceService, mockTraceSpan, mockRealStatement);
    inOrder.verify(mockTraceService).open("JDBC.Statement#execute");
    inOrder.verify(mockRealStatement).execute("some sql text");
    inOrder.verify(mockTraceSpan).close();
  }
}
