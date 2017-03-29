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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.mockito.Mockito.doThrow;

/** Unit tests for {@link ConnectionInvocationHandler}. */
@RunWith(JUnit4.class)
public class ConnectionInvocationHandlerTest {

  private final TraceService mockTraceService = mock(TraceService.class);
  private final TraceService.Span mockTraceSpan = mock(TraceService.Span.class);
  private final Connection mockRealConnection = mock(Connection.class);
  private final Statement mockRealStatement = mock(Statement.class);
  private final Proxy mockProxy = mock(Proxy.class);

  private ConnectionInvocationHandler connectionInvocationHandler;

  @Before
  public void before() {
    when(mockTraceService.open(any(String.class))).thenReturn(mockTraceSpan);
    connectionInvocationHandler =
        new ConnectionInvocationHandler(
            mockRealConnection, TraceOptions.of(new Properties()), mockTraceService);
  }

  @Test
  public void invoke_close() throws Throwable {
    connectionInvocationHandler.invoke(
        mockProxy, Connection.class.getDeclaredMethod("close"), new Object[0]);

    verify(mockRealConnection).close();
  }

  @Test
  public void invoke_commit_fails() throws Throwable {
    Exception expectedException = new SQLException();
    doThrow(expectedException).when(mockRealConnection).commit();

    try {
      connectionInvocationHandler.invoke(
              mockProxy, Connection.class.getDeclaredMethod("commit"), new Object[0]);
      fail("expected SQLException");
    } catch (Exception actualException) {
      assertThat(actualException).isSameAs(expectedException);
    }

    verify(mockRealConnection).commit();
  }

  @Test
  public void invoke_createStatement() throws Throwable {
    when(mockRealConnection.createStatement()).thenReturn(mockRealStatement);

    Object o =
        connectionInvocationHandler.invoke(
            mockProxy, Connection.class.getDeclaredMethod("createStatement"), new Object[0]);

    verify(mockRealConnection).createStatement();
    assertThat(o instanceof Statement).isTrue();

    ((Statement) o).execute("some sql text");
    verify(mockRealStatement).execute("some sql text");
  }
}
