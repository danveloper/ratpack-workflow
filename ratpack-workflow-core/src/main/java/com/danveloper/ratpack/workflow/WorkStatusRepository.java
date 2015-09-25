package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;

import java.util.List;

public interface WorkStatusRepository {

  Promise<WorkStatus> create(WorkConfigSource source);

  Promise<WorkStatus> save(WorkStatus status);

  Promise<List<WorkStatus>> list();

  Promise<List<WorkStatus>> listRunning();

  Promise<WorkStatus> get(String id);

  Promise<Boolean> lock(String id);

  Promise<Boolean> unlock(String id);
}
