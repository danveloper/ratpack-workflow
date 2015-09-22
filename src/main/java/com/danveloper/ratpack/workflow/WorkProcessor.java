package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;

public interface WorkProcessor {
  Promise<String> start(FlowStatus flowStatus);
  Promise<String> start(WorkStatus workStatus);
}
