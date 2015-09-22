package com.danveloper.ratpack.workflow;

import com.danveloper.ratpack.workflow.internal.WorkChainWork;

public interface Works {
  static Work all(Work work) {
    return work::handle;
  }

  static Work chain(Work... works) {
    if (works.length == 0) {
      return WorkContext::next;
    } else if (works.length == 1) {
      return works[0];
    } else {
      return new WorkChainWork(works);
    }
  }
}
