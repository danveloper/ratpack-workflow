package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import static ratpack.jackson.Jackson.json;

public class FlowListHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    FlowStatusRepository flowStatusRepository = ctx.get(FlowStatusRepository.class);
    flowStatusRepository.list().then(flows ->
      ctx.render(json(flows))
    );
  }
}
