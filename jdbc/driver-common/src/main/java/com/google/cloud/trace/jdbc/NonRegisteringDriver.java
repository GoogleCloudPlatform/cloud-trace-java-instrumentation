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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * A custom JDBC driver which intercepts calls to the actual JDBC driver (e.g. Cloud SQL's) and
 * collects and sends latency data about JDBC calls to Stackdriver Trace for visualization in the
 * Google Cloud Console.
 *
 * <p>Implementation details: This JDBC driver intercepts {@link #connect} for gathering latency
 * data about the actual method call. The returned {@link Connection} object is wrapped in a dynamic
 * proxy for further instrumentation by means of a custom {@link
 * java.lang.reflect.InvocationHandler}, {@link ConnectionInvocationHandler}.
 *
 * <p>To facilitate easier testing, this driver does not register itself with {@link
 * java.sql.DriverManager}.
 */
class NonRegisteringDriver implements Driver {

  /** Prefix identifying connections through this JDBC driver. */
  private static final String URL_PREFIX = "jdbc:stackdriver:";

  /** Major and Minor version of the new driver. */
  private static final int MAJOR_VERSION = 1;

  private static final int MINOR_VERSION = 0;

  private final TraceService traceService;

  public NonRegisteringDriver() {
    this(TraceService.getInstance());
  }

  @VisibleForTesting
  NonRegisteringDriver(TraceService traceService) {
    this.traceService = checkNotNull(traceService);
  }

  @Override
  public boolean acceptsURL(@Nullable String url) throws SQLException {
    checkNotNullUrl(url);

    return url.startsWith(URL_PREFIX);
  }

  @Override
  public Connection connect(@Nullable String url, @Nullable Properties info) throws SQLException {
    return connect(url, info, TraceOptions.of(info));
  }

  @VisibleForTesting
  Connection connect(String url, Properties info, TraceOptions traceOptions) throws SQLException {
    checkNotNull(traceOptions);

    if (!acceptsURL(url)) {
      return null;
    }

    String realUrl = getRealUrl(url);

    if (traceOptions.enabled()) {
      try (TraceService.Span span = traceService.open("JDBC.Driver#connect")) {
        span.annotate(Label.DATABASE_URL, traceOptions.urlScrubber().apply(realUrl));
        Connection conn = DriverManager.getConnection(realUrl, info);
        return Proxies.newProxyInstance(
            conn, new ConnectionInvocationHandler(conn, traceOptions, traceService));
      }
    } else {
      return DriverManager.getConnection(realUrl, info);
    }
  }

  @Override
  public int getMajorVersion() {
    return MAJOR_VERSION;
  }

  @Override
  public int getMinorVersion() {
    return MINOR_VERSION;
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(@Nullable String url, @Nullable Properties info)
      throws SQLException {
    checkNotNullUrl(url);

    return DriverManager.getDriver(getRealUrl(url)).getPropertyInfo(getRealUrl(url), info);
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  private static String getRealUrl(String url) {
    checkNotNull(url);

    return "jdbc:" + url.substring(URL_PREFIX.length());
  }

  private static void checkNotNullUrl(String url) throws SQLException {
    if (url == null) {
      throw new SQLException("url is null");
    }
  }
}
