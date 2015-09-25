package com.danveloper.ratpack.workflow.groovy.internal;

import com.danveloper.ratpack.workflow.groovy.GroovyRatpackWorkflowEmbeddedApp;
import ratpack.server.RatpackServer;
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.http.TestHttpClient;

import java.net.URI;

public class DefaultGroovyRatpackWorkflowEmbeddedApp implements GroovyRatpackWorkflowEmbeddedApp {
  private final EmbeddedApp delegate;

  public DefaultGroovyRatpackWorkflowEmbeddedApp(EmbeddedApp delegate) {
    this.delegate = delegate;
  }

  @Override
  public RatpackServer getServer() {
    return delegate.getServer();
  }

  @Override
  public URI getAddress() {
    return delegate.getAddress();
  }

  @Override
  public TestHttpClient getHttpClient() {
    return delegate.getHttpClient();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
