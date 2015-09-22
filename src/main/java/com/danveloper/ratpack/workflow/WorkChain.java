package com.danveloper.ratpack.workflow;

import ratpack.func.Action;

public interface WorkChain {
  default WorkChain all(Work work) {
    return work("", Works.all(work));
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
}
