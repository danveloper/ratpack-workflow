package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.danveloper.ratpack.workflow.server.WorkChainConfig;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.registry.Registry;
import ratpack.server.StartEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultWorkProcessor implements WorkProcessor {
  private Work[] works;
  private Registry registry;
  private WorkChainConfig config;
  private WorkStatusRepository workStatusRepository;
  private FlowStatusRepository flowStatusRepository;

  @Override
  public void onStart(StartEvent event) throws Exception {
    registry = event.getRegistry();
    config = registry.get(WorkChainConfig.class);
    workStatusRepository = config.getWorkStatusRepository();
    flowStatusRepository = config.getFlowStatusRepositoryFunction().apply(config.getWorkStatusRepository());
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
    status.setState(WorkState.RUNNING);
    status.setStartTime(System.currentTimeMillis());
    return flowStatusRepository.save(status).flatMap(st -> start(st.getWorks().get(0))).map(s -> status.getId());
  }

  @Override
  public Promise<String> start(WorkStatus workStatus) {
    try {
      MutableWorkStatus mstatus = workStatus.toMutable();
      mstatus.setStartTime(System.currentTimeMillis());
      mstatus.setState(WorkState.RUNNING);
      return workStatusRepository.save(mstatus).flatMap(l ->
        DefaultWorkContext.start(works, workStatus, config.getWorkStatusRepository(), registry)
      );
    } catch (Exception e) {
      return Promise.of(f -> f.error(e));
    }
  }

  private class FlowSupervisor implements Runnable {

    public void run() {
      Execution.fork().start(e -> {
        flowStatusRepository.listRunning().then(flows -> {
          for (FlowStatus flow : flows) {
            boolean workFailed = flow.getWorks().stream().anyMatch(st -> st.getState() == WorkState.FAILED);
            if (workFailed) {
              failFlow(flow);
            } else {
              Optional<WorkStatus> workOption = flow.getWorks()
                  .stream().filter(workStatus -> workStatus.getState() == WorkState.NOT_STARTED).findFirst();
              if (workOption.isPresent()) {
                WorkStatus workStatus = workOption.get();
                start(workStatus).operation().then();
              } else {
                completeFlow(flow);
              }
            }
          }
        });
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
}
