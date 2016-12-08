package com.google.cloud.trace.jaxrs;

import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.http.HttpRequest;
import com.google.cloud.trace.http.HttpResponse;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import java.io.IOException;
import java.net.URI;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

/**
 * Traces JAX-RS client HTTP requests.
 */
public class TraceClientFilter implements ClientRequestFilter, ClientResponseFilter {

  private final TraceHttpRequestInterceptor requestInterceptor;
  private final TraceHttpResponseInterceptor responseInterceptor;

  private static final String TRACE_CONTEXT_PROPERTY = "TRACE-CONTEXT";

  public TraceClientFilter() {
    this(new TraceHttpRequestInterceptor(), new TraceHttpResponseInterceptor());
  }

  public TraceClientFilter(TraceHttpRequestInterceptor requestInterceptor,
      TraceHttpResponseInterceptor responseInterceptor) {
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
  }

  public void filter(ClientRequestContext requestContext) throws IOException {
    TraceContext traceContext = requestInterceptor.process(new RequestAdapter(requestContext));
    requestContext.getHeaders().add(SpanContextFactory.headerKey(),
        SpanContextFactory.toHeader(traceContext.getHandle().getCurrentSpanContext()));
    requestContext.setProperty(TRACE_CONTEXT_PROPERTY, traceContext);
  }

  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    TraceContext traceContext = (TraceContext) requestContext.getProperty(TRACE_CONTEXT_PROPERTY);
    responseInterceptor.process(new ResponseAdapter(responseContext), traceContext);
  }

  private static class RequestAdapter implements HttpRequest {

    private final ClientRequestContext request;

    private RequestAdapter(ClientRequestContext request) {
      this.request = request;
    }

    public String getMethod() {
      return request.getMethod();
    }

    public URI getURI() {
      return request.getUri();
    }

    public String getHeader(String name) {
      return request.getHeaderString(name);
    }

    public String getProtocol() {
      // Not provided, so this will be ignored by the interceptor.
      return null;
    }
  }

  private static class ResponseAdapter implements HttpResponse {

    private final ClientResponseContext response;

    private ResponseAdapter(ClientResponseContext response) {
      this.response = response;
    }

    public String getHeader(String name) {
      return response.getHeaderString(name);
    }

    public int getStatus() {
      return response.getStatus();
    }
  }
}
