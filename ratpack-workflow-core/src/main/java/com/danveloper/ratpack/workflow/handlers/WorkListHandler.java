package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.Page;
import com.danveloper.ratpack.workflow.WorkStatus;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Map;

import static ratpack.jackson.Jackson.json;

public class WorkListHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    Map<String, String> qps = ctx.getRequest().getQueryParams();
    Integer offset = qps.containsKey("offset") ? Integer.valueOf(qps.get("offset")) : 0;
    Integer limit = qps.containsKey("limit") ? Integer.valueOf(qps.get("limit")) : 10;
    Boolean running = qps.containsKey("running") ? Boolean.valueOf(qps.get("running")) : false;

    WorkStatusRepository workStatusRepository = ctx.get(WorkStatusRepository.class);

    Promise<Page<WorkStatus>> pagePromise = running ? workStatusRepository.listRunning(offset, limit) :
        workStatusRepository.list(offset, limit);
    pagePromise.then(page -> ctx.render(json(page)));
  }
}
