package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.GroovyWorkChain;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.handling.Handlers;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfigBuilder;

public class GroovyRatpackWorkflowServerSpec extends RatpackWorkflowServerSpec {
  public GroovyRatpackWorkflowServerSpec(RatpackServerSpec delegate) {
    super(delegate);
  }

  public static GroovyRatpackWorkflowServerSpec from(RatpackServerSpec spec) {
    return spec instanceof GroovyRatpackWorkflowServerSpec ? (GroovyRatpackWorkflowServerSpec) spec : new GroovyRatpackWorkflowServerSpec(spec);
  }

  public GroovyRatpackWorkflowServerSpec workflow(@DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> action) throws Exception {
    super.workflow(wc -> {
      action.setDelegate(wc);
      action.setResolveStrategy(Closure.DELEGATE_FIRST);
      action.call();
    });
    return this;
  }

  public GroovyRatpackWorkflowServerSpec handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return (GroovyRatpackWorkflowServerSpec)handler(r -> Handlers.chain(r, Groovy.chainAction(handlers)));
  }

  public GroovyRatpackWorkflowServerSpec serverConfig(@DelegatesTo(value = ServerConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> action) throws Exception {
    return from(GroovyRatpackWorkflowServerSpec.super.serverConfig(ClosureUtil.delegatingAction(ServerConfigBuilder.class, action)));
  }

  public GroovyRatpackWorkflowServerSpec registryOf(@DelegatesTo(value = RegistrySpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> action) throws Exception {
    return from(GroovyRatpackWorkflowServerSpec.super.registryOf(ClosureUtil.delegatingAction(RegistrySpec.class, action)));
  }
}