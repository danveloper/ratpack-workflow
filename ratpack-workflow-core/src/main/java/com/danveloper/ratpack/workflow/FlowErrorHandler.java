package com.danveloper.ratpack.workflow;

import ratpack.registry.Registry;

public interface FlowErrorHandler {
  void error(Registry registry, FlowStatus status);
}
