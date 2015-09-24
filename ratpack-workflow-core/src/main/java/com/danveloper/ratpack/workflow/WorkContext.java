package com.danveloper.ratpack.workflow;

import ratpack.exec.Execution;
import ratpack.registry.Registry;

public interface WorkContext extends Registry {
  WorkStatus getStatus();
  Long getStartTime();
  Long getEndTime();

  WorkConfigSource getConfig();
  void message(String message);

  void insert(Registry registry, Work...works);
  void insert(Work...works);
  void next();
  void next(Registry registry);
  void retry();
  void retry(Registry registry);
  void fail(Throwable t);
  void complete();
  Execution getExecution();
}
