package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.handlers.*;
import com.google.common.collect.Maps;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import ratpack.func.Action;
import ratpack.impose.Impositions;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.internal.DefaultRatpackServer;
import ratpack.server.internal.ServerCapturer;
import ratpack.util.Exceptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public interface RatpackWorkflow {

  class RegistryHolder {
    public static Registry registry;
  }

  static RatpackServer of(Action<? super RatpackWorkflowServerSpec> definition) throws Exception {
    Action<RatpackServerSpec> definitionAction = d -> {
      RatpackWorkflowServerSpec spec = new RatpackWorkflowServerSpec(d);
      definition.execute(spec);
    };

    RatpackServer s1 = new DefaultRatpackServer(definitionAction, Impositions.current());

    ServerCapturer.capture(s1);
    return s1;
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
