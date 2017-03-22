package com.google.cloud.trace.jaxrs;

import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.http.HttpRequest;
import com.google.cloud.trace.http.HttpResponse;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import java.io.IOException;
import java.net.URI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * Traces HTTP requests sent to a JAX-RS container.
 */
public class TraceContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final TraceHttpRequestInterceptor requestInterceptor;
  private final TraceHttpResponseInterceptor responseInterceptor;

  private static final String TRACE_CONTEXT_PROPERTY = "TRACE-CONTEXT";

  public TraceContainerFilter() {
    this(new TraceHttpRequestInterceptor(), new TraceHttpResponseInterceptor());
  }

  public TraceContainerFilter(TraceHttpRequestInterceptor requestInterceptor,
      TraceHttpResponseInterceptor responseInterceptor) {
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
  }

  public void filter(ContainerRequestContext requestContext) throws IOException {
    TraceContext traceContext = requestInterceptor.process(new RequestAdapter(requestContext));
    requestContext.setProperty(TRACE_CONTEXT_PROPERTY, traceContext);
  }

  public void filter(ContainerRequestContext requestContext,
      ContainerResponseContext responseContext)
      throws IOException {
    TraceContext traceContext = (TraceContext) requestContext.getProperty(TRACE_CONTEXT_PROPERTY);
    responseInterceptor.process(new ResponseAdapter(responseContext), traceContext);
  }

  private static class RequestAdapter implements HttpRequest {

    private final ContainerRequestContext request;

    private RequestAdapter(ContainerRequestContext request) {
      this.request = request;
    }

    public String getMethod() {
      return request.getMethod();
    }

    public URI getURI() {
      return request.getUriInfo().getRequestUri();
    }

    public String getHeader(String name) {
      return request.getHeaderString(name);
    }

    public String getProtocol() {
      // Not provided so this will be ignored by the interceptor.
      return null;
    }
  }

  private static class ResponseAdapter implements HttpResponse {

    private final ContainerResponseContext response;

    private ResponseAdapter(ContainerResponseContext response) {
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
