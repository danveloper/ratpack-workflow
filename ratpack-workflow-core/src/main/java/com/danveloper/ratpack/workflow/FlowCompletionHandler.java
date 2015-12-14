package com.danveloper.ratpack.workflow;

import ratpack.exec.Operation;
import ratpack.registry.Registry;

public interface FlowCompletionHandler {
  Operation complete(Registry registry, FlowStatus flowStatus);
}
