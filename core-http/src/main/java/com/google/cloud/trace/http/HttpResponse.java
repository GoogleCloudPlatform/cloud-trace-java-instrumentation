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

package com.google.cloud.trace.http;

/**
 * Common interface for HTTP responses.
 */
public interface HttpResponse {

  /**
   * Get a header value.
   * @param name The name of the header.
   * @return The value of the header.
   */
  String getHeader(String name);

  /**
   * Get the status code.
   * @return The status code from the response.
   */
  int getStatus();
}
