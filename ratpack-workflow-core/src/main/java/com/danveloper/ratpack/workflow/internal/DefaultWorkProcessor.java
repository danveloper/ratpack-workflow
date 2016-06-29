package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.danveloper.ratpack.workflow.server.WorkChainConfig;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import ratpack.exec.Promise;
import ratpack.registry.Registry;
import ratpack.service.StartEvent;

import java.util.List;
import java.util.Optional;

public class DefaultWorkProcessor implements WorkProcessor {
  private Work[] works;
  private Registry registry;
  private WorkStatusRepository workStatusRepository;
  private FlowStatusRepository flowStatusRepository;
  private List<FlowPreStartInterceptor> flowPreStartInterceptors;

  @Override
  public void onStart(StartEvent event) throws Exception {
    registry = event.getRegistry();

    boolean alreadyApplied = Iterables.any(registry.getAll(WorkCompletionHandler.class),
        h -> h.getClass().isAssignableFrom(FlowProgressingWorkCompletionHandler.class));

    if (!alreadyApplied) {
      registry = registry.join(Registry.of(r -> r.add(WorkCompletionHandler.class,
          new FlowProgressingWorkCompletionHandler())));
    }
    flowPreStartInterceptors = Lists.newArrayList(registry.getAll(FlowPreStartInterceptor.class));
    flowPreStartInterceptors.add(new StatusFlowInterceptor());
    WorkChainConfig config = registry.get(WorkChainConfig.class);
    workStatusRepository = registry.get(WorkStatusRepository.class);
    flowStatusRepository = registry.get(FlowStatusRepository.class);
    WorkChain chain = config.getWorkChainFunction().apply(registry);
    config.getAction().execute(chain);
    List<WorkChainDecorator> workChainDecorators = Lists.newArrayList(registry.getAll(WorkChainDecorator.class));
    workChainDecorators.forEach(decorator -> decorator.decorate(chain));
    works = chain.getWorks().toArray(new Work[chain.getWorks().size()]);
  }

  @Override
  public Promise<String> start(FlowStatus flowStatus) {
    if (flowStatus.getState() != WorkState.NOT_STARTED) {
      throw new IllegalStateException("Trying to start an already-started flow");
    }
    if (flowStatus.getWorks().size() == 0) {
      throw new IllegalArgumentException("No work found for flow");
    }

    MutableFlowStatus status = flowStatus.toMutable();
    Promise<FlowStatus> p = null;
    for (FlowPreStartInterceptor interceptor : flowPreStartInterceptors) {
      if (p == null) {
        p = interceptor.intercept(status);
      } else {
        p = p.flatMap(s1 -> interceptor.intercept(s1.toMutable()));
      }
    }
    return p.flatMap(flowStatusRepository::save).flatMap(st ->
        start(st.getWorks().get(0), registry.join(Registry.single(FlowStatus.class, status)))).map(s -> status.getId());
  }

  @Override
  public Promise<String> start(WorkStatus workStatus) {
    return start(workStatus, registry);
  }

  @Override
  public Promise<String> start(WorkStatus workStatus, Registry registry) {
    try {
      MutableWorkStatus mstatus = workStatus.toMutable();
      mstatus.setStartTime(System.currentTimeMillis());
      mstatus.setState(WorkState.RUNNING);
      return workStatusRepository.save(mstatus).flatMap(l ->
        DefaultWorkContext.start(works, workStatus, registry.get(WorkStatusRepository.class), registry)
      );
    } catch (Exception e) {
      return Promise.async(f -> f.error(e));
    }
  }

  private static class StatusFlowInterceptor implements FlowPreStartInterceptor {
    @Override
    public Promise<FlowStatus> intercept(MutableFlowStatus status) {
      status.setState(WorkState.RUNNING);
      status.setStartTime(System.currentTimeMillis());
      return Promise.value(status);
    }
  }
}
