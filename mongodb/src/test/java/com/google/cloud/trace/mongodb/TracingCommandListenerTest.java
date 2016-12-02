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

package com.google.cloud.trace.mongodb;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.trace.TestTracer;
import com.google.cloud.trace.TestTracer.AnnotateEvent;
import com.google.cloud.trace.TestTracer.EndSpanEvent;
import com.google.cloud.trace.TestTracer.StartSpanEvent;
import com.google.cloud.trace.core.Label;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.Test;

public class TracingCommandListenerTest {

  private static final BsonDocument findCommand = BsonDocument
      .parse("{'find': 'test-collection', 'batchSize': 20}");
  private static final CommandStartedEvent findStartEvent = new CommandStartedEvent(1, null,
      "test-database", "find", findCommand);
  private static final CommandSucceededEvent findSucceededEvent = new CommandSucceededEvent(1, null,
      "find", new BsonDocument(), 150);
  private static final CommandFailedEvent findFailedEvent = new CommandFailedEvent(1, null, "find",
      125, new Throwable("Error!"));

  private static final BsonDocument createCommand = BsonDocument
      .parse("{'create': 'test-collection2'}");
  private static final CommandStartedEvent createStartEvent = new CommandStartedEvent(2, null,
      "test-database", "create", createCommand);

  private TestTracer tracer;
  private TracingCommandListener commandListener;

  @Before
  public void setup() {
    tracer = new TestTracer();
    commandListener = new TracingCommandListener(tracer);
  }

  @Test
  public void testCommandStarted() {
    commandListener.commandStarted(findStartEvent);
    assertThat(tracer.startSpanEvents).hasSize(1);
    StartSpanEvent startSpanEvent = tracer.startSpanEvents.get(0);
    assertThat(startSpanEvent.getName()).isEqualTo("find");
    assertThat(startSpanEvent.getOptions()).isEqualTo(null);
    assertThat(tracer.annotateEvents).hasSize(1);
    AnnotateEvent annotateEvent = tracer.annotateEvents.get(0);
    assertThat(annotateEvent.getLabels().getLabels()).containsAllOf(
        new Label("/mongodb/commandName", "find"),
        new Label("/mongodb/databaseName", "test-database"),
        new Label("/mongodb/collectionName", "test-collection"),
        new Label("/mongodb/requestId", "1"),
        new Label("/mongodb/batchSize", "20")
    );
    assertThat(annotateEvent.getTraceContext()).isEqualTo(startSpanEvent.getTraceContext());
  }

  @Test
  public void testCommandSucceeded() {
    commandListener.commandStarted(findStartEvent);
    StartSpanEvent startEvent = tracer.startSpanEvents.get(0);
    tracer.reset();
    commandListener.commandSucceeded(findSucceededEvent);
    assertThat(tracer.endSpanEvents).hasSize(1);
    EndSpanEvent endSpanEvent = tracer.endSpanEvents.get(0);
    assertThat(endSpanEvent.getTraceContext()).isEqualTo(startEvent.getTraceContext());
    assertThat(endSpanEvent.getEndSpanOptions()).isNull();
    assertThat(tracer.annotateEvents).hasSize(1);
    AnnotateEvent annotateEvent = tracer.annotateEvents.get(0);
    assertThat(annotateEvent.getTraceContext()).isEqualTo(endSpanEvent.getTraceContext());
    assertThat(annotateEvent.getLabels().getLabels()).contains(
        new Label("/mongodb/status", "SUCCESS")
    );
  }

  @Test
  public void testCommandFailed() {
    commandListener.commandStarted(findStartEvent);
    StartSpanEvent startEvent = tracer.startSpanEvents.get(0);
    tracer.reset();
    commandListener.commandFailed(findFailedEvent);
    assertThat(tracer.endSpanEvents).hasSize(1);
    EndSpanEvent endSpanEvent = tracer.endSpanEvents.get(0);
    assertThat(endSpanEvent.getTraceContext()).isEqualTo(startEvent.getTraceContext());
    assertThat(endSpanEvent.getEndSpanOptions()).isNull();
    assertThat(tracer.annotateEvents).hasSize(1);
    AnnotateEvent annotateEvent = tracer.annotateEvents.get(0);
    assertThat(annotateEvent.getTraceContext()).isEqualTo(endSpanEvent.getTraceContext());
    assertThat(annotateEvent.getLabels().getLabels()).containsAllOf(
        new Label("/mongodb/status", "FAILURE"),
        new Label("/mongodb/error", "Error!")
    );
  }

  @Test
  public void testCommandSucceeded_MismatchedRequests() {
    commandListener.commandStarted(createStartEvent);
    commandListener.commandSucceeded(findSucceededEvent);
    assertThat(tracer.endSpanEvents).isEmpty();
  }

  @Test
  public void testCommandFailed_MismatchedRequests() {
    commandListener.commandStarted(createStartEvent);
    commandListener.commandFailed(findFailedEvent);
    assertThat(tracer.endSpanEvents).isEmpty();
  }
}
