package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.WorkChain;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.danveloper.ratpack.workflow.internal.DefaultWorkChain;
import com.danveloper.ratpack.workflow.internal.InMemoryFlowStatusRepository;
import com.danveloper.ratpack.workflow.internal.InMemoryWorkStatusRepository;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.registry.Registry;

public class WorkChainConfig {
  Action<WorkChain> action = wc -> {};
  Function<Registry, WorkChain> workChainFunction = DefaultWorkChain::new;
  WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository();
  Function<WorkStatusRepository, FlowStatusRepository> flowStatusRepository = InMemoryFlowStatusRepository::new;

  public Action<WorkChain> getAction() {
    return action;
  }

  public Function<Registry, WorkChain> getWorkChainFunction() {
    return workChainFunction;
  }

  public WorkStatusRepository getWorkStatusRepository() {
    return workStatusRepository;
  }

  public Function<WorkStatusRepository, FlowStatusRepository> getFlowStatusRepositoryFunction() {
    return flowStatusRepository;
  }
}
