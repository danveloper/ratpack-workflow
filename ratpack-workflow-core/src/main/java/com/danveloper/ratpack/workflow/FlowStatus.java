package com.danveloper.ratpack.workflow;

import com.danveloper.ratpack.workflow.internal.DefaultFlowStatus;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class", defaultImpl = DefaultFlowStatus.class)
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

  default ImmutableFlowStatus toImmutable() {
    return new ImmutableFlowStatus(this);
  }

  static FlowStatus of(String id, FlowConfigSource config) {
    return DefaultFlowStatus.of(id, config);
  }

  static FlowStatus of(FlowConfigSource config) {
    return DefaultFlowStatus.of(config);
  }

  class ImmutableFlowStatus implements FlowStatus {
    private final FlowStatus delegate;

    private ImmutableFlowStatus(FlowStatus delegate) {
      this.delegate = delegate;
    }

    @Override
    public String getId() {
      return delegate.getId();
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public String getDescription() {
      return delegate.getDescription();
    }

    @Override
    public WorkState getState() {
      return delegate.getState();
    }

    @Override
    public Long getStartTime() {
      return delegate.getStartTime();
    }

    @Override
    public Long getEndTime() {
      return delegate.getEndTime();
    }

    @Override
    public Map<String, String> getTags() {
      return delegate.getTags();
    }

    @Override
    public List<WorkStatus> getWorks() {
      return delegate.getWorks();
    }

    public MutableFlowStatus toMutable() {
      throw new IllegalStateException("FlowStatus is immutable.");
    }
  }
}
