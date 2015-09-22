package com.danveloper.ratpack.workflow;

import ratpack.config.ConfigData;
import ratpack.exec.Promise;

public interface WorkProcessor {

  Promise<String> submitSingle(WorkConfigSource configSource);

  Promise<String> submitFlow(ConfigData config);
}
