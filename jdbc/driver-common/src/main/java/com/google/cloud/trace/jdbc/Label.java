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

/**
 * Enumeration of the label keys for span annotations that are set by <i>Stackdriver Trace for
 * JDBC</i>.
 */
enum Label {
  /**
   * Span annotations labeled with {@link #DATABASE_URL} (key {@value #DATABASE_URL}) contain the
   * JDBC database URL that is used for connecting to the database.
   */
  DATABASE_URL("g.co/jdbc/url"),

  /**
   * Span annotations labeled with {@link #SQL_TEXT} (key {@value #SQL_TEXT}) contain the SQL
   * statement that is sent to the database for execution.
   */
  SQL_TEXT("g.co/jdbc/sql"),

  /**
   * Span annotations labeled with {@link #ERROR_NAME} (key {@value #ERROR_NAME}) contain the error
   * name.
   */
  ERROR_NAME("/error/name"),

  /**
   * Span annotations labeled with {@link #ERROR_MESSAGE} (key {@value #ERROR_MESSAGE}) contain the
   * error message.
   */
  ERROR_MESSAGE("/error/message");

  private final String key;

  private Label(String key) {
    this.key = checkNotNull(key);
  }

  String key() {
    return key;
  }
}
