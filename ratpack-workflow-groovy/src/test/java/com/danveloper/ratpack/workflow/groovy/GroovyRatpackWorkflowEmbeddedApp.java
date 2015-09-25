package com.danveloper.ratpack.workflow.groovy;

import com.danveloper.ratpack.workflow.groovy.internal.DefaultGroovyRatpackWorkflowEmbeddedApp;
import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow;
import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflowServerSpec;
import com.danveloper.ratpack.workflow.server.RatpackWorkflow;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.http.TestHttpClient;

public interface GroovyRatpackWorkflowEmbeddedApp extends EmbeddedApp {
  static GroovyRatpackWorkflowEmbeddedApp from(EmbeddedApp embeddedApp) {
    return embeddedApp instanceof GroovyRatpackWorkflowEmbeddedApp ? (GroovyRatpackWorkflowEmbeddedApp) embeddedApp : new DefaultGroovyRatpackWorkflowEmbeddedApp(embeddedApp);
  }

  static GroovyRatpackWorkflowEmbeddedApp of(@DelegatesTo(value = GroovyRatpackWorkflowServerSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> definition) throws Exception {
    return from(EmbeddedApp.fromServer(GroovyRatpackWorkflow.of(definition)));
  }

  static GroovyRatpackWorkflowEmbeddedApp fromServer(ServerConfigBuilder serverConfig, @DelegatesTo(value = GroovyRatpackWorkflowServerSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> definition) {
    return from(EmbeddedApp.fromServer(serverConfig.build(), s -> ClosureUtil.configureDelegateFirst(GroovyRatpackWorkflowServerSpec.from(s), definition)));
  }

  static GroovyRatpackWorkflowEmbeddedApp fromServer(ServerConfig serverConfig, @DelegatesTo(value = GroovyRatpackWorkflowServerSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> definition) {
    return from(EmbeddedApp.fromServer(serverConfig, s -> ClosureUtil.configureDelegateFirst(GroovyRatpackWorkflowServerSpec.from(s), definition)));
  }

  static GroovyRatpackWorkflowEmbeddedApp fromHandler(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return from(EmbeddedApp.fromHandler(Groovy.groovyHandler(handler)));
  }

  static GroovyRatpackWorkflowEmbeddedApp fromHandlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return from(EmbeddedApp.fromHandlers(Groovy.chainAction(handlers)));
  }

  default void test(@DelegatesTo(value = TestHttpClient.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test) throws Exception {
    test(ClosureUtil.delegatingAction(TestHttpClient.class, test));
  }
}
