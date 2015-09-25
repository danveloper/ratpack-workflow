package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.danveloper.ratpack.workflow.WorkProcessor;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.HashMap;

import static ratpack.jackson.Jackson.json;

public class WorkSubmissionHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    WorkProcessor workProcessor = ctx.get(WorkProcessor.class);
    WorkStatusRepository workStatusRepository = ctx.get(WorkStatusRepository.class);
    ctx.getRequest().getBody().flatMap(body -> {
      String json = body.getText();
      ConfigData configData = ConfigData.of(d -> d
              .json(ByteSource.wrap(json.getBytes()))
              .build()
      );
      return workStatusRepository.create(WorkConfigSource.of(configData));
    }).flatMap(workProcessor::start)
      .then(id -> {
        ctx.getResponse().status(202);
        ctx.render(json(new HashMap<String, String>() {{
          put("id", id);
        }}));
      });
  }
}
