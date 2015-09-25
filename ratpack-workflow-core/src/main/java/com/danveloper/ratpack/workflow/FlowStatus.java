package com.danveloper.ratpack.workflow;

import com.danveloper.ratpack.workflow.internal.DefaultFlowStatus;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface FlowStatus {
  String getId();
  String getName();
  String getDescription();
  WorkState getState();
  Long getStartTime();
  Long getEndTime();
  Map<String, String> getTags();
  List<WorkStatus> getWorks();

  default MutableFlowStatus toMutable() {
    if (this instanceof MutableFlowStatus) {
      return (MutableFlowStatus)this;
    } else {
      throw new IllegalStateException("FlowStatus is not mutable");
    }
  }

  static FlowStatus of(String id, FlowConfigSource config) {
    return DefaultFlowStatus.of(id, config);
  }

  static FlowStatus of(FlowConfigSource config) {
    return DefaultFlowStatus.of(config);
  }
}
