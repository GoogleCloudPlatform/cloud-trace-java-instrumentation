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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;

/** Unit tests for {@link NonRegisteringDriver}. */
@RunWith(JUnit4.class)
public class NonRegisteringDriverTest {

  private final TraceService mockTraceService = mock(TraceService.class);
  private final TraceService.Span mockTraceSpan = mock(TraceService.Span.class);
  private final Driver mockRealDriver = mock(Driver.class);
  private final Connection mockRealConnection = mock(Connection.class);

  private NonRegisteringDriver driver;

  @Before
  public void before() throws Exception {
    when(mockTraceService.open(any(String.class))).thenReturn(mockTraceSpan);
    driver = new NonRegisteringDriver(mockTraceService);
    DriverManager.registerDriver(mockRealDriver);
  }

  @After
  public void after() throws Exception {
    DriverManager.deregisterDriver(mockRealDriver);
  }

  @Test
  public void acceptsURL() throws Exception {
    assertThat(driver.acceptsURL("jdbc:stackdriver:")).isTrue();
    assertThat(
            driver.acceptsURL(
                "jdbc:stackdriver:google:mysql://google.com:stschmidt-cloud-trace:db/test"))
        .isTrue();

    assertThat(driver.acceptsURL("jdbc:google:mysql://google.com:stschmidt-cloud-trace:db/test"))
        .isFalse();
    assertThat(driver.acceptsURL("jdbc:stackdriver")).isFalse();
    assertThat(driver.acceptsURL("")).isFalse();
  }

  @Test
  public void acceptsURL_SQLException_for_null() {
    try {
      driver.acceptsURL(null);
      fail("expected SQLException");
    } catch (SQLException expected) {
    }
  }

  @Test
  public void connect() throws Exception {
    when(mockRealDriver.connect(any(String.class), any(Properties.class)))
        .thenReturn(mockRealConnection);

    Properties properties = new Properties();

    Connection conn =
        driver.connect("jdbc:stackdriver:mockdriver?user=root&password=123456", properties);
    conn.close();

    assertThat(conn).isNotNull();

    InOrder inOrder = inOrder(mockTraceService, mockTraceSpan, mockRealDriver, mockRealConnection);
    inOrder.verify(mockTraceService).open("JDBC.Driver#connect");
    inOrder
        .verify(mockTraceSpan)
        .annotate(Label.DATABASE_URL, Optional.of("jdbc:mockdriver?<...>"));
    inOrder.verify(mockRealDriver).connect("jdbc:mockdriver?user=root&password=123456", properties);
    inOrder.verify(mockTraceSpan).close();
    inOrder.verify(mockRealConnection).close();
    verifyNoMoreInteractions(mockTraceService, mockTraceSpan, mockRealDriver, mockRealConnection);
  }

  @Test
  public void connect_disabled() throws Exception {
    when(mockRealDriver.connect(any(String.class), any(Properties.class)))
        .thenReturn(mockRealConnection);

    Properties properties =
        new Properties() {
          {
            put("stackdriver.trace.enabled", "false");
          }
        };

    Connection conn =
        driver.connect("jdbc:stackdriver:mockdriver?user=root&password=123456", properties);
    conn.close();

    assertThat(conn).isNotNull();

    InOrder inOrder = inOrder(mockTraceService, mockRealDriver, mockRealConnection);
    inOrder.verify(mockRealDriver).connect("jdbc:mockdriver?user=root&password=123456", properties);
    inOrder.verify(mockRealConnection).close();
    verifyNoMoreInteractions(
        mockTraceService, mockRealDriver, mockRealConnection); // No trace span created.
  }

  @Test
  public void connect_url_scrubbed() throws Exception {
    when(mockRealDriver.connect(any(String.class), any(Properties.class)))
        .thenReturn(mockRealConnection);

    Properties properties = new Properties();

    TraceOptions traceOptions =
        TraceOptions.builder()
            .setEnabled(true)
            .setSqlScrubber(Scrubbers.KEEP)
            .setUrlScrubber(
                new Scrubbers.Scrubber() {
                  @Override
                  public Optional<String> apply(String input) {
                    return Optional.of("#scrubbed URL#");
                  }
                })
            .build();

    Connection conn =
        driver.connect(
            "jdbc:stackdriver:mockdriver?user=root&password=123456", properties, traceOptions);
    conn.close();

    assertThat(conn).isNotNull();

    InOrder inOrder = inOrder(mockTraceService, mockTraceSpan, mockRealDriver, mockRealConnection);
    inOrder.verify(mockTraceService).open("JDBC.Driver#connect");
    inOrder.verify(mockTraceSpan).annotate(Label.DATABASE_URL, Optional.of("#scrubbed URL#"));
    inOrder.verify(mockRealDriver).connect("jdbc:mockdriver?user=root&password=123456", properties);
    inOrder.verify(mockTraceSpan).close();
    inOrder.verify(mockRealConnection).close();
    verifyNoMoreInteractions(mockTraceService, mockTraceSpan, mockRealDriver, mockRealConnection);
  }

  @Test
  public void connect_fails() throws Exception {
    when(mockRealDriver.connect(eq("jdbc:mockdriver"), any(Properties.class)))
        .thenThrow(new SQLException());

    Properties info = new Properties();

    try {
      driver.connect("jdbc:stackdriver:mockdriver", info);
      fail("expected SQLException");
    } catch (SQLException expected) {
    }

    InOrder inOrder = inOrder(mockTraceService, mockTraceSpan, mockRealDriver);
    inOrder.verify(mockTraceService).open("JDBC.Driver#connect");
    inOrder.verify(mockTraceSpan).annotate(Label.DATABASE_URL, Optional.of("jdbc:mockdriver"));
    inOrder.verify(mockRealDriver).connect("jdbc:mockdriver", info);
    inOrder.verify(mockTraceSpan).close();
  }

  @Test
  public void getMajorVersion() {
    assertThat(driver.getMajorVersion()).isEqualTo(1);
  }

  @Test
  public void getMinorVersion() {
    assertThat(driver.getMinorVersion()).isEqualTo(0);
  }

  @Test
  public void getPropertyInfo() throws Exception {
    DriverPropertyInfo[] driverPropertyInfos = new DriverPropertyInfo[0];
    when(mockRealDriver.acceptsURL("jdbc:mockdriver")).thenReturn(true);
    when(mockRealDriver.getPropertyInfo(eq("jdbc:mockdriver"), any(Properties.class)))
        .thenReturn(driverPropertyInfos);

    Properties info = new Properties();
    assertThat(driver.getPropertyInfo("jdbc:stackdriver:mockdriver", info))
        .isSameAs(driverPropertyInfos);
    verify(mockRealDriver).getPropertyInfo("jdbc:mockdriver", info);
  }

  @Test
  public void getParentLogger() {
    try {
      driver.getParentLogger();
      fail("expected SQLFeatureNotSupportedException");
    } catch (SQLFeatureNotSupportedException expected) {
    }
  }

  @Test
  public void jdbcCompliant() {
    assertThat(driver.jdbcCompliant()).isFalse();
  }
}
