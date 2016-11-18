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

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import java.util.Properties;

/**
 * {@link TraceOptions} holds all client-configurable settings for <i>Stackdriver Trace for
 * JDBC</i>.
 *
 * <p>Clients can configure trace options on a per-connection basis via JDBC connection properties
 * at creation time of a new JDBC connection. Trace options are immutable for the duration of the
 * connection.
 *
 * <p>The following example shows how to enable unredacted tracing of SQL statements and JDBC
 * database URLs:
 *
 * <pre>
 * Properties info = new Properties();
 * info.setProperty("stackdriver.trace.sql", "full");
 * info.setProperty("stackdriver.trace.url", "full");
 *
 * Connection conn = DriverManager.getConnection(url, info);
 * </pre>
 */
@AutoValue
abstract class TraceOptions {

  abstract boolean enabled();

  abstract Scrubbers.Scrubber urlScrubber();

  abstract Scrubbers.Scrubber sqlScrubber();

  /**
   * Reads trace options from the specified JDBC connection properties.
   *
   * <p>The following property names are supported:
   *
   * <ul>
   *   <li>{@code stackdriver.trace.enabled}: Whether <i>Stackdriver Trace for JDBC</i> is enabled.
   *       Defaults to "true", but note that this only takes effect if the <i>Stackdriver Trace for
   *       JDBC</i> driver is actually triggered.
   *   <li>{@code stackdriver.trace.sql}: Level of detail for tracing SQL statements.
   *   <li>{@code stackdriver.trace.url}: Level of detail for tracing JDBC database URLs.
   * </ul>
   *
   * By default, SQL statements and JDBC database URLs are scrubbed of potentially sensitive
   * information before tracing. Use one of the following property values to override this default
   * behavior:
   *
   * <ul>
   *   <li>{@code none}: Disables tracing.
   *   <li>{@code full}: Enables tracing of the full, verbatim string as passed to the JDBC driver.
   * </ul>
   */
  static TraceOptions of(Properties info) {
    checkNotNull(info);

    return builder()
        .setEnabled(Boolean.valueOf(info.getProperty("stackdriver.trace.enabled", "true")))
        .setSqlScrubber(getScrubber(info, "stackdriver.trace.sql", Scrubbers.SQL_SCRUBBER))
        .setUrlScrubber(getScrubber(info, "stackdriver.trace.url", Scrubbers.URL_SCRUBBER))
        .build();
  }

  private static Scrubbers.Scrubber getScrubber(
      Properties info, String propertyName, Scrubbers.Scrubber defaultScrubber) {
    checkNotNull(info);
    checkNotNull(propertyName);
    checkNotNull(defaultScrubber);

    String traceDetails = info.getProperty(propertyName);
    if ("full".equals(traceDetails)) {
      return Scrubbers.KEEP;
    } else if ("none".equals(traceDetails)) {
      return Scrubbers.DROP;
    } else {
      return defaultScrubber;
    }
  }

  @VisibleForTesting
  static Builder builder() {
    return new AutoValue_TraceOptions.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setEnabled(boolean enabled);

    abstract Builder setSqlScrubber(Scrubbers.Scrubber scrubber);

    abstract Builder setUrlScrubber(Scrubbers.Scrubber scrubber);

    abstract TraceOptions build();
  }
}
