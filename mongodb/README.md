# Using Trace with MongoDB

To trace requests to MongoDB, use the `TracingCommandListener`.

```java
MongoClientOptions options = MongoClientOptions.builder()
    .addCommandListener(new TracingCommandListener())
    .build();
MongoClient client = new MongoClient("hostname", options);
```

Note: The `TracingCommandListener` works with the standard MongoClient but is currently unsupported with the async MongoDB client.

## Labels
The following labels are recorded by the `TracingCommandListener`

| Label key                | Label value                                                                             |
|--------------------------|-----------------------------------------------------------------------------------------|
|`/mongodb/databaseName`   | The name of the database.                                                               |
|`/mongodb/command`        | The name of the command (e.g. "create", "find").                                        |
|`/mongodb/requestId`      | The MongoDB requestId.                                                                  |
|`/mongodb/batchSize`      | The batch size for the command (if applicable).                                         |
|`/mongodb/collectionName` | The name of the collection in the command (if applicable).                              |
|`/mongodb/status`         | The status of the command. `"SUCCESS"` if the command succeeded, `"FAILURE"` otherwise. |
|`/mongodb/error`          | The message from the `Throwable` associated with the error (if applicable).             |

