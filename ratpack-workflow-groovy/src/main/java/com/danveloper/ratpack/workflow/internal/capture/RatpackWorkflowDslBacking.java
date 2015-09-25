package com.danveloper.ratpack.workflow.internal.capture;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow;
import groovy.lang.Closure;
import ratpack.func.Function;

public class RatpackWorkflowDslBacking implements GroovyRatpackWorkflow.Ratpack {
  private RatpackWorkflowDslClosures closures;

  public RatpackWorkflowDslBacking(RatpackWorkflowDslClosures closures) {
    this.closures = closures;
  }

  @Override
  public void bindings(Closure<?> configurer) {
    closures.setBindings(configurer);
  }

  @Override
  public void handlers(Closure<?> configurer) {
    closures.setHandlers(configurer);
  }

  @Override
  public void serverConfig(Closure<?> configurer) {
    closures.setServerConfig(configurer);
  }

  @Override
  public void workflow(Closure<?> configurer) {
    closures.setWorkflows(configurer);
  }

  @Override
  public void workRepo(WorkStatusRepository workRepo) {
    closures.setWorkRepo(workRepo);
  }

  @Override
  public void flowRepo(Function<WorkStatusRepository, FlowStatusRepository> flowRepoFunction) {
    closures.setFlowRepoFunction(flowRepoFunction);
  }
}
