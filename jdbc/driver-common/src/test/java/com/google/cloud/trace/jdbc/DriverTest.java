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
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.cloud.trace.jdbc.Driver}. */
@RunWith(JUnit4.class)
public class DriverTest {

  @Test
  public void selfRegistration() throws Exception {
    // Without ever explicitly referring to com.google.cloud.trace.jdbc.Driver, it has been
    // loaded and registered via the service provider mechanism.
    assertThat(getDriverNames()).contains("com.google.cloud.trace.jdbc.Driver");
  }

  private static Iterable<String> getDriverNames() {
    return Iterables.transform(
        Collections.list(DriverManager.getDrivers()),
        new Function<Driver, String>() {
          @Override
          public String apply(Driver input) {
            checkNotNull(input);
            return input.getClass().getName();
          }
        });
  }
}
