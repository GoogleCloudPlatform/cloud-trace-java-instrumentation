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

import com.google.common.reflect.TypeToken;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/** Methods for creating dynamic proxies. */
final class Proxies {

  private Proxies() {}

  /** Returns all interfaces implemented by the given object. */
  private static Class<?>[] getInterfaces(Object o) {
    checkNotNull(o);

    return TypeToken.of(o.getClass()).getTypes().interfaces().rawTypes().toArray(new Class<?>[0]);
  }

  /**
   * Returns a new dynamic proxy for the specified object and {@link InvocationHandler}.
   *
   * <p>The newly created dynamic proxy implements all interfaces of the given object to allow for
   * downcasting to vendor-specific interfaces.
   */
  @SuppressWarnings("unchecked")
  static <T> T newProxyInstance(T o, InvocationHandler h) {
    checkNotNull(o);
    checkNotNull(h);

    return (T) Proxy.newProxyInstance(o.getClass().getClassLoader(), Proxies.getInterfaces(o), h);
  }
}
