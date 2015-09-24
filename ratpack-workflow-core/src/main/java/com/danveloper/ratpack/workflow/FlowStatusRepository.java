package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;

import java.util.List;

public interface FlowStatusRepository {
  Promise<FlowStatus> create(FlowConfigSource config);
  Promise<FlowStatus> save(FlowStatus status);
  Promise<List<FlowStatus>> list();
  Promise<FlowStatus> get(String id);
  Promise<List<FlowStatus>> listRunning();
}
