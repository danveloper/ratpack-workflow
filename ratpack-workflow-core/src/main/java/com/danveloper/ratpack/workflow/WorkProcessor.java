package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;
import ratpack.registry.Registry;
import ratpack.service.Service;

public interface WorkProcessor extends Service {
  Promise<String> start(FlowStatus flowStatus);
  Promise<String> start(WorkStatus workStatus);
  Promise<String> start(WorkStatus workStatus, Registry registry);
}
