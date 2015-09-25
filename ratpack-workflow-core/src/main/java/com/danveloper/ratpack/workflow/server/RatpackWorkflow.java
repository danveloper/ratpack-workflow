package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.handlers.*;
import ratpack.func.Action;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.internal.ServerCapturer;

public interface RatpackWorkflow {

  class RegistryHolder {
    Registry overrides;
  }

  static RatpackServer of(Action<? super RatpackWorkflowServerSpec> definition) throws Exception {
    final RegistryHolder holder = new RegistryHolder();
    RatpackServer server =  RatpackServer.of(d -> {
      RatpackWorkflowServerSpec spec = new RatpackWorkflowServerSpec((RatpackServerSpec)d);
      definition.execute(spec);
      holder.overrides = spec.getRegistry();
    });
    ServerCapturer.capture(server).registry(r -> r.join(holder.overrides));
    return server;
  }

  static RatpackServer start(Action<? super RatpackWorkflowServerSpec> definition) throws Exception {
    RatpackServer server = of(definition);
    server.start();
    return server;
  }

  static FlowListHandler flowListHandler() {
    return new FlowListHandler();
  }

  static FlowStatusGetHandler flowStatusGetHandler() {
    return new FlowStatusGetHandler();
  }

  static FlowSubmissionHandler flowSubmissionHandler() {
    return new FlowSubmissionHandler();
  }

  static WorkListHandler workListHandler() {
    return new WorkListHandler();
  }

  static WorkStatusGetHandler workStatusGetHandler() {
    return new WorkStatusGetHandler();
  }

  static WorkSubmissionHandler workSubmissionHandler() {
    return new WorkSubmissionHandler();
  }
}
