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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedInts;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/** Collection of methods for scrubbing sensitive data before tracing. */
final class Scrubbers {

  private static final ImmutableList<String> SQL_STATEMENT_KEYWORDS =
      ImmutableList.of("insert", "update", "delete", "select", "call");

  interface Scrubber extends Function<String, Optional<String>> {}

  /**
   * Scrubs potentially sensitive data from a given JDBC database URL.
   *
   * <p>The following parts of a JDBC database URL are currently considered potentially sensitive
   * and are thus scrubbed:
   *
   * <ul>
   *   <li>query parameters, which might contain database user and plain text password, e.g. {@code
   *       jdbc:mysql://localhost/test?user=minty&password=greatsqldb}
   *   <li>user/password as in user/password@host, e.g. {@code
   *       jdbc:oracle:thin:scott/tiger@localhost:1521:orcl}
   * </ul>
   */
  static final Scrubber URL_SCRUBBER =
      new Scrubber() {
        @Override
        public Optional<String> apply(String url) {
          checkNotNull(url);

          // Scrub query parameters.
          url = url.replaceAll("(.*?)\\?.*", "$1?<...>");

          // Scrub user/password as in user/password@host.
          url = url.replaceAll("(.*):.*?/.*?@(.*)", "$1:<...>@$2");

          return Optional.of(url);
        }
      };

  /**
   * Scrubs potentially sensitive data from a given SQL text.
   *
   * <p>Implementation note: As a safe default, only the type of the SQL statement ("insert",
   * "update", "delete", "select", "call") is returned, if it can be determined. The currently
   * implemented algorithm is pretty dumb and errs on the safe side.
   */
  static final Scrubber SQL_SCRUBBER =
      new Scrubber() {
        @Override
        public Optional<String> apply(String sql) {
          checkNotNull(sql);

          final String lowerCasedSql = sql.toLowerCase();

          Collection<Pair<String, Integer>> keywordPositions =
              Collections2.transform(
                  SQL_STATEMENT_KEYWORDS,
                  new Function<String, Pair<String, Integer>>() {
                    @Override
                    public Pair<String, Integer> apply(String input) {
                      checkNotNull(input);
                      return new Pair(input, lowerCasedSql.indexOf(input));
                    }
                  });

          Pair<String, Integer> firstKeyword =
              Collections.min(
                  keywordPositions,
                  new Comparator<Pair<String, Integer>>() {
                    @Override
                    public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                      return UnsignedInts.compare(o1.second, o2.second);
                    }
                  });

          if (firstKeyword.second >= 0) {
            return Optional.of(firstKeyword.first + " <...>");
          } else {
            return Optional.of("<...>");
          }
        }
      };

  /** Keeps everything, scrubs nothing. */
  static final Scrubber KEEP =
      new Scrubber() {
        @Override
        public Optional<String> apply(String input) {
          checkNotNull(input);
          return Optional.of(input);
        }
      };

  /** Scrubs everything, leaves nothing to trace. */
  static final Scrubber DROP =
      new Scrubber() {
        @Override
        public Optional<String> apply(String input) {
          checkNotNull(input);
          return Optional.absent();
        }
      };

  private static class Pair<A, B> {

    public final A first;
    public final B second;

    public Pair(A first, B second) {
      this.first = first;
      this.second = second;
    }
  }
}
