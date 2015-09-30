package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import ratpack.exec.Promise;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

import static ratpack.rx.RxRatpack.observe;
import static ratpack.rx.RxRatpack.promiseSingle;

public class InMemoryFlowStatusRepository implements FlowStatusRepository {
  private final Map<String, FlowStatus> storage = Maps.newConcurrentMap();
  private final Map<String, Set<String>> tagStorage = Maps.newConcurrentMap();

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
      status.getTags().forEach((key, val) -> {
        String storageKey = "tags:"+key+":"+val;
        if (!tagStorage.containsKey(storageKey)) {
          tagStorage.put(storageKey, Sets.newConcurrentHashSet());
        }
        tagStorage.get(storageKey).add(status.getId());
      });
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
  public Promise<FlowStatus> get(String id) {
    return Promise.value(storage.get(id));
  }

  @Override
  public Promise<Page<FlowStatus>> list(Integer offset, Integer limit) {
    Integer startIdx = limit * offset;
    Integer endIdx = limit + startIdx;
    List<FlowStatus> flows = Lists.newArrayList(storage.values()).subList(startIdx, endIdx > storage.size() ? storage.size() : endIdx);
    return Promise.value(new Page<>(offset, limit, (int)Math.ceil(storage.size() / limit), flows));
  }

  @Override
  public Promise<Page<FlowStatus>> listRunning(Integer offset, Integer limit) {
    Integer startIdx = limit * offset;
    Integer endIdx = limit + startIdx;
    List<FlowStatus> flows = storage.values().stream()
        .filter(st -> st.getState() == WorkState.RUNNING).collect(Collectors.toList());
    List<FlowStatus> values = flows.subList(startIdx, endIdx > flows.size() ? flows.size() : endIdx);
    return Promise.value(new Page<>(offset, limit, (int)Math.ceil(flows.size() / limit), values));
  }

  @Override
  public Promise<Page<FlowStatus>> findByTag(Integer offset, Integer limit, String key, String value) {
    String storageKey = "tags:"+key+":"+value;
    if (!tagStorage.containsKey(storageKey)) {
      return Promise.value(new Page<>(offset, limit, 0, Lists.newArrayList()));
    } else {
      Integer startIdx = limit * offset;
      Integer endIdx = limit + startIdx;

      Set<String> ids = tagStorage.get(storageKey);
      List<FlowStatus> taggedStatuses = ids.stream().map(storage::get).collect(Collectors.toList());
      List<FlowStatus> values = taggedStatuses.subList(startIdx, endIdx > taggedStatuses.size() ? taggedStatuses.size() : endIdx);
      return Promise.value(new Page<>(offset, limit, (int)Math.ceil(taggedStatuses.size() / limit), values));
    }
  }
}
