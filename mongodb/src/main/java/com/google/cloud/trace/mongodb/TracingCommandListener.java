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

import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.TraceContext;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import java.util.HashMap;
import java.util.Map;
import org.bson.BsonDocument;

/**
 * Traces each commands sent that is sent to the MongoDB server. It records start and end
 * timestamps, the request id, the database name, the collection name, the request id, the command's
 * batch size, and result status.
 */
public class TracingCommandListener implements CommandListener {

  private final Tracer tracer;

  /**
   * A mapping from command name to the key in the document that contains the collection name.
   * The key can be different for each command. For many commands, the key is the name of the
   * command, but that isn't always the case (such as with the "group" and "getMore" commands).
   *
   * See the MongoDB command documentation for more details.
   */
  private static final Map<String, String> collectionKeyByCommand = new HashMap<String, String>();
  static {
    collectionKeyByCommand.put("create", "create");
    collectionKeyByCommand.put("count", "count");
    collectionKeyByCommand.put("distinct", "distinct");
    collectionKeyByCommand.put("group", "ns");
    collectionKeyByCommand.put("geoNear", "geoNear");
    collectionKeyByCommand.put("find", "find");
    collectionKeyByCommand.put("insert", "insert");
    collectionKeyByCommand.put("update", "update");
    collectionKeyByCommand.put("delete", "delete");
    collectionKeyByCommand.put("findAndModify", "findAndModify");
    collectionKeyByCommand.put("getMore", "collection");
  }

  /**
   * Because no state is passed between calls to commandStarted and commandSucceeded/commandFailed,
   * we need to keep track of the TraceContext for each thread. This will work for the normal
   * MongoDB client, but may cause issues when using the Async client. We can compare the requestId
   * to determine if a mismatch occurs to prevent recording incorrect endSpan events.
   */
  private static final ThreadLocal<MongoDBCommandTraceContext> contexts =
      new ThreadLocal<MongoDBCommandTraceContext>();

  public TracingCommandListener() {
    this(Trace.getTracer());
  }

  public TracingCommandListener(Tracer tracer) {
    this.tracer = tracer;
  }

  public void commandStarted(CommandStartedEvent event) {
    BsonDocument document = event.getCommand();
    Labels.Builder labels = Labels.builder();
    String commandName = event.getCommandName();
    labels.add(MongoLabels.COMMAND_NAME, commandName);
    String databaseName = event.getDatabaseName();
    labels.add(MongoLabels.DATABASE_NAME, databaseName);
    labels.add(MongoLabels.REQUEST_ID, Integer.toString(event.getRequestId()));
    if (document.containsKey("batchSize")) {
      int batchSize = document.getInt32("batchSize").getValue();
      labels.add(MongoLabels.BATCH_SIZE, Integer.toString(batchSize));
    }
    String collectionKey = collectionKeyByCommand.get(commandName);
    if (collectionKey != null && document.containsKey(collectionKey)) {
      String collectionName = document.getString(collectionKey).getValue();
      labels.add(MongoLabels.COLLECTION_NAME, collectionName);
    }

    TraceContext context = tracer.startSpan(commandName);
    tracer.annotateSpan(context, labels.build());
    contexts.set(new MongoDBCommandTraceContext(context, event.getRequestId()));
  }

  public void commandSucceeded(CommandSucceededEvent event) {
    MongoDBCommandTraceContext commandContext = contexts.get();
    if (commandContext == null || commandContext.getRequestId() != event.getRequestId()) {
      contexts.remove();
      return;
    }
    Labels.Builder labels = Labels.builder();
    labels.add(MongoLabels.STATUS, "SUCCESS");
    tracer.annotateSpan(commandContext.getContext(), labels.build());
    tracer.endSpan(commandContext.getContext());
    contexts.remove();
  }

  public void commandFailed(CommandFailedEvent event) {
    MongoDBCommandTraceContext commandContext = contexts.get();
    if (commandContext == null || commandContext.getRequestId() != event.getRequestId()) {
      // The context doesn't match the requestId for the event so don't make any endSpan() or
      // annotate() calls. Drop the context since it likely can't be used again.
      contexts.remove();
      return;
    }
    Labels.Builder labels = Labels.builder();
    labels.add(MongoLabels.STATUS, "FAILURE");
    labels.add(MongoLabels.ERROR, event.getThrowable().getMessage());
    tracer.annotateSpan(commandContext.getContext(), labels.build());
    tracer.endSpan(commandContext.getContext());
    contexts.remove();
  }

  /**
   * Wrapper for the TraceContext to associate it with a specific MongoDB request id.
   */
  private static class MongoDBCommandTraceContext {

    private final TraceContext context;
    private final int requestId;

    MongoDBCommandTraceContext(TraceContext context, int requestId) {
      this.context = context;
      this.requestId = requestId;
    }

    TraceContext getContext() {
      return context;
    }

    int getRequestId() {
      return requestId;
    }
  }
}
