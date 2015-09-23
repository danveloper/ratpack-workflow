package com.danveloper.ratpack.workflow.handlers;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.HashMap;

import static ratpack.jackson.Jackson.json;

public class FlowStatusGetHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    FlowStatusRepository flowStatusRepository = ctx.get(FlowStatusRepository.class);

    String id = ctx.getPathTokens().get("id");
    if (id == null) {
      ctx.getResponse().status(400);
      ctx.render(json(new HashMap<String, String>() {{
        put("status", "400");
        put("message", "bad request. An id is required.");
      }}));
    } else {
      flowStatusRepository.get(id).then(flowStatus -> ctx.render(json(flowStatus)));
    }
  }
}
