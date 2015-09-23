package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.WorkStatusRepository;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import static ratpack.jackson.Jackson.json;

public class WorkListHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    WorkStatusRepository workStatusRepository = ctx.get(WorkStatusRepository.class);
    workStatusRepository.list().then(works -> ctx.render(json(works)));
  }
}
