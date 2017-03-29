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

package com.google.cloud.trace.jdbc.example.gce;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.trace.GrpcSpanContextHandler;
import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.SpanContextHandlerTracer;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.jdbc.ThreadLocalTracerStore;
import com.google.cloud.trace.core.ConstantTraceOptionsFactory;
import com.google.cloud.trace.core.JavaTimestampFactory;
import com.google.cloud.trace.core.TimestampFactory;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.grpc.v1.GrpcTraceConsumer;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.sink.TraceSink;
import com.google.cloud.trace.v1.TraceSinkV1;
import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.cloud.trace.v1.producer.TraceProducer;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;

/**
 * {@link Main} demonstrates the usage of the <i>Stackdriver Trace for JDBC</i> driver in a
 * small test client.
 */
public class Main {
  // The JDBC database URL is prefixed with "jdbc:stackdriver:" to trigger the Stackdriver Trace
  // for JDBC driver.
  private static final String JDBC_URL = "jdbc:stackdriver:mysql:///exampledatabase?user=root"
      + "&useSSL=true"
      + "&socketFactory=com.google.cloud.sql.mysql.SocketFactory"
      + "&cloudSqlInstance=google.com:stschmidt-cloud-trace:us-central1:example-instance";

  public static void main(String[] args) throws IOException, SQLException {
    checkNotNull(args);
    checkArgument(
        args.length == 1, "Expected exactly one command line argument, the cloud project ID");

    String projectId = args[0];
    Tracer tracer = TraceGrpcApiService.builder().setProjectId(projectId).build().getTracer();

    // Make the Tracer available to Stackdriver Trace for JDBC.
    // TODO: Replace this with the upcoming mechanism in the Cloud Trace for Java SDK.
    ThreadLocalTracerStore.setCurrent(tracer);

    TraceContext traceContext = tracer.startSpan("example request");

    System.out.println("Hello Stackdriver Trace for JDBC!");
    System.out.println("Your lucky number is " + doStuff());
    System.out.println("Help me find out why this request is so slow!");
    System.out.println(
        "Hint: https://console.cloud.google.com/traces/overview?project=" + projectId);

    tracer.endSpan(traceContext);
  }

  // Not using a connection pool and otherwise doing weird stuff just to make a point.
  private static int doStuff() throws SQLException {
    Properties info = new Properties();
    info.put("stackdriver.trace.sql", "full");
    info.put("stackdriver.trace.url", "full");

    try (Connection conn = DriverManager.getConnection(JDBC_URL, info);
         Statement stmt = conn.createStatement()) {
      stmt.execute("create table if not exists t1 (id int)");
      stmt.execute("insert into t1 values (" + new Random().nextInt() + ")");

      try (ResultSet rs = stmt.executeQuery("select id from t1")) {
        while (rs.next()) {
        }
      }
    }

    try (Connection conn = DriverManager.getConnection(JDBC_URL, info);
         PreparedStatement pStmt = conn.prepareStatement("insert into t1 values (?)")) {
      pStmt.setInt(1, new Random().nextInt());
      pStmt.execute();
    }

    try (Connection conn = DriverManager.getConnection(JDBC_URL, info);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("select count(*) from t1")) {
      rs.next();
      return rs.getInt(1);
    }
  }
}
