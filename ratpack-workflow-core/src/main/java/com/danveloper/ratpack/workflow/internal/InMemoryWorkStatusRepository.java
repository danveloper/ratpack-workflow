package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
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
  private final Map<String, Boolean> locks = Maps.newConcurrentMap();

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
  public Promise<Page<WorkStatus>> list(Integer offset, Integer limit) {
    Integer startIdx = limit * offset;
    Integer endIdx = limit + startIdx;
    List<WorkStatus> values = Lists.newArrayList(storage.values()).subList(startIdx, endIdx > storage.size() ? storage.size() : endIdx);
    return Promise.value(new Page<>(offset, limit, (int)Math.ceil(storage.size() / limit), values));
  }

  @Override
  public Promise<Page<WorkStatus>> listRunning(Integer offset, Integer limit) {
    Integer startIdx = limit * offset;
    Integer endIdx = limit + startIdx;
    List<WorkStatus> works = storage.values().stream()
        .filter(st -> st.getState() == WorkState.RUNNING).collect(Collectors.toList());
    List<WorkStatus> values = works.subList(startIdx, endIdx > works.size() ? works.size() : endIdx);
    return Promise.value(new Page<>(offset, limit, (int)Math.ceil(works.size() / limit), values));
  }

  @Override
  public Promise<WorkStatus> get(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    return Promise.value(storage.get(id));
  }

  @Override
  public Promise<Boolean> lock(String id) {
    if (locks.containsKey(id)) {
      return Promise.value(Boolean.FALSE);
    } else {
      locks.put(id, Boolean.TRUE);
      return Promise.value(Boolean.TRUE);
    }
  }

  @Override
  public Promise<Boolean> unlock(String id) {
    if (locks.containsKey(id)) {
      locks.remove(id);
    }
    return Promise.value(Boolean.TRUE);
  }
}
