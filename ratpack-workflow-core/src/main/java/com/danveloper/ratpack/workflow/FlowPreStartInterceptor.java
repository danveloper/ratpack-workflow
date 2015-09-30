package com.danveloper.ratpack.workflow;

import ratpack.exec.Promise;

public interface FlowPreStartInterceptor {

  Promise<FlowStatus> intercept(MutableFlowStatus flowStatus);
}
