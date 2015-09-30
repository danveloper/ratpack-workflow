package com.danveloper.ratpack.workflow;

public interface FlowPreStartInterceptor {

  FlowStatus intercept(MutableFlowStatus flowStatus);
}
