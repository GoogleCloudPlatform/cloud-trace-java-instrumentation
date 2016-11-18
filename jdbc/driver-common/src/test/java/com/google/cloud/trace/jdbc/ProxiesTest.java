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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link Proxies}. */
@RunWith(JUnit4.class)
public class ProxiesTest {

  private final VendorSpecificPreparedStatement mockStatement =
      mock(VendorSpecificPreparedStatement.class);

  private static interface VendorSpecificPreparedStatement extends PreparedStatement {

    void vendorSpecificMethod();
  }

  @Test
  public void newProxyInstance() throws Exception {
    InvocationHandler mockInvocationHandler =
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            assertThat(method.getName()).isEqualTo("vendorSpecificMethod");
            return method.invoke(mockStatement, args);
          }
        };

    Object proxy = Proxies.newProxyInstance(mockStatement, mockInvocationHandler);

    assertThat(proxy instanceof VendorSpecificPreparedStatement).isTrue();
    ((VendorSpecificPreparedStatement) proxy).vendorSpecificMethod();
    verify(mockStatement).vendorSpecificMethod();
  }
}
