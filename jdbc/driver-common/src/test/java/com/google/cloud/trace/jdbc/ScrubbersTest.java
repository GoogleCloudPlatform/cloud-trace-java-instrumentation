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

import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link Scrubbers}. */
@RunWith(JUnit4.class)
public class ScrubbersTest {

  @Test
  public void scrubUrl() {
    assertThat(
            Scrubbers.URL_SCRUBBER.apply(
                "jdbc:mysql://localhost/test?user=minty&password=greatsqldb"))
        .isEqualTo(Optional.of("jdbc:mysql://localhost/test?<...>"));
    assertThat(Scrubbers.URL_SCRUBBER.apply("jdbc:oracle:thin:scott/tiger@localhost:1521:orcl"))
        .isEqualTo(Optional.of("jdbc:oracle:thin:<...>@localhost:1521:orcl"));
  }

  @Test
  public void scrubSql() {
    assertThat(Scrubbers.SQL_SCRUBBER.apply("some unknown sql text"))
        .isEqualTo(Optional.of("<...>"));
    assertThat(Scrubbers.SQL_SCRUBBER.apply("insert into table t1 values (?)"))
        .isEqualTo(Optional.of("insert <...>"));
    assertThat(Scrubbers.SQL_SCRUBBER.apply("select * from t1"))
        .isEqualTo(Optional.of("select <...>"));
    assertThat(Scrubbers.SQL_SCRUBBER.apply("update t1 set bobby = ?"))
        .isEqualTo(Optional.of("update <...>"));
    assertThat(Scrubbers.SQL_SCRUBBER.apply("deLETE from t1 where bobby = 1"))
        .isEqualTo(Optional.of("delete <...>"));
    assertThat(Scrubbers.SQL_SCRUBBER.apply("? = call myproc(?)"))
        .isEqualTo(Optional.of("call <...>"));
    assertThat(Scrubbers.SQL_SCRUBBER.apply("/* actually not an insert */ select 1"))
        .isEqualTo(Optional.of("insert <...>"));
  }

  @Test
  public void drop() {
    assertThat(Scrubbers.DROP.apply("something")).isEqualTo(Optional.absent());
  }

  @Test
  public void keep() {
    assertThat(Scrubbers.KEEP.apply("something")).isEqualTo(Optional.of("something"));
  }
}
