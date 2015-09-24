package com.danveloper.ratpack.workflow;

import com.danveloper.ratpack.workflow.internal.DefaultWorkChain;
import ratpack.func.Action;
import ratpack.registry.Registry;

import java.util.List;

public interface WorkChain {
  default WorkChain all(Work work) {
    return work("", work::handle);
  }

  default WorkChain work(String type, Work work) {
    return work(type, "", work);
  }

  WorkChain work(String type, String version, Work work);
  WorkChain work(String type, String version, Class<? extends Work> work);

  default WorkChain flow(String type, Action<WorkChain> subchain) throws Exception {
    return flow(type, "", subchain);
  }

  WorkChain flow(String type, String version, Action<WorkChain> subchain) throws Exception;

  List<Work> getWorks();

  static WorkChain of(Action<WorkChain> configurer) throws Exception {
    return of(Registry.empty(), configurer);
  }

  static WorkChain of(Registry registry, Action<WorkChain> configurer) throws Exception {
    DefaultWorkChain chain = new DefaultWorkChain(registry);
    configurer.execute(chain);
    return chain;
  }
}
