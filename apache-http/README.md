# Using Trace with Apache HTTP Client
```java
import com.google.cloud.trace.apachehttp.TraceRequestInterceptor;
import com.google.cloud.trace.apachehttp.TraceResponseInterceptor;

public static void main(String[] args) {
  CloseableHttpClient client = HttpClients.custom()
      .addInterceptorLast(new TraceRequestInterceptor())
      .addInterceptorFirst(new TraceResponseInterceptor()).build();
  HttpGet httpGet = new HttpGet("http://www.example.com/");
  CloseableHttpResponse response = client.execute(httpGet);
  try {
    // Read the response...
  } finally {
    response.close();
  }
}
```

