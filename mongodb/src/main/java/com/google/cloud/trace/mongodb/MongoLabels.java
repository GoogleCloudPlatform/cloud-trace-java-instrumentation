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

/**
 * Common MongoDB label constants.
 */
public class MongoLabels {
  /**
   * The name of the command (e.g. "create", "find").
   */
  public static final String COMMAND_NAME = "/mongodb/commandName";

  /**
   * The name of the database.
   */
  public static final String DATABASE_NAME = "/mongodb/databaseName";

  /**
   * The MongoDB requestId.
   */
  public static final String REQUEST_ID = "/mongodb/requestId";

  /**
   * The batch size for the command (if applicable).
   */
  public static final String BATCH_SIZE = "/mongodb/batchSize";

  /**
   * The name of the collection in the command (if applicable).
   */
  public static final String COLLECTION_NAME = "/mongodb/collectionName";

  /**
   * The status of the command. "SUCCESS" if the command succeeded, "FAILURE" otherwise.
   */
  public static final String STATUS = "/mongodb/status";

  /**
   * The message from the {@link Throwable} associated with the error (if applicable).
   */
  public static final String ERROR = "/mongodb/error";

}
