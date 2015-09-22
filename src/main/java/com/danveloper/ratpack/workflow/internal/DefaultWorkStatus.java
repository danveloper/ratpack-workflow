package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.danveloper.ratpack.workflow.WorkState;
import com.danveloper.ratpack.workflow.WorkStatus;

import java.util.List;

public class DefaultWorkStatus implements WorkStatus {
  private String id;
  private WorkConfigSource config;
  private Long startTime;
  private Long endTime;
  private WorkState state;
  private Throwable error;
  private List<WorkStatusMessage> messages;

  void setId(String id) {
    this.id = id;
  }

  void setConfig(WorkConfigSource config) {
    this.config = config;
  }

  void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  void setState(WorkState state) {
    this.state = state;
  }

  void setError(Throwable error) {
    this.error = error;
  }

  void setMessages(List<WorkStatusMessage> messages) {
    this.messages = messages;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public WorkConfigSource getConfig() {
    return this.config;
  }

  @Override
  public Long getStartTime() {
    return this.startTime;
  }

  @Override
  public Long getEndTime() {
    return this.endTime;
  }

  @Override
  public WorkState getState() {
    return this.state;
  }

  @Override
  public Throwable getError() {
    return this.error;
  }

  @Override
  public List<WorkStatusMessage> getMessages() {
    return this.messages;
  }
}
