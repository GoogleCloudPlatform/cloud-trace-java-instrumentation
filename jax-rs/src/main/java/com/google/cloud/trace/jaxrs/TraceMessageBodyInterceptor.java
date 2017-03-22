package com.google.cloud.trace.jaxrs;

import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.TraceContext;
import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

/**
 * Traces time spent in {@link javax.ws.rs.ext.MessageBodyReader#readFrom} and
 * {@link javax.ws.rs.ext.MessageBodyWriter#writeTo}.
 */
public class TraceMessageBodyInterceptor implements ReaderInterceptor, WriterInterceptor {

  private final Tracer tracer;

  public TraceMessageBodyInterceptor() {
    this(Trace.getTracer());
  }

  public TraceMessageBodyInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  public Object aroundReadFrom(ReaderInterceptorContext context)
      throws IOException, WebApplicationException {
    TraceContext traceContext = tracer.startSpan("MessageBodyReader#readFrom");
    Object result;
    try {
      result = context.proceed();
    } finally {
      tracer.endSpan(traceContext);
    }
    return result;
  }

  public void aroundWriteTo(WriterInterceptorContext context)
      throws IOException, WebApplicationException {
    TraceContext traceContext = tracer.startSpan("MessageBodyWriter#writeTo");
    try {
      context.proceed();
    } finally {
      tracer.endSpan(traceContext);
    }
  }
}
