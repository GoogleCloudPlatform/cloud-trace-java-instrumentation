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

package com.google.cloud.trace.jdbc.example.gae;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link TestServlet} demonstrates the usage of the <i>Stackdriver Trace for JDBC</i> driver in a
 * small test servlet.
 */
public class TestServlet extends HttpServlet {

  // JDBC database URL is prefixed with "jdbc:stackdriver:" to trigger the Stackdriver Trace for
  // JDBC driver.
  private static final String JDBC_URL
          = "jdbc:stackdriver:google:mysql://"
          + "google.com:stschmidt-cloud-trace:us-central1:example-instance/exampledatabase"
          + "?user=root";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");

    PrintWriter pw = response.getWriter();
    pw.println("<h1>Hello Stackdriver Trace for JDBC!</h1>");

    try {
      pw.println("Your lucky number is " + doStuff());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    pw.println("<h2>Help me find out why this request is so slow!</h2>");
    pw.println(
            "<a href=https://console.cloud.google.com/traces/overview"
            + "?project=google.com:stschmidt-cloud-trace>Hint</a>");
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
