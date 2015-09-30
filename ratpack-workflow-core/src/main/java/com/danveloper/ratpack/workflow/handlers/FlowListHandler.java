package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.FlowStatus;
import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.Page;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Map;

import static ratpack.jackson.Jackson.json;

public class FlowListHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    Map<String, String> qps = ctx.getRequest().getQueryParams();
    Integer offset = qps.containsKey("offset") ? Integer.valueOf(qps.get("offset")) : 0;
    Integer limit = qps.containsKey("limit") ? Integer.valueOf(qps.get("limit")) : 10;
    Boolean running = qps.containsKey("running") ? Boolean.valueOf(qps.get("running")) : false;

    FlowStatusRepository flowStatusRepository = ctx.get(FlowStatusRepository.class);

    Promise<Page<FlowStatus>> pagePromise = running ? flowStatusRepository.listRunning(offset, limit) :
        flowStatusRepository.list(offset, limit);
    pagePromise.then(page -> {
      ctx.render(json(page));
    });
  }
}
