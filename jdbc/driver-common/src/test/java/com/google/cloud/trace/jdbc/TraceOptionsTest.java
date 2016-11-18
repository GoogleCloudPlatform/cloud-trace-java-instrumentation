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

import java.util.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TraceOptions}. */
@RunWith(JUnit4.class)
public class TraceOptionsTest {

  @Test
  public void of_empty_properties() {
    TraceOptions traceOptions = TraceOptions.of(new Properties());

    assertThat(traceOptions.enabled()).isTrue();
    assertThat(traceOptions.sqlScrubber()).isSameAs(Scrubbers.SQL_SCRUBBER);
    assertThat(traceOptions.urlScrubber()).isSameAs(Scrubbers.URL_SCRUBBER);
  }

  @Test
  public void of_disabled() {
    TraceOptions traceOptions =
        TraceOptions.of(
            new Properties() {
              {
                put("stackdriver.trace.enabled", "false");
              }
            });

    assertThat(traceOptions.enabled()).isFalse();
    assertThat(traceOptions.sqlScrubber()).isSameAs(Scrubbers.SQL_SCRUBBER);
    assertThat(traceOptions.urlScrubber()).isSameAs(Scrubbers.URL_SCRUBBER);
  }

  @Test
  public void of_disabled_sql_full_url() {
    TraceOptions traceOptions =
        TraceOptions.of(
            new Properties() {
              {
                put("stackdriver.trace.sql", "none");
                put("stackdriver.trace.url", "full");
              }
            });

    assertThat(traceOptions.enabled()).isTrue();
    assertThat(traceOptions.sqlScrubber()).isSameAs(Scrubbers.DROP);
    assertThat(traceOptions.urlScrubber()).isSameAs(Scrubbers.KEEP);
  }

  @Test
  public void of_full_sql_disabled_url() {
    TraceOptions traceOptions =
        TraceOptions.of(
            new Properties() {
              {
                put("stackdriver.trace.sql", "full");
                put("stackdriver.trace.url", "none");
              }
            });

    assertThat(traceOptions.enabled()).isTrue();
    assertThat(traceOptions.sqlScrubber()).isSameAs(Scrubbers.KEEP);
    assertThat(traceOptions.urlScrubber()).isSameAs(Scrubbers.DROP);
  }

  @Test
  public void of_unknown() {
    TraceOptions traceOptions =
        TraceOptions.of(
            new Properties() {
              {
                put("stackdriver.trace.sql", "unknown");
                put("stackdriver.trace.url", "unknown");
              }
            });

    assertThat(traceOptions.enabled()).isTrue();
    assertThat(traceOptions.sqlScrubber()).isSameAs(Scrubbers.SQL_SCRUBBER);
    assertThat(traceOptions.urlScrubber()).isSameAs(Scrubbers.URL_SCRUBBER);
  }
}
