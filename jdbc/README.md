# Google Cloud Trace for JDBC

[![Build Status][travis-image]][travis-url] [![Maven
Central][maven-image]][maven-url]

*Google Cloud Trace for JDBC* is a custom
[JDBC](http://www.oracle.com/technetwork/java/overview-141217.html) driver which
intercepts calls to the actual JDBC driver (e.g. [Cloud
SQL](https://cloud.google.com/sql/)'s) and collects and sends latency data about
JDBC calls to [Stackdriver Trace](https://cloud.google.com/trace/) for
visualization in the [Google Cloud Console](https://console.cloud.google.com/).

Here's how the trace details look like in the Google Cloud Console for an
example request:

![Screenshot of example trace details in Google Cloud
Console](src/site/resources/images/example-trace-details.png)

## Supported platforms

To use *Google Cloud Trace for JDBC*, you must have a Java application that
you'd like to trace. The application can be on [Google Cloud
Platform](https://cloud.google.com/), on-premise, or another cloud platform.

## Usage

To enable *Google Cloud Trace for JDBC* for your application, perform the
following one-time setup:

1.  Modify your JDBC database URLs to trigger the *Google Cloud Trace for JDBC*
    driver.
2.  Add a compile/runtime dependency on the *Google Cloud Trace for JDBC*
    driver.
3.  On platforms other than the [Google App Engine Java Standard
    Environment](https://cloud.google.com/appengine/docs/java/), and if not done
    so already, initialize the [Google Cloud Trace SDK for
    Java](https://github.com/GoogleCloudPlatform/cloud-trace-java).

### Modifying the JDBC database URL

Modify the JDBC database URL to trigger the *Google Cloud Trace for JDBC* driver
by replacing the `jdbc:` prefix with `jdbc:stackdriver:`.

For example, when running on Google App Engine and connecting to a Cloud SQL
instance, the JDBC database URL is `jdbc:google:mysql://<instance connection
name>`. Change this database URL to `jdbc:stackdriver:google:mysql://<instance
connection name>`.

### Adding the compile/runtime dependency

The *Google Cloud Trace for JDBC* driver comes in two variants:

One variant uses the [Google Cloud Trace SDK for
Java](https://github.com/GoogleCloudPlatform/cloud-trace-java) under the hood
for sending trace details to Stackdriver Trace. This variant works on all Java 8
platforms, but also requires additional setup as described below.

The other variant is specialized for applications deployed in the [Google App
Engine Java Standard
Environment](https://cloud.google.com/appengine/docs/java/), and makes use of
App Engine's native Trace API.

Make sure to choose the correct variant for your platform.

#### Google App Engine Java Standard Environment

To use *Google Cloud Trace for JDBC* driver in applications deployed in the
[Google App Engine Java Standard
Environment](https://cloud.google.com/appengine/docs/java/), add the following
Maven dependency to your application's `pom.xml` file:

```xml
    <!-- Google Cloud Trace for JDBC for App Engine Standard Environment. -->
    <dependency>
      <groupId>com.google.cloud.trace.jdbc</groupId>
      <artifactId>driver-appengine-standard</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```

Also make sure to add the following dependencies for accessing App Engine APIs,
in particular the Trace API:

```xml
    <!-- App Engine APIs. -->
    <dependency>
      <groupId>com.google.appengine</groupId>
      <artifactId>appengine-api-1.0-sdk</artifactId>
      <version>${appengine.target.version}</version>
    </dependency>
    <dependency>
      <groupId>com.google.appengine</groupId>
      <artifactId>appengine-api-labs</artifactId>
      <version>${appengine.target.version}</version>
    </dependency>
```

Please see [example-appengine-standard/](example-appengine-standard/) for a
small, self-contained example.

Congrats, no additional setup is required for this platform! After you've
rebuilt and deployed the application, check the [Stackdriver Trace
UI](https://console.cloud.google.com/traces/overview) for trace details of slow
requests.

#### All other platforms

For all other platforms, add the following Maven dependency to your
application's `pom.xml` file:

```xml
    <dependency>
      <groupId>com.google.cloud.trace.jdbc</groupId>
      <artifactId>driver</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```

Furthermore, some additional setup is required as described below:

First, the application has to initialize the *Google Cloud Trace SDK for Java*
as shown in the [Cloud Trace SDK for Java
samples](https://github.com/GoogleCloudPlatform/cloud-trace-java/tree/master/samples).
Second, the application has to provide a
[Tracer](https://github.com/GoogleCloudPlatform/cloud-trace-java/blob/master/sdk/core/src/main/java/com/google/cloud/trace/Tracer.java)
object for use by the *Stackdriver Trace for JDBC* driver.

Please see [example-computeengine/](example-computeengine/) for a small,
self-contained example.

**Important**: When deploying to Google Compute Engine (or Google Container
Engine) make sure to add the access scope
`https://www.googleapis.com/auth/trace.append` for using the Cloud Trace API
from Compute Engine instances. This scope is not (yet) enabled by default for
the [Compute Engine default service
account](https://cloud.google.com/compute/docs/access/service-accounts#accesscopesiam).

[travis-image]: https://travis-ci.org/GoogleCloudPlatform/cloud-trace-java-instrumentation.svg?branch=master
[travis-url]: https://travis-ci.org/GoogleCloudPlatform/cloud-trace-java-instrumentation
[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.trace/instrumentation/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.trace/instrumentation
