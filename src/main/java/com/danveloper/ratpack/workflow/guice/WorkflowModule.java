package com.danveloper.ratpack.workflow.guice;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.WorkChain;
import com.danveloper.ratpack.workflow.WorkProcessor;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.danveloper.ratpack.workflow.handlers.*;
import com.danveloper.ratpack.workflow.internal.DefaultWorkProcessor;
import com.danveloper.ratpack.workflow.internal.InMemoryFlowStatusRepository;
import com.danveloper.ratpack.workflow.internal.InMemoryWorkStatusRepository;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import ratpack.func.Action;

public class WorkflowModule extends AbstractModule {
  private final Action<WorkChain> chain;
  private WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository();
  private FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository);

  private WorkflowModule(Action<WorkChain> chain) {
    this.chain = chain;
  }

  public static FlowListHandler flowListHandler() {
    return new FlowListHandler();
  }

  public static FlowStatusGetHandler flowStatusGetHandler() {
    return new FlowStatusGetHandler();
  }

  public static FlowSubmissionHandler flowSubmissionHandler() {
    return new FlowSubmissionHandler();
  }

  public static WorkListHandler workListHandler() {
    return new WorkListHandler();
  }

  public static WorkStatusGetHandler workStatusGetHandler() {
    return new WorkStatusGetHandler();
  }

  public static WorkSubmissionHandler workSubmissionHandler() {
    return new WorkSubmissionHandler();
  }

  public static WorkflowModule of(Action<WorkChain> chain) {
    return new WorkflowModule(chain);
  }

  public WorkflowModule withWorkStatusRepository(WorkStatusRepository workStatusRepository) {
    this.workStatusRepository = workStatusRepository;
    return this;
  }

  public WorkflowModule withFlowStatusRepository(FlowStatusRepository flowStatusRepository) {
    this.flowStatusRepository = flowStatusRepository;
    return this;
  }

  @Override
  protected void configure() {
    bind(WorkStatusRepository.class).toInstance(workStatusRepository);
    bind(FlowStatusRepository.class).toInstance(flowStatusRepository);
    bind(WorkProcessor.class).toInstance(new DefaultWorkProcessor(chain, workStatusRepository, flowStatusRepository));
  }
}
