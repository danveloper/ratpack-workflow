package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.*;
import com.danveloper.ratpack.workflow.internal.DefaultWorkProcessor;
import com.danveloper.ratpack.workflow.internal.FlowProgressingWorkCompletionHandler;
import com.danveloper.ratpack.workflow.internal.InMemoryFlowStatusRepository;
import com.danveloper.ratpack.workflow.internal.InMemoryWorkStatusRepository;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.server.internal.DelegatingRatpackServerSpec;
import ratpack.util.Exceptions;

public class RatpackWorkflowServerSpec extends DelegatingRatpackServerSpec {
  private WorkChainConfig workChainConfig = new WorkChainConfig();

  private Registry defaultRegistry = Exceptions.uncheck(() -> Registry.of(r -> {
    WorkStatusRepository workStatus = new InMemoryWorkStatusRepository();
    r.add(WorkStatusRepository.class, workStatus)
        .add(WorkChainConfig.class, workChainConfig)
        .add(WorkProcessor.class, new DefaultWorkProcessor())
        .add(WorkCompletionHandler.class, new FlowProgressingWorkCompletionHandler())
        .add(FlowStatusRepository.class, new InMemoryFlowStatusRepository(workStatus));
    }
  ));

  public RatpackWorkflowServerSpec(RatpackServerSpec delegate) {
    super(delegate);
    delegate.registry(defaultRegistry);
  }

  public RatpackWorkflowServerSpec workflow(Action<WorkChain> action) throws Exception {
    workChainConfig.action = action;
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec registry(Function<? super Registry, ? extends Registry> function) {
    super.registry(r -> function.andThen(defaultRegistry::join).apply(r));
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec serverConfig(ServerConfig serverConfig) {
    super.serverConfig(serverConfig);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec handler(Function<? super Registry, ? extends Handler> handlerFactory) {
    super.handler(handlerFactory);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec registryOf(Action<? super RegistrySpec> action) throws Exception {
    super.registryOf(action);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec registry(Registry registry) {
    super.registry(registry);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec serverConfig(ServerConfigBuilder builder) {
    super.serverConfig(builder);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec serverConfig(Action<? super ServerConfigBuilder> action) throws Exception {
    super.serverConfig(action);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec handler(Class<? extends Handler> handlerType) {
    super.handler(handlerType);
    return this;
  }

  @Override
  public RatpackWorkflowServerSpec handlers(Action<? super Chain> handlers) {
    super.handlers(handlers);
    return this;
  }
}
