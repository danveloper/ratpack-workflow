package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ratpack.exec.Promise;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static ratpack.rx.RxRatpack.observe;
import static ratpack.rx.RxRatpack.promiseSingle;

public class InMemoryFlowStatusRepository implements FlowStatusRepository {
  private final Map<String, FlowStatus> storage = Maps.newConcurrentMap();

  private final WorkStatusRepository workStatusRepository;

  public InMemoryFlowStatusRepository(WorkStatusRepository workStatusRepository) {
    this.workStatusRepository = workStatusRepository;
  }

  @Override
  public Promise<FlowStatus> create(FlowConfigSource config) {
    Observable<DefaultFlowStatus> statusObs = Observable.just(new DefaultFlowStatus()).map(status -> {
      status.setId(new UUID(new Random().nextLong(), new Random().nextLong()).toString());
      status.setName(config.getName());
      status.setDescription(config.getDescription());
      status.setTags(config.getTags());
      status.setState(WorkState.NOT_STARTED);
      return status;
    });

    Observable<List<WorkStatus>> workStatusesObs = Observable.from(config.getWorks())
        .flatMap(workConfig -> observe(workStatusRepository.create(workConfig))).toList();

    Observable<DefaultFlowStatus> zippedStatus = Observable.zip(statusObs, workStatusesObs, (status, workStatuses) -> {
      status.setWorks(workStatuses);
      storage.put(status.getId(), status);
      return status;
    });

    return promiseSingle(zippedStatus).flatMap(st -> get(st.getId()));
  }

  @Override
  public Promise<FlowStatus> save(FlowStatus status) {
    if (status == null || status.getId() == null) {
      throw new IllegalArgumentException("status cannot be null");
    }
    storage.put(status.getId(), status);
    return get(status.getId());
  }

  @Override
  public Promise<List<FlowStatus>> list() {
    return Promise.value(Lists.newArrayList(storage.values()));
  }

  @Override
  public Promise<FlowStatus> get(String id) {
    return Promise.value(storage.get(id));
  }

  @Override
  public Promise<List<FlowStatus>> listRunning() {
    List<FlowStatus> flows = storage.values().stream()
        .filter(st -> st.getState() == WorkState.RUNNING).collect(Collectors.toList());
    return Promise.value(flows);
  }
}
