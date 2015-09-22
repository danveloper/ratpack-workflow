package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.Work;
import com.danveloper.ratpack.workflow.WorkContext;

public class WorkChainWork implements Work {

  private final Work[] works;

  public WorkChainWork(Work[] works) {
    this.works = works;
  }

  @Override
  public void handle(WorkContext ctx) {
    ctx.insert(works);
  }

  public Work[] getWorks() {
    return this.works;
  }
}
