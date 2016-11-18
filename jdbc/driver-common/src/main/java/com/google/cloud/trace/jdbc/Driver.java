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

import com.google.auto.service.AutoService;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A custom JDBC driver which intercepts calls to the actual JDBC driver (e.g. Cloud SQL's) and
 * collects and sends latency data about JDBC calls to Stackdriver Trace for visualization in the
 * Google Cloud Console.
 *
 * <p>This JDBC driver registers itself using the Java Standard Edition <a
 * href="https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Service_Provider">Service
 * Provider</a> mechanism. The JAR file must include the file {@code
 * META-INF/services/java.sql.Driver} containing the name of this class.
 *
 * @see DriverManager
 */
@AutoService(java.sql.Driver.class)
public final class Driver extends NonRegisteringDriver {

  static {
    try {
      DriverManager.registerDriver(new Driver());
    } catch (SQLException e) {
      throw new RuntimeException("Cannot register driver!", e);
    }
  }
}
