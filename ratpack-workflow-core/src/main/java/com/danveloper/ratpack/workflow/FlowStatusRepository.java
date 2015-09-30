package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;

import java.util.List;

public interface FlowStatusRepository {
  Promise<FlowStatus> create(FlowConfigSource config);
  Promise<FlowStatus> save(FlowStatus status);
  Promise<FlowStatus> get(String id);
  Promise<Page<FlowStatus>> list(Integer offset, Integer limit);
  Promise<Page<FlowStatus>> listRunning(Integer offset, Integer limit);
  Promise<Page<FlowStatus>> findByTag(Integer offset, Integer limit, String key, String value);
}
