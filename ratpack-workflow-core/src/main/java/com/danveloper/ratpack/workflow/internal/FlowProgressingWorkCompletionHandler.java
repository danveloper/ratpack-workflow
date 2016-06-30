package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ratpack.api.Nullable;
import ratpack.exec.Operation;
import ratpack.registry.Registry;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.internal.DefaultEvent;
import ratpack.stream.Streams;
import ratpack.util.Exceptions;

import java.util.List;
import java.util.Optional;

public class FlowProgressingWorkCompletionHandler implements WorkCompletionHandler, Service {
  private boolean initialized;

  private Registry registry;
  private FlowStatusRepository flowStatusRepository;
  private List<FlowCompletionHandler> flowCompletionHandlers;
  private WorkProcessor workProcessor;

  @Override
  public void onStart(StartEvent event) throws Exception {
    registry = event.getRegistry();
    flowCompletionHandlers = Lists.newArrayList(Sets.newLinkedHashSet(registry.getAll(FlowCompletionHandler.class)));
    workProcessor = registry.get(WorkProcessor.class);
    flowStatusRepository = registry.get(FlowStatusRepository.class);
  }

  @Override
  public Operation complete(Registry registry, @Nullable WorkStatus workStatus) {
    if (!initialized) {
      Exceptions.uncheck(() -> onStart(new DefaultEvent(registry, false)));
    }
    initialized = true;

    Optional<FlowStatus> flowStatusOption = registry.maybeGet(FlowStatus.class);
    Operation retVal = Operation.noop();

    if (flowStatusOption.isPresent()) {
      FlowStatus f = flowStatusOption.get();
      retVal = flowStatusRepository.get(f.getId()).next(flowStatus -> {
        if (workStatus.getState() == WorkState.COMPLETED) {
          Optional<WorkStatus> workOption = flowStatus.getWorks()
              .stream().filter(ws -> ws.getState() == WorkState.NOT_STARTED).findFirst();
          if (workOption.isPresent()) {
            WorkStatus nextWorkStatus = workOption.get();
            workProcessor.start(nextWorkStatus, registry).operation().then();
          } else {
            completeFlow(flowStatus);
          }
        } else if (workStatus.getState() == WorkState.FAILED) {
          failFlow(flowStatus);
        }
      }).operation();
    }

    return retVal;
  }

  private void failFlow(FlowStatus flow) {
    endFlow(flow, WorkState.FAILED);
    Optional<FlowErrorHandler> o = registry.maybeGet(FlowErrorHandler.class);
    if (o.isPresent()) {
      FlowErrorHandler errorHandler = o.get();
      errorHandler.error(registry, flow);
    }
  }

  private void completeFlow(FlowStatus flow) {
    endFlow(flow, WorkState.COMPLETED);
  }

  private void endFlow(FlowStatus flow, WorkState state) {
    MutableFlowStatus mflow = flow.toMutable();
    mflow.setEndTime(System.currentTimeMillis());
    mflow.setState(state);
    flowStatusRepository.save(mflow).operation().then();
    Streams.publish(flowCompletionHandlers).flatMap(h -> h.complete(registry, flow.toImmutable()).promise()).toList()
        .operation().then();
  }
}
