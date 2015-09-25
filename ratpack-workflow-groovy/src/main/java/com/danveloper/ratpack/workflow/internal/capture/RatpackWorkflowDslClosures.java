package com.danveloper.ratpack.workflow.internal.capture;

import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow;
import groovy.lang.Closure;
import ratpack.func.Block;
import ratpack.func.Function;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.internal.capture.RatpackScriptBacking;

public class RatpackWorkflowDslClosures {

  private Closure<?> workflows;
  private Closure<?> handlers;
  private Closure<?> bindings;
  private Closure<?> serverConfig;

  public Closure<?> getHandlers() {
    return handlers;
  }

  public Closure<?> getBindings() {
    return bindings;
  }

  public Closure<?> getServerConfig() {
    return serverConfig;
  }

  public Closure<?> getWorkflows() {
    return workflows;
  }

  public void setHandlers(Closure<?> handlers) {
    this.handlers = handlers;
  }

  public void setBindings(Closure<?> bindings) {
    this.bindings = bindings;
  }

  public void setServerConfig(Closure<?> serverConfig) {
    this.serverConfig = serverConfig;
  }

  public void setWorkflows(Closure<?> workflows) {
    this.workflows = workflows;
  }

  public static RatpackWorkflowDslClosures capture(Function<? super RatpackWorkflowDslClosures, ? extends GroovyRatpackWorkflow.Ratpack> function, Block action) throws Exception {
    RatpackWorkflowDslClosures closures = new RatpackWorkflowDslClosures();
    GroovyRatpackWorkflow.Ratpack receiver = function.apply(closures);
    RatpackScriptBacking.withBacking(closure -> ClosureUtil.configureDelegateFirst(receiver, closure), action);
    return closures;
  }
}
