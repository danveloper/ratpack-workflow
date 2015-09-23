package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.FlowConfigSource;
import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.WorkProcessor;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.HashMap;
import java.util.Map;

import static ratpack.jackson.Jackson.json;

public class FlowSubmissionHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    FlowStatusRepository flowStatusRepository = ctx.get(FlowStatusRepository.class);
    WorkProcessor workProcessor = ctx.get(WorkProcessor.class);

    ctx.getRequest().getBody().flatMap(body -> {
      String json = body.getText();
      ConfigData configData = ConfigData.of(d -> d
        .json(ByteSource.wrap(json.getBytes()))
        .build()
      );
      return flowStatusRepository.create(FlowConfigSource.of(configData));
    }).flatMap(flowStatus -> workProcessor.start(flowStatus))
      .then(id -> {
        ctx.getResponse().status(202);
        Map<String, String> resp = Maps.newHashMap();
        resp.put("id", id);
        ctx.render(json(resp));
      });
  }
}
