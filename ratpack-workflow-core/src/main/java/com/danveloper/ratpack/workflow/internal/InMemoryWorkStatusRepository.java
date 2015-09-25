package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.danveloper.ratpack.workflow.WorkState;
import com.danveloper.ratpack.workflow.WorkStatus;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ratpack.exec.Promise;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class InMemoryWorkStatusRepository implements WorkStatusRepository {
  private final Map<String, WorkStatus> storage = Maps.newConcurrentMap();

  @Override
  public Promise<WorkStatus> create(WorkConfigSource source) {
    String id = new UUID(new Random().nextLong(), new Random().nextLong()).toString();

    DefaultWorkStatus status = new DefaultWorkStatus();
    status.setId(id);
    status.setConfig(source);
    status.setState(WorkState.NOT_STARTED);
    status.setMessages(Lists.newArrayList());

    storage.put(id, status);

    return get(id);
  }

  @Override
  public Promise<WorkStatus> save(WorkStatus status) {
    if (status == null || status.getId() == null) {
      throw new IllegalArgumentException("status or status id cannot be null");
    }

    storage.put(status.getId(), status);
    return get(status.getId());
  }

  @Override
  public Promise<List<WorkStatus>> list() {
    return Promise.value(Lists.newArrayList(storage.values()));
  }

  @Override
  public Promise<List<WorkStatus>> listRunning() {
    List<WorkStatus> works = storage.values().stream()
        .filter(st -> st.getState() == WorkState.RUNNING).collect(Collectors.toList());
    return Promise.value(works);
  }

  @Override
  public Promise<WorkStatus> get(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    return Promise.value(storage.get(id));
  }
}
