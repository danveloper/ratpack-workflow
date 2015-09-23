package com.danveloper.ratpack.workflow.guice;

import com.danveloper.ratpack.workflow.*;
import com.danveloper.ratpack.workflow.handlers.*;
import com.danveloper.ratpack.workflow.internal.DefaultWorkChain;
import com.danveloper.ratpack.workflow.internal.DefaultWorkProcessor;
import com.danveloper.ratpack.workflow.internal.InMemoryFlowStatusRepository;
import com.danveloper.ratpack.workflow.internal.InMemoryWorkStatusRepository;
import com.google.inject.Provides;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.Guice;
import ratpack.registry.Registry;

public class WorkflowModule extends ConfigurableModule<WorkflowModule.Config> {
  public static class Config {
    private Factory<WorkChain> chainFactory = () -> null;
    private WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository();
    private FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository);

    public Config chainFactory(Factory<WorkChain> factory) {
      this.chainFactory = factory;
      return this;
    }

    public Config workStatusRepository(WorkStatusRepository repo) {
      this.workStatusRepository = repo;
      return this;
    }

    public Config flowStatusRepository(FlowStatusRepository repo) {
      this.flowStatusRepository = repo;
      return this;
    }
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

  public static Registry registry(Registry base, Action<WorkChain> configurer) throws Exception{
    return Guice.registry(b -> {
      b.module(WorkflowModule.class, config -> {
        WorkChain chain = new DefaultWorkChain(base);
        configurer.execute(chain);
        config.chainFactory(() -> chain);
      });
    }).apply(base);
  }

  @Override
  protected void configure() {
  }

  @Provides
  WorkStatusRepository workStatusRepository(Config config) {
    return config.workStatusRepository;
  }

  @Provides
  FlowStatusRepository flowStatusRepository(Config config) {
    return config.flowStatusRepository;
  }

  @Provides
  WorkProcessor workProcessor(Config config, WorkStatusRepository workStatusRepository, FlowStatusRepository flowStatusRepository) throws Exception {
    WorkChain workChain = config.chainFactory.create();
    Work[] works = workChain.getWorks().toArray(new Work[workChain.getWorks().size()]);
    return new DefaultWorkProcessor(works, workStatusRepository, flowStatusRepository);
  }
}
