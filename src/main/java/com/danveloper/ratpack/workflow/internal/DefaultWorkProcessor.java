package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.server.Service;
import ratpack.server.StartEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultWorkProcessor implements WorkProcessor, Service {
  private final Work[] works;
  private final WorkStatusRepository workStatusRepository;
  private final FlowStatusRepository flowStatusRepository;

  public DefaultWorkProcessor(Work[] works, WorkStatusRepository workStatusRepository, FlowStatusRepository flowStatusRepository) {
    this.works = works;
    this.workStatusRepository = workStatusRepository;
    this.flowStatusRepository = flowStatusRepository;
  }

  @Override
  public void onStart(StartEvent event) {
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

    DefaultFlowStatus status = (DefaultFlowStatus)flowStatus;
    status.setState(WorkState.RUNNING);
    status.setStartTime(System.currentTimeMillis());
    return flowStatusRepository.save(status).flatMap(st -> start(st.getWorks().get(0))).map(s -> status.getId());
  }

  @Override
  public Promise<String> start(WorkStatus workStatus) {
    try {
      return DefaultWorkContext.start(works, workStatus, workStatusRepository);
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
                start(workStatus);
              } else {
                failFlow(flow);
              }
            }
          }
        });
      });
    }

    private void failFlow(FlowStatus flow) {
      DefaultFlowStatus dflow = (DefaultFlowStatus)flow;
      dflow.setEndTime(System.currentTimeMillis());
      dflow.setState(WorkState.FAILED);
      flowStatusRepository.save(dflow).operation().then();
    }
  }
}