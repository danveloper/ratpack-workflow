package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.*;
import com.danveloper.ratpack.workflow.internal.DefaultWorkProcessor;
import com.danveloper.ratpack.workflow.internal.FlowProgressingWorkCompletionHandler;
import ratpack.func.Action;
import ratpack.registry.Registry;
import ratpack.server.RatpackServerSpec;
import ratpack.server.internal.DelegatingRatpackServerSpec;

public class RatpackWorkflowServerSpec extends DelegatingRatpackServerSpec {
  private WorkChainConfig workChainConfig = new WorkChainConfig();

  public RatpackWorkflowServerSpec(RatpackServerSpec delegate) {
    super(delegate);
  }

  public Registry getRegistryDefaults() throws Exception {
    final FlowStatusRepository flowRepo = workChainConfig.getFlowStatusRepositoryFunction().apply(workChainConfig.workStatusRepository);
    return Registry.of(r -> r
        .add(WorkStatusRepository.class, workChainConfig.workStatusRepository)
        .add(FlowStatusRepository.class, flowRepo)
        .add(WorkChainConfig.class, workChainConfig)
        .add(WorkProcessor.class, new DefaultWorkProcessor())
        .add(WorkCompletionHandler.class, new FlowProgressingWorkCompletionHandler())
    );
  }

  public RatpackWorkflowServerSpec workflow(Action<WorkChain> action) throws Exception {
    workChainConfig.action = action;
    return this;
  }

}
