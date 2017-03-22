package com.google.cloud.trace.instrumentation.servlet;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Trace;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.SpanContextHandle;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.http.HttpRequest;
import com.google.cloud.trace.http.HttpResponse;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import java.io.IOException;
import java.net.URI;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Records tracing information for Servlet HTTP requests.
 */
public class TraceServletFilter implements Filter {

  private final TraceHttpRequestInterceptor requestInterceptor;
  private final TraceHttpResponseInterceptor responseInterceptor;
  private final SpanContextHandler contextHandler;
  private final SpanContextFactory contextFactory;

  public TraceServletFilter() {
    this(Trace.getSpanContextHandler(), Trace.getSpanContextFactory(),
        new TraceHttpRequestInterceptor(),
        new TraceHttpResponseInterceptor());
  }

  public TraceServletFilter(SpanContextHandler contextHandler, SpanContextFactory contextFactory,
      TraceHttpRequestInterceptor requestInterceptor,
      TraceHttpResponseInterceptor responseInterceptor) {
    this.contextHandler = contextHandler;
    this.contextFactory = contextFactory;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
  }

  public void init(FilterConfig filterConfig) throws ServletException {

  }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String header = httpRequest.getHeader(SpanContextFactory.headerKey());
    SpanContextHandle incomingContext = null;
    if (header != null) {
      incomingContext = contextHandler.attach(contextFactory.fromHeader(header));
    }
    TraceContext traceContext = requestInterceptor.process(new RequestAdapter(httpRequest));
    try {
      filterChain.doFilter(request, response);
    } finally {
      responseInterceptor
          .process(new ResponseAdapter((HttpServletResponse) response), traceContext);
      if (incomingContext != null) {
        incomingContext.detach();
      }
    }
  }

  public void destroy() {

  }

  private static class RequestAdapter implements HttpRequest {

    private final HttpServletRequest request;

    private RequestAdapter(HttpServletRequest request) {
      this.request = request;
    }

    public String getMethod() {
      return request.getMethod();
    }

    public URI getURI() {
      return URI.create(request.getRequestURL().toString());
    }

    public String getHeader(String name) {
      return request.getHeader(name);
    }

    public String getProtocol() {
      return request.getProtocol();
    }
  }

  private static class ResponseAdapter implements HttpResponse {

    private final HttpServletResponse response;

    private ResponseAdapter(HttpServletResponse response) {
      this.response = response;
    }

    public String getHeader(String name) {
      return response.getHeader(name);
    }

    public int getStatus() {
      return response.getStatus();
    }
  }
}
