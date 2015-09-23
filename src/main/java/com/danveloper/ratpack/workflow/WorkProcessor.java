package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;
import ratpack.server.Service;

public interface WorkProcessor extends Service {
  Promise<String> start(FlowStatus flowStatus);
  Promise<String> start(WorkStatus workStatus);
}
