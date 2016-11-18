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

import com.google.common.base.Optional;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

/**
 * {@link InvocationHandler} for intercepting calls to the given {@link Connection} and recording
 * latency data for Stackdriver Trace.
 */
final class ConnectionInvocationHandler implements InvocationHandler {

  /** The wrapped JDBC {@link Connection}. */
  private final Connection conn;

  private final TraceOptions traceOptions;

  private final TraceService traceService;

  ConnectionInvocationHandler(
      Connection conn, TraceOptions traceOptions, TraceService traceService) {
    this.conn = checkNotNull(conn);
    this.traceOptions = checkNotNull(traceOptions);
    this.traceService = checkNotNull(traceService);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    checkNotNull(method);

    // Extract the sql text when creating a CallableStatement or a PreparedStatement and then pass
    // it to the StatementInvocationHandler which will annotate the trace span with this sql text.
    Optional<String> sql;
    if ((method.getName().equals("prepareCall") || method.getName().equals("prepareStatement"))
        && method.getParameterTypes().length > 0
        && method.getParameterTypes()[0] == String.class) {
      sql = Optional.fromNullable((String) args[0]);
    } else {
      sql = Optional.absent();
    }

    Object o = method.invoke(conn, args);
    if (o instanceof Statement) {
      Statement stmt = (Statement) o;
      o =
          Proxies.newProxyInstance(
              stmt, new StatementInvocationHandler(stmt, traceOptions, traceService, sql));
    }

    return o;
  }
}
