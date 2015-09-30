package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.danveloper.ratpack.workflow.server.WorkChainConfig;
import com.google.common.collect.Lists;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.registry.Registry;
import ratpack.server.StartEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultWorkProcessor implements WorkProcessor {
  private Work[] works;
  private Registry registry;
  private WorkChainConfig config;
  private WorkStatusRepository workStatusRepository;
  private FlowStatusRepository flowStatusRepository;
  private List<FlowPreStartInterceptor> flowPreStartInterceptors;

  @Override
  public void onStart(StartEvent event) throws Exception {
    registry = event.getRegistry();
    flowPreStartInterceptors = Lists.newArrayList(registry.getAll(FlowPreStartInterceptor.class));
    flowPreStartInterceptors.add(new StatusFlowInterceptor());
    config = registry.get(WorkChainConfig.class);
    workStatusRepository = registry.get(WorkStatusRepository.class);
    flowStatusRepository = registry.get(FlowStatusRepository.class);
    WorkChain chain = config.getWorkChainFunction().apply(registry);
    config.getAction().execute(chain);
    this.works = chain.getWorks().toArray(new Work[chain.getWorks().size()]);
    ExecController.require().getExecutor().scheduleAtFixedRate(new FlowSupervisor(), 0, 1, TimeUnit.SECONDS);
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
    return p.flatMap(flowStatusRepository::save).flatMap(st -> start(st.getWorks().get(0))).map(s -> status.getId());
  }

  @Override
  public Promise<String> start(WorkStatus workStatus) {
    try {
      MutableWorkStatus mstatus = workStatus.toMutable();
      mstatus.setStartTime(System.currentTimeMillis());
      mstatus.setState(WorkState.RUNNING);
      return workStatusRepository.save(mstatus).flatMap(l ->
        DefaultWorkContext.start(works, workStatus, registry.get(WorkStatusRepository.class), registry)
      );
    } catch (Exception e) {
      return Promise.of(f -> f.error(e));
    }
  }

  private class FlowSupervisor implements Runnable {

    public void run() {
      Execution.fork().start(e ->
        loadFlows(0, 10)
      );
    }

    private void loadFlows(Integer offset, Integer limit) {
      flowStatusRepository.listRunning(offset, limit).then(page -> {
        List<FlowStatus> flows = page.getObjs();
        for (FlowStatus flow : flows) {
          boolean workFailed = flow.getWorks().stream().anyMatch(st -> st.getState() == WorkState.FAILED);
          if (workFailed) {
            failFlow(flow);
          } else {
            Optional<WorkStatus> workOption = flow.getWorks()
                .stream().filter(workStatus -> workStatus.getState() == WorkState.NOT_STARTED).findFirst();
            if (workOption.isPresent()) {
              WorkStatus workStatus = workOption.get();
              workStatusRepository.lock(workStatus.getId()).then(locked -> {
                if (locked) {
                  start(workStatus).flatMap(l -> workStatusRepository.unlock(workStatus.getId())).operation().then();
                }
              });
            } else {
              Optional<WorkStatus> workOption2 = flow.getWorks()
                  .stream().filter(workStatus -> workStatus.getState() == WorkState.RUNNING).findFirst();
              boolean stillRunning = workOption2.isPresent();
              if (!stillRunning) {
                completeFlow(flow);
              }
            }
          }
        }
        if (page.getNumPages() > offset) {
          loadFlows(offset+1, limit);
        }
      });
    }

    private void failFlow(FlowStatus flow) {
      endFlow(flow, WorkState.FAILED);
    }

    private void completeFlow(FlowStatus flow) {
      endFlow(flow, WorkState.COMPLETED);
    }

    private void endFlow(FlowStatus flow, WorkState state) {
      MutableFlowStatus mflow = flow.toMutable();
      mflow.setEndTime(System.currentTimeMillis());
      mflow.setState(state);
      flowStatusRepository.save(mflow).operation().then();
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
