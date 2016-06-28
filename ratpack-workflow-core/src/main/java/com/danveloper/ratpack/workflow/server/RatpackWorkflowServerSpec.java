package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.WorkChain;
import com.danveloper.ratpack.workflow.WorkProcessor;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.danveloper.ratpack.workflow.internal.DefaultWorkProcessor;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.registry.Registry;
import ratpack.server.RatpackServerSpec;
import ratpack.server.internal.DelegatingRatpackServerSpec;
import ratpack.util.Exceptions;

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
    );
  }

  public RatpackWorkflowServerSpec workflow(Action<WorkChain> action) throws Exception {
    workChainConfig.action = action;
    return this;
  }

}
