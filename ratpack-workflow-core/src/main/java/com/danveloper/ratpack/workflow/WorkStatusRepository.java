package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;

public interface WorkStatusRepository {

  Promise<WorkStatus> create(WorkConfigSource source);

  Promise<WorkStatus> save(WorkStatus status);

  Promise<Page<WorkStatus>> list(Integer offset, Integer limit);

  Promise<Page<WorkStatus>> listRunning(Integer offset, Integer limit);

  Promise<WorkStatus> get(String id);

  Promise<Boolean> lock(String id);

  Promise<Boolean> unlock(String id);
}
