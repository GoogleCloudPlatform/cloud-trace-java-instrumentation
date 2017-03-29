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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;

/**
 * {@link InvocationHandler} for intercepting calls to the given {@link Statement} and recording
 * latency data for Stackdriver Trace.
 */
final class StatementInvocationHandler implements InvocationHandler {

  /** The wrapped JDBC {@link Statement}. */
  private final Statement statement;

  private final TraceOptions traceOptions;

  private final TraceService traceService;

  private final Optional<String> sql;

  StatementInvocationHandler(
      Statement statement,
      TraceOptions traceOptions,
      TraceService traceService,
      Optional<String> sql) {
    this.statement = checkNotNull(statement);
    this.traceOptions = checkNotNull(traceOptions);
    this.traceService = checkNotNull(traceService);
    this.sql = checkNotNull(sql);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    checkNotNull(method);

    if (!method.getName().startsWith("execute")) {
      // Intentionally not tracing this call as no statement is executed.
      try {
        return method.invoke(statement, args);
      } catch (InvocationTargetException e) {
        // Rethrow the exception from the underlying method.
        throw e.getCause();
      }
    }

    Optional<String> sql;
    if (method.getParameterTypes().length > 0 && method.getParameterTypes()[0] == String.class) {
      sql = Optional.fromNullable((String) args[0]);
    } else if (this.sql.isPresent()) {
      sql = this.sql;
    } else {
      sql = Optional.absent();
    }

    try (TraceService.Span span = traceService.open("JDBC.Statement#" + method.getName())) {
      if (sql.isPresent()) {
        span.annotate(Label.SQL_TEXT, traceOptions.sqlScrubber().apply(sql.get()));
      }

      try {
        return method.invoke(statement, args);
      } catch (InvocationTargetException e) {
        // Rethrow the exception from the underlying method.
        throw e.getCause();
      }
    }
  }
}
